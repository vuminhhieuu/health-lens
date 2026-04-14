"use client";

import { useEffect, useState } from "react";

import { useAuthStore } from "@/stores/authStore";
import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";

export function useAuthBootstrap() {
  const [isLoading, setIsLoading] = useState(true);
  const setAuth = useAuthStore((s) => s.setAuth);
  const clearAuth = useAuthStore((s) => s.clearAuth);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  useEffect(() => {
    const bootstrap = async () => {
      if (isAuthenticated) {
        setIsLoading(false);
        return;
      }

      try {
        const response = await apiClient.post(API_ROUTES.AUTH.REFRESH);
        const { user, accessToken } = response.data?.data || {};

        if (user && accessToken) {
          setAuth(user, accessToken);
        } else {
          clearAuth();
        }
      } catch {
        clearAuth();
      } finally {
        setIsLoading(false);
      }
    };

    bootstrap();
  }, [isAuthenticated, setAuth, clearAuth]);

  return isLoading;
}