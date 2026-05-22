"use client";

import { CONSENT_VERSION } from "@healthlens/shared/constants";
import { useQuery } from "@tanstack/react-query";
import type { LucideIcon } from "lucide-react";
import {
  AlertCircle,
  CheckCircle2,
  ExternalLink,
  FileText,
  Scale,
  Shield,
  ShieldCheck,
  Trash2,
} from "lucide-react";
import Link from "next/link";

import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { breadcrumbFromSettings } from "@/lib/layout/dashboardBreadcrumbTrails";
import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { useAuthStore } from "@/stores/authStore";

import { SettingsAccountNav } from "../_components/SettingsAccountNav";
import { SettingsDirectContactCard } from "../_components/SettingsDirectContactCard";
import { settingsCardClassName, settingsTipCardClassName } from "../_components/settingsStyles";

type ConsentStatus = {
  consentGiven: boolean;
  consentVersion?: string | null;
  consentedAt?: string | null;
};

function formatConsentDate(value?: string | null) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function LegalDocumentRow({
  href,
  label,
  description,
  icon: Icon,
}: {
  href: string;
  label: string;
  description: string;
  icon: LucideIcon;
}) {
  return (
    <li>
      <a
        href={href}
        target="_blank"
        rel="noopener noreferrer"
        className="group flex gap-4 rounded-xl bg-[#e9f6f3] p-4 transition hover:bg-[#d8ebe6]"
      >
        <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-white text-[#00685f] shadow-sm ring-1 ring-[#bcc9c6]/25">
          <Icon className="h-5 w-5" aria-hidden="true" />
        </span>
        <span className="min-w-0 flex-1">
          <span className="flex items-start justify-between gap-2">
            <span className="font-bold text-[#121e1c] group-hover:text-[#00685f]">
              {label}
              <span className="sr-only"> (mở trong tab mới)</span>
            </span>
            <ExternalLink
              className="mt-0.5 h-4 w-4 shrink-0 text-[#00685f] opacity-70 group-hover:opacity-100"
              aria-hidden="true"
            />
          </span>
          <span className="mt-1 block text-sm leading-6 text-[#4e6360]">{description}</span>
        </span>
      </a>
    </li>
  );
}

type ConsentVisualState = "active" | "stale" | "missing" | "loading" | "error";

function consentVisualState(
  isLoading: boolean,
  isError: boolean,
  consentGiven: boolean,
  hasStaleConsent: boolean,
): ConsentVisualState {
  if (isLoading) return "loading";
  if (isError) return "error";
  if (consentGiven) return "active";
  if (hasStaleConsent) return "stale";
  return "missing";
}

const consentStatusBadge: Record<
  Exclude<ConsentVisualState, "loading" | "error">,
  { label: string; className: string; icon: typeof CheckCircle2 }
> = {
  active: {
    label: "Đang hiệu lực",
    className: "bg-[#e9f6f3] text-[#00685f] ring-1 ring-[#00685f]/15",
    icon: CheckCircle2,
  },
  stale: {
    label: "Cần cập nhật",
    className: "bg-[#fff4e8] text-[#924628] ring-1 ring-[#924628]/15",
    icon: AlertCircle,
  },
  missing: {
    label: "Chưa đồng thuận",
    className: "bg-[#fff8f7] text-[#ba1a1a] ring-1 ring-[#ba1a1a]/15",
    icon: AlertCircle,
  },
};

function ConsentMetricTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-[#bcc9c6]/25 bg-[#f8fcfb] px-4 py-3.5">
      <p className="text-xs font-bold uppercase tracking-wide text-[#6d7a77]">{label}</p>
      <p className="mt-1.5 text-base font-bold tabular-nums text-[#121e1c]">{value}</p>
    </div>
  );
}

type ConsentStatusPanelProps = {
  isLoading: boolean;
  isError: boolean;
  onRetry: () => void;
  consentStatus?: ConsentStatus;
  activePolicyVersion: string;
};

function ConsentStatusPanel({
  isLoading,
  isError,
  onRetry,
  consentStatus,
  activePolicyVersion,
}: ConsentStatusPanelProps) {
  if (isLoading) {
    return (
      <div
        className="flex min-h-[140px] items-center justify-center rounded-2xl border border-dashed border-[#bcc9c6]/40 bg-[#f8fcfb]"
        role="status"
        aria-live="polite"
      >
        <div className="flex flex-col items-center gap-3">
          <div className="h-8 w-8 animate-spin rounded-full border-b-2 border-[#00685f]" />
          <span className="text-sm font-medium text-[#6d7a77]">Đang tải trạng thái…</span>
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="rounded-2xl border border-[#f2b8b5]/60 bg-[#fff8f7] p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-start gap-3">
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#ffdad6] text-[#ba1a1a]">
              <AlertCircle className="h-5 w-5" aria-hidden="true" />
            </span>
            <div>
              <p className="font-bold text-[#121e1c]">Không tải được trạng thái</p>
              <p className="mt-1 text-sm text-[#6d7a77]">
                Bạn vẫn có thể xem tài liệu pháp lý và gửi yêu cầu xóa tài khoản bên dưới.
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onRetry}
            className="shrink-0 rounded-xl bg-[#00685f] px-5 py-2.5 text-sm font-bold text-white transition hover:bg-[#008378]"
          >
            Thử lại
          </button>
        </div>
      </div>
    );
  }

  const consentGiven = consentStatus?.consentGiven ?? false;
  const recordedVersion = consentStatus?.consentVersion ?? null;
  const consentedAtLabel = formatConsentDate(consentStatus?.consentedAt);
  const hasStaleConsent = !consentGiven && Boolean(recordedVersion);
  const consentTimeLabel = consentedAtLabel ?? (consentGiven ? "Đã ghi nhận" : "—");
  const visualState = consentVisualState(isLoading, isError, consentGiven, hasStaleConsent);
  const badge = consentStatusBadge[visualState as keyof typeof consentStatusBadge];
  const BadgeIcon = badge.icon;

  const helperText = consentGiven
    ? "Đồng thuận của bạn đang áp dụng cho phiên bản chính sách mới nhất."
    : hasStaleConsent
      ? "Bạn đã đồng thuận trước đó; hệ thống sẽ nhắc xác nhận lại khi cần."
      : "Hộp thoại đồng thuận sẽ hiện khi bạn dùng tính năng liên quan dữ liệu sức khỏe.";

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2 border-b border-[#bcc9c6]/20 pb-5">
        <span
          className={`inline-flex shrink-0 items-center gap-2 rounded-full px-3.5 py-1.5 text-sm font-bold ${badge.className}`}
        >
          <BadgeIcon className="h-4 w-4 shrink-0" aria-hidden="true" />
          {badge.label}
        </span>
        <p className="min-w-0 text-sm leading-6 text-[#6d7a77]">{helperText}</p>
      </div>

      <div
        className={`grid gap-3 ${
          hasStaleConsent ? "sm:grid-cols-3" : "sm:grid-cols-2"
        }`}
      >
        <ConsentMetricTile label="Chính sách hiện tại" value={activePolicyVersion} />
        {hasStaleConsent ? (
          <ConsentMetricTile label="Đã đồng thuận trước" value={recordedVersion ?? "—"} />
        ) : null}
        <ConsentMetricTile label="Thời điểm ghi nhận" value={consentTimeLabel} />
      </div>
    </div>
  );
}

export default function PrivacySettingsPage() {
  const sessionConsentGiven = useAuthStore((s) => s.consentGiven);
  const sessionConsentVersion = useAuthStore((s) => s.consentVersion);
  const activeConsentVersion = useAuthStore((s) => s.activeConsentVersion);
  const activePolicyVersion = activeConsentVersion ?? CONSENT_VERSION;

  const {
    data: consentStatus,
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: [
      "consentStatus",
      sessionConsentGiven,
      sessionConsentVersion,
      activePolicyVersion,
    ],
    queryFn: async () => {
      const response = await apiClient.get<ConsentStatus>(API_ROUTES.CONSENT.ME);
      return response.data;
    },
    staleTime: 30_000,
    refetchOnWindowFocus: true,
  });

  return (
    <DashboardPageShell
      title="Quyền riêng tư"
      subtitle="Xem trạng thái đồng thuận xử lý dữ liệu sức khỏe và các tài liệu pháp lý."
      breadcrumbs={breadcrumbFromSettings("Riêng tư")}
    >
      <div className="grid grid-cols-1 items-start gap-8 lg:grid-cols-3">
        <div className="space-y-8 lg:col-span-2">
          <section className={`${settingsCardClassName} relative overflow-hidden`}>
            <div className="absolute top-0 right-0 -mt-16 -mr-16 h-32 w-32 rounded-bl-full bg-[#00685f]/5" />

            <div className="relative mb-8 flex flex-col gap-6 md:flex-row md:items-center">
              <div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-2xl bg-[#e9f6f3] text-[#00685f] ring-4 ring-[#e9f6f3] shadow-md">
                <Shield className="h-10 w-10" aria-hidden="true" />
              </div>
              <div className="min-w-0 flex-grow">
                <h2 className="text-2xl font-bold text-[#121e1c]">Đồng thuận dữ liệu sức khỏe</h2>
                <p className="mt-2 text-sm leading-6 text-[#6d7a77]">
                  Xem nhanh phiên bản chính sách và thời điểm bạn đã xác nhận. Xác nhận lần đầu vẫn qua
                  hộp thoại khi đăng nhập.
                </p>
              </div>
            </div>

            <ConsentStatusPanel
              isLoading={isLoading}
              isError={isError}
              onRetry={() => void refetch()}
              consentStatus={consentStatus}
              activePolicyVersion={activePolicyVersion}
            />
          </section>

          <section className={settingsCardClassName}>
            <div className="mb-6 flex items-center gap-3">
              <Scale className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
              <h2 className="text-xl font-bold text-[#121e1c]">Chính sách & điều khoản</h2>
            </div>
            <p className="mb-6 text-sm leading-6 text-[#6d7a77]">
              Mở tài liệu đầy đủ trên tab mới để đọc chi tiết quyền và nghĩa vụ của bạn.
            </p>
            <ul className="space-y-3">
              <LegalDocumentRow
                href="/privacy"
                label="Chính sách quyền riêng tư"
                description="Cách HealthLens thu thập, lưu trữ và bảo vệ dữ liệu cá nhân của bạn."
                icon={Shield}
              />
              <LegalDocumentRow
                href="/terms"
                label="Điều khoản sử dụng"
                description="Quy định sử dụng dịch vụ và giới hạn trách nhiệm của HealthLens."
                icon={FileText}
              />
            </ul>
          </section>

          <section className={settingsCardClassName}>
            <div className="mb-6 flex items-center gap-3">
              <ShieldCheck className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
              <h2 className="text-xl font-bold text-[#121e1c]">Quyền của bạn (NĐ 13/2023)</h2>
            </div>
            <ul className="space-y-4 text-sm leading-6 text-[#3d4947]">
              <li className="flex gap-3 rounded-xl bg-[#e9f6f3] p-4">
                <span className="font-bold text-[#00685f]">1.</span>
                <span>
                  Bạn có quyền <strong>biết</strong> cách dữ liệu sức khỏe được thu thập và sử dụng trong
                  HealthLens.
                </span>
              </li>
              <li className="flex gap-3 rounded-xl bg-[#e9f6f3] p-4">
                <span className="font-bold text-[#00685f]">2.</span>
                <span>
                  Bạn có quyền <strong>rút lại đồng thuận</strong> hoặc yêu cầu hạn chế xử lý theo quy định
                  pháp luật.
                </span>
              </li>
              <li className="flex gap-3 rounded-xl bg-[#e9f6f3] p-4">
                <span className="font-bold text-[#00685f]">3.</span>
                <span>
                  Bạn có quyền <strong>xóa tài khoản</strong> và dữ liệu liên quan thông qua quy trình trong
                  ứng dụng.
                </span>
              </li>
            </ul>
          </section>
        </div>

        <div className="space-y-8">
          <section className={settingsCardClassName}>
            <div className="mb-8 flex items-center gap-3">
              <Shield className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
              <h2 className="text-xl font-bold text-[#121e1c]">Bảo mật & tài khoản</h2>
            </div>
            <SettingsAccountNav active="privacy" />
          </section>

          <section className={settingsTipCardClassName}>
            <div className="relative z-10">
              <div className="mb-6 flex items-center gap-3">
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-white/50 backdrop-blur-md">
                  <ShieldCheck className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
                </div>
                <h3 className="text-xl font-bold text-[#121e1c]">Dữ liệu được bảo vệ thế nào?</h3>
              </div>
              <ul className="space-y-3 text-sm leading-relaxed text-[#274d48]">
                <li className="rounded-xl bg-white/45 px-4 py-3">
                  Dữ liệu y tế được mã hóa và chỉ dùng cho mục đích cung cấp dịch vụ.
                </li>
                <li className="rounded-xl bg-white/45 px-4 py-3">
                  HealthLens không chia sẻ dữ liệu với bên thứ ba nếu không có sự đồng ý của bạn.
                </li>
                <li className="rounded-xl bg-white/45 px-4 py-3">
                  Khuyến nghị y tế từ ứng dụng chỉ mang tính tham khảo, không thay tư vấn bác sĩ.
                </li>
              </ul>
            </div>
            <div
              className="pointer-events-none absolute -bottom-10 -right-10 h-40 w-40 rounded-full bg-[#00685f]/10 blur-3xl"
              aria-hidden="true"
            />
          </section>

          <section className="rounded-3xl border border-[#f2b8b5]/60 bg-[#fff8f7] p-8 shadow-[0_8px_32px_rgba(18,30,28,0.04)]">
            <div className="mb-6 flex items-center gap-3">
              <Trash2 className="h-6 w-6 text-[#ba1a1a]" aria-hidden="true" />
              <h3 className="text-xl font-bold text-[#121e1c]">Quyền được xóa</h3>
            </div>
            <p className="text-sm leading-relaxed text-[#3d4947]">
              Gửi yêu cầu xóa tài khoản và dữ liệu liên quan theo quy định về bảo vệ dữ liệu cá nhân.
            </p>
            <Link
              href="/settings/delete-account"
              className="mt-5 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl border-2 border-[#ba1a1a]/25 bg-white px-4 text-sm font-bold text-[#ba1a1a] transition hover:border-[#ba1a1a]/45 hover:bg-[#ba1a1a]/5 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#ba1a1a]"
            >
              <Trash2 className="h-4 w-4" aria-hidden="true" />
              Đi tới trang xóa tài khoản
            </Link>
          </section>

          <SettingsDirectContactCard
            title="Cần hỗ trợ?"
            description="Liên hệ đội ngũ nếu bạn có thắc mắc về quyền riêng tư hoặc đồng thuận dữ liệu."
          />
        </div>
      </div>
    </DashboardPageShell>
  );
}
