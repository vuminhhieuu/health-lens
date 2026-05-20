"use client";

import Link from "next/link";

import { useAuthBootstrap } from "@/hooks/useAuthBootstrap";
import { useAuthStore } from "@/stores/authStore";

type LandingGuestCtasProps = {
  className?: string;
};

const primaryClass =
  "inline-flex min-h-12 items-center justify-center rounded-lg bg-[#00685f] px-6 text-sm font-semibold text-white transition hover:bg-[#005049] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f] sm:text-base";

function CtaSkeleton({ className }: LandingGuestCtasProps) {
  return (
    <div
      className={`flex flex-col gap-4 sm:flex-row sm:items-center ${className}`.trim()}
      aria-hidden="true"
      data-testid="landing-guest-ctas-skeleton"
    >
      <div className="h-12 w-44 animate-pulse rounded-lg bg-[#e1ebe8]" />
      <div className="h-5 w-52 animate-pulse rounded bg-[#e1ebe8]" />
    </div>
  );
}

export function LandingGuestCtas({ className = "" }: LandingGuestCtasProps) {
  const isLoading = useAuthBootstrap();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  if (isLoading) {
    return <CtaSkeleton className={className} />;
  }

  if (isAuthenticated) {
    return null;
  }

  return (
    <div
      className={`flex flex-col gap-4 sm:flex-row sm:items-center ${className}`.trim()}
      data-testid="landing-guest-ctas"
    >
      <Link href="/register" className={primaryClass}>
        Bắt đầu miễn phí
      </Link>
      <p className="text-sm text-[#4e6360]">
        Đã có tài khoản?{" "}
        <Link
          href="/login"
          className="font-semibold text-[#00685f] underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
        >
          Đăng nhập
        </Link>
      </p>
    </div>
  );
}
