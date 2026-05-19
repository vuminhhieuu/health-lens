import { readSessionStorage, removeSessionStorage, writeSessionStorage } from "./sessionStorage";

/** Survives client-side navigation when sessionStorage is blocked. */
const memoryGuards = new Set<string>();

export const INVITATION_LOGIN_REDIRECT_ATTEMPTED_PARAM = "invitationLoginRedirectAttempted";

export function isLoginRedirectGuardActive(guardKey: string): boolean {
  if (memoryGuards.has(guardKey)) {
    return true;
  }
  return readSessionStorage(guardKey) === "1";
}

/** Returns true when the guard was persisted to sessionStorage. */
export function setLoginRedirectGuard(guardKey: string): boolean {
  memoryGuards.add(guardKey);
  return writeSessionStorage(guardKey, "1");
}

export function clearLoginRedirectGuard(guardKey: string): void {
  memoryGuards.delete(guardKey);
  removeSessionStorage(guardKey);
}

export function hasInvitationLoginRedirectAttemptedParam(
  searchParams: Pick<URLSearchParams, "get">,
): boolean {
  return searchParams.get(INVITATION_LOGIN_REDIRECT_ATTEMPTED_PARAM) === "1";
}

export function appendInvitationLoginRedirectAttemptedParam(pathWithQuery: string): string {
  const separator = pathWithQuery.includes("?") ? "&" : "?";
  return `${pathWithQuery}${separator}${INVITATION_LOGIN_REDIRECT_ATTEMPTED_PARAM}=1`;
}

/** @internal Test-only reset of in-memory guards. */
export function resetLoginRedirectGuardsForTests(): void {
  memoryGuards.clear();
}
