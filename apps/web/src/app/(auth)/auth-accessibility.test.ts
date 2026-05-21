import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const authPages = [
  "login/page.tsx",
  "register/page.tsx",
  "forgot-password/page.tsx",
  "reset-password/page.tsx",
  "invitations/accept/page.tsx",
  "health-record-invitations/accept/page.tsx",
  "verify-email/VerifyEmailClient.tsx",
] as const;

describe("auth accessibility wiring", () => {
  it.each(authPages)("includes live region or alert semantics in %s", (relativePath) => {
    const source = readFileSync(
      join(process.cwd(), "src/app/(auth)", relativePath),
      "utf8",
    );

    expect(source).toMatch(
      /aria-live|role="alert"|role="status"|Invitation(Error|Loading)State/,
    );
  });

  it("invitation accept routes use shared state components", () => {
    const profileInvite = readFileSync(
      join(process.cwd(), "src/app/(auth)/invitations/accept/page.tsx"),
      "utf8",
    );
    const recordInvite = readFileSync(
      join(process.cwd(), "src/app/(auth)/health-record-invitations/accept/page.tsx"),
      "utf8",
    );

    expect(profileInvite).toContain("InvitationLoadingState");
    expect(profileInvite).toContain("InvitationErrorState");
    expect(recordInvite).toContain("InvitationLoadingState");
    expect(recordInvite).toContain("InvitationErrorState");
  });
});
