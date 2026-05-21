"use client";

import Link from "next/link";
import type { ReactNode } from "react";

import { authenticatedContentShell } from "@/lib/layout/shell";

type BreadcrumbItem = {
  label: string;
  href?: string;
};

type DashboardPageShellProps = {
  title: string;
  subtitle?: string;
  breadcrumbs?: BreadcrumbItem[];
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
        <div className="sticky top-16 z-30 border-b border-[#bcc9c6]/25 bg-[#effcf9]/95 backdrop-blur">
          <nav className={`${contentShellClassName} py-3 text-sm font-medium text-[#6d7a77]`}>
            <div className="flex flex-wrap items-center gap-1">
              {breadcrumbs.map((item, index) => (
                <span key={`${item.label}-${index}`} className="inline-flex items-center gap-1">
                  {item.href ? (
                    <Link href={item.href} className="hover:text-[#00685f] hover:underline">
                      {item.label}
                    </Link>
                  ) : (
                    <span className="text-[#3d4947]">{item.label}</span>
                  )}
                  {index < breadcrumbs.length - 1 ? <span>/</span> : null}
                </span>
              ))}
            </div>
          </nav>
        </div>
      ) : null}

      <div className={`${contentShellClassName} pb-10 ${hasBreadcrumbs ? "pt-6" : "pt-10"}`}>
        <header className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div className="min-w-0">
            <h1 className="text-3xl font-bold tracking-tight text-[#121e1c] lg:text-4xl">{title}</h1>
            {subtitle ? <p className="mt-2 text-sm text-[#4e6360] lg:text-base">{subtitle}</p> : null}
          </div>

          {actions ? <div className="w-full lg:w-auto">{actions}</div> : null}
        </header>
        <div className="mt-8">{children}</div>
      </div>
    </main>
  );
}
