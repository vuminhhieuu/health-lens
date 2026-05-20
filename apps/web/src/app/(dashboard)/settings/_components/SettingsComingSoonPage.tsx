import Link from "next/link";
import type { LucideIcon } from "lucide-react";

import { DashboardPageShell } from "@/components/layout/DashboardPageShell";

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
      breadcrumbs={[
        { label: "Trang chủ", href: "/home" },
        { label: "Cài đặt", href: "/settings" },
        { label: title },
      ]}
    >
      <section className="rounded-xl border border-[#bcc9c6]/30 bg-white p-6 shadow-sm sm:p-8">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-start">
          <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-xl bg-[#e9f6f3] text-[#00685f]">
            <Icon className="h-7 w-7" aria-hidden="true" />
          </div>
          <div className="min-w-0">
            <p className="text-sm font-bold uppercase tracking-wider text-[#00685f]">Sắp có</p>
            <h2 className="mt-2 text-2xl font-bold text-[#121e1c]">Tính năng đang được chuẩn bị</h2>
            <p className="mt-3 max-w-2xl text-sm leading-6 text-[#4e6360]">
              Mục này sẽ được hoàn thiện trong các story tiếp theo. Bạn có thể quay lại trung tâm
              cài đặt để tiếp tục quản lý tài khoản.
            </p>
            <Link
              href="/settings"
              className="mt-6 inline-flex min-h-12 items-center rounded-full bg-[#00685f] px-5 text-sm font-bold text-white transition hover:bg-[#008378] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005049]"
            >
              Quay lại Cài đặt
            </Link>
          </div>
        </div>
      </section>
    </DashboardPageShell>
  );
}
