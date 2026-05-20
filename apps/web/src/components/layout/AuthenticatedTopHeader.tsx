"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { Bell, HelpCircle, LogOut, Search, Settings, User } from "lucide-react";

type HeaderNavItem = {
  name: string;
  href: string;
  isActive: boolean;
};

type AuthenticatedTopHeaderProps = {
  navItems: HeaderNavItem[];
  avatarInitial: string;
  avatarUrl?: string | null;
  brandHref?: string;
  onLogout: () => void | Promise<void>;
  className?: string;
};

export function AuthenticatedTopHeader({
  navItems,
  avatarInitial,
  avatarUrl,
  brandHref = "/",
  onLogout,
  className = "fixed top-0 z-50 w-full border-b border-[#bcc9c6]/20 bg-[#e9f6f3]/80 px-6 py-3 shadow-sm backdrop-blur-md print:hidden",
}: AuthenticatedTopHeaderProps) {
  const [isAvatarMenuOpen, setIsAvatarMenuOpen] = useState(false);
  const avatarMenuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (avatarMenuRef.current && !avatarMenuRef.current.contains(event.target as Node)) {
        setIsAvatarMenuOpen(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <header className={className}>
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-8">
          <Link href={brandHref} className="text-2xl font-bold tracking-tight text-[#005049]">
            HealthLens
          </Link>
          <nav className="hidden items-center gap-6 md:flex">
            {navItems.map((item) =>
              item.isActive ? (
                <Link
                  key={item.name}
                  href={item.href}
                  className="rounded-lg bg-white/50 px-3 py-1 font-semibold text-[#00685f]"
                >
                  {item.name}
                </Link>
              ) : (
                <Link
                  key={item.name}
                  href={item.href}
                  className="rounded-lg px-3 py-1 font-medium text-[#6d7a77] transition-colors hover:bg-[#e9f6f3]/50"
                >
                  {item.name}
                </Link>
              ),
            )}
          </nav>
        </div>

        <div className="flex items-center gap-4">
          <div className="relative hidden sm:block">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#6d7a77]" />
            <input
              type="text"
              placeholder="Tìm kiếm..."
              className="w-64 rounded-full bg-[#e9f6f3] py-2 pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-[#00685f]/20"
            />
          </div>
          <button
            type="button"
            title="Mở thông báo"
            aria-label="Mở thông báo"
            className="relative rounded-full p-2 text-[#3d4947] transition-colors hover:bg-[#e9f6f3]"
          >
            <Bell className="h-5 w-5" />
            <span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-[#924628]"></span>
          </button>
          <Link
            href="/questions"
            title="Mở trang thắc mắc"
            aria-label="Mở trang thắc mắc"
            className="flex min-h-12 min-w-12 items-center justify-center rounded-full p-2 text-[#3d4947] transition-colors hover:bg-[#e9f6f3] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
          >
            <HelpCircle className="h-5 w-5" />
          </Link>
          <div className="relative" ref={avatarMenuRef}>
            <button
              onClick={() => setIsAvatarMenuOpen((value) => !value)}
              className="flex h-10 w-10 items-center justify-center overflow-hidden rounded-full border-2 border-[#89f5e7] bg-[#d8e5e2] text-sm font-black text-[#00685f] transition-all focus:outline-none focus:ring-2 focus:ring-[#00685f]/50"
              aria-label="Mở menu tài khoản"
            >
              {avatarUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img alt="Ảnh đại diện" className="h-full w-full object-cover" src={avatarUrl} />
              ) : (
                <span aria-hidden="true">{avatarInitial}</span>
              )}
            </button>

            {isAvatarMenuOpen ? (
              <div className="animate-in fade-in zoom-in-95 absolute right-0 z-50 mt-2 w-48 origin-top-right rounded-2xl border border-[#bcc9c6]/20 bg-white py-2 shadow-xl duration-100">
                <Link
                  href="/settings/profile"
                  onClick={() => setIsAvatarMenuOpen(false)}
                  className="flex items-center gap-3 px-4 py-2.5 text-sm font-medium text-[#3d4947] transition-colors hover:bg-[#e9f6f3]"
                >
                  <User className="h-4 w-4" />
                  Hồ sơ cá nhân
                </Link>
                <Link
                  href="/settings"
                  onClick={() => setIsAvatarMenuOpen(false)}
                  className="flex items-center gap-3 px-4 py-2.5 text-sm font-medium text-[#3d4947] transition-colors hover:bg-[#e9f6f3]"
                >
                  <Settings className="h-4 w-4" />
                  Cài đặt
                </Link>
                <div className="my-1 h-px bg-[#bcc9c6]/20"></div>
                <button
                  onClick={() => {
                    setIsAvatarMenuOpen(false);
                    void onLogout();
                  }}
                  className="flex w-full items-center gap-3 px-4 py-2.5 text-sm font-medium text-[#ba1a1a] transition-colors hover:bg-[#ffdad6]/40"
                >
                  <LogOut className="h-4 w-4" />
                  Đăng xuất
                </button>
              </div>
            ) : null}
          </div>
        </div>
      </div>
    </header>
  );
}
