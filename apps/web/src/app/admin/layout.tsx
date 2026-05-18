"use client";

import { useEffect, useCallback, useRef, useState } from "react";
import { useRouter, usePathname } from "next/navigation";
import Link from "next/link";
import {
  ShieldCheck, Database, ClipboardList, BarChart3, LogOut,
} from "lucide-react";

const ADMIN_SESSION_TIMEOUT_MS = 15 * 60 * 1000; // 15 minutes

type AdminNavItem = {
  name: string;
  href: string;
  icon: typeof BarChart3;
  exact: boolean;
  /** Pathname bắt đầu bằng một trong các chuỗi này vẫn coi mục này là active (vd. /import dưới reference-data, không gồm /approvals). */
  alsoActiveWhenPathnameStartsWith?: string[];
};

const adminNavItems: AdminNavItem[] = [
  { name: "Thống kê", href: "/admin", icon: BarChart3, exact: true },
  {
    name: "Dữ liệu tham chiếu",
    href: "/admin/reference-data",
    icon: Database,
    exact: true,
    alsoActiveWhenPathnameStartsWith: ["/admin/reference-data/import"],
  },
  { name: "Phê duyệt", href: "/admin/reference-data/approvals", icon: ShieldCheck, exact: false },
  { name: "Nhật ký hoạt động", href: "/admin/audit-log", icon: ClipboardList, exact: false },
];

export default function AdminLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const router = useRouter();
  const pathname = usePathname();
  const [isReady, setIsReady] = useState(false);
  const lastActivityRef = useRef(0);

  // Initialize last activity on mount
  useEffect(() => {
    lastActivityRef.current = Date.now();
  }, []);

  // Skip layout auth check for admin login page
  const isLoginPage = pathname === "/admin/login";

  const getAdminToken = useCallback(() => {
    if (typeof window === "undefined") return null;
    return sessionStorage.getItem("admin_access_token");
  }, []);

  const handleLogout = useCallback(() => {
    if (typeof window !== "undefined") {
      sessionStorage.removeItem("admin_access_token");
      router.replace("/admin/login");
    }
  }, [router]);

  // Check admin session on mount
  useEffect(() => {
    if (isLoginPage) return;

    const token = getAdminToken();
    if (!token) {
      router.replace("/admin/login");
      return;
    }

    setTimeout(() => setIsReady(true), 0);
  }, [isLoginPage, getAdminToken, router]);

  // Session timeout: auto-redirect after 15 minutes inactivity
  useEffect(() => {
    if (isLoginPage) return;

    const resetTimer = () => {
      lastActivityRef.current = Date.now();
    };

    const checkTimeout = () => {
      const elapsed = Date.now() - lastActivityRef.current;
      if (elapsed >= ADMIN_SESSION_TIMEOUT_MS) {
        handleLogout();
      }
    };

    // Listen for user activity
    const events = ["mousedown", "keydown", "scroll", "touchstart"];
    events.forEach((event) => window.addEventListener(event, resetTimer));

    const interval = setInterval(checkTimeout, 30_000); // Check every 30s

    return () => {
      events.forEach((event) => window.removeEventListener(event, resetTimer));
      clearInterval(interval);
    };
  }, [isLoginPage, handleLogout]);

  // Login page renders without layout chrome
  if (isLoginPage) {
    return <>{children}</>;
  }

  if (!isReady) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-slate-50">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-teal-600" />
      </div>
    );
  }

  return (
    <div className="bg-slate-50 text-slate-900 min-h-screen font-sans">
      {/* Top Bar */}
      <header className="fixed top-0 w-full z-50 bg-white/90 backdrop-blur-md border-b border-slate-200 px-6 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <ShieldCheck className="h-6 w-6 text-teal-600" />
          <span className="text-lg font-bold tracking-tight text-slate-900">
            HealthLens <span className="text-teal-600">Admin</span>
          </span>
        </div>

        <button
          onClick={handleLogout}
          className="flex items-center gap-2 text-sm text-slate-600 hover:text-teal-600 transition px-3 py-2 rounded-lg hover:bg-slate-100"
        >
          <LogOut className="h-4 w-4" />
          Đăng xuất
        </button>
      </header>

      <div className="flex pt-14 min-h-screen">
        {/* Sidebar */}
        <aside className="hidden md:flex h-screen w-64 flex-col fixed left-0 bg-white p-6 gap-2 border-r border-slate-200 shadow-sm">
          <div className="mb-8 px-2">
            <p className="text-xs font-bold uppercase tracking-widest text-slate-400 mb-4">
              Quản trị
            </p>
          </div>

          <nav className="flex flex-col gap-1 flex-grow">
            {adminNavItems.map((item) => {
              const Icon = item.icon;
              const alsoActive = item.alsoActiveWhenPathnameStartsWith?.some(
                (prefix) =>
                  pathname === prefix || (pathname != null && pathname.startsWith(`${prefix}/`)),
              );
              const isActive = alsoActive
                ? true
                : item.exact
                  ? pathname === item.href
                  : pathname === item.href || pathname?.startsWith(`${item.href}/`);

              return (
                <Link
                  key={item.name}
                  href={item.href}
                  className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all ${
                    isActive
                      ? "bg-teal-50 text-teal-700 font-semibold border-r-4 border-teal-600"
                      : "text-slate-600 hover:text-slate-900 hover:bg-slate-100"
                  }`}
                >
                  <Icon className="w-5 h-5" />
                  <span className="font-medium">{item.name}</span>
                </Link>
              );
            })}
          </nav>

          <div className="mt-auto text-xs text-slate-400 px-4 py-2">
            Session: 15 phút
          </div>
        </aside>

        {/* Main Content */}
        <div className="flex-grow md:ml-64 flex flex-col w-full h-full p-6">
          {children}
        </div>
      </div>
    </div>
  );
}
