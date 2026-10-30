import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { AuthorizationResponse, OAuthError } from '../../core/auth/oauth.models';

/**
 * redirect_uri of the authco client. In the top window it exchanges the code and resumes
 * the navigation that needed the login. Inside the silent-renewal iframe it only hands the
 * response to the parent window.
 */
@Component({
  selector: 'app-auth-callback',
  template: `
    @if (!inIframe) {
      <main class="screen">
        @if (errorMessage(); as message) {
          <h1>No pudimos iniciar sesión</h1>
          <p>{{ message }}</p>
          <button type="button" class="btn btn-brand" (click)="retry()">Intentar de nuevo</button>
        } @else {
          <p class="wait">Entrando…</p>
        }
      </main>
    }
  `,
  styles: `
    .screen { min-height: 100vh; display: grid; place-content: center; justify-items: center; gap: 1rem; padding: 2rem; text-align: center; background: var(--ground); color: var(--ink); }
    h1 { font-family: var(--font-display); font-weight: 400; font-size: 2rem; margin: 0; }
    p { max-width: 28rem; margin: 0; color: var(--ink-muted); }
    .wait { font-family: var(--font-label); text-transform: uppercase; letter-spacing: 0.14em; color: var(--brand); }
  `
})
export default class AuthCallbackComponent implements OnInit {
  readonly inIframe = window.parent !== window;
  readonly errorMessage = signal<string | null>(null);

  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  ngOnInit() {
    const query = this.route.snapshot.queryParamMap;
    const response: AuthorizationResponse = {
      code: query.get('code'),
      state: query.get('state'),
      error: query.get('error'),
      error_description: query.get('error_description')
    };

    if (this.inIframe) {
      window.parent.postMessage(response, window.location.origin);
      return;
    }

    this.auth.completeLogin(response).subscribe({
      next: (returnUrl) => this.router.navigateByUrl(returnUrl, { replaceUrl: true }),
      error: (error: unknown) => this.errorMessage.set(error instanceof OAuthError
        ? 'La respuesta del inicio de sesión no es válida o expiró.'
        : 'No pudimos completar el inicio de sesión. Inténtalo de nuevo.')
    });
  }

  retry() {
    this.errorMessage.set(null);
    this.auth.login('/');
  }
}
