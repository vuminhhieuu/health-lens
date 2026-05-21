"use client";

import { useEffect, useState } from "react";

import { useAuthHydrated } from "@/hooks/useAuthHydrated";
import { refreshSessionOnce } from "@/lib/auth/refreshSession";
import { useAuthStore } from "@/stores/authStore";

export function useAuthBootstrap() {
  const hydrated = useAuthHydrated();
  const [isBootstrapping, setIsBootstrapping] = useState(true);

  useEffect(() => {
    if (!hydrated) {
      return;
    }

    let cancelled = false;

    const bootstrap = async () => {
      const refreshed = await refreshSessionOnce();
      if (cancelled) {
        return;
      }

      if (!refreshed && !useAuthStore.getState().isAuthenticated) {
        useAuthStore.getState().clearAuth();
      }

      setIsBootstrapping(false);
    };

    void bootstrap();

    return () => {
      cancelled = true;
    };
  }, [hydrated]);

  return !hydrated || isBootstrapping;
}
