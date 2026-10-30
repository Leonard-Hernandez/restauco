/** What survives the round trip to authco: the request parameters only this tab knows. */
export interface PendingLogin {
  state: string;
  nonce: string;
  codeVerifier: string;
  returnUrl: string;
}

/** Query parameters authco sends back to the redirect_uri. */
export interface AuthorizationResponse {
  code: string | null;
  state: string | null;
  error: string | null;
  error_description: string | null;
}

export interface TokenResponse {
  access_token: string;
  id_token?: string;
  token_type: string;
  expires_in: number;
}

/** The session persisted in sessionStorage. */
export interface TokenSet {
  accessToken: string;
  idToken: string | null;
  /** Epoch millis after which the access token must not be used. */
  expiresAt: number;
  /** Display name and email from the ID token, for the UI only. */
  name: string | null;
  email: string | null;
}

export class OAuthError extends Error {
  constructor(public readonly code: string, message: string) {
    super(message);
  }
}
