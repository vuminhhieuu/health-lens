import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { useAuthStore } from "@/stores/authStore";

/** Clears server session and client auth state. */
export async function logoutCurrentUser(): Promise<void> {
  try {
    await apiClient.post(API_ROUTES.AUTH.LOGOUT);
  } catch {
    // Still clear local session if the API call fails.
  } finally {
    useAuthStore.getState().clearAuth();
  }
}
