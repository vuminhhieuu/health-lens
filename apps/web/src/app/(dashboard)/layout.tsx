"use client";

import { useEffect, useState, useRef } from "react";
import { useRouter } from "next/navigation";

import { useAuthStore } from "@/stores/authStore";
import { useAuthBootstrap } from "@/hooks/useAuthBootstrap";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import {
  Search, Bell, HelpCircle,
  Home, FileText, User, Users, Settings, LogOut, Shield
} from "lucide-react";
import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";

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
  const isLoading = useAuthBootstrap();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const user = useAuthStore((s) => s.user);

  const [isAvatarMenuOpen, setIsAvatarMenuOpen] = useState(false);
  const [displayName, setDisplayName] = useState<string | null>(null);
  const avatarMenuRef = useRef<HTMLDivElement>(null);

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
    function handleClickOutside(event: MouseEvent) {
      if (avatarMenuRef.current && !avatarMenuRef.current.contains(event.target as Node)) {
        setIsAvatarMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      const returnPath =
        typeof window !== "undefined"
          ? `${window.location.pathname}${window.location.search}`
          : "";
      router.replace(`/login?returnUrl=${encodeURIComponent(returnPath || "/home")}`);
    }
  }, [isLoading, isAuthenticated, router]);

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

  const avatarInitial = (displayName ?? user?.fullName ?? user?.email ?? "U").trim()[0]?.toUpperCase() ?? "U";
  const avatarUrl = currentUser?.avatarUrl ?? null;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  const navItems = [
    { name: "Trang chủ", href: "/home", icon: Home, exact: true },
    { name: "Kết quả khám", href: "/health-records", icon: FileText, exact: false },
    { name: "Hồ sơ của tôi", href: "/settings/profile", icon: User, exact: true },
    { name: "Hồ sơ gia đình", href: "/profiles", icon: Users, exact: false },
    { name: "Cài đặt", href: "/settings", icon: Settings, exact: true },
  ];
  const isAdmin = user?.role === "ROLE_ADMIN";

  const isProfileHistoryRoute = /^\/profiles\/[^/]+\/history(?:\/.*)?$/.test(pathname ?? "");
  const isNavItemActive = (item: { href: string; exact: boolean }) => {
    if (item.href === "/health-records") {
      return pathname === item.href || pathname?.startsWith(`${item.href}/`) || isProfileHistoryRoute;
    }
    if (item.href === "/settings/profile") {
      return pathname === item.href;
    }
    if (item.href === "/settings") {
      return pathname === item.href || (pathname?.startsWith(`${item.href}/`) && pathname !== "/settings/profile");
    }
    if (item.href === "/profiles") {
      if (isProfileHistoryRoute) return false;
      return pathname === item.href || pathname?.startsWith(`${item.href}/`);
    }
    return item.exact ? pathname === item.href : pathname === item.href || pathname?.startsWith(`${item.href}/`);
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

  return (
    <div className="bg-[#effcf9] text-[#121e1c] min-h-screen">
      {/* Top Navigation Bar */}
      <header className="fixed top-0 w-full z-50 bg-[#e9f6f3]/80 backdrop-blur-md flex justify-between items-center px-6 py-3 shadow-sm border-b border-[#bcc9c6]/20 print:hidden">
        <div className="flex items-center gap-8">
          <span className="text-xl font-bold tracking-tight text-[#005049]">HealthLens</span>

          {/* Horizontal Nav Links in Header */}
          <nav className="hidden md:flex items-center gap-6">
            {navItems.slice(0, 3).map((item) => {
              const isActive = isNavItemActive(item);
              if (isActive) {
                return (
                  <Link key={item.name} href={item.href} className="text-[#00685f] font-semibold px-3 py-1 rounded-lg bg-white/50">
                    {item.name}
                  </Link>
                );
              }
              return (
                <Link key={item.name} href={item.href} className="text-[#6d7a77] font-medium hover:bg-[#e9f6f3]/50 transition-colors px-3 py-1 rounded-lg">
                  {item.name}
                </Link>
              );
            })}
          </nav>
        </div>

        <div className="flex items-center gap-4">
          <div className="relative hidden sm:block">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-[#6d7a77] w-4 h-4" />
            <input
              type="text"
              placeholder="Tìm kiếm..."
              className="pl-10 pr-4 py-2 bg-[#e9f6f3] border-none rounded-full w-64 focus:ring-2 focus:ring-[#00685f]/20 text-sm outline-none"
            />
          </div>
          <button className="p-2 text-[#3d4947] hover:bg-[#e9f6f3] transition-colors rounded-full relative">
            <Bell className="w-5 h-5" />
            <span className="absolute top-2 right-2 w-2 h-2 bg-[#924628] rounded-full"></span>
          </button>
          <Link
            href="/questions"
            title="Mở trang thắc mắc"
            aria-label="Mở trang thắc mắc"
            className="flex min-h-12 min-w-12 items-center justify-center rounded-full p-2 text-[#3d4947] transition-colors hover:bg-[#e9f6f3] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
          >
            <HelpCircle className="w-5 h-5" />
          </Link>
          <div className="relative" ref={avatarMenuRef}>
            <button
              onClick={() => setIsAvatarMenuOpen(!isAvatarMenuOpen)}
              className="flex w-10 h-10 items-center justify-center rounded-full overflow-hidden border-2 border-[#89f5e7] bg-[#d8e5e2] text-sm font-black text-[#00685f] focus:outline-none focus:ring-2 focus:ring-[#00685f]/50 transition-all"
              aria-label="Mở menu tài khoản"
            >
              {avatarUrl ? (
                /* eslint-disable-next-line @next/next/no-img-element */
                <img alt="Ảnh đại diện" className="w-full h-full object-cover" src={avatarUrl} />
              ) : (
                <span aria-hidden="true">{avatarInitial}</span>
              )}
            </button>

            {isAvatarMenuOpen && (
              <div className="absolute right-0 mt-2 w-48 bg-white rounded-2xl shadow-xl border border-[#bcc9c6]/20 py-2 z-50 animate-in fade-in zoom-in-95 duration-100 origin-top-right">
                <Link
                  href="/settings/profile"
                  onClick={() => setIsAvatarMenuOpen(false)}
                  className="flex items-center gap-3 px-4 py-2.5 text-sm font-medium text-[#3d4947] hover:bg-[#e9f6f3] transition-colors"
                >
                  <User className="w-4 h-4" />
                  Hồ sơ cá nhân
                </Link>
                <Link
                  href="/settings"
                  onClick={() => setIsAvatarMenuOpen(false)}
                  className="flex items-center gap-3 px-4 py-2.5 text-sm font-medium text-[#3d4947] hover:bg-[#e9f6f3] transition-colors"
                >
                  <Settings className="w-4 h-4" />
                  Cài đặt
                </Link>
                <div className="h-px bg-[#bcc9c6]/20 my-1"></div>
                <button
                  onClick={() => {
                    setIsAvatarMenuOpen(false);
                    logout();
                  }}
                  className="w-full flex items-center gap-3 px-4 py-2.5 text-sm font-medium text-[#ba1a1a] hover:bg-[#ffdad6]/40 transition-colors"
                >
                  <LogOut className="w-4 h-4" />
                  Đăng xuất
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      <div className="flex pt-16 min-h-screen print:block print:pt-0">
        {/* Sidebar Navigation */}
        <aside className="hidden md:flex h-screen w-72 flex-col fixed left-0 bg-gradient-to-b from-[#e9f6f3] to-transparent p-6 gap-2 border-r border-[#bcc9c6]/20 print:hidden">
          <div className="mb-8 px-2">
            <p className="text-xs font-bold uppercase tracking-widest text-[#6d7a77] mb-4">Tài khoản</p>
            <div className="flex items-center gap-3 mb-6 bg-white p-3 rounded-2xl shadow-sm border border-[#bcc9c6]/20">
              <div className="w-12 h-12 rounded-xl bg-[#008378] flex items-center justify-center overflow-hidden text-white font-black">
                {avatarUrl ? (
                  /* eslint-disable-next-line @next/next/no-img-element */
                  <img src={avatarUrl} alt="Ảnh đại diện" className="h-full w-full object-cover" />
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
                  <Link key={item.name} href={item.href} className="flex items-center gap-3 px-4 py-3 bg-white text-[#00685f] rounded-xl shadow-sm font-bold border-r-4 border-[#00685f] transition-all duration-200">
                    <Icon className="w-5 h-5" />
                    <span className="font-medium">{item.name}</span>
                  </Link>
                );
              }

              return (
                <Link key={item.name} href={item.href} className="flex items-center gap-3 px-4 py-3 text-[#6d7a77] hover:text-[#00685f] hover:bg-white/60 rounded-xl transition-all">
                  <Icon className="w-5 h-5" />
                  <span className="font-medium">{item.name}</span>
                </Link>
              );
            })}
          </nav>

          <div className="mt-auto flex flex-col gap-4">
            <button disabled className="w-full bg-gradient-to-br from-[#00685f] to-[#008378] text-white py-3 rounded-xl font-bold shadow-lg shadow-[#00685f]/20 active:scale-95 duration-200 opacity-60 cursor-not-allowed">
              Đặt lịch khám
            </button>
            <button onClick={logout} className="flex items-center gap-3 px-4 py-3 text-[#ba1a1a] font-medium hover:bg-[#ffdad6]/40 rounded-xl transition-all w-full">
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
        <Link href="/health-records" className={getMobileNavClassName("/health-records")}>
          <FileText className="w-5 h-5" />
          <span className="text-[10px] font-bold">Sức khỏe</span>
        </Link>
        <Link href="/settings/profile" className={getMobileNavClassName("/settings/profile")}>
          <User className={`w-5 h-5 ${isNavItemActive({ href: "/settings/profile", exact: true }) ? "fill-current" : ""}`} />
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
