/**
 * Crypto helpers for the Authorization Code + PKCE flow (RFC 7636).
 *
 * crypto.subtle only exists in secure contexts (https, localhost, *.localhost), which
 * covers every place the site is served from.
 */

const UNRESERVED = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';

/** Random string from the RFC 7636 unreserved alphabet (43..128 chars for a verifier). */
export function randomString(length = 64): string {
  // Rejection sampling: bytes above the last full multiple of the alphabet would bias it.
  const limit = 256 - (256 % UNRESERVED.length);
  let result = '';
  while (result.length < length) {
    const bytes = crypto.getRandomValues(new Uint8Array(length));
    for (const byte of bytes) {
      if (byte < limit && result.length < length) {
        result += UNRESERVED[byte % UNRESERVED.length];
      }
    }
  }
  return result;
}

export function base64UrlEncode(bytes: Uint8Array): string {
  let binary = '';
  bytes.forEach((byte) => (binary += String.fromCharCode(byte)));
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

/** S256 code challenge: BASE64URL(SHA256(ASCII(code_verifier))). */
export async function codeChallengeS256(verifier: string): Promise<string> {
  if (!globalThis.crypto?.subtle) {
    throw new Error('PKCE needs crypto.subtle: serve the site over https or from localhost');
  }
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier));
  return base64UrlEncode(new Uint8Array(digest));
}
