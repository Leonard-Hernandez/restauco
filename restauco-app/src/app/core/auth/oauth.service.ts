import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { catchError, firstValueFrom, map, Observable, tap, throwError } from 'rxjs';
import { appConfig, postLogoutRedirectUri, redirectUri } from '../config/app-config';
import { codeChallengeS256, randomString } from './oauth-crypto';
import { decodeJwtPayload } from './jwt';
import { AuthorizationResponse, OAuthError, PendingLogin, TokenResponse, TokenSet } from './oauth.models';

const PENDING_LOGIN_KEY = 'restauco.oauth.pending';
const SESSION_KEY = 'restauco.auth.session';
const SILENT_RENEW_TIMEOUT_MS = 10_000;

/**
 * OAuth 2.1 Authorization Code + PKCE (S256) with authco, plus OIDC state/nonce and
 * RP-initiated logout. authco issues no refresh tokens to public clients, so the session
 * is renewed with a `prompt=none` authorization request in a hidden iframe, which only
 * succeeds while the authco session cookie is alive.
 *
 * The session lives in sessionStorage: it dies with the tab and is never shared across tabs.
 */
@Injectable({ providedIn: 'root' })
export class OAuthService {

  private http = inject(HttpClient);

  private _session = signal<TokenSet | null>(readStoredSession());
  readonly session = this._session.asReadonly();

  private renewal: Promise<boolean> | null = null;

  /** Redirects the browser to the authco authorization endpoint. */
  async startLogin(returnUrl: string = '/'): Promise<void> {
    const pending = newPendingLogin(safeReturnUrl(returnUrl));
    sessionStorage.setItem(PENDING_LOGIN_KEY, JSON.stringify(pending));
    window.location.assign(await authorizeUrl(pending));
  }

  /** Validates the authorization response, exchanges the code and stores the session. Emits the return URL. */
  completeLogin(response: AuthorizationResponse): Observable<string> {
    const pending = this.takePendingLogin();
    return this.redeem(response, pending).pipe(
      tap((tokens) => this.storeSession(tokens)),
      map(() => pending!.returnUrl)
    );
  }

  /** Renews the session without user interaction; false when authco needs the user. */
  renewSilently(): Promise<boolean> {
    this.renewal ??= this.runSilentRenew().finally(() => (this.renewal = null));
    return this.renewal;
  }

  /** OIDC RP-initiated logout URL; ends the authco session and comes back to the app. */
  endSessionUrl(idToken: string): string {
    const params = new URLSearchParams({
      id_token_hint: idToken,
      client_id: appConfig().clientId,
      post_logout_redirect_uri: postLogoutRedirectUri()
    });
    return `${appConfig().authUrl}/connect/logout?${params}`;
  }

  clearSession() {
    sessionStorage.removeItem(SESSION_KEY);
    this._session.set(null);
  }

  private storeSession(tokens: TokenSet) {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(tokens));
    this._session.set(tokens);
  }

  private async runSilentRenew(): Promise<boolean> {
    if (!this._session()) {
      return false;
    }
    // Kept in memory: the iframe hands the response back to this window, so the
    // interactive pending login in sessionStorage is never touched.
    const pending = newPendingLogin('/');
    const response = await loadInHiddenIframe(await authorizeUrl(pending, { prompt: 'none' }));
    if (!response) {
      return false;
    }
    try {
      const tokens = await firstValueFrom(this.redeem(response, pending));
      // A logout while the iframe was loading wins over the renewal.
      if (!this._session()) {
        return false;
      }
      this.storeSession(tokens);
      return true;
    } catch {
      return false;
    }
  }

  private redeem(response: AuthorizationResponse, pending: PendingLogin | null): Observable<TokenSet> {
    if (response.error) {
      return throwError(() => new OAuthError(response.error!, response.error_description ?? 'Authorization was denied'));
    }
    if (!pending || !response.code || response.state !== pending.state) {
      return throwError(() => new OAuthError('invalid_state', 'The login response does not match a login started here'));
    }

    const config = appConfig();
    const body = new HttpParams({
      fromObject: {
        grant_type: 'authorization_code',
        code: response.code,
        redirect_uri: redirectUri(),
        client_id: config.clientId,
        code_verifier: pending.codeVerifier,
        resource: config.resource
      }
    });
    const headers = new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' });

    return this.http.post<TokenResponse>(`${config.authUrl}/oauth2/token`, body.toString(), { headers }).pipe(
      catchError((error: HttpErrorResponse) => throwError(() => new OAuthError(
        error.error?.error ?? 'token_request_failed',
        error.error?.error_description ?? 'Could not exchange the authorization code'))),
      map((tokens) => toTokenSet(tokens, pending.nonce))
    );
  }

  private takePendingLogin(): PendingLogin | null {
    const raw = sessionStorage.getItem(PENDING_LOGIN_KEY);
    sessionStorage.removeItem(PENDING_LOGIN_KEY);
    return parseJson<PendingLogin>(raw);
  }
}

/** Only same-app paths: never an absolute or protocol-relative URL (open redirect) nor /auth. */
export function safeReturnUrl(url: string | null | undefined): string {
  return url && /^\/(?![/\\]|auth(?:[/?#]|$))/.test(url) ? url : '/';
}

function newPendingLogin(returnUrl: string): PendingLogin {
  return { state: randomString(32), nonce: randomString(32), codeVerifier: randomString(64), returnUrl };
}

async function authorizeUrl(pending: PendingLogin, extra: Record<string, string> = {}): Promise<string> {
  const config = appConfig();
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: config.clientId,
    redirect_uri: redirectUri(),
    scope: 'openid profile email',
    state: pending.state,
    nonce: pending.nonce,
    code_challenge: await codeChallengeS256(pending.codeVerifier),
    code_challenge_method: 'S256',
    resource: config.resource,
    ...extra
  });
  return `${config.authUrl}/oauth2/authorize?${params}`;
}

/** Loads the authorization URL in a hidden iframe and waits for the callback page to post back. */
function loadInHiddenIframe(url: string): Promise<AuthorizationResponse | null> {
  return new Promise((resolve) => {
    const iframe = document.createElement('iframe');
    iframe.style.display = 'none';
    iframe.setAttribute('aria-hidden', 'true');

    const finish = (response: AuthorizationResponse | null) => {
      clearTimeout(timer);
      window.removeEventListener('message', onMessage);
      iframe.remove();
      resolve(response);
    };
    const onMessage = (event: MessageEvent) => {
      if (event.origin === window.location.origin && event.source === iframe.contentWindow
        && event.data && typeof event.data === 'object') {
        finish(event.data as AuthorizationResponse);
      }
    };
    const timer = setTimeout(() => finish(null), SILENT_RENEW_TIMEOUT_MS);

    window.addEventListener('message', onMessage);
    iframe.src = url;
    document.body.appendChild(iframe);
  });
}

function toTokenSet(response: TokenResponse, nonce: string): TokenSet {
  if (!response.access_token || response.token_type?.toLowerCase() !== 'bearer') {
    throw new OAuthError('invalid_token_response', 'The token response has no bearer access token');
  }
  const idToken = response.id_token ?? null;
  const claims = idToken ? validateIdToken(idToken, nonce) : null;
  return {
    accessToken: response.access_token,
    idToken,
    expiresAt: Date.now() + response.expires_in * 1000,
    name: typeof claims?.['name'] === 'string' ? claims['name'] : null,
    email: typeof claims?.['email'] === 'string' ? claims['email'] : null
  };
}

// The ID token comes straight from the token endpoint over the back channel, so its
// signature need not be checked here (OIDC Core 3.1.3.7); its binding to this login does.
function validateIdToken(idToken: string, nonce: string): Record<string, unknown> {
  const claims = decodeJwtPayload(idToken);
  const aud = claims?.['aud'];
  const audiences = Array.isArray(aud) ? aud : [aud];
  if (!claims
    || claims['nonce'] !== nonce
    || !audiences.includes(appConfig().clientId)
    || claims['iss'] !== appConfig().authUrl) {
    throw new OAuthError('invalid_id_token', 'The ID token does not belong to this login');
  }
  return claims;
}

function readStoredSession(): TokenSet | null {
  return parseJson<TokenSet>(sessionStorage.getItem(SESSION_KEY));
}

function parseJson<T>(raw: string | null): T | null {
  if (!raw) return null;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}
