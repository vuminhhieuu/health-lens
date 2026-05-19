"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { format, parseISO } from "date-fns";
import { vi } from "date-fns/locale";
import { ClipboardList, Loader2, X } from "lucide-react";

import { adminApiClient } from "@/lib/api/adminApiClient";
import { ApiPaths } from "@healthlens/shared/constants";

const RESOURCE_TYPE_REFERENCE_DATA = "REFERENCE_DATA";
const RESOURCE_TYPE_HEALTH_RECORD = "HEALTH_RECORD";
const RESOURCE_TYPE_PROFILE = "PROFILE";
const RESOURCE_TYPE_AUTH = "AUTH";
const RESOURCE_TYPE_USER = "USER";
const RESOURCE_TYPE_CONSENT = "CONSENT";

type AuditLogOutcome = "SUCCESS" | "FAILURE";

type AuditLogEntry = {
  id: string;
  actorEmail: string;
  action: string;
  resourceType: string;
  resourceId: string | null;
  entityLabel: string;
  detailSummary: string;
  outcome?: AuditLogOutcome;
  oldValueJson: string | null;
  newValueJson: string | null;
  ipAddress: string | null;
  createdAt: string;
};

type AuditLogPage = {
  content: AuditLogEntry[];
  totalElements: number;
  page: number;
  limit: number;
};

type OnlineRagCitationEntry = {
  id: string;
  healthRecordId: string;
  metricName: string;
  answerHash: string;
  sourceSnapshotId: string | null;
  sourceUrl: string;
  publisher: string;
  retrievedAt: string | null;
  snapshotHash: string;
  reviewStatus: "APPROVED" | "REVIEW_REQUIRED" | "REJECTED";
  excluded: boolean;
  cacheHit: boolean;
  usableForAi: boolean;
  reviewRequired: boolean;
  rejected: boolean;
  stale: boolean;
  retrievalSource: string;
  promptVersion: string;
  modelVersion: string;
  createdAt: string;
};

type OnlineRagCitationPage = {
  content: OnlineRagCitationEntry[];
  totalElements: number;
  page: number;
  limit: number;
};

type CitationFilters = {
  healthRecordId: string;
  metricName: string;
  sourceUrl: string;
  publisher: string;
  snapshotHash: string;
  answerHash: string;
  reviewStatus: string;
  page: number;
  limit: number;
};

type ReferenceMetricOption = {
  id: string;
  name: string;
  displayNameVi: string;
  unit: string;
};

type ListQuery = {
  resourceType: string;
  resourceId: string;
  actorEmail: string;
  action: string;
  from: string;
  to: string;
  page: number;
  limit: number;
};

const DEFAULT_LIMIT = 20;
const DEFAULT_CITATION_LIMIT = 10;

type AuditViewScope = "reference" | "all";

const ACTION_LABEL_VI: Record<string, string> = {
  UPDATE_REFERENCE_METRIC_DISPLAY: "Cập nhật tên hiển thị",
  CREATE_REFERENCE_METRIC: "Tạo chỉ số",
  UPDATE_REFERENCE_METRIC: "Sửa chỉ số",
  DEACTIVATE_REFERENCE_METRIC: "Ngưng áp dụng",
  REACTIVATE_REFERENCE_METRIC: "Kích hoạt lại",
  SUBMIT_REFERENCE_CHANGE_SET: "Gửi duyệt",
  PUBLISH_CHANGE_SET: "Kích hoạt thay đổi",
  APPROVE_CHANGE_SET: "Phê duyệt",
  REJECT_CHANGE_SET: "Từ chối",
  CONFIRM_REFERENCE_IMPORT: "Xác nhận import",
  CREATE_HEALTH_RECORD: "Tạo hồ sơ",
  CONFIRM_HEALTH_RECORD: "Xác nhận hồ sơ",
  UPDATE_HEALTH_RECORD_METRICS: "Cập nhật chỉ số hồ sơ",
  DOWNLOAD_HEALTH_RECORD_PDF: "Tải PDF hồ sơ",
  DELETE_HEALTH_RECORD: "Xóa hồ sơ",
  INVITE_HEALTH_RECORD_SHARE: "Mời chia sẻ hồ sơ",
  ACCEPT_HEALTH_RECORD_SHARE: "Chấp nhận chia sẻ hồ sơ",
  REVOKE_HEALTH_RECORD_SHARE: "Thu hồi chia sẻ hồ sơ",
  INVITE_PROFILE_SHARE: "Mời chia sẻ hồ sơ gia đình",
  CANCEL_PROFILE_INVITATION: "Hủy lời mời hồ sơ",
  ACCEPT_PROFILE_INVITATION: "Chấp nhận lời mời hồ sơ",
  REJECT_PROFILE_INVITATION: "Từ chối lời mời hồ sơ",
  REVOKE_PROFILE_SHARE: "Thu hồi chia sẻ hồ sơ",
  RESEND_PROFILE_INVITATION: "Gửi lại lời mời hồ sơ",
  CREATE_PROFILE: "Tạo hồ sơ gia đình",
  UPDATE_PROFILE: "Cập nhật hồ sơ gia đình",
  LOGIN: "Đăng nhập",
  LOGIN_FAILED: "Đăng nhập thất bại",
  LOGOUT: "Đăng xuất",
  REGISTER: "Đăng ký",
  VERIFY_EMAIL: "Xác thực email",
  REFRESH_TOKEN: "Làm mới phiên",
  FORGOT_PASSWORD: "Quên mật khẩu",
  RESET_PASSWORD: "Đặt lại mật khẩu",
  ADMIN_LOGIN: "Đăng nhập admin",
  ADMIN_TOTP_SETUP: "Thiết lập TOTP admin",
  ADMIN_TOTP_VERIFY: "Xác minh TOTP admin",
  UPDATE_USER: "Cập nhật tài khoản",
  REQUEST_ACCOUNT_DELETION: "Yêu cầu xóa tài khoản",
  CANCEL_ACCOUNT_DELETION: "Hủy yêu cầu xóa tài khoản",
  RECORD_CONSENT: "Đồng ý điều khoản",
  REVOKE_CONSENT: "Thu hồi đồng ý",
};

const ACTION_FILTER_GROUPS: { label: string; actions: string[] }[] = [
  {
    label: "Xác thực",
    actions: [
      "LOGIN",
      "LOGIN_FAILED",
      "LOGOUT",
      "REGISTER",
      "VERIFY_EMAIL",
      "REFRESH_TOKEN",
      "FORGOT_PASSWORD",
      "RESET_PASSWORD",
      "ADMIN_LOGIN",
      "ADMIN_TOTP_SETUP",
      "ADMIN_TOTP_VERIFY",
    ],
  },
  {
    label: "Tài khoản & hồ sơ",
    actions: [
      "UPDATE_USER",
      "REQUEST_ACCOUNT_DELETION",
      "CANCEL_ACCOUNT_DELETION",
      "RECORD_CONSENT",
      "REVOKE_CONSENT",
      "CREATE_PROFILE",
      "UPDATE_PROFILE",
    ],
  },
  {
    label: "Hồ sơ sức khỏe",
    actions: [
      "CREATE_HEALTH_RECORD",
      "CONFIRM_HEALTH_RECORD",
      "UPDATE_HEALTH_RECORD_METRICS",
      "DOWNLOAD_HEALTH_RECORD_PDF",
      "DELETE_HEALTH_RECORD",
      "INVITE_HEALTH_RECORD_SHARE",
      "ACCEPT_HEALTH_RECORD_SHARE",
      "REVOKE_HEALTH_RECORD_SHARE",
    ],
  },
  {
    label: "Chia sẻ hồ sơ gia đình",
    actions: [
      "INVITE_PROFILE_SHARE",
      "CANCEL_PROFILE_INVITATION",
      "ACCEPT_PROFILE_INVITATION",
      "REJECT_PROFILE_INVITATION",
      "REVOKE_PROFILE_SHARE",
      "RESEND_PROFILE_INVITATION",
    ],
  },
  {
    label: "Dữ liệu tham chiếu",
    actions: [
      "UPDATE_REFERENCE_METRIC_DISPLAY",
      "CREATE_REFERENCE_METRIC",
      "UPDATE_REFERENCE_METRIC",
      "DEACTIVATE_REFERENCE_METRIC",
      "REACTIVATE_REFERENCE_METRIC",
      "SUBMIT_REFERENCE_CHANGE_SET",
      "PUBLISH_CHANGE_SET",
      "APPROVE_CHANGE_SET",
      "REJECT_CHANGE_SET",
      "CONFIRM_REFERENCE_IMPORT",
    ],
  },
];

const REFERENCE_ACTION_GROUP = ACTION_FILTER_GROUPS.find(
  (g) => g.label === "Dữ liệu tham chiếu",
)!;

const REFERENCE_ACTIONS = new Set(REFERENCE_ACTION_GROUP.actions);

function validateDateRange(from: string, to: string): string | null {
  if (from && to && from > to) {
    return "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.";
  }
  return null;
}

function auditFiltersFromSearchParams(
  searchParams: URLSearchParams,
  viewScope: AuditViewScope,
): Omit<ListQuery, "page" | "limit"> {
  const resourceId = searchParams.get("resourceId")?.trim() ?? "";
  const resourceType =
    viewScope === "reference"
      ? RESOURCE_TYPE_REFERENCE_DATA
      : (searchParams.get("resourceType")?.trim() ?? "");
  return {
    resourceType,
    resourceId,
    actorEmail: searchParams.get("actorEmail")?.trim() ?? "",
    action: searchParams.get("action")?.trim() ?? "",
    from: searchParams.get("from") ?? "",
    to: searchParams.get("to") ?? "",
  };
}

function appendAuditFilterParams(
  params: URLSearchParams,
  scope: AuditViewScope,
  filters: Omit<ListQuery, "page" | "limit">,
): void {
  if (scope === "all" && filters.resourceType.trim()) {
    params.set("resourceType", filters.resourceType.trim());
  }
  if (filters.resourceId.trim()) {
    params.set("resourceId", filters.resourceId.trim());
  }
  if (filters.actorEmail.trim()) {
    params.set("actorEmail", filters.actorEmail.trim());
  }
  if (filters.action.trim()) {
    params.set("action", filters.action.trim());
  }
  if (filters.from) {
    params.set("from", filters.from);
  }
  if (filters.to) {
    params.set("to", filters.to);
  }
}

function buildAuditLogUrl(scope: AuditViewScope, filters: Omit<ListQuery, "page" | "limit">): string {
  const params = new URLSearchParams();
  if (scope === "all") {
    params.set("view", "all");
  }
  appendAuditFilterParams(params, scope, filters);
  const qs = params.toString();
  return qs ? `/admin/audit-log?${qs}` : "/admin/audit-log";
}

const RESOURCE_TYPE_LABEL_VI: Record<string, string> = {
  [RESOURCE_TYPE_REFERENCE_DATA]: "Dữ liệu tham chiếu",
  [RESOURCE_TYPE_HEALTH_RECORD]: "Hồ sơ sức khỏe",
  [RESOURCE_TYPE_PROFILE]: "Hồ sơ gia đình",
  [RESOURCE_TYPE_AUTH]: "Xác thực",
  [RESOURCE_TYPE_USER]: "Tài khoản",
  [RESOURCE_TYPE_CONSENT]: "Đồng ý điều khoản",
};

function formatJsonBlock(raw: string | null | undefined): string {
  if (raw == null || raw === "") {
    return "—";
  }
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

function actionLabelVi(action: string): string {
  return ACTION_LABEL_VI[action] ?? action;
}

/** Nhãn ngắn cho cột Hành động (loại thao tác), theo prototype Stitch. */
const ACTION_CATEGORY_VI: Record<string, string> = {
  LOGIN: "Đăng nhập",
  LOGIN_FAILED: "Đăng nhập",
  ADMIN_LOGIN: "Đăng nhập",
  LOGOUT: "Đăng xuất",
  REGISTER: "Đăng ký",
  VERIFY_EMAIL: "Xác thực",
  REFRESH_TOKEN: "Đăng nhập",
  FORGOT_PASSWORD: "Đăng nhập",
  RESET_PASSWORD: "Đăng nhập",
  ADMIN_TOTP_SETUP: "MFA",
  ADMIN_TOTP_VERIFY: "MFA",
  CREATE_HEALTH_RECORD: "Tạo mới",
  CONFIRM_HEALTH_RECORD: "Tạo mới",
  CREATE_REFERENCE_METRIC: "Tạo mới",
  CREATE_PROFILE: "Tạo mới",
  CONFIRM_REFERENCE_IMPORT: "Tạo mới",
  PUBLISH_CHANGE_SET: "Tạo mới",
  DELETE_HEALTH_RECORD: "Xóa",
  DEACTIVATE_REFERENCE_METRIC: "Xóa",
  REVOKE_PROFILE_SHARE: "Xóa",
  REVOKE_HEALTH_RECORD_SHARE: "Xóa",
  CANCEL_PROFILE_INVITATION: "Xóa",
  CANCEL_ACCOUNT_DELETION: "Xóa",
  REQUEST_ACCOUNT_DELETION: "Xóa",
  REJECT_CHANGE_SET: "Xóa",
  REJECT_PROFILE_INVITATION: "Xóa",
  DOWNLOAD_HEALTH_RECORD_PDF: "Xuất dữ liệu",
  APPROVE_CHANGE_SET: "Phê duyệt",
  SUBMIT_REFERENCE_CHANGE_SET: "Phê duyệt",
  INVITE_PROFILE_SHARE: "Chia sẻ",
  INVITE_HEALTH_RECORD_SHARE: "Chia sẻ",
  ACCEPT_PROFILE_INVITATION: "Chia sẻ",
  ACCEPT_HEALTH_RECORD_SHARE: "Chia sẻ",
  RESEND_PROFILE_INVITATION: "Chia sẻ",
  RECORD_CONSENT: "Cập nhật",
  REVOKE_CONSENT: "Cập nhật",
  UPDATE_USER: "Cập nhật",
  UPDATE_PROFILE: "Cập nhật",
  UPDATE_HEALTH_RECORD_METRICS: "Cập nhật",
  UPDATE_REFERENCE_METRIC: "Cập nhật",
  UPDATE_REFERENCE_METRIC_DISPLAY: "Cập nhật",
  REACTIVATE_REFERENCE_METRIC: "Cập nhật",
};

function actionCategoryVi(action: string): string {
  if (ACTION_CATEGORY_VI[action]) {
    return ACTION_CATEGORY_VI[action];
  }
  if (
    action.includes("DELETE") ||
    action.includes("REVOKE") ||
    action.includes("DEACTIVATE") ||
    action.includes("CANCEL")
  ) {
    return "Xóa";
  }
  if (action.includes("CREATE") || action.includes("CONFIRM") || action.includes("PUBLISH")) {
    return "Tạo mới";
  }
  if (action.includes("DOWNLOAD") || action.includes("EXPORT")) {
    return "Xuất dữ liệu";
  }
  if (action.includes("APPROVE") || action.includes("SUBMIT") || action.includes("REJECT")) {
    return "Phê duyệt";
  }
  if (action.includes("INVITE") || action.includes("SHARE") || action.includes("ACCEPT")) {
    return "Chia sẻ";
  }
  if (action.includes("LOGIN") || action.includes("LOGOUT") || action.includes("REGISTER")) {
    return "Đăng nhập";
  }
  if (action.includes("TOTP")) {
    return "MFA";
  }
  return "Cập nhật";
}

function resolveOutcome(entry: Pick<AuditLogEntry, "outcome" | "action">): AuditLogOutcome {
  if (entry.outcome === "FAILURE" || entry.outcome === "SUCCESS") {
    return entry.outcome;
  }
  return entry.action === "LOGIN_FAILED" || entry.action.endsWith("_FAILED")
    ? "FAILURE"
    : "SUCCESS";
}

function outcomeLabelVi(outcome: AuditLogOutcome): string {
  return outcome === "FAILURE" ? "Thất bại" : "Thành công";
}

function outcomeBadgeClass(outcome: AuditLogOutcome): string {
  return outcome === "FAILURE"
    ? "bg-[#924628]/10 text-[#924628]"
    : "bg-emerald-50 text-emerald-800";
}

function actionBadgeClass(action: string): string {
  if (action.includes("TOTP")) {
    return "bg-slate-200 text-slate-700";
  }
  if (action.includes("DOWNLOAD") || action.includes("EXPORT")) {
    return "bg-[#ffb59a]/30 text-[#924628]";
  }
  if (
    action.includes("DELETE") ||
    action.includes("REJECT") ||
    action.includes("FAILED") ||
    action.includes("REVOKE") ||
    action.includes("DEACTIVATE") ||
    action.includes("CANCEL")
  ) {
    return "bg-[#924628]/10 text-[#924628]";
  }
  if (
    action.includes("CREATE") ||
    action.includes("APPROVE") ||
    action.includes("REGISTER") ||
    action.includes("ACCEPT") ||
    action.includes("PUBLISH")
  ) {
    return "bg-[#c2ebe3] text-[#456b66]";
  }
  if (
    action.includes("LOGIN") ||
    action === "REFRESH_TOKEN" ||
    action.includes("VERIFY")
  ) {
    return "bg-[#c2ebe3] text-[#456b66]";
  }
  if (action.includes("INVITE") || action.includes("SHARE")) {
    return "bg-[#008378]/10 text-[#008378]";
  }
  return "bg-[#008378]/10 text-[#008378]";
}

function hashString(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i += 1) {
    h = (h << 5) - h + s.charCodeAt(i);
    h |= 0;
  }
  return Math.abs(h);
}

/** Màu avatar xen kẽ giống các dòng mẫu trong prototype. */
function avatarBadgeClass(email: string): string {
  const key = email.trim() || "?";
  const variants = [
    "bg-[#c2ebe3] text-[#456b66]",
    "bg-[#ffb59a] text-[#370e00]",
    "bg-[#deebe8] text-[#115e59]",
    "bg-[#ffdad6] text-[#93000a]",
  ];
  return variants[hashString(key) % variants.length];
}

function initialsFromEmail(email: string): string {
  const local = email.split("@")[0]?.trim() ?? "";
  if (!local) return "?";
  const parts = local.split(/[._-]+/).filter(Boolean);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase().slice(0, 2);
  }
  return local.slice(0, 2).toUpperCase();
}

/** Hiển thị dòng tên giả lập từ email (prototype có tên + email; API chỉ có email). */
function displayNameFromEmail(email: string): string {
  const local = email.split("@")[0]?.trim() ?? "";
  if (!local) return "—";
  const parts = local.split(/[._-]+/).filter(Boolean);
  if (parts.length === 0) {
    return local.charAt(0).toUpperCase() + local.slice(1).toLowerCase();
  }
  return parts
    .map((p) => p.charAt(0).toUpperCase() + p.slice(1).toLowerCase())
    .join(" ");
}

function rowDetailText(row: AuditLogEntry): string {
  const base =
    row.detailSummary?.trim() ||
    row.entityLabel?.trim() ||
    `${row.resourceType}${row.resourceId ? ` · ${row.resourceId}` : ""}`;
  return base.length > 160 ? `${base.slice(0, 157)}…` : base;
}

function buildListParams(q: ListQuery): Record<string, string | number> {
  const params: Record<string, string | number> = {
    page: q.page,
    limit: q.limit,
  };
  if (q.resourceType.trim()) {
    params.resourceType = q.resourceType.trim();
  }
  if (q.resourceId.trim()) {
    params.resourceId = q.resourceId.trim();
  }
  if (q.actorEmail.trim()) {
    params.actorEmail = q.actorEmail.trim();
  }
  if (q.action.trim()) {
    params.action = q.action.trim();
  }
  if (q.from) {
    params.from = q.from;
  }
  if (q.to) {
    params.to = q.to;
  }
  return params;
}

function buildCitationParams(q: CitationFilters): Record<string, string | number> {
  const params: Record<string, string | number> = {
    page: q.page,
    limit: q.limit,
  };
  if (q.healthRecordId.trim()) params.healthRecordId = q.healthRecordId.trim();
  if (q.metricName.trim()) params.metricName = q.metricName.trim();
  if (q.sourceUrl.trim()) params.sourceUrl = q.sourceUrl.trim();
  if (q.publisher.trim()) params.publisher = q.publisher.trim();
  if (q.snapshotHash.trim()) params.snapshotHash = q.snapshotHash.trim();
  if (q.answerHash.trim()) params.answerHash = q.answerHash.trim();
  if (q.reviewStatus.trim()) params.reviewStatus = q.reviewStatus.trim();
  return params;
}

function reviewStatusLabel(status: OnlineRagCitationEntry["reviewStatus"]): string {
  if (status === "APPROVED") return "Đã duyệt";
  if (status === "REJECTED") return "Từ chối";
  return "Cần rà soát";
}

function reviewStatusClass(entry: OnlineRagCitationEntry): string {
  if (entry.reviewStatus === "APPROVED" && !entry.excluded && entry.usableForAi && !entry.stale) {
    return "bg-emerald-50 text-emerald-800";
  }
  if (entry.reviewStatus === "REJECTED" || entry.excluded) {
    return "bg-[#924628]/10 text-[#924628]";
  }
  return "bg-amber-50 text-amber-800";
}

function reviewStatusDetail(row: OnlineRagCitationEntry): string | null {
  if (row.stale) {
    return "Snapshot đã quá TTL; không gắn nhãn evidence đã duyệt.";
  }
  if (row.reviewStatus !== "APPROVED" || row.excluded || !row.usableForAi) {
    return "Chỉ là metadata audit; không gắn nhãn evidence đã duyệt.";
  }
  return null;
}

function buildPageList(current: number, total: number): (number | "ellipsis")[] {
  if (total <= 1) {
    return [0];
  }
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i);
  }
  const pages = new Set<number>([0, total - 1, current]);
  if (current > 0) {
    pages.add(current - 1);
  }
  if (current < total - 1) {
    pages.add(current + 1);
  }
  const sorted = [...pages].sort((a, b) => a - b);
  const out: (number | "ellipsis")[] = [];
  let prev = -2;
  for (const p of sorted) {
    if (p - prev > 1) {
      out.push("ellipsis");
    }
    out.push(p);
    prev = p;
  }
  return out;
}

function jsonPanelLabel(
  oldJson: string | null,
  newJson: string | null,
  side: "before" | "after",
): string {
  const empty = side === "before" ? !oldJson : !newJson;
  if (empty && side === "before" && newJson) {
    return "Không có dữ liệu trước (tạo mới)";
  }
  if (empty && side === "after" && oldJson) {
    return "Không có dữ liệu sau (đã xóa / vô hiệu)";
  }
  return side === "before" ? "Trước khi thay đổi" : "Sau khi thay đổi";
}

function AuditDetailModal({
  entry,
  onClose,
}: {
  entry: AuditLogEntry;
  onClose: () => void;
}) {
  const at = new Date(entry.createdAt);
  const email = entry.actorEmail?.trim() || "";

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [onClose]);

  return (
    <div
      className="fixed inset-0 z-[70] flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-labelledby="audit-detail-title"
      onClick={onClose}
    >
      <div
        className="flex max-h-[90vh] w-full max-w-5xl flex-col overflow-hidden rounded-[2rem] bg-white shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between border-b border-[#d8e5e2] px-6 py-5">
          <div>
            <h3 id="audit-detail-title" className="text-xl font-bold text-[#121e1c]">
              Chi tiết thay đổi
            </h3>
            <p className="mt-1 text-sm text-[#3d4947]">
              {rowDetailText(entry)}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full p-2 text-[#3d4947] transition hover:bg-[#e9f6f3]"
            aria-label="Đóng"
            title="Đóng"
          >
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>

        <div className="flex flex-wrap gap-3 border-b border-[#e9f6f3] bg-[#f8fafc] px-6 py-4 text-xs">
          <div>
            <span className="font-bold uppercase tracking-wider text-[#0d9488]">Thời gian</span>
            <p className="mt-0.5 font-medium text-[#121e1c]">
              {format(at, "HH:mm:ss · dd/MM/yyyy", { locale: vi })}
            </p>
          </div>
          <div className="min-w-[10rem] flex-1">
            <span className="font-bold uppercase tracking-wider text-[#0d9488]">Người thực hiện</span>
            <p className="mt-0.5 font-medium text-[#121e1c]">{displayNameFromEmail(email)}</p>
            <p className="text-[10px] italic text-[#3d4947]">{email || "—"}</p>
          </div>
          <div>
            <span className="font-bold uppercase tracking-wider text-[#0d9488]">Hành động</span>
            <p className="mt-1">
              <span
                className={`inline-block rounded px-2 py-0.5 text-[11px] font-semibold ${actionBadgeClass(entry.action)}`}
              >
                {actionLabelVi(entry.action)}
              </span>
            </p>
          </div>
          <div>
            <span className="font-bold uppercase tracking-wider text-[#0d9488]">Đối tượng</span>
            <p className="mt-0.5 font-medium text-[#121e1c]">{entry.entityLabel || "—"}</p>
          </div>
          <div>
            <span className="font-bold uppercase tracking-wider text-[#0d9488]">IP</span>
            <p className="mt-0.5 font-mono text-[#3d4947]">{entry.ipAddress ?? "—"}</p>
          </div>
        </div>

        <div className="grid flex-1 gap-4 overflow-hidden p-6 md:grid-cols-2">
          <div className="flex min-h-0 flex-col rounded-2xl border border-[#d8e5e2] bg-[#f8fafc]">
            <p className="border-b border-[#d8e5e2] bg-white px-4 py-2 text-[11px] font-bold uppercase tracking-wider text-[#3d4947]">
              {jsonPanelLabel(entry.oldValueJson, entry.newValueJson, "before")}
            </p>
            <pre className="hl-custom-scrollbar max-h-[50vh] flex-1 overflow-auto p-4 text-xs leading-relaxed text-[#121e1c]">
              {formatJsonBlock(entry.oldValueJson)}
            </pre>
          </div>
          <div className="flex min-h-0 flex-col rounded-2xl border border-[#89f5e7]/40 bg-[#e9f6f3]/60">
            <p className="border-b border-[#89f5e7]/40 bg-white px-4 py-2 text-[11px] font-bold uppercase tracking-wider text-[#00685f]">
              {jsonPanelLabel(entry.oldValueJson, entry.newValueJson, "after")}
            </p>
            <pre className="hl-custom-scrollbar max-h-[50vh] flex-1 overflow-auto p-4 text-xs leading-relaxed text-[#121e1c]">
              {formatJsonBlock(entry.newValueJson)}
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
}

function resolveInitialAuditState(searchParams: URLSearchParams): {
  viewScope: AuditViewScope;
  filters: Omit<ListQuery, "page" | "limit">;
} {
  const viewScope: AuditViewScope =
    searchParams.get("view") === "all" ? "all" : "reference";
  return {
    viewScope,
    filters: auditFiltersFromSearchParams(searchParams, viewScope),
  };
}

function OnlineRagCitationPanel() {
  const [draft, setDraft] = useState<CitationFilters>({
    healthRecordId: "",
    metricName: "",
    sourceUrl: "",
    publisher: "",
    snapshotHash: "",
    answerHash: "",
    reviewStatus: "",
    page: 0,
    limit: DEFAULT_CITATION_LIMIT,
  });
  const [applied, setApplied] = useState(draft);
  const [exporting, setExporting] = useState(false);

  const query = useQuery({
    queryKey: ["admin-online-rag-citations", applied],
    queryFn: async () => {
      const res = await adminApiClient.get<OnlineRagCitationPage>(
        ApiPaths.ADMIN.ONLINE_RAG_CITATIONS,
        { params: buildCitationParams(applied) },
      );
      return res.data;
    },
  });

  const rows = query.data?.content ?? [];
  const total = query.data?.totalElements ?? 0;
  const totalPages = query.data?.limit
    ? Math.max(1, Math.ceil(total / query.data.limit))
    : 1;

  const apply = useCallback(() => {
    setApplied((prev) => ({ ...prev, ...draft, page: 0 }));
  }, [draft]);

  const reset = useCallback(() => {
    const cleared: CitationFilters = {
      healthRecordId: "",
      metricName: "",
      sourceUrl: "",
      publisher: "",
      snapshotHash: "",
      answerHash: "",
      reviewStatus: "",
      page: 0,
      limit: DEFAULT_CITATION_LIMIT,
    };
    setDraft(cleared);
    setApplied(cleared);
  }, []);

  const exportCsv = useCallback(async () => {
    if (total === 0) return;
    setExporting(true);
    try {
      const params = buildCitationParams(applied);
      delete params.page;
      delete params.limit;
      const search = new URLSearchParams();
      Object.entries(params).forEach(([key, value]) => search.set(key, String(value)));
      const response = await adminApiClient.get(
        `${ApiPaths.ADMIN.ONLINE_RAG_CITATIONS_EXPORT}?${search.toString()}`,
        { responseType: "blob" },
      );
      const blob = new Blob([response.data], { type: "text/csv;charset=utf-8" });
      const href = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = href;
      a.download = "online-rag-citations.csv";
      a.click();
      URL.revokeObjectURL(href);
    } finally {
      setExporting(false);
    }
  }, [applied, total]);

  return (
    <section className="flex flex-col overflow-hidden rounded-[28px] bg-white shadow-sm ring-1 ring-slate-200">
      <div className="flex flex-col justify-between gap-3 border-b border-slate-200 bg-slate-50 px-6 py-4 lg:flex-row lg:items-start">
        <div>
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-teal-700">travel_explore</span>
            <h2 className="font-bold text-slate-900">Citation online RAG</h2>
          </div>
          <p className="mt-1 max-w-3xl text-xs leading-relaxed text-slate-500">
            Metadata nguồn online đã ảnh hưởng hoặc được cân nhắc cho giải thích AI. Nội dung snapshot thô không hiển thị ở màn này.
          </p>
        </div>
        <button
          type="button"
          onClick={exportCsv}
          disabled={exporting || total === 0}
          className="inline-flex shrink-0 items-center justify-center gap-2 rounded-xl bg-teal-700 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {exporting ? <Loader2 className="h-4 w-4 animate-spin" /> : <span className="material-symbols-outlined text-lg">download</span>}
          Xuất citation
        </button>
      </div>

      <div className="grid grid-cols-1 gap-3 border-b border-slate-100 p-5 md:grid-cols-2 xl:grid-cols-4">
        <input
          className="rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
          placeholder="Health record ID"
          value={draft.healthRecordId}
          onChange={(e) => setDraft((d) => ({ ...d, healthRecordId: e.target.value }))}
        />
        <input
          className="rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
          placeholder="Tên chỉ số"
          value={draft.metricName}
          onChange={(e) => setDraft((d) => ({ ...d, metricName: e.target.value }))}
        />
        <input
          className="rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
          placeholder="Source URL hoặc domain"
          value={draft.sourceUrl}
          onChange={(e) => setDraft((d) => ({ ...d, sourceUrl: e.target.value }))}
        />
        <div className="relative">
          <select
            className="w-full cursor-pointer appearance-none rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 pr-10 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
            value={draft.reviewStatus}
            onChange={(e) => setDraft((d) => ({ ...d, reviewStatus: e.target.value }))}
          >
            <option value="">Mọi trạng thái</option>
            <option value="APPROVED">Đã duyệt</option>
            <option value="REVIEW_REQUIRED">Cần rà soát</option>
            <option value="REJECTED">Từ chối</option>
          </select>
          <span className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-[#00685f]">▾</span>
        </div>
        <input
          className="rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
          placeholder="Publisher"
          value={draft.publisher}
          onChange={(e) => setDraft((d) => ({ ...d, publisher: e.target.value }))}
        />
        <input
          className="rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
          placeholder="Snapshot hash"
          value={draft.snapshotHash}
          onChange={(e) => setDraft((d) => ({ ...d, snapshotHash: e.target.value }))}
        />
        <input
          className="rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
          placeholder="Answer hash"
          value={draft.answerHash}
          onChange={(e) => setDraft((d) => ({ ...d, answerHash: e.target.value }))}
        />
        <div className="flex items-center justify-end gap-2">
          <button type="button" onClick={reset} className="rounded-full px-4 py-2 text-sm font-semibold text-[#3d4947] transition hover:bg-[#e9f6f3]">
            Đặt lại
          </button>
          <button type="button" onClick={apply} className="rounded-full bg-[#00685f] px-5 py-2 text-sm font-bold text-white transition hover:bg-[#005049]">
            Lọc
          </button>
        </div>
      </div>

      {query.isLoading ? (
        <div className="flex items-center justify-center gap-2 py-14 text-[#3d4947]">
          <Loader2 className="h-5 w-5 animate-spin text-[#00685f]" />
          Đang tải citation…
        </div>
      ) : query.isError ? (
        <div className="p-8 text-center text-[#ba1a1a]">Không thể tải citation online RAG.</div>
      ) : rows.length === 0 ? (
        <div className="p-10 text-center text-sm text-[#3d4947]">Chưa có citation online RAG phù hợp.</div>
      ) : (
        <div className="hl-custom-scrollbar overflow-x-auto">
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50">
                <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">Nguồn</th>
                <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">Record / chỉ số</th>
                <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">Review</th>
                <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">Hash</th>
                <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">Thu thập</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rows.map((row) => (
                <tr key={row.id} className="transition-colors hover:bg-teal-50/30">
                  <td className="max-w-md px-6 py-4">
                    <p className="text-sm font-bold text-[#121e1c]">{row.publisher || "—"}</p>
                    <p className="truncate font-mono text-[11px] text-slate-500" title={row.sourceUrl}>{row.sourceUrl}</p>
                    <p className="mt-1 text-[11px] text-slate-500">
                      {row.cacheHit ? "Cache hit" : "Fetch mới"} · {row.retrievalSource}
                    </p>
                  </td>
                  <td className="px-6 py-4">
                    <p className="font-mono text-[11px] text-slate-600">{row.healthRecordId}</p>
                    <p className="mt-1 text-sm font-semibold text-slate-900">{row.metricName}</p>
                  </td>
                  <td className="whitespace-nowrap px-6 py-4">
                    <span className={`inline-block rounded px-2 py-1 text-xs font-semibold ${reviewStatusClass(row)}`}>
                      {reviewStatusLabel(row.reviewStatus)}
                    </span>
                    {reviewStatusDetail(row) ? (
                      <p className="mt-1 max-w-[13rem] text-[11px] leading-snug text-[#924628]">
                        {reviewStatusDetail(row)}
                      </p>
                    ) : null}
                  </td>
                  <td className="px-6 py-4">
                    <p className="font-mono text-[11px] text-slate-600" title={row.snapshotHash}>
                      {row.snapshotHash.slice(0, 12)}…
                    </p>
                    <p className="mt-1 font-mono text-[10px] text-slate-400" title={row.answerHash}>
                      answer {row.answerHash.slice(0, 10)}…
                    </p>
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-xs text-slate-600">
                    {row.retrievedAt ? format(new Date(row.retrievedAt), "HH:mm · dd/MM/yyyy", { locale: vi }) : "—"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {rows.length > 0 ? (
        <div className="flex items-center justify-between border-t border-[#e9f6f3] bg-[#e9f6f3]/20 px-6 py-4">
          <span className="text-xs font-medium text-[#3d4947]">
            {rows.length.toLocaleString("vi-VN")} / {total.toLocaleString("vi-VN")} citation
          </span>
          <div className="flex items-center gap-2">
            <button
              type="button"
              disabled={applied.page <= 0}
              onClick={() => setApplied((p) => ({ ...p, page: Math.max(0, p.page - 1) }))}
              className="flex h-8 w-8 items-center justify-center rounded-lg text-[#3d4947] transition hover:bg-[#deebe8] disabled:opacity-40"
              aria-label="Trang citation trước"
              title="Trang citation trước"
            >
              <span className="material-symbols-outlined text-base">chevron_left</span>
            </button>
            <span className="text-xs font-semibold text-slate-600">{applied.page + 1}/{totalPages}</span>
            <button
              type="button"
              disabled={applied.page + 1 >= totalPages}
              onClick={() => setApplied((p) => ({ ...p, page: p.page + 1 }))}
              className="flex h-8 w-8 items-center justify-center rounded-lg text-[#3d4947] transition hover:bg-[#deebe8] disabled:opacity-40"
              aria-label="Trang citation sau"
              title="Trang citation sau"
            >
              <span className="material-symbols-outlined text-base">chevron_right</span>
            </button>
          </div>
        </div>
      ) : null}
    </section>
  );
}

export default function AuditLogPage() {
  const queryClient = useQueryClient();
  const router = useRouter();
  const searchParams = useSearchParams();
  const initial = useMemo(() => resolveInitialAuditState(searchParams), [searchParams]);

  const [viewScope, setViewScope] = useState<AuditViewScope>(initial.viewScope);
  const [draft, setDraft] = useState(initial.filters);
  const [applied, setApplied] = useState<ListQuery>({
    ...initial.filters,
    page: 0,
    limit: DEFAULT_LIMIT,
  });
  const [detailEntry, setDetailEntry] = useState<AuditLogEntry | null>(null);
  const [exporting, setExporting] = useState(false);
  const [exportMessage, setExportMessage] = useState<string | null>(null);
  const [dateRangeError, setDateRangeError] = useState<string | null>(null);

  useEffect(() => {
    const resolved = resolveInitialAuditState(searchParams);
    setViewScope(resolved.viewScope);
    setDraft(resolved.filters);
    setApplied((prev) => ({
      ...resolved.filters,
      page: 0,
      limit: prev.limit,
    }));
  }, [searchParams]);

  useEffect(() => {
    const id = "hl-material-symbols-outlined";
    if (document.getElementById(id)) {
      return;
    }
    const link = document.createElement("link");
    link.id = id;
    link.rel = "stylesheet";
    link.href =
      "https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0";
    document.head.appendChild(link);
  }, []);

  const { data: metrics = [], isLoading: metricsLoading } = useQuery({
    queryKey: ["admin-reference-metrics", "audit-filters"],
    queryFn: async () => {
      const res = await adminApiClient.get<ReferenceMetricOption[]>(
        ApiPaths.ADMIN.REFERENCE_METRICS,
      );
      return res.data;
    },
  });

  const listQuery = useQuery({
    queryKey: ["admin-audit-logs", applied],
    queryFn: async () => {
      const res = await adminApiClient.get<AuditLogPage>(ApiPaths.ADMIN.AUDIT_LOGS, {
        params: buildListParams(applied),
      });
      return res.data;
    },
  });

  const normalizeFilters = useCallback(
    (filters: Omit<ListQuery, "page" | "limit">): Omit<ListQuery, "page" | "limit"> => ({
      ...filters,
      resourceType:
        viewScope === "reference" ? RESOURCE_TYPE_REFERENCE_DATA : filters.resourceType,
      action:
        viewScope === "reference" && filters.action && !REFERENCE_ACTIONS.has(filters.action)
          ? ""
          : filters.action,
    }),
    [viewScope],
  );

  const applyFilters = useCallback(() => {
    const rangeError = validateDateRange(draft.from, draft.to);
    if (rangeError) {
      setDateRangeError(rangeError);
      return;
    }
    setDateRangeError(null);
    setExportMessage(null);
    const next = normalizeFilters(draft);
    setDraft(next);
    setApplied((prev) => ({
      ...prev,
      ...next,
      page: 0,
    }));
    router.replace(buildAuditLogUrl(viewScope, next), { scroll: false });
  }, [draft, normalizeFilters, router, viewScope]);

  const resetDraft = useCallback(() => {
    const cleared: Omit<ListQuery, "page" | "limit"> = {
      resourceType: viewScope === "reference" ? RESOURCE_TYPE_REFERENCE_DATA : "",
      resourceId: "",
      actorEmail: "",
      action: "",
      from: "",
      to: "",
    };
    setDateRangeError(null);
    setExportMessage(null);
    setDraft(cleared);
    setApplied((prev) => ({
      ...prev,
      ...cleared,
      page: 0,
    }));
    router.replace(buildAuditLogUrl(viewScope, cleared), { scroll: false });
  }, [router, viewScope]);

  const handleViewScopeChange = useCallback(
    (scope: AuditViewScope) => {
      setViewScope(scope);
      setDateRangeError(null);
      setExportMessage(null);
      const next: Omit<ListQuery, "page" | "limit"> = {
        ...draft,
        resourceType: scope === "reference" ? RESOURCE_TYPE_REFERENCE_DATA : "",
        resourceId: scope === "reference" ? draft.resourceId : "",
        action:
          scope === "reference" && draft.action && !REFERENCE_ACTIONS.has(draft.action)
            ? ""
            : draft.action,
      };
      setDraft(next);
      setApplied((prev) => ({
        ...prev,
        ...next,
        page: 0,
      }));
      router.replace(buildAuditLogUrl(scope, next), { scroll: false });
    },
    [draft, router],
  );

  const totalPages = useMemo(() => {
    if (!listQuery.data) {
      return 0;
    }
    const { totalElements, limit } = listQuery.data;
    if (limit <= 0) {
      return 0;
    }
    return Math.max(1, Math.ceil(totalElements / limit));
  }, [listQuery.data]);

  const pageButtons = useMemo(
    () => buildPageList(applied.page, totalPages),
    [applied.page, totalPages],
  );

  const rows = listQuery.data?.content ?? [];
  const total = listQuery.data?.totalElements ?? 0;

  const handleExportCsv = useCallback(async () => {
    if (total === 0) {
      setExportMessage("Không có dữ liệu để xuất với bộ lọc hiện tại.");
      return;
    }
    setExportMessage(null);
    setExporting(true);
    try {
      const params = buildListParams(applied);
      delete params.page;
      delete params.limit;

      const search = new URLSearchParams();
      Object.entries(params).forEach(([k, v]) => {
        search.set(k, String(v));
      });

      const url = `${ApiPaths.ADMIN.AUDIT_LOGS_EXPORT}?${search.toString()}`;
      const response = await adminApiClient.get(url, { responseType: "blob" });

      const blob = new Blob([response.data], { type: "text/csv;charset=utf-8" });
      const href = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = href;
      a.download = "system-audit-logs.csv";
      a.click();
      URL.revokeObjectURL(href);
    } finally {
      setExporting(false);
    }
  }, [applied, total]);

  const showMetricFilter =
    viewScope === "reference" ||
    draft.resourceType === "" ||
    draft.resourceType === RESOURCE_TYPE_REFERENCE_DATA;

  const hasActiveFilters = useMemo(
    () =>
      Boolean(
        applied.from ||
          applied.to ||
          applied.resourceId ||
          applied.actorEmail.trim() ||
          applied.action ||
          (viewScope === "all" && applied.resourceType),
      ),
    [applied, viewScope],
  );

  const actionFilterGroups =
    viewScope === "reference" ? [REFERENCE_ACTION_GROUP] : ACTION_FILTER_GROUPS;

  const appliedFilterChips = useMemo(() => {
    const chips: string[] = [];
    if (applied.from || applied.to) {
      try {
        const fromLabel = applied.from
          ? format(parseISO(applied.from), "dd/MM/yyyy", { locale: vi })
          : "…";
        const toLabel = applied.to
          ? format(parseISO(applied.to), "dd/MM/yyyy", { locale: vi })
          : "…";
        chips.push(`Thời gian: ${fromLabel} – ${toLabel}`);
      } catch {
        chips.push("Thời gian đã chọn");
      }
    }
    if (applied.resourceType) {
      chips.push(RESOURCE_TYPE_LABEL_VI[applied.resourceType] ?? applied.resourceType);
    }
    const metricName = metrics.find((m) => m.id === applied.resourceId)?.displayNameVi;
    if (applied.resourceId && metricName) {
      chips.push(`Chỉ số: ${metricName}`);
    }
    if (applied.actorEmail.trim()) {
      chips.push(`Người thực hiện: ${applied.actorEmail.trim()}`);
    }
    if (applied.action) {
      chips.push(`Hành động: ${actionLabelVi(applied.action)}`);
    }
    return chips;
  }, [applied, metrics]);

  return (
    <div className="space-y-6 text-slate-900">
      <header className="flex flex-col gap-4 rounded-[28px] bg-white p-6 shadow-sm ring-1 ring-slate-200 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0">
          <h1 className="flex items-center gap-3 text-2xl font-bold text-slate-900">
            <ClipboardList className="h-7 w-7 shrink-0 text-teal-600" aria-hidden="true" />
            Nhật ký hoạt động
          </h1>
          <p className="mt-2 max-w-3xl text-sm text-slate-500">
            {viewScope === "reference"
              ? "Truy vết ai đã thay đổi dữ liệu tham chiếu, khi nào và nội dung trước/sau."
              : "Xem toàn bộ hoạt động quản trị và người dùng trên hệ thống."}
          </p>
          <div
            className="mt-4 inline-flex rounded-full bg-slate-100 p-1"
            role="tablist"
            aria-label="Phạm vi nhật ký"
          >
            <button
              type="button"
              role="tab"
              aria-selected={viewScope === "reference"}
              onClick={() => handleViewScopeChange("reference")}
              className={`rounded-full px-4 py-2 text-xs font-semibold transition ${
                viewScope === "reference"
                  ? "bg-white text-teal-800 shadow-sm"
                  : "text-slate-600 hover:text-teal-700"
              }`}
            >
              Dữ liệu tham chiếu
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={viewScope === "all"}
              onClick={() => handleViewScopeChange("all")}
              className={`rounded-full px-4 py-2 text-xs font-semibold transition ${
                viewScope === "all"
                  ? "bg-white text-teal-800 shadow-sm"
                  : "text-slate-600 hover:text-teal-700"
              }`}
            >
              Toàn hệ thống
            </button>
          </div>
        </div>
        <div className="flex flex-col items-stretch gap-2 sm:items-end">
          {exportMessage ? (
            <p className="max-w-xs text-right text-xs font-medium text-amber-800" role="status">
              {exportMessage}
            </p>
          ) : null}
          <button
          type="button"
          onClick={handleExportCsv}
          disabled={exporting || total === 0}
          className="inline-flex shrink-0 items-center justify-center gap-2 rounded-xl bg-teal-700 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {exporting ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <span className="material-symbols-outlined text-lg">download</span>
          )}
          Xuất CSV
        </button>
        </div>
      </header>

      <div className="rounded-[28px] bg-white p-5 shadow-sm ring-1 ring-slate-200 sm:p-6">
        <p className="mb-4 text-xs font-bold uppercase tracking-wider text-slate-500">
          Bộ lọc
        </p>

        {dateRangeError ? (
          <p className="mb-4 rounded-xl border border-[#ffb59a]/50 bg-[#ffb59a]/10 px-3 py-2 text-sm text-[#924628]">
            {dateRangeError}
          </p>
        ) : null}

        <div
          className={`grid grid-cols-1 gap-4 md:grid-cols-2 ${
            viewScope === "all" ? "xl:grid-cols-4" : "xl:grid-cols-3"
          }`}
        >
          <div>
            <label className="mb-2 block text-xs font-bold uppercase text-[#0d9488]">
              Từ ngày
            </label>
            <input
              type="date"
              className="w-full rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
              value={draft.from}
              onChange={(e) => {
                setDateRangeError(null);
                setDraft((d) => ({ ...d, from: e.target.value }));
              }}
            />
          </div>
          <div>
            <label className="mb-2 block text-xs font-bold uppercase text-[#0d9488]">
              Đến ngày
            </label>
            <input
              type="date"
              className="w-full rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
              value={draft.to}
              onChange={(e) => {
                setDateRangeError(null);
                setDraft((d) => ({ ...d, to: e.target.value }));
              }}
            />
          </div>
          <div>
            <label className="mb-2 block text-xs font-bold uppercase text-[#0d9488]">
              Chỉ số
            </label>
            <div className="relative">
              <select
                className="w-full cursor-pointer appearance-none rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 pr-10 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15 disabled:cursor-not-allowed disabled:opacity-50"
                value={draft.resourceId}
                disabled={!showMetricFilter || metricsLoading}
                onChange={(e) => setDraft((d) => ({ ...d, resourceId: e.target.value }))}
              >
                <option value="">Tất cả chỉ số</option>
                {metrics.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.displayNameVi}
                  </option>
                ))}
              </select>
              <span className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-[#00685f]">
                ▾
              </span>
            </div>
            {viewScope === "all" && !showMetricFilter ? (
              <p className="mt-1 text-[10px] text-[#3d4947]">
                Chỉ lọc theo chỉ số khi loại tài nguyên là dữ liệu tham chiếu.
              </p>
            ) : null}
          </div>
          {viewScope === "all" ? (
            <div>
              <label className="mb-2 block text-xs font-bold uppercase text-[#0d9488]">
                Loại tài nguyên
              </label>
              <div className="relative">
                <select
                  className="w-full cursor-pointer appearance-none rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 pr-10 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
                  value={draft.resourceType}
                  onChange={(e) =>
                    setDraft((d) => ({
                      ...d,
                      resourceType: e.target.value,
                      resourceId:
                        e.target.value === "" || e.target.value === RESOURCE_TYPE_REFERENCE_DATA
                          ? d.resourceId
                          : "",
                    }))
                  }
                >
                  <option value="">Tất cả loại</option>
                  <option value={RESOURCE_TYPE_REFERENCE_DATA}>
                    {RESOURCE_TYPE_LABEL_VI[RESOURCE_TYPE_REFERENCE_DATA]}
                  </option>
                  <option value={RESOURCE_TYPE_HEALTH_RECORD}>
                    {RESOURCE_TYPE_LABEL_VI[RESOURCE_TYPE_HEALTH_RECORD]}
                  </option>
                  <option value={RESOURCE_TYPE_PROFILE}>
                    {RESOURCE_TYPE_LABEL_VI[RESOURCE_TYPE_PROFILE]}
                  </option>
                  <option value={RESOURCE_TYPE_AUTH}>
                    {RESOURCE_TYPE_LABEL_VI[RESOURCE_TYPE_AUTH]}
                  </option>
                  <option value={RESOURCE_TYPE_USER}>
                    {RESOURCE_TYPE_LABEL_VI[RESOURCE_TYPE_USER]}
                  </option>
                  <option value={RESOURCE_TYPE_CONSENT}>
                    {RESOURCE_TYPE_LABEL_VI[RESOURCE_TYPE_CONSENT]}
                  </option>
                </select>
                <span className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-[#00685f]">
                  ▾
                </span>
              </div>
            </div>
          ) : null}
        </div>

        <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
          <div>
            <label className="mb-2 block text-xs font-bold uppercase text-[#0d9488]">
              Người thực hiện
            </label>
            <input
              type="text"
              className="w-full rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
              placeholder="Email admin hoặc người dùng…"
              value={draft.actorEmail}
              onChange={(e) => setDraft((d) => ({ ...d, actorEmail: e.target.value }))}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  applyFilters();
                }
              }}
            />
          </div>
          <div>
            <label className="mb-2 block text-xs font-bold uppercase text-[#0d9488]">
              Loại hành động
            </label>
            <div className="relative">
              <select
                className="w-full cursor-pointer appearance-none rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 pr-10 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
                value={draft.action}
                onChange={(e) => setDraft((d) => ({ ...d, action: e.target.value }))}
              >
                <option value="">Tất cả hành động</option>
                {actionFilterGroups.map((group) => (
                  <optgroup key={group.label} label={group.label}>
                    {group.actions.map((action) => (
                      <option key={action} value={action}>
                        {ACTION_LABEL_VI[action] ?? action}
                      </option>
                    ))}
                  </optgroup>
                ))}
              </select>
              <span className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-[#00685f]">
                ▾
              </span>
            </div>
          </div>
        </div>

        <div className="mt-5 flex flex-col gap-3 border-t border-[#e9f6f3] pt-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex min-h-[1.75rem] flex-wrap gap-2">
            {appliedFilterChips.length === 0 ? (
              <span className="text-xs text-[#3d4947]">Chưa áp dụng bộ lọc nào</span>
            ) : (
              appliedFilterChips.map((chip) => (
                <span
                  key={chip}
                  className="rounded-full bg-[#e9f6f3] px-3 py-1 text-[11px] font-semibold text-[#134e4a]"
                >
                  {chip}
                </span>
              ))
            )}
          </div>
          <div className="flex shrink-0 items-center gap-2">
            <button
              type="button"
              onClick={resetDraft}
              className="rounded-full px-4 py-2 text-sm font-semibold text-[#3d4947] transition hover:bg-[#e9f6f3] hover:text-[#00685f]"
            >
              Đặt lại
            </button>
            <button
              type="button"
              onClick={applyFilters}
              className="rounded-full bg-[#00685f] px-5 py-2 text-sm font-bold text-white transition hover:bg-[#005049]"
            >
              Áp dụng
            </button>
          </div>
        </div>
      </div>

      <div className="flex flex-col overflow-hidden rounded-[28px] bg-white shadow-sm ring-1 ring-slate-200">
        <div className="flex flex-col justify-between gap-3 border-b border-slate-200 bg-slate-50 px-6 py-4 sm:flex-row sm:items-center">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-teal-700">history</span>
            <span className="font-bold text-slate-800">
              {listQuery.isLoading
                ? "Đang tải…"
                : `${total.toLocaleString("vi-VN")} bản ghi`}
            </span>
          </div>
          <button
            type="button"
            onClick={() => queryClient.invalidateQueries({ queryKey: ["admin-audit-logs"] })}
            className="inline-flex items-center justify-center rounded-lg p-2 text-slate-600 transition hover:bg-teal-50"
            title="Tải lại"
            aria-label="Tải lại"
          >
            <span
              className={`material-symbols-outlined text-[20px] ${listQuery.isFetching ? "inline-block animate-spin" : ""}`}
            >
              refresh
            </span>
          </button>
        </div>
        {listQuery.isLoading ? (
          <div className="flex items-center justify-center gap-2 py-20 text-[#3d4947]">
            <Loader2 className="h-6 w-6 animate-spin text-[#00685f]" />
            Đang tải…
          </div>
        ) : listQuery.isError ? (
          <div className="p-10 text-center text-[#ba1a1a]">
            Không thể tải nhật ký. Vui lòng thử lại sau.
          </div>
        ) : rows.length === 0 ? (
          <div className="p-14 text-center text-sm text-[#3d4947]">
            {hasActiveFilters
              ? "Không có bản ghi phù hợp với bộ lọc hiện tại."
              : viewScope === "reference"
                ? "Chưa có hoạt động nào trên dữ liệu tham chiếu."
                : "Chưa có hoạt động được ghi nhận."}
          </div>
        ) : (
          <div className="hl-custom-scrollbar overflow-x-auto">
            <table className="w-full border-collapse text-left">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50">
                  <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">
                    Thời gian
                  </th>
                  <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">
                    Người dùng
                  </th>
                  <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">
                    Hành động
                  </th>
                  <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">
                    Trạng thái
                  </th>
                  <th className="whitespace-nowrap px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">
                    Tóm tắt
                  </th>
                  <th className="whitespace-nowrap px-6 py-3 text-center text-xs font-semibold uppercase tracking-wider text-slate-500">
                    IP
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rows.map((row) => {
                  const at = new Date(row.createdAt);
                  const email = row.actorEmail?.trim() || "";
                  return (
                    <tr key={row.id} className="transition-colors hover:bg-teal-50/30">
                      <td className="px-6 py-4">
                        <div className="flex flex-col">
                          <span className="text-sm font-bold text-[#121e1c]">
                            {format(at, "HH:mm:ss", { locale: vi })}
                          </span>
                          <span className="text-[10px] font-medium text-[#3d4947]">
                            {format(at, "dd/MM/yyyy", { locale: vi })}
                          </span>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div
                            className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-[11px] font-bold ${avatarBadgeClass(email)}`}
                            aria-hidden
                          >
                            {initialsFromEmail(email)}
                          </div>
                          <div className="min-w-0 leading-tight">
                            <span className="block truncate text-sm font-bold text-[#121e1c]">
                              {displayNameFromEmail(email)}
                            </span>
                            <span className="block truncate text-[10px] italic text-[#3d4947]">
                              {email || "—"}
                            </span>
                          </div>
                        </div>
                      </td>
                      <td className="whitespace-nowrap px-6 py-4">
                        <span
                          className={`inline-block rounded px-2 py-1 text-xs font-semibold ${actionBadgeClass(row.action)}`}
                        >
                          {actionLabelVi(row.action)}
                        </span>
                      </td>
                      <td className="whitespace-nowrap px-6 py-4">
                        <span
                          className={`inline-block rounded px-2 py-1 text-xs font-semibold ${outcomeBadgeClass(resolveOutcome(row))}`}
                        >
                          {outcomeLabelVi(resolveOutcome(row))}
                        </span>
                      </td>
                      <td className="max-w-md px-6 py-4">
                        <p
                          className="line-clamp-2 text-xs leading-relaxed text-slate-600"
                          title={row.detailSummary?.trim() || undefined}
                        >
                          {row.detailSummary?.trim() || "—"}
                        </p>
                        <button
                          type="button"
                          onClick={() => setDetailEntry(row)}
                          className="mt-1 text-xs font-semibold text-teal-700 hover:underline"
                          aria-label={`Xem chi tiết: ${row.entityLabel || row.action}`}
                        >
                          Xem chi tiết
                        </button>
                      </td>
                      <td className="px-6 py-4 text-center">
                        <span className="inline-block rounded bg-slate-100 px-2 py-0.5 font-mono text-[11px] text-slate-600">
                          {row.ipAddress ?? "—"}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {!listQuery.isLoading &&
        !listQuery.isError &&
        rows.length > 0 ? (
          <div className="flex flex-col items-center justify-between gap-4 border-t border-[#e9f6f3] bg-[#e9f6f3]/20 px-6 py-4 sm:flex-row">
            <span className="text-xs font-medium text-[#3d4947]">
              Đang hiển thị {rows.length.toLocaleString("vi-VN")} trên {total.toLocaleString("vi-VN")}{" "}
              nhật ký
            </span>
            <div className="flex flex-wrap items-center justify-center gap-1">
              <button
                type="button"
                disabled={applied.page <= 0}
                onClick={() => setApplied((p) => ({ ...p, page: 0 }))}
                className="flex h-8 w-8 items-center justify-center rounded-lg text-[#3d4947] transition hover:bg-[#deebe8] disabled:opacity-40"
                aria-label="Trang đầu"
                title="Trang đầu"
              >
                <span className="material-symbols-outlined text-base" aria-hidden="true">first_page</span>
              </button>
              <button
                type="button"
                disabled={applied.page <= 0}
                onClick={() => setApplied((p) => ({ ...p, page: Math.max(0, p.page - 1) }))}
                className="flex h-8 w-8 items-center justify-center rounded-lg text-[#3d4947] transition hover:bg-[#deebe8] disabled:opacity-40"
                aria-label="Trang trước"
                title="Trang trước"
              >
                <span className="material-symbols-outlined text-base" aria-hidden="true">chevron_left</span>
              </button>
              {pageButtons.map((item, idx) =>
                item === "ellipsis" ? (
                  <span key={`ellipsis-${idx}`} className="px-2 text-[#3d4947]">
                    ...
                  </span>
                ) : (
                  <button
                    key={`page-${item}`}
                    type="button"
                    onClick={() => setApplied((p) => ({ ...p, page: item }))}
                    className={`flex h-8 w-8 items-center justify-center rounded-lg text-xs font-bold transition ${
                      applied.page === item
                        ? "bg-[#00685f] text-white shadow-sm"
                        : "text-[#3d4947] hover:bg-[#deebe8]"
                    }`}
                  >
                    {item + 1}
                  </button>
                ),
              )}
              <button
                type="button"
                disabled={applied.page + 1 >= totalPages}
                onClick={() => setApplied((p) => ({ ...p, page: p.page + 1 }))}
                className="flex h-8 w-8 items-center justify-center rounded-lg text-[#3d4947] transition hover:bg-[#deebe8] disabled:opacity-40"
                aria-label="Trang sau"
                title="Trang sau"
              >
                <span className="material-symbols-outlined text-base" aria-hidden="true">chevron_right</span>
              </button>
              <button
                type="button"
                disabled={applied.page + 1 >= totalPages}
                onClick={() => setApplied((p) => ({ ...p, page: totalPages - 1 }))}
                className="flex h-8 w-8 items-center justify-center rounded-lg text-[#3d4947] transition hover:bg-[#deebe8] disabled:opacity-40"
                aria-label="Trang cuối"
                title="Trang cuối"
              >
                <span className="material-symbols-outlined text-base" aria-hidden="true">last_page</span>
              </button>
            </div>
          </div>
        ) : null}
      </div>

      <OnlineRagCitationPanel />

      {detailEntry ? (
        <AuditDetailModal entry={detailEntry} onClose={() => setDetailEntry(null)} />
      ) : null}
    </div>
  );
}
