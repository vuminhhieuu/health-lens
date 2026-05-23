import type { LucideIcon } from "lucide-react";
import { Shield } from "lucide-react";
import type { ReactNode } from "react";

import {
  SettingsAccountNav,
  type SettingsAccountNavActive,
} from "./SettingsAccountNav";
import { settingsCardClassName } from "./settingsStyles";

type SettingsAccountSidebarProps = {
  active?: SettingsAccountNavActive;
  title?: string;
  icon?: LucideIcon;
  footer?: ReactNode;
};

export function SettingsAccountSidebar({
  active,
  title = "Bảo mật & tài khoản",
  icon: Icon = Shield,
  footer,
}: SettingsAccountSidebarProps) {
  return (
    <section className={settingsCardClassName}>
      <div className="mb-8 flex items-center gap-3">
        <Icon className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
        <h2 className="text-xl font-bold text-[#121e1c]">{title}</h2>
      </div>
      <SettingsAccountNav active={active} />
      {footer ? (
        <div className="mt-6 border-t border-[#bcc9c6]/20 pt-6">{footer}</div>
      ) : null}
    </section>
  );
}
