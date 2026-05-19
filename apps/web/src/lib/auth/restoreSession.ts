import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { syncActiveConsentVersion } from "@/lib/consent/syncActiveConsentVersion";
import { useAuthStore } from "@/stores/authStore";

/**
 * Renew access token from the HttpOnly refresh cookie, then accept can run as the logged-in user.
 * Call this before invitation accept when the user may arrive from email (Gmail, Mailhog).
 */
export async function tryRestoreSession(): Promise<boolean> {
  try {
    const response = await apiClient.post(API_ROUTES.AUTH.REFRESH);
    const {
      user: refreshedUser,
      accessToken,
      consentGiven,
      consentVersion,
    } = response.data?.data ?? {};

    if (!accessToken || !refreshedUser) {
      return false;
    }

    useAuthStore.getState().setAuth(
      {
        id: String(refreshedUser.id),
        email: refreshedUser.email,
        role: refreshedUser.role,
        fullName: refreshedUser.fullName ?? undefined,
      },
      accessToken,
      {
        consentGiven: consentGiven ?? false,
        consentVersion:
          consentVersion === undefined || consentVersion === null
            ? null
            : String(consentVersion),
      },
    );
    await syncActiveConsentVersion().catch(() => undefined);
    return true;
  } catch {
    return false;
  }
}
