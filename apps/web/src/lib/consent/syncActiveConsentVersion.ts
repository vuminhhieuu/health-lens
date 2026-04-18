import { ApiPaths } from "@healthlens/shared/constants";

import { apiClient } from "@/lib/api/apiClient";
import { useAuthStore } from "@/stores/authStore";

/**
 * Loads the server-authoritative consent version into the auth store.
 * Requires a valid access token (authenticated session).
 */
export async function syncActiveConsentVersion(): Promise<void> {
  if (!useAuthStore.getState().isAuthenticated) {
    return;
  }
  try {
    const { data } = await apiClient.get<{ version: string }>(
      ApiPaths.CONSENT.ME_ACTIVE_VERSION,
    );
    const v = data?.version?.trim();
    useAuthStore.getState().setActiveConsentVersion(
      v && v.length > 0 ? v : null,
    );
  } catch {
    useAuthStore.getState().setActiveConsentVersion(null);
  }
}
