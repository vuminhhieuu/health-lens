import { describe, expect, it } from "vitest";

import { parseAcceptProfileInvitationResult } from "./acceptInvitationResult";

describe("parseAcceptProfileInvitationResult", () => {
  it("accepts a valid backend payload", () => {
    const profileId = "550e8400-e29b-41d4-a716-446655440000";
    expect(
      parseAcceptProfileInvitationResult({
        outcome: "require-login",
        redirectUrl: "/login?returnUrl=%2Finvitations%2Faccept",
        profileId,
      }),
    ).toEqual({
      outcome: "require-login",
      redirectUrl: "/login?returnUrl=%2Finvitations%2Faccept",
      profileId,
    });
  });

  it("rejects legacy require-register outcome", () => {
    expect(
      parseAcceptProfileInvitationResult({
        outcome: "require-register",
        redirectUrl: "/register",
        profileId: "550e8400-e29b-41d4-a716-446655440000",
      }),
    ).toBeNull();
  });

  it("rejects malformed payloads", () => {
    expect(parseAcceptProfileInvitationResult(null)).toBeNull();
    expect(parseAcceptProfileInvitationResult({ outcome: "accepted" })).toBeNull();
  });
});
