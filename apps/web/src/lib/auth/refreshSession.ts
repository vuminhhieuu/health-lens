import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { syncActiveConsentVersion } from "@/lib/consent/syncActiveConsentVersion";
import { useAuthStore } from "@/stores/authStore";

let refreshInFlight: Promise<boolean> | null = null;

/**
 * Single-flight refresh: parallel callers (bootstrap, 401 interceptor, invitation accept)
 * share one POST /auth/refresh to avoid refresh-token rotation races that revoke the session.
 */
export function refreshSessionOnce(): Promise<boolean> {
  refreshInFlight ??= performRefresh().finally(() => {
    refreshInFlight = null;
  });
  return refreshInFlight;
}

async function performRefresh(): Promise<boolean> {
  try {
    const response = await apiClient.post(API_ROUTES.AUTH.REFRESH);
    const {
      user: refreshedUser,
      accessToken,
      consentGiven,
      consentVersion,
    } = response.data?.data ?? {};

    const { user: currentUser } = useAuthStore.getState();
    const resolvedUser = refreshedUser ?? currentUser;

    if (!accessToken || !resolvedUser) {
      return false;
    }

    useAuthStore.getState().setAuth(
      {
        id: String(resolvedUser.id),
        email: resolvedUser.email,
        role: resolvedUser.role,
        fullName: resolvedUser.fullName ?? undefined,
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
