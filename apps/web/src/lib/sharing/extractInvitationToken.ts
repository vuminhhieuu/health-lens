/** Parses `token` from SPA accept paths such as `/invitations/accept?token=...`. */
export function extractInvitationToken(acceptPath: string): string | null {
  try {
    const normalized = acceptPath.startsWith("/")
      ? `http://local${acceptPath}`
      : acceptPath;
    const url = new URL(normalized);
    return url.searchParams.get("token");
  } catch {
    return null;
  }
}
