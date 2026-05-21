import type { LucideIcon } from "lucide-react";
import {
  ChevronRight,
  ExternalLink,
  FileText,
  HelpCircle,
  Info,
  Shield,
} from "lucide-react";
import Link from "next/link";

import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { APP_DISPLAY_NAME, getAppBuildLabel, getAppVersionLabel } from "@/lib/appVersion";
import { PUBLIC_SUPPORT_HREF } from "@/lib/supportContact";

import { SettingsAccountNav } from "../_components/SettingsAccountNav";
import { SettingsDirectContactCard } from "../_components/SettingsDirectContactCard";
import { settingsCardClassName, settingsTipCardClassName } from "../_components/settingsStyles";

function AboutMetricTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-[#bcc9c6]/25 bg-[#f8fcfb] px-4 py-3.5">
      <p className="text-xs font-bold uppercase tracking-wide text-[#6d7a77]">{label}</p>
      <p className="mt-1.5 text-base font-bold tabular-nums text-[#121e1c]">{value}</p>
    </div>
  );
}

function ResourceLinkRow({
  href,
  label,
  description,
  icon: Icon,
  openInNewTab = false,
}: {
  href: string;
  label: string;
  description: string;
  icon: LucideIcon;
  openInNewTab?: boolean;
}) {
  const rowClassName =
    "group flex gap-4 rounded-xl bg-[#e9f6f3] p-4 transition hover:bg-[#d8ebe6]";

  const content = (
    <>
      <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-white text-[#00685f] shadow-sm ring-1 ring-[#bcc9c6]/25">
        <Icon className="h-5 w-5" aria-hidden="true" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="flex items-start justify-between gap-2">
          <span className="font-bold text-[#121e1c] group-hover:text-[#00685f]">
            {label}
            {openInNewTab ? <span className="sr-only"> (mở trong tab mới)</span> : null}
          </span>
          {openInNewTab ? (
            <ExternalLink
              className="mt-0.5 h-4 w-4 shrink-0 text-[#00685f] opacity-70 group-hover:opacity-100"
              aria-hidden="true"
            />
          ) : (
            <ChevronRight
              className="mt-0.5 h-4 w-4 shrink-0 text-[#00685f] opacity-70 group-hover:opacity-100"
              aria-hidden="true"
            />
          )}
        </span>
        <span className="mt-1 block text-sm leading-6 text-[#4e6360]">{description}</span>
      </span>
    </>
  );

  return (
    <li>
      {openInNewTab ? (
        <a href={href} target="_blank" rel="noopener noreferrer" className={rowClassName}>
          {content}
        </a>
      ) : (
        <Link href={href} className={rowClassName}>
          {content}
        </Link>
      )}
    </li>
  );
}

export default function AboutSettingsPage() {
  const versionLabel = getAppVersionLabel();
  const buildLabel = getAppBuildLabel();

  return (
    <DashboardPageShell
      title="Giới thiệu"
      subtitle="Phiên bản ứng dụng, tài liệu pháp lý và kênh hỗ trợ khi bạn cần trợ giúp."
      breadcrumbs={[
        { label: "Trang chủ", href: "/home" },
        { label: "Cài đặt", href: "/settings" },
        { label: "Giới thiệu" },
      ]}
    >
      <div className="grid grid-cols-1 items-start gap-8 lg:grid-cols-3">
        <div className="space-y-8 lg:col-span-2">
          <section className={`${settingsCardClassName} relative overflow-hidden`}>
            <div className="absolute top-0 right-0 -mt-16 -mr-16 h-32 w-32 rounded-bl-full bg-[#00685f]/5" />

            <div className="relative mb-8 flex flex-col gap-6 md:flex-row md:items-center">
              <div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-2xl bg-[#e9f6f3] text-[#00685f] ring-4 ring-[#e9f6f3] shadow-md">
                <Info className="h-10 w-10" aria-hidden="true" />
              </div>
              <div className="min-w-0 flex-grow">
                <h2 className="text-2xl font-bold text-[#121e1c]">{APP_DISPLAY_NAME}</h2>
                <p className="mt-2 text-sm leading-6 text-[#6d7a77]">
                  Nền tảng theo dõi và giải thích kết quả sức khỏe cá nhân. Thông tin phiên bản giúp
                  bạn báo lỗi chính xác khi liên hệ hỗ trợ.
                </p>
              </div>
            </div>

            <div className={`grid gap-3 ${buildLabel ? "sm:grid-cols-2" : "max-w-sm"}`}>
              <AboutMetricTile label="Phiên bản" value={versionLabel} />
              {buildLabel ? <AboutMetricTile label="Bản build" value={buildLabel} /> : null}
            </div>
          </section>

          <section className={settingsCardClassName}>
            <div className="mb-6 flex items-center gap-3">
              <Shield className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
              <h2 className="text-xl font-bold text-[#121e1c]">Tài liệu & trợ giúp công khai</h2>
            </div>
            <p className="mb-6 text-sm leading-6 text-[#6d7a77]">
              Chính sách và điều khoản mở tab mới; trung tâm trợ giúp mở trong cùng ứng dụng để bạn
              quay lại Cài đặt dễ hơn.
            </p>
            <ul className="space-y-3">
              <ResourceLinkRow
                href="/privacy"
                label="Chính sách quyền riêng tư"
                description="Cách chúng tôi thu thập và bảo vệ dữ liệu của bạn."
                icon={Shield}
                openInNewTab
              />
              <ResourceLinkRow
                href="/terms"
                label="Điều khoản sử dụng"
                description="Quy định sử dụng dịch vụ và trách nhiệm pháp lý."
                icon={FileText}
                openInNewTab
              />
              <ResourceLinkRow
                href={PUBLIC_SUPPORT_HREF}
                label="Trung tâm trợ giúp"
                description="Hướng dẫn nhanh, FAQ và kênh liên hệ khi gặp sự cố."
                icon={HelpCircle}
              />
            </ul>
          </section>
        </div>

        <div className="space-y-8">
          <section className={settingsCardClassName}>
            <div className="mb-8 flex items-center gap-3">
              <Info className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
              <h2 className="text-xl font-bold text-[#121e1c]">Bảo mật & tài khoản</h2>
            </div>
            <SettingsAccountNav active="about" />
          </section>

          <section className={settingsTipCardClassName}>
            <div className="relative z-10">
              <div className="mb-6 flex items-center gap-3">
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-white/50 backdrop-blur-md">
                  <HelpCircle className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
                </div>
                <h3 className="text-xl font-bold text-[#121e1c]">Báo lỗi hiệu quả hơn</h3>
              </div>
              <p className="text-sm leading-relaxed text-[#274d48]">
                Khi gửi email hỗ trợ, hãy ghi rõ phiên bản{" "}
                <strong className="font-bold text-[#121e1c]">{versionLabel}</strong>
                {buildLabel ? (
                  <>
                    {" "}
                    và mã build <strong className="font-bold text-[#121e1c]">{buildLabel}</strong>
                  </>
                ) : null}{" "}
                cùng mô tả các bước bạn đã thực hiện.
              </p>
            </div>
            <div
              className="pointer-events-none absolute -bottom-10 -right-10 h-40 w-40 rounded-full bg-[#00685f]/10 blur-3xl"
              aria-hidden="true"
            />
          </section>

          <SettingsDirectContactCard
            mailtoSubject={`${APP_DISPLAY_NAME} - Hỗ trợ (v${versionLabel})`}
          />
        </div>
      </div>
    </DashboardPageShell>
  );
}
