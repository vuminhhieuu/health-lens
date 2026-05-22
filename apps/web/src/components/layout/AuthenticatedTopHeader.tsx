"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { HelpCircle, Home, LogOut, Settings, User } from "lucide-react";

import { NotificationBell } from "@/components/features/notifications/NotificationBell";

import {
  authenticatedHeaderInnerClassName,
  authenticatedHeaderSurfaceClassName,
} from "@/lib/layout/shell";
import SafeImage from "@/components/ui/SafeImage";

type AuthenticatedTopHeaderProps = {
  avatarInitial: string;
  avatarUrl?: string | null;
  brandHref?: string;
  onLogout: () => void | Promise<void>;
  className?: string;
  innerClassName?: string;
};

export function AuthenticatedTopHeader({
  avatarInitial,
  avatarUrl,
  brandHref = "/",
  onLogout,
  className = `fixed top-0 z-50 w-full ${authenticatedHeaderSurfaceClassName}`,
  innerClassName = authenticatedHeaderInnerClassName,
}: AuthenticatedTopHeaderProps) {
  const [isAvatarMenuOpen, setIsAvatarMenuOpen] = useState(false);
  const avatarMenuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        avatarMenuRef.current &&
        !avatarMenuRef.current.contains(event.target as Node)
      ) {
        setIsAvatarMenuOpen(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <header className={className}>
      <div className={innerClassName}>
        <Link
          href={brandHref}
          className="text-2xl font-bold tracking-tight text-[#005049]"
        >
          HealthLens
        </Link>

        <div className="flex shrink-0 items-center gap-4">
          <NotificationBell />
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
              type="button"
              onClick={() => setIsAvatarMenuOpen((value) => !value)}
              className="flex h-11 w-11 shrink-0 items-center justify-center overflow-hidden rounded-full border-2 border-[#89f5e7] bg-[#d8e5e2] text-sm font-black text-[#00685f] transition-all focus:outline-none focus:ring-2 focus:ring-[#00685f]/50"
              aria-label="Mở menu tài khoản"
              aria-expanded={isAvatarMenuOpen}
              aria-haspopup="true"
            >
              {avatarUrl ? (
                <SafeImage
                  raw
                  src={avatarUrl}
                  alt="Ảnh đại diện"
                  width={44}
                  height={44}
                  className="h-11 w-11 object-cover"
                />
              ) : (
                <span aria-hidden="true">{avatarInitial}</span>
              )}
            </button>

            {isAvatarMenuOpen ? (
              <div className="animate-in fade-in zoom-in-95 absolute right-0 z-50 mt-2 w-48 origin-top-right rounded-2xl border border-[#bcc9c6]/20 bg-white py-2 shadow-xl duration-100">
                <Link
                  href="/home"
                  onClick={() => setIsAvatarMenuOpen(false)}
                  className="flex items-center gap-3 px-4 py-2.5 text-sm font-medium text-[#3d4947] transition-colors hover:bg-[#e9f6f3]"
                >
                  <Home className="h-4 w-4" />
                  Trang chủ
                </Link>
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
                <div className="my-1 h-px bg-[#bcc9c6]/20" aria-hidden="true" />
                <button
                  type="button"
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
