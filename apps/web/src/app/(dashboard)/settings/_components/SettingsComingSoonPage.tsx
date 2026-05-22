import Link from "next/link";
import type { LucideIcon } from "lucide-react";

import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { breadcrumbFromSettings } from "@/lib/layout/dashboardBreadcrumbTrails";

import { SettingsPageCard, SettingsPageIntro } from "./SettingsPageCard";

type SettingsComingSoonPageProps = {
  title: string;
  description: string;
  icon: LucideIcon;
};

export function SettingsComingSoonPage({
  title,
  description,
  icon: Icon,
}: SettingsComingSoonPageProps) {
  return (
    <DashboardPageShell
      title={title}
      subtitle={description}
      breadcrumbs={breadcrumbFromSettings(title)}
    >
      <div className="mx-auto w-full max-w-3xl">
        <SettingsPageCard>
          <SettingsPageIntro icon={Icon} eyebrow="Sắp có" title="Tính năng đang được chuẩn bị" description={description} />
          <Link
            href="/settings"
            className="mt-8 inline-flex min-h-12 items-center rounded-full bg-[#00685f] px-5 text-sm font-bold text-white transition hover:bg-[#008378] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005049]"
          >
            Quay lại Cài đặt
          </Link>
        </SettingsPageCard>
      </div>
    </DashboardPageShell>
  );
}
