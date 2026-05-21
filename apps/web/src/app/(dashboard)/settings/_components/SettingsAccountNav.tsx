import type { LucideIcon } from "lucide-react";
import { Info, KeyRound, Shield, UserRound } from "lucide-react";
import Link from "next/link";

export type SettingsAccountNavActive = "change-password" | "privacy" | "profile" | "about";

type NavItemProps = {
  href?: string;
  icon: LucideIcon;
  label: string;
  active?: boolean;
  disabled?: boolean;
};

function NavItem({ href, icon: Icon, label, active, disabled }: NavItemProps) {
  const content = (
    <div className={`flex items-center gap-3 py-2 ${disabled ? "opacity-60" : "group"}`}>
      <div
        className={`rounded-lg p-2 transition-colors ${
          active
            ? "bg-[#00685f] text-white"
            : disabled
              ? "bg-[#e9f6f3] text-[#6d7a77]"
              : "bg-[#e9f6f3] text-[#00685f] group-hover:bg-[#008378] group-hover:text-white"
        }`}
      >
        <Icon className="h-4 w-4" aria-hidden="true" />
      </div>
      <span
        className={`font-medium ${
          active ? "text-[#00685f]" : disabled ? "text-[#6d7a77]" : "text-[#3d4947] group-hover:text-[#00685f]"
        }`}
      >
        {label}
      </span>
    </div>
  );

  if (disabled || !href) {
    return (
      <div role="listitem" aria-disabled="true">
        {content}
      </div>
    );
  }

  return (
    <Link href={href} aria-current={active ? "page" : undefined}>
      {content}
    </Link>
  );
}

type SettingsAccountNavProps = {
  active: SettingsAccountNavActive;
};

export function SettingsAccountNav({ active }: SettingsAccountNavProps) {
  return (
    <nav className="space-y-2" aria-label="Lối tắt tài khoản & bảo mật">
      <NavItem
        href="/settings/change-password"
        icon={KeyRound}
        label="Đổi mật khẩu"
        active={active === "change-password"}
      />
      <NavItem
        href="/settings/privacy"
        icon={Shield}
        label="Quyền riêng tư"
        active={active === "privacy"}
      />
      <NavItem
        href="/settings/profile"
        icon={UserRound}
        label="Hồ sơ của tôi"
        active={active === "profile"}
      />
      <NavItem
        href="/settings/about"
        icon={Info}
        label="Giới thiệu"
        active={active === "about"}
      />
      <NavItem icon={Shield} label="Xác thực hai yếu tố (sắp có)" disabled />
    </nav>
  );
}
