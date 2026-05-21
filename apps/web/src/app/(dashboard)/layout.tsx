"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { useAuthStore } from "@/stores/authStore";
import { useAuthBootstrap } from "@/hooks/useAuthBootstrap";
import { useAuthHydrated } from "@/hooks/useAuthHydrated";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import {
  Home,
  FileText,
  User,
  Users,
  Settings,
  LogOut,
  Shield,
} from "lucide-react";
import SafeImage from "@/components/ui/SafeImage";
import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { AuthenticatedTopHeader } from "@/components/layout/AuthenticatedTopHeader";
import { LoadingState } from "@/components/ui";

type CurrentUser = {
  id?: string;
  email?: string;
  fullName?: string;
  birthDate?: string | null;
  gender?: string | null;
  emailVerified?: boolean;
  avatarUrl?: string | null;
};

export default function DashboardLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const router = useRouter();
  const pathname = usePathname();
  const hydrated = useAuthHydrated();
  const isBootstrapping = useAuthBootstrap();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const authReady = hydrated && !isBootstrapping;
  const user = useAuthStore((s) => s.user);

  const [displayName, setDisplayName] = useState<string | null>(null);

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

  useEffect(() => {
    if (!authReady || isAuthenticated) {
      return;
    }

    const returnPath =
      typeof window !== "undefined"
        ? `${window.location.pathname}${window.location.search}`
        : "";
    router.replace(`/login?returnUrl=${encodeURIComponent(returnPath || "/home")}`);
  }, [authReady, isAuthenticated, router]);

  // Derive display name from the shared current-user query used by profile settings.
  useEffect(() => {
    const fullName = currentUser?.fullName ?? user?.fullName;
    if (!fullName) {
      setDisplayName(null);
      return;
    }
    const parts = fullName.trim().split(/\s+/);
    setDisplayName(parts.slice(-2).join(" "));
  }, [currentUser?.fullName, user?.fullName]);

  const avatarInitial =
    (displayName ?? user?.fullName ?? user?.email ?? "U")
      .trim()[0]
      ?.toUpperCase() ?? "U";
  const avatarUrl = currentUser?.avatarUrl ?? null;

  if (!authReady) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <LoadingState
          title="Đang tải không gian làm việc"
          className="min-h-32 border-none bg-transparent shadow-none"
        />
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  const navItems = [
    { name: "Trang chủ", href: "/home", icon: Home, exact: true },
    {
      name: "Kết quả khám",
      href: "/health-records",
      icon: FileText,
      exact: false,
    },
    {
      name: "Hồ sơ của tôi",
      href: "/settings/profile",
      icon: User,
      exact: true,
    },
    { name: "Hồ sơ gia đình", href: "/profiles", icon: Users, exact: false },
    { name: "Cài đặt", href: "/settings", icon: Settings, exact: true },
  ];
  const isAdmin = user?.role === "ROLE_ADMIN";

  const isProfileHistoryRoute = /^\/profiles\/[^/]+\/history(?:\/.*)?$/.test(
    pathname ?? "",
  );
  const isNavItemActive = (item: { href: string; exact: boolean }) => {
    if (item.href === "/health-records") {
      return (
        pathname === item.href ||
        pathname?.startsWith(`${item.href}/`) ||
        isProfileHistoryRoute
      );
    }
    if (item.href === "/settings/profile") {
      return pathname === item.href;
    }
    if (item.href === "/settings") {
      return (
        pathname === item.href ||
        (pathname?.startsWith(`${item.href}/`) &&
          pathname !== "/settings/profile")
      );
    }
    if (item.href === "/profiles") {
      if (isProfileHistoryRoute) return false;
      return pathname === item.href || pathname?.startsWith(`${item.href}/`);
    }
    return item.exact
      ? pathname === item.href
      : pathname === item.href || pathname?.startsWith(`${item.href}/`);
  };

  const logout = async () => {
    try {
      await apiClient.post(API_ROUTES.AUTH.LOGOUT);
    } catch (e) {
      console.error("Logout API failed", e);
    } finally {
      useAuthStore.getState().clearAuth();
      router.push("/login");
    }
  };

  const getMobileNavClassName = (href: string) => {
    const isActive = isNavItemActive({ href, exact: true });
    return `flex flex-col items-center gap-1 ${isActive ? "text-[#00685f]" : "text-[#6d7a77]"}`;
  };

  const topHeaderNavItems = navItems.slice(0, 3).map((item) => ({
    name: item.name,
    href: item.href,
    isActive: isNavItemActive(item),
  }));

  return (
    <div className="bg-[#effcf9] text-[#121e1c] min-h-screen">
      <AuthenticatedTopHeader
        navItems={topHeaderNavItems}
        avatarInitial={avatarInitial}
        avatarUrl={avatarUrl}
        brandHref="/"
        onLogout={logout}
      />

      <div className="flex pt-16 min-h-screen print:block print:pt-0">
        {/* Sidebar Navigation */}
        <aside className="hidden md:flex h-screen w-72 flex-col fixed left-0 bg-gradient-to-b from-[#e9f6f3] to-transparent p-6 gap-2 border-r border-[#bcc9c6]/20 print:hidden">
          <div className="mb-8 px-2">
            <p className="text-xs font-bold uppercase tracking-widest text-[#6d7a77] mb-4">
              Tài khoản
            </p>
            <div className="flex items-center gap-3 mb-6 bg-white p-3 rounded-2xl shadow-sm border border-[#bcc9c6]/20">
              <div className="w-12 h-12 rounded-xl bg-[#008378] flex items-center justify-center overflow-hidden text-white font-black">
                {avatarUrl ? (
                  <SafeImage
                    raw
                    src={avatarUrl}
                    alt="Ảnh đại diện"
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <User className="h-6 w-6" aria-hidden="true" />
                )}
              </div>
              <div>
                <h4 className="font-bold text-[#005049] line-clamp-1">
                  {displayName || user?.email || "Người dùng"}
                </h4>
              </div>
            </div>
            {isAdmin ? (
              <Link
                href="/admin/audit-log"
                className="inline-flex w-full items-center gap-2 rounded-xl border border-[#89f5e7]/70 bg-[#ecfdfa] px-3 py-2 text-sm font-semibold text-[#00685f] hover:bg-[#e1faf5]"
              >
                <Shield className="h-4 w-4" />
                Khu vực quản trị
              </Link>
            ) : null}
          </div>

          <nav className="flex flex-col gap-2 flex-grow">
            {navItems.map((item) => {
              const isActive = isNavItemActive(item);
              const Icon = item.icon;

              if (isActive) {
                return (
                  <Link
                    key={item.name}
                    href={item.href}
                    className="flex items-center gap-3 px-4 py-3 bg-white text-[#00685f] rounded-xl shadow-sm font-bold border-r-4 border-[#00685f] transition-all duration-200"
                  >
                    <Icon className="w-5 h-5" />
                    <span className="font-medium">{item.name}</span>
                  </Link>
                );
              }

              return (
                <Link
                  key={item.name}
                  href={item.href}
                  className="flex items-center gap-3 px-4 py-3 text-[#6d7a77] hover:text-[#00685f] hover:bg-white/60 rounded-xl transition-all"
                >
                  <Icon className="w-5 h-5" />
                  <span className="font-medium">{item.name}</span>
                </Link>
              );
            })}
          </nav>

          <div className="mt-auto flex flex-col gap-4">
            <button
              disabled
              className="w-full bg-gradient-to-br from-[#00685f] to-[#008378] text-white py-3 rounded-xl font-bold shadow-lg shadow-[#00685f]/20 active:scale-95 duration-200 opacity-60 cursor-not-allowed"
            >
              Đặt lịch khám
            </button>
            <button
              onClick={logout}
              className="flex items-center gap-3 px-4 py-3 text-[#ba1a1a] font-medium hover:bg-[#ffdad6]/40 rounded-xl transition-all w-full"
            >
              <LogOut className="w-5 h-5" />
              <span>Đăng xuất</span>
            </button>
          </div>
        </aside>

        {/* Main Content Area */}
        <div className="flex-grow md:ml-72 flex flex-col w-full h-full print:ml-0 print:block">
          {children}
        </div>
      </div>

      {/* Mobile Navigation Shell */}
      <nav className="md:hidden fixed bottom-0 w-full bg-white/95 backdrop-blur-lg px-6 py-4 flex justify-between items-center shadow-[0_-8px_32px_rgba(18,30,28,0.06)] z-50 border-t border-[#bcc9c6]/20 print:hidden">
        <Link href="/home" className={getMobileNavClassName("/home")}>
          <Home className="w-5 h-5" />
          <span className="text-[10px] font-bold">Trang chủ</span>
        </Link>
        <Link
          href="/health-records"
          className={getMobileNavClassName("/health-records")}
        >
          <FileText className="w-5 h-5" />
          <span className="text-[10px] font-bold">Sức khỏe</span>
        </Link>
        <Link
          href="/settings/profile"
          className={getMobileNavClassName("/settings/profile")}
        >
          <User
            className={`w-5 h-5 ${isNavItemActive({ href: "/settings/profile", exact: true }) ? "fill-current" : ""}`}
          />
          <span className="text-[10px] font-bold">Hồ sơ</span>
        </Link>
        <Link href="/settings" className={getMobileNavClassName("/settings")}>
          <Settings className="w-5 h-5" />
          <span className="text-[10px] font-bold">Cài đặt</span>
        </Link>
      </nav>
    </div>
  );
}
