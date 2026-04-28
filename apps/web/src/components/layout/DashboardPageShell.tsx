"use client";

import type { ReactNode } from "react";

type DashboardPageShellProps = {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
  children: ReactNode;
};

export function DashboardPageShell({ title, subtitle, actions, children }: DashboardPageShellProps) {
  return (
    <main className="flex flex-col gap-8 px-6 py-10 pb-28 md:pb-10">
      <header className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-[#005049]">{title}</h1>
          {subtitle ? <p className="mt-2 max-w-2xl text-sm text-[#4e6360]">{subtitle}</p> : null}
        </div>
        {actions ? <div className="shrink-0">{actions}</div> : null}
      </header>
      {children}
    </main>
  );
}
