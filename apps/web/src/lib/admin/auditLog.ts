import { format, parseISO } from "date-fns";
import { vi } from "date-fns/locale";

export const RESOURCE_TYPE_REFERENCE_DATA = "REFERENCE_DATA";
export const RESOURCE_TYPE_HEALTH_RECORD = "HEALTH_RECORD";
export const RESOURCE_TYPE_PROFILE = "PROFILE";
export const RESOURCE_TYPE_AUTH = "AUTH";
export const RESOURCE_TYPE_USER = "USER";
export const RESOURCE_TYPE_CONSENT = "CONSENT";
export const RESOURCE_TYPE_OCR_JOB = "OCR_JOB";
export const RESOURCE_TYPE_LLM_CALL = "LLM_CALL";
export const RESOURCE_TYPE_RAG_RETRIEVAL = "RAG_RETRIEVAL";

export type AuditLogOutcome = "SUCCESS" | "FAILURE";

export type AuditLogEntry = {
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
  metadataJson: string | null;
  correlationId: string | null;
  requestId: string | null;
  traceId: string | null;
  ipAddress: string | null;
  createdAt: string;
};

export type AuditLogPage = {
  content: AuditLogEntry[];
  totalElements: number;
  page: number;
  limit: number;
};

export type OnlineRagCitationEntry = {
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

export type OnlineRagCitationPage = {
  content: OnlineRagCitationEntry[];
  totalElements: number;
  page: number;
  limit: number;
};

export type CitationFilters = {
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

export type ListQuery = {
  resourceType: string;
  resourceId: string;
  actorEmail: string;
  action: string;
  correlationId: string;
  from: string;
  to: string;
  page: number;
  limit: number;
};

export const DEFAULT_LIMIT = 20;
export const DEFAULT_CITATION_LIMIT = 10;

export type AuditViewScope = "reference" | "all";

export const ACTION_LABEL_VI: Record<string, string> = {
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
  COMPLETE_ACCOUNT_DELETION: "Hoàn tất xóa tài khoản",
  RECORD_CONSENT: "Đồng ý điều khoản",
  REVOKE_CONSENT: "Thu hồi đồng ý",
  OCR_JOB_SUCCEEDED: "OCR hoàn tất",
  OCR_JOB_FAILED_RETRYABLE: "OCR lỗi có thể thử lại",
  OCR_JOB_FAILED_TERMINAL: "OCR lỗi kết thúc",
  OCR_JOB_DEAD_LETTERED: "OCR vào DLQ",
  LLM_CALL_SUCCEEDED: "LLM thành công",
  LLM_CALL_FAILED: "LLM thất bại",
  RAG_RETRIEVAL: "Truy xuất RAG",
};

export const ACTION_FILTER_GROUPS: { label: string; actions: string[] }[] = [
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
      "COMPLETE_ACCOUNT_DELETION",
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
  {
    label: "AI/OCR/RAG",
    actions: [
      "OCR_JOB_SUCCEEDED",
      "OCR_JOB_FAILED_RETRYABLE",
      "OCR_JOB_FAILED_TERMINAL",
      "OCR_JOB_DEAD_LETTERED",
      "LLM_CALL_SUCCEEDED",
      "LLM_CALL_FAILED",
      "RAG_RETRIEVAL",
    ],
  },
];

export const REFERENCE_ACTION_GROUP = ACTION_FILTER_GROUPS.find(
  (g) => g.label === "Dữ liệu tham chiếu",
)!;

export const REFERENCE_ACTIONS = new Set(REFERENCE_ACTION_GROUP.actions);

export const RESOURCE_TYPE_LABEL_VI: Record<string, string> = {
  [RESOURCE_TYPE_REFERENCE_DATA]: "Dữ liệu tham chiếu",
  [RESOURCE_TYPE_HEALTH_RECORD]: "Hồ sơ sức khỏe",
  [RESOURCE_TYPE_PROFILE]: "Hồ sơ gia đình",
  [RESOURCE_TYPE_AUTH]: "Xác thực",
  [RESOURCE_TYPE_USER]: "Tài khoản",
  [RESOURCE_TYPE_CONSENT]: "Đồng ý điều khoản",
  [RESOURCE_TYPE_OCR_JOB]: "OCR job",
  [RESOURCE_TYPE_LLM_CALL]: "LLM call",
  [RESOURCE_TYPE_RAG_RETRIEVAL]: "RAG retrieval",
};

export type ReferenceMetricOption = {
  id: string;
  name: string;
  displayNameVi: string;
  unit: string;
};

export function validateDateRange(from: string, to: string): string | null {
  if (from && to && from > to) {
    return "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.";
  }
  return null;
}

export function auditFiltersFromSearchParams(
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
    correlationId: searchParams.get("correlationId")?.trim() ?? "",
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
  if (filters.correlationId.trim()) {
    params.set("correlationId", filters.correlationId.trim());
  }
  if (filters.from) {
    params.set("from", filters.from);
  }
  if (filters.to) {
    params.set("to", filters.to);
  }
}

export function buildAuditLogUrl(
  scope: AuditViewScope,
  filters: Omit<ListQuery, "page" | "limit">,
): string {
  const params = new URLSearchParams();
  if (scope === "all") {
    params.set("view", "all");
  }
  appendAuditFilterParams(params, scope, filters);
  const qs = params.toString();
  return qs ? `/admin/audit-log?${qs}` : "/admin/audit-log";
}

export function traceIdentifiersForDisplay(entry: {
  correlationId?: string | null;
  requestId?: string | null;
  traceId?: string | null;
}): Array<[string, string]> {
  return [
    ["Correlation ID", entry.correlationId],
    ["Request ID", entry.requestId],
    ["Trace ID", entry.traceId],
  ].filter((item): item is [string, string] => typeof item[1] === "string" && item[1].trim().length > 0);
}

export function buildTraceOnlyFilters(correlationId: string): Omit<ListQuery, "page" | "limit"> {
  return {
    resourceType: "",
    resourceId: "",
    actorEmail: "",
    action: "",
    correlationId: correlationId.trim(),
    from: "",
    to: "",
  };
}

export function formatJsonBlock(raw: string | null | undefined): string {
  if (raw == null || raw === "") {
    return "—";
  }
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

export function actionLabelVi(action: string): string {
  return ACTION_LABEL_VI[action] ?? action;
}

export function resolveOutcome(entry: Pick<AuditLogEntry, "outcome" | "action">): AuditLogOutcome {
  if (entry.outcome === "FAILURE" || entry.outcome === "SUCCESS") {
    return entry.outcome;
  }
  return entry.action === "LOGIN_FAILED" ||
    entry.action.endsWith("_FAILED") ||
    entry.action.includes("_FAILED_") ||
    entry.action.endsWith("_DEAD_LETTERED")
    ? "FAILURE"
    : "SUCCESS";
}

export function outcomeLabelVi(outcome: AuditLogOutcome): string {
  return outcome === "FAILURE" ? "Thất bại" : "Thành công";
}

export function outcomeBadgeClass(outcome: AuditLogOutcome): string {
  return outcome === "FAILURE"
    ? "bg-[#924628]/10 text-[#924628]"
    : "bg-emerald-50 text-emerald-800";
}

export function actionBadgeClass(action: string): string {
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
export function avatarBadgeClass(email: string): string {
  const key = email.trim() || "?";
  const variants = [
    "bg-[#c2ebe3] text-[#456b66]",
    "bg-[#ffb59a] text-[#370e00]",
    "bg-[#deebe8] text-[#115e59]",
    "bg-[#ffdad6] text-[#93000a]",
  ];
  return variants[hashString(key) % variants.length];
}

export function initialsFromEmail(email: string): string {
  const local = email.split("@")[0]?.trim() ?? "";
  if (!local) return "?";
  const parts = local.split(/[._-]+/).filter(Boolean);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase().slice(0, 2);
  }
  return local.slice(0, 2).toUpperCase();
}

/** Hiển thị dòng tên giả lập từ email (prototype có tên + email; API chỉ có email). */
export function displayNameFromEmail(email: string): string {
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

export function rowDetailText(row: AuditLogEntry): string {
  const base =
    row.detailSummary?.trim() ||
    row.entityLabel?.trim() ||
    `${row.resourceType}${row.resourceId ? ` · ${row.resourceId}` : ""}`;
  return base.length > 160 ? `${base.slice(0, 157)}…` : base;
}

export function buildListParams(q: ListQuery): Record<string, string | number> {
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
  if (q.correlationId.trim()) {
    params.correlationId = q.correlationId.trim();
  }
  if (q.from) {
    params.from = q.from;
  }
  if (q.to) {
    params.to = q.to;
  }
  return params;
}

export function buildCitationParams(q: CitationFilters): Record<string, string | number> {
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

export function reviewStatusLabel(status: OnlineRagCitationEntry["reviewStatus"]): string {
  if (status === "APPROVED") return "Đã duyệt";
  if (status === "REJECTED") return "Từ chối";
  return "Cần rà soát";
}

export function reviewStatusClass(entry: OnlineRagCitationEntry): string {
  if (entry.reviewStatus === "APPROVED" && !entry.excluded && entry.usableForAi && !entry.stale) {
    return "bg-emerald-50 text-emerald-800";
  }
  if (entry.reviewStatus === "REJECTED" || entry.excluded) {
    return "bg-[#924628]/10 text-[#924628]";
  }
  return "bg-amber-50 text-amber-800";
}

export function reviewStatusDetail(row: OnlineRagCitationEntry): string | null {
  if (row.stale) {
    return "Snapshot đã quá TTL; không gắn nhãn evidence đã duyệt.";
  }
  if (row.reviewStatus !== "APPROVED" || row.excluded || !row.usableForAi) {
    return "Chỉ là metadata audit; không gắn nhãn evidence đã duyệt.";
  }
  return null;
}

export function buildPageList(current: number, total: number): (number | "ellipsis")[] {
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
  let prev = -1;
  for (const p of sorted) {
    if (out.length > 0 && p - prev > 1) {
      out.push("ellipsis");
    }
    out.push(p);
    prev = p;
  }
  return out;
}

export function jsonPanelLabel(
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

export function resolveInitialAuditState(searchParams: URLSearchParams): {
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

export function auditHasActiveFilters(
  applied: Omit<ListQuery, "page" | "limit">,
  viewScope: AuditViewScope,
): boolean {
  return Boolean(
    applied.from ||
      applied.to ||
      applied.resourceId ||
      applied.actorEmail.trim() ||
      applied.action ||
      applied.correlationId.trim() ||
      (viewScope === "all" && applied.resourceType),
  );
}

export function buildAppliedFilterChips(
  applied: Omit<ListQuery, "page" | "limit">,
  metrics: ReferenceMetricOption[],
): string[] {
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
  if (applied.correlationId.trim()) {
    chips.push(`Trace: ${applied.correlationId.trim()}`);
  }
  return chips;
}
