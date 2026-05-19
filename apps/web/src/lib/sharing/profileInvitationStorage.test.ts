import { afterEach, describe, expect, it } from "vitest";

import {
  clearProfileInvitationFlowStorage,
  PENDING_PROFILE_INVITATION_TOKEN_KEY,
  profileInvitationLoginRedirectGuardKey,
} from "./profileInvitationStorage";
import { readSessionStorage, writeSessionStorage } from "@/lib/browser/sessionStorage";

describe("profileInvitationStorage", () => {
  afterEach(() => {
    window.sessionStorage.clear();
  });

  it("clears pending token and per-token guard key", () => {
    writeSessionStorage(PENDING_PROFILE_INVITATION_TOKEN_KEY, "token-a");
    writeSessionStorage(profileInvitationLoginRedirectGuardKey("token-a"), "1");

    clearProfileInvitationFlowStorage("token-a");

    expect(readSessionStorage(PENDING_PROFILE_INVITATION_TOKEN_KEY)).toBeNull();
    expect(readSessionStorage(profileInvitationLoginRedirectGuardKey("token-a"))).toBeNull();
  });

  it("clears only pending token when no token is provided", () => {
    writeSessionStorage(PENDING_PROFILE_INVITATION_TOKEN_KEY, "token-b");
    writeSessionStorage(profileInvitationLoginRedirectGuardKey("token-b"), "1");

    clearProfileInvitationFlowStorage();

    expect(readSessionStorage(PENDING_PROFILE_INVITATION_TOKEN_KEY)).toBeNull();
    expect(readSessionStorage(profileInvitationLoginRedirectGuardKey("token-b"))).toBe("1");
  });
});
