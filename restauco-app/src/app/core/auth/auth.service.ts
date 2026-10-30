import { computed, inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthorizationResponse } from './oauth.models';
import { OAuthService } from './oauth.service';

/** Silent renewal starts this long before the access token expires. */
const RENEW_BEFORE_EXPIRY_MS = 60_000;

/**
 * Signed-in user (authco). Customers and staff use the same session; staff access to the
 * panel is decided by the restauco API per restaurant.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private router = inject(Router);
  private oauth = inject(OAuthService);

  private expiryTimer: ReturnType<typeof setTimeout> | null = null;
  private redirecting = false;

  readonly token = computed(() => {
    const session = this.oauth.session();
    return session && Date.now() < session.expiresAt ? session.accessToken : null;
  });
  readonly isAuthenticated = computed(() => this.token() !== null);
  readonly displayName = computed(() => this.oauth.session()?.name ?? this.oauth.session()?.email ?? null);

  constructor() {
    // The silent-renewal iframe runs this app too: only the top window keeps the session alive.
    const session = this.oauth.session();
    if (window.parent === window && session && Date.now() < session.expiresAt) {
      this.scheduleRenewal(session.expiresAt);
    }
  }

  login(returnUrl: string = this.router.url): Promise<void> {
    this.redirecting = true;
    return this.oauth.startLogin(returnUrl).catch((error) => {
      this.redirecting = false;
      throw error;
    });
  }

  completeLogin(response: AuthorizationResponse): Observable<string> {
    return this.oauth.completeLogin(response).pipe(
      tap(() => this.scheduleRenewal(this.oauth.session()!.expiresAt))
    );
  }

  /** Clears the local session and ends the authco session (OIDC RP-initiated logout). */
  logout() {
    const idToken = this.oauth.session()?.idToken;
    this.clearSession();
    if (idToken) {
      this.redirecting = true;
      window.location.assign(this.oauth.endSessionUrl(idToken));
      return;
    }
    this.router.navigateByUrl('/');
  }

  /** The API rejected the token and silent renewal is not possible: log in again. */
  restartLogin() {
    if (this.redirecting) return;
    this.clearSession();
    this.login(this.router.url);
  }

  private clearSession() {
    this.clearTimer();
    this.oauth.clearSession();
  }

  /** Renews the session shortly before it expires; if that fails, restarts the login at expiry. */
  private scheduleRenewal(expiresAt: number) {
    this.clearTimer();
    this.expiryTimer = setTimeout(async () => {
      if (await this.oauth.renewSilently()) {
        this.scheduleRenewal(this.oauth.session()!.expiresAt);
      } else if (this.oauth.session()) {
        this.expiryTimer = setTimeout(() => this.restartLogin(), delayUntil(expiresAt));
      }
    }, delayUntil(expiresAt - RENEW_BEFORE_EXPIRY_MS));
  }

  private clearTimer() {
    if (this.expiryTimer !== null) {
      clearTimeout(this.expiryTimer);
      this.expiryTimer = null;
    }
  }
}

// setTimeout overflows above ~24.8 days; access tokens live 15 minutes.
function delayUntil(epochMillis: number): number {
  return Math.min(Math.max(epochMillis - Date.now(), 0), 2_147_483_647);
}
