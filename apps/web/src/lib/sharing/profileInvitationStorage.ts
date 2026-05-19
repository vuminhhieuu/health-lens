import { clearLoginRedirectGuard } from "@/lib/browser/loginRedirectGuard";
import { removeSessionStorage } from "@/lib/browser/sessionStorage";

export const PENDING_PROFILE_INVITATION_TOKEN_KEY = "pending-profile-invitation-token";

export function profileInvitationLoginRedirectGuardKey(token: string): string {
  return `invitation-login-redirect:${token}`;
}

/** Clears stashed token and per-token login-redirect guard after a terminal outcome. */
export function clearProfileInvitationFlowStorage(token?: string): void {
  removeSessionStorage(PENDING_PROFILE_INVITATION_TOKEN_KEY);
  if (token) {
    clearLoginRedirectGuard(profileInvitationLoginRedirectGuardKey(token));
  }
}
