"use client";

import { useEffect, useState } from "react";

import { useAuthStore } from "@/stores/authStore";
import { apiClient } from "@/lib/api/apiClient";
import { syncActiveConsentVersion } from "@/lib/consent/syncActiveConsentVersion";
import { API_ROUTES } from "@/lib/api/routes";

export function useAuthBootstrap() {
  const [isLoading, setIsLoading] = useState(true);
  const setAuth = useAuthStore((s) => s.setAuth);
  const clearAuth = useAuthStore((s) => s.clearAuth);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const user = useAuthStore((s) => s.user);

  useEffect(() => {
    const bootstrap = async () => {
      if (isAuthenticated) {
        void syncActiveConsentVersion();
        setIsLoading(false);
        return;
      }

      try {
        const response = await apiClient.post(API_ROUTES.AUTH.REFRESH);
        const {
          user: refreshedUser,
          accessToken,
          consentGiven,
          consentVersion,
        } = response.data?.data || {};
        const resolvedUser = refreshedUser ?? user;

        // Backend refresh may return only accessToken on some versions.
        if (resolvedUser && accessToken) {
          setAuth(resolvedUser, accessToken, {
            consentGiven: consentGiven ?? false,
            consentVersion:
              consentVersion === undefined || consentVersion === null
                ? null
                : String(consentVersion),
          });
          await syncActiveConsentVersion();
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
  }, [isAuthenticated, user, setAuth, clearAuth]);

  return isLoading;
}