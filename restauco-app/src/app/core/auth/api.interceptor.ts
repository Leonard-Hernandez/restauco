import { HttpErrorResponse, HttpEvent, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import { appConfig } from '../config/app-config';
import { AuthService } from './auth.service';

/**
 * restauco API requests carry the restaurant (X-Tenant) and, when signed in, the authco
 * access token. Never sent to authco or third parties. A 401 on a signed-in request means
 * the token expired or was revoked: log in again.
 */
export function apiInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> {
  const config = appConfig();
  if (!req.url.startsWith(`${config.apiUrl}/`)) {
    return next(req);
  }

  const auth = inject(AuthService);
  // Public endpoints never get the token: a stale one would turn them into a 401 (the
  // resource server rejects any invalid bearer) and bounce a guest to the login.
  const token = isPublic(req.method, req.url.slice(config.apiUrl.length)) ? null : auth.token();
  const setHeaders: Record<string, string> = { 'X-Tenant': config.tenant };
  if (token) {
    setHeaders['Authorization'] = `Bearer ${token}`;
  }

  return next(req.clone({ setHeaders })).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && token) {
        auth.restartLogin();
      }
      return throwError(() => error);
    })
  );
}

const PUBLIC_POST = /^\/api\/public\/tables\/[0-9a-f-]{36}\/orders$/;

export function isPublic(method: string, path: string): boolean {
  return (method === 'GET' && path.startsWith('/api/public/')) || (method === 'POST' && PUBLIC_POST.test(path));
}
