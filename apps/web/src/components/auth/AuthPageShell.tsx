import type { ReactNode } from "react";

import { AuthPageFooter } from "@/components/auth/AuthPageFooter";
import { AuthPageHeader } from "@/components/auth/AuthPageHeader";
import { authenticatedContentShell } from "@/lib/layout/shell";

type AuthPageShellProps = {
  children: ReactNode;
  headerActions?: ReactNode;
  footer?: boolean;
  decoration?: ReactNode;
  /** Wider card for register; default matches login/forgot-password. */
  cardClassName?: string;
};

const defaultCardClassName =
  "w-full max-w-[460px] rounded-xl bg-white p-8 shadow-[0_8px_32px_rgba(18,30,28,0.08)] md:p-10";

const authMainClassName = `${authenticatedContentShell} flex min-h-screen items-center justify-center pb-12 pt-24`;

export function AuthPageShell({
  children,
  headerActions,
  footer = false,
  decoration,
  cardClassName = defaultCardClassName,
}: AuthPageShellProps) {
  return (
    <div className="min-h-screen bg-gradient-to-br from-[#effcf9] via-[#e9f6f3] to-[#d8e5e2] text-[#121e1c]">
      <AuthPageHeader actions={headerActions} />
      <main className={authMainClassName}>
        <section className={cardClassName}>
          {children}
          {footer ? <AuthPageFooter /> : null}
        </section>
      </main>
      {decoration}
    </div>
  );
}
