import { afterEach, describe, expect, it, vi } from "vitest";

import {
  appendInvitationLoginRedirectAttemptedParam,
  clearLoginRedirectGuard,
  hasInvitationLoginRedirectAttemptedParam,
  INVITATION_LOGIN_REDIRECT_ATTEMPTED_PARAM,
  isLoginRedirectGuardActive,
  resetLoginRedirectGuardsForTests,
  setLoginRedirectGuard,
} from "./loginRedirectGuard";

describe("loginRedirectGuard", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    resetLoginRedirectGuardsForTests();
    window.sessionStorage.clear();
  });

  it("uses sessionStorage when available", () => {
    expect(setLoginRedirectGuard("guard-a")).toBe(true);
    expect(isLoginRedirectGuardActive("guard-a")).toBe(true);
    clearLoginRedirectGuard("guard-a");
    expect(isLoginRedirectGuardActive("guard-a")).toBe(false);
  });

  it("falls back to in-memory guard when sessionStorage write fails", () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("denied");
    });
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("denied");
    });

    expect(setLoginRedirectGuard("guard-b")).toBe(false);
    expect(isLoginRedirectGuardActive("guard-b")).toBe(true);
  });

  it("appends the attempted-login query param for return URLs", () => {
    expect(appendInvitationLoginRedirectAttemptedParam("/invitations/accept?token=abc")).toBe(
      `/invitations/accept?token=abc&${INVITATION_LOGIN_REDIRECT_ATTEMPTED_PARAM}=1`,
    );
    expect(hasInvitationLoginRedirectAttemptedParam(new URLSearchParams("invitationLoginRedirectAttempted=1"))).toBe(
      true,
    );
  });
});
