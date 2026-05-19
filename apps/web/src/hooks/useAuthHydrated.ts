import { useSyncExternalStore } from "react";

import { useAuthStore } from "@/stores/authStore";

/** True after persisted auth state has been read from localStorage. */
export function useAuthHydrated(): boolean {
  return useSyncExternalStore(
    (onStoreChange) => useAuthStore.persist.onFinishHydration(onStoreChange),
    () => useAuthStore.persist.hasHydrated(),
    () => false,
  );
}
