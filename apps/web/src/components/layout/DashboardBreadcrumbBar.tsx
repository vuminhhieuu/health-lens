"use client";

import Link from "next/link";
import { ChevronRight } from "lucide-react";

import {
  authenticatedContentShell,
  authenticatedStickyBelowHeaderClass,
  dashboardBreadcrumbBarSurfaceClassName,
  dashboardBreadcrumbNavClassName,
} from "@/lib/layout/shell";

export type DashboardBreadcrumbItem = {
  label: string;
  href?: string;
};

type DashboardBreadcrumbBarProps = {
  items: DashboardBreadcrumbItem[];
  contentShellClassName?: string;
};

export function DashboardBreadcrumbBar({
  items,
  contentShellClassName = authenticatedContentShell,
}: DashboardBreadcrumbBarProps) {
  if (items.length === 0) {
    return null;
  }

  return (
    <div
      className={`${authenticatedStickyBelowHeaderClass} ${dashboardBreadcrumbBarSurfaceClassName}`}
    >
      <nav
        aria-label="Breadcrumb"
        className={`${contentShellClassName} ${dashboardBreadcrumbNavClassName}`}
      >
        <ol className="flex flex-wrap items-center gap-2">
          {items.map((item, index) => {
            const isLast = index === items.length - 1;

            return (
              <li
                key={`${item.label}-${index}`}
                className="inline-flex min-h-6 items-center gap-2"
              >
                {item.href && !isLast ? (
                  <Link
                    href={item.href}
                    className="rounded-md px-0.5 text-[#6d7a77] transition-colors hover:text-[#00685f] hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
                  >
                    {item.label}
                  </Link>
                ) : (
                  <span
                    className={
                      isLast
                        ? "font-semibold text-[#005049]"
                        : "text-[#6d7a77]"
                    }
                    aria-current={isLast ? "page" : undefined}
                  >
                    {item.label}
                  </span>
                )}
                {!isLast ? (
                  <ChevronRight
                    className="h-4 w-4 shrink-0 text-[#9aaba7]"
                    aria-hidden="true"
                  />
                ) : null}
              </li>
            );
          })}
        </ol>
      </nav>
    </div>
  );
}
