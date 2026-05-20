import { z } from "zod";

/** Matches `AcceptInvitationResultResponse` from the API. */
export const acceptProfileInvitationResultSchema = z.object({
  outcome: z.enum(["accepted", "require-login", "expired"]),
  redirectUrl: z.string().min(1),
  profileId: z.string().uuid(),
});

export type AcceptProfileInvitationResult = z.infer<typeof acceptProfileInvitationResultSchema>;

export function parseAcceptProfileInvitationResult(data: unknown): AcceptProfileInvitationResult | null {
  const parsed = acceptProfileInvitationResultSchema.safeParse(data);
  return parsed.success ? parsed.data : null;
}
