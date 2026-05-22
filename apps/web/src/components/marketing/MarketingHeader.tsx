"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";

import { useAuthBootstrap } from "@/hooks/useAuthBootstrap";
import { AuthenticatedTopHeader } from "@/components/layout/AuthenticatedTopHeader";
import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";
import {
  authenticatedHeaderInnerClassName,
  authenticatedHeaderSurfaceClassName,
} from "@/lib/layout/shell";
import {
  publicMarketingHeaderClassName,
  publicMarketingHeaderInnerClassName,
} from "@/lib/marketing/layout";
import { useAuthStore } from "@/stores/authStore";

type CurrentUser = {
  avatarUrl?: string | null;
};

export function MarketingHeader() {
  const router = useRouter();
  const isLoading = useAuthBootstrap();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const user = useAuthStore((s) => s.user);
  const clearAuth = useAuthStore((s) => s.clearAuth);
  const { data: currentUser } = useQuery({
    queryKey: ["currentUser"],
    queryFn: async () => {
      const response = await apiClient.get(API_ROUTES.USERS.ME);
      return response.data?.data as CurrentUser;
    },
    enabled: isAuthenticated,
    staleTime: 12 * 60 * 1000,
    refetchInterval: 12 * 60 * 1000,
    refetchOnWindowFocus: true,
  });

  const displayName = user?.fullName?.trim() || user?.email || "Người dùng";
  const avatarInitial = displayName[0]?.toUpperCase() ?? "U";
  const avatarUrl = currentUser?.avatarUrl ?? null;
  const logout = async () => {
    try {
      await apiClient.post(API_ROUTES.AUTH.LOGOUT);
    } catch {
      // Continue with local sign out if backend logout fails.
    } finally {
      clearAuth();
      router.push("/login");
    }
  };

  if (isLoading) {
    return (
      <header aria-hidden="true" className={publicMarketingHeaderClassName}>
        <div className={`${publicMarketingHeaderInnerClassName} gap-4`}>
          <div className="h-8 w-36 animate-pulse rounded-lg bg-[#e1ebe8]" />
          <div className="flex shrink-0 items-center gap-2 sm:gap-3">
            <div className="h-10 w-24 animate-pulse rounded-lg bg-[#e1ebe8]" />
            <div className="h-10 w-24 animate-pulse rounded-lg bg-[#e1ebe8]" />
          </div>
        </div>
      </header>
    );
  }

  if (isAuthenticated) {
    return (
      <AuthenticatedTopHeader
        avatarInitial={avatarInitial}
        avatarUrl={avatarUrl}
        brandHref="/"
        onLogout={logout}
        className={`sticky top-0 z-50 w-full ${authenticatedHeaderSurfaceClassName}`}
        innerClassName={authenticatedHeaderInnerClassName}
      />
    );
  }

  return (
    <header className={publicMarketingHeaderClassName}>
      <div className={`${publicMarketingHeaderInnerClassName} gap-4`}>
        <Link
          href="/"
          className="text-2xl font-bold tracking-tight text-[#005049] transition hover:text-[#00685f] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
        >
          HealthLens
        </Link>
        <nav aria-label="Tài khoản" className="flex shrink-0 items-center gap-2 sm:gap-3">
          <Link
            href="/login"
            className="inline-flex h-10 items-center justify-center rounded-lg border border-[#bcc9c6] bg-white px-3 text-sm font-semibold text-[#00685f] transition hover:bg-[#e9f6f3] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f] sm:px-4"
          >
            Đăng nhập
          </Link>
          <Link
            href="/register"
            className="inline-flex h-10 items-center justify-center rounded-lg bg-[#00685f] px-3 text-sm font-semibold text-white transition hover:bg-[#005049] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f] sm:px-4"
          >
            Đăng ký
          </Link>
        </nav>
      </div>
    </header>
  );
}
