import { refreshSessionOnce } from "@/lib/auth/refreshSession";

/**
 * Renew access token from the HttpOnly refresh cookie, then accept can run as the logged-in user.
 * Call this before invitation accept when the user may arrive from email (Gmail, Mailhog).
 */
export async function tryRestoreSession(): Promise<boolean> {
  return refreshSessionOnce();
}
