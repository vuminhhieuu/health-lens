"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

import { useAuthStore } from "@/stores/authStore";
import { useAuthBootstrap } from "@/hooks/useAuthBootstrap";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Search, Bell, HelpCircle,
  Home, FileText, User, Users, Settings, LogOut
} from "lucide-react";

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

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      const currentPath = window.location.pathname;
      router.replace(`/login?returnUrl=${encodeURIComponent(currentPath)}`);
    }
  }, [isLoading, isAuthenticated, router]);

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
    { name: "Trang chủ", href: "/", icon: Home, exact: true },
    { name: "Kết quả khám", href: "/health-records", icon: FileText, exact: false },
    { name: "Hồ sơ của tôi", href: "/settings/profile", icon: User, exact: true },
    { name: "Hồ sơ gia đình", href: "/profiles", icon: Users, exact: false },
    { name: "Cài đặt", href: "/settings", icon: Settings, exact: true },
  ];

  const logout = () => {
    useAuthStore.getState().clearAuth();
    router.push("/login");
  };

  return (
    <div className="bg-[#effcf9] text-[#121e1c] min-h-screen">
      {/* Top Navigation Bar */}
      <header className="fixed top-0 w-full z-50 bg-[#e9f6f3]/80 backdrop-blur-md flex justify-between items-center px-6 py-3 shadow-sm border-b border-[#bcc9c6]/20">
        <div className="flex items-center gap-8">
          <span className="text-xl font-bold tracking-tight text-[#005049]">HealthLens</span>

          {/* Horizontal Nav Links in Header */}
          <nav className="hidden md:flex items-center gap-6">
            {navItems.slice(0, 3).map((item) => {
              const isActive = item.exact
                ? pathname === item.href
                : pathname === item.href || pathname?.startsWith(`${item.href}/`);
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
          <button className="p-2 text-[#3d4947] hover:bg-[#e9f6f3] transition-colors rounded-full">
            <HelpCircle className="w-5 h-5" />
          </button>
          <div className="w-10 h-10 rounded-full overflow-hidden border-2 border-[#89f5e7] bg-[#d8e5e2]">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img alt="Avatar" className="w-full h-full object-cover" src="https://ui-avatars.com/api/?name=H+L&background=00685f&color=fff&size=256" />
          </div>
        </div>
      </header>

      <div className="flex pt-16 min-h-screen">
        {/* Sidebar Navigation */}
        <aside className="hidden md:flex h-screen w-72 flex-col fixed left-0 bg-gradient-to-b from-[#e9f6f3] to-transparent p-6 gap-2 border-r border-[#bcc9c6]/20">
          <div className="mb-8 px-2">
            <p className="text-xs font-bold uppercase tracking-widest text-[#6d7a77] mb-4">Tài khoản</p>
            <div className="flex items-center gap-3 mb-6 bg-white p-3 rounded-2xl shadow-sm border border-[#bcc9c6]/20">
              <div className="w-12 h-12 rounded-xl bg-[#008378] flex items-center justify-center text-white">
                <User className="w-6 h-6" />
              </div>
              <div>
                <h4 className="font-bold text-[#005049] line-clamp-1">{user?.email || "Người dùng"}</h4>
                <p className="text-xs text-[#6d7a77]">Vai trò: {user?.role || "USER"}</p>
              </div>
            </div>
          </div>

          <nav className="flex flex-col gap-2 flex-grow">
            {navItems.map((item) => {
              const isActive = item.exact
                ? pathname === item.href
                : pathname === item.href || pathname?.startsWith(`${item.href}/`);
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
        <div className="flex-grow md:ml-72 flex flex-col w-full h-full">
          {children}
        </div>
      </div>

      {/* Mobile Navigation Shell */}
      <nav className="md:hidden fixed bottom-0 w-full bg-white/95 backdrop-blur-lg px-6 py-4 flex justify-between items-center shadow-[0_-8px_32px_rgba(18,30,28,0.06)] z-50 border-t border-[#bcc9c6]/20">
        <Link href="/" className="flex flex-col items-center gap-1 text-[#6d7a77]">
          <Home className="w-5 h-5" />
          <span className="text-[10px] font-bold">Trang chủ</span>
        </Link>
        <Link href="/health-records" className="flex flex-col items-center gap-1 text-[#6d7a77]">
          <FileText className="w-5 h-5" />
          <span className="text-[10px] font-bold">Sức khỏe</span>
        </Link>
        <Link href="/settings/profile" className="flex flex-col items-center gap-1 text-[#00685f]">
          <User className="w-5 h-5 fill-current" />
          <span className="text-[10px] font-bold">Hồ sơ</span>
        </Link>
        <Link href="/settings" className="flex flex-col items-center gap-1 text-[#6d7a77]">
          <Settings className="w-5 h-5" />
          <span className="text-[10px] font-bold">Cài đặt</span>
        </Link>
      </nav>
    </div>
  );
}
