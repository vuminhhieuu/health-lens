"use client";

import type { ReactNode } from "react";

import { authenticatedContentShell } from "@/lib/layout/shell";

import {
  DashboardBreadcrumbBar,
  type DashboardBreadcrumbItem,
} from "./DashboardBreadcrumbBar";

type DashboardPageShellProps = {
  title: string;
  subtitle?: string;
  breadcrumbs?: DashboardBreadcrumbItem[];
  actions?: ReactNode;
  children: ReactNode;
  maxWidthClassName?: string;
};

export function DashboardPageShell({
  title,
  subtitle,
  breadcrumbs = [],
  actions,
  children,
  maxWidthClassName = "",
}: DashboardPageShellProps) {
  const hasBreadcrumbs = breadcrumbs.length > 0;
  const contentShellClassName = maxWidthClassName
    ? `mx-auto w-full ${maxWidthClassName} px-6`
    : authenticatedContentShell;

  return (
    <main className="min-h-screen w-full bg-[#effcf9]">
      {hasBreadcrumbs ? (
        <DashboardBreadcrumbBar
          items={breadcrumbs}
          contentShellClassName={contentShellClassName}
        />
      ) : null}

      <div
        className={`${contentShellClassName} pb-10 ${hasBreadcrumbs ? "pt-8" : "pt-10"}`}
      >
        <header className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div className="min-w-0">
            <h1 className="text-3xl font-bold tracking-tight text-[#121e1c] lg:text-4xl">
              {title}
            </h1>
            {subtitle ? (
              <p className="mt-2 text-sm text-[#4e6360] lg:text-base">{subtitle}</p>
            ) : null}
          </div>

          {actions ? <div className="w-full lg:w-auto">{actions}</div> : null}
        </header>
        <div className="mt-8">{children}</div>
      </div>
    </main>
  );
}
