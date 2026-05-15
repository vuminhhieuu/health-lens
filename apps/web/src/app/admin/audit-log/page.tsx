"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { format, parseISO } from "date-fns";
import { vi } from "date-fns/locale";
import { Loader2, X } from "lucide-react";

import { adminApiClient } from "@/lib/api/adminApiClient";
import { ApiPaths } from "@healthlens/shared/constants";

const RESOURCE_TYPE_REFERENCE_DATA = "REFERENCE_DATA";
const RESOURCE_TYPE_HEALTH_RECORD = "HEALTH_RECORD";
const RESOURCE_TYPE_PROFILE = "PROFILE";
const RESOURCE_TYPE_AUTH = "AUTH";
const RESOURCE_TYPE_USER = "USER";
const RESOURCE_TYPE_CONSENT = "CONSENT";

type AuditLogEntry = {
  id: string;
  actorEmail: string;
  action: string;
  resourceType: string;
  resourceId: string | null;
  entityLabel: string;
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

const DEFAULT_LIMIT = 50;

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

function actionBadgeClass(action: string): string {
  if (
    action.includes("DELETE") ||
    action.includes("REJECT") ||
    action.includes("FAILED") ||
    action.includes("REVOKE") ||
    action.includes("DEACTIVATE")
  ) {
    return "bg-[#924628]/10 text-[#924628]";
  }
  if (
    action.includes("CREATE") ||
    action.includes("INSERT") ||
    action.includes("APPROVE") ||
    action.includes("LOGIN") ||
    action.includes("REGISTER") ||
    action.includes("ACCEPT")
  ) {
    return "bg-[#c2ebe3] text-[#456b66]";
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

function detailSummary(row: AuditLogEntry): string {
  const base = row.entityLabel?.trim() || `${row.resourceType}${row.resourceId ? ` · ${row.resourceId}` : ""}`;
  return base.length > 120 ? `${base.slice(0, 117)}…` : base;
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

function AuditDetailModal({
  entry,
  onClose,
}: {
  entry: AuditLogEntry;
  onClose: () => void;
}) {
  return (
    <div
      className="fixed inset-0 z-[70] flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-labelledby="audit-detail-title"
    >
      <div className="flex max-h-[90vh] w-full max-w-5xl flex-col overflow-hidden rounded-[2rem] bg-white shadow-2xl">
        <div className="flex items-start justify-between border-b border-[#d8e5e2] px-6 py-5">
          <div>
            <h3 id="audit-detail-title" className="text-xl font-bold text-[#121e1c]">
              Chi tiết thay đổi
            </h3>
            <p className="mt-1 text-sm text-[#3d4947]">
              {actionLabelVi(entry.action)} · {entry.entityLabel}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full p-2 text-[#3d4947] transition hover:bg-[#e9f6f3]"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="grid flex-1 gap-4 overflow-hidden p-6 md:grid-cols-2">
          <div className="flex min-h-0 flex-col rounded-2xl border border-[#d8e5e2] bg-[#f8fafc]">
            <p className="border-b border-[#d8e5e2] bg-white px-4 py-2 text-[11px] font-bold uppercase tracking-wider text-[#3d4947]">
              Trước khi thay đổi
            </p>
            <pre className="hl-custom-scrollbar max-h-[55vh] flex-1 overflow-auto p-4 text-xs leading-relaxed text-[#121e1c]">
              {formatJsonBlock(entry.oldValueJson)}
            </pre>
          </div>
          <div className="flex min-h-0 flex-col rounded-2xl border border-[#89f5e7]/40 bg-[#e9f6f3]/60">
            <p className="border-b border-[#89f5e7]/40 bg-white px-4 py-2 text-[11px] font-bold uppercase tracking-wider text-[#00685f]">
              Sau khi thay đổi
            </p>
            <pre className="hl-custom-scrollbar max-h-[55vh] flex-1 overflow-auto p-4 text-xs leading-relaxed text-[#121e1c]">
              {formatJsonBlock(entry.newValueJson)}
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function AuditLogPage() {
  const queryClient = useQueryClient();
  const [draft, setDraft] = useState({
    resourceType: "",
    resourceId: "",
    actorEmail: "",
    action: "",
    from: "",
    to: "",
  });
  const [applied, setApplied] = useState<ListQuery>({
    resourceType: "",
    resourceId: "",
    actorEmail: "",
    action: "",
    from: "",
    to: "",
    page: 0,
    limit: DEFAULT_LIMIT,
  });
  const [detailEntry, setDetailEntry] = useState<AuditLogEntry | null>(null);
  const [exporting, setExporting] = useState(false);

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

  const applyFilters = useCallback(() => {
    setApplied((prev) => ({
      ...prev,
      ...draft,
      page: 0,
    }));
  }, [draft]);

  const resetDraft = useCallback(() => {
    setDraft({ resourceType: "", resourceId: "", actorEmail: "", action: "", from: "", to: "" });
    setApplied((prev) => ({
      ...prev,
      resourceType: "",
      resourceId: "",
      actorEmail: "",
      action: "",
      from: "",
      to: "",
      page: 0,
    }));
  }, []);

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

  const handleExportCsv = useCallback(async () => {
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
  }, [applied]);

  const rows = listQuery.data?.content ?? [];
  const total = listQuery.data?.totalElements ?? 0;

  const showMetricFilter =
    draft.resourceType === "" || draft.resourceType === RESOURCE_TYPE_REFERENCE_DATA;

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
    <section className="-mx-6 -mt-2 mb-2 min-h-0 bg-[#effcf9] px-8 pb-12 pt-2 text-[#121e1c] antialiased">
      <header className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <nav className="mb-1 flex text-[10px] font-bold uppercase tracking-widest text-[#00685f]/60">
            <span>Admin</span>
            <span className="mx-2">/</span>
            <span className="text-[#00685f]">Nhật ký hoạt động</span>
          </nav>
          <h1 className="text-xl font-bold tracking-tight text-teal-800">Nhật ký hoạt động</h1>
          <p className="mt-1 max-w-2xl text-sm text-[#3d4947]">
            Truy vết ai đã thay đổi dữ liệu tham chiếu, khi nào và nội dung trước/sau.
          </p>
        </div>
        <button
          type="button"
          onClick={handleExportCsv}
          disabled={exporting}
          className="inline-flex shrink-0 items-center justify-center gap-2 rounded-full bg-[#00685f] px-5 py-2.5 text-sm font-bold text-white shadow-sm transition hover:bg-[#005049] disabled:cursor-not-allowed disabled:opacity-50"
        >
          {exporting ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <span className="material-symbols-outlined text-lg">download</span>
          )}
          Xuất CSV
        </button>
      </header>

      <div className="mb-6 rounded-3xl border border-[#deebe8]/80 bg-white p-5 shadow-sm sm:p-6">
        <p className="mb-4 text-[11px] font-black uppercase tracking-widest text-[#0d9488]">
          Bộ lọc
        </p>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
          <div>
            <label className="mb-2 block text-xs font-bold uppercase text-[#0d9488]">
              Từ ngày
            </label>
            <input
              type="date"
              className="w-full rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
              value={draft.from}
              onChange={(e) => setDraft((d) => ({ ...d, from: e.target.value }))}
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
              onChange={(e) => setDraft((d) => ({ ...d, to: e.target.value }))}
            />
          </div>
          <div>
            <label className="mb-2 block text-xs font-bold uppercase text-[#0d9488]">
              Chỉ số
            </label>
            <select
              className="w-full cursor-pointer appearance-none rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15 disabled:cursor-not-allowed disabled:opacity-50"
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
            {!showMetricFilter ? (
              <p className="mt-1 text-[10px] text-[#3d4947]">
                Chỉ lọc theo chỉ số khi loại tài nguyên là dữ liệu tham chiếu.
              </p>
            ) : null}
          </div>
          <div>
            <label className="mb-2 block text-xs font-bold uppercase text-[#0d9488]">
              Loại tài nguyên
            </label>
            <select
              className="w-full cursor-pointer appearance-none rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
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
          </div>
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
            <select
              className="w-full cursor-pointer appearance-none rounded-xl border border-[#deebe8] bg-[#f8fafc] px-3 py-2.5 text-sm outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#00685f]/15"
              value={draft.action}
              onChange={(e) => setDraft((d) => ({ ...d, action: e.target.value }))}
            >
              <option value="">Tất cả hành động</option>
              {ACTION_FILTER_GROUPS.map((group) => (
                <optgroup key={group.label} label={group.label}>
                  {group.actions.map((action) => (
                    <option key={action} value={action}>
                      {ACTION_LABEL_VI[action] ?? action}
                    </option>
                  ))}
                </optgroup>
              ))}
            </select>
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

      <div className="flex flex-col overflow-hidden rounded-3xl bg-white shadow-sm">
        <div className="flex flex-col justify-between gap-3 border-b border-[#e9f6f3] bg-[#e9f6f3]/50 px-6 py-4 sm:flex-row sm:items-center">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-[#00685f]">history</span>
            <span className="font-bold text-[#134e4a]">
              {listQuery.isLoading
                ? "Đang tải…"
                : `${total.toLocaleString("vi-VN")} bản ghi`}
            </span>
          </div>
          <button
            type="button"
            onClick={() => queryClient.invalidateQueries({ queryKey: ["admin-audit-logs"] })}
            className="inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-semibold text-[#0d9488] transition hover:bg-[#ccfbf1]"
            title="Tải lại"
          >
            <span
              className={`material-symbols-outlined text-[20px] ${listQuery.isFetching ? "inline-block animate-spin" : ""}`}
            >
              refresh
            </span>
            Tải lại
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
            Không có bản ghi phù hợp với bộ lọc hiện tại.
          </div>
        ) : (
          <div className="hl-custom-scrollbar overflow-x-auto">
            <table className="w-full border-collapse text-left">
              <thead>
                <tr className="border-b border-[#d8e5e2] bg-[#deebe8]/30">
                  <th className="px-6 py-4 text-[11px] font-black uppercase tracking-widest text-[#0f766e]/70">
                    Thời gian
                  </th>
                  <th className="px-6 py-4 text-[11px] font-black uppercase tracking-widest text-[#0f766e]/70">
                    Người dùng
                  </th>
                  <th className="px-6 py-4 text-[11px] font-black uppercase tracking-widest text-[#0f766e]/70">
                    Hành động
                  </th>
                  <th className="px-6 py-4 text-[11px] font-black uppercase tracking-widest text-[#0f766e]/70">
                    Chi tiết
                  </th>
                  <th className="px-6 py-4 text-center text-[11px] font-black uppercase tracking-widest text-[#0f766e]/70">
                    Địa chỉ IP
                  </th>
                  <th className="px-6 py-4 text-center text-[11px] font-black uppercase tracking-widest text-[#0f766e]/70">
                    Trạng thái
                  </th>
                  <th className="px-6 py-4 text-right text-[11px] font-black uppercase tracking-widest text-[#0f766e]/70">
                    Thao tác
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#e9f6f3]">
                {rows.map((row) => {
                  const at = new Date(row.createdAt);
                  const email = row.actorEmail?.trim() || "";
                  return (
                    <tr key={row.id} className="group transition-colors hover:bg-teal-50/30">
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
                      <td className="px-6 py-4">
                        <span
                          className={`inline-block rounded px-2 py-1 text-xs font-semibold ${actionBadgeClass(row.action)}`}
                        >
                          {actionLabelVi(row.action)}
                        </span>
                      </td>
                      <td className="max-w-xs px-6 py-4">
                        <p className="truncate text-xs text-[#3d4947]" title={detailSummary(row)}>
                          {detailSummary(row)}
                        </p>
                      </td>
                      <td className="px-6 py-4 text-center">
                        <span className="inline-block rounded bg-[#e9f6f3] px-2 py-0.5 font-mono text-[11px] text-[#3d4947]">
                          {row.ipAddress ?? "—"}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-center">
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-[#00685f]/10 px-3 py-1 text-[10px] font-black uppercase tracking-tighter text-[#00685f]">
                          <span className="h-1.5 w-1.5 rounded-full bg-[#00685f]" />
                          Thành công
                        </span>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <button
                          type="button"
                          onClick={() => setDetailEntry(row)}
                          className="inline-flex h-8 w-8 items-center justify-center rounded-full text-[#00685f] opacity-0 transition-all hover:bg-[#00685f]/10 group-hover:opacity-100"
                          title="Xem chi tiết"
                        >
                          <span className="material-symbols-outlined text-lg">visibility</span>
                        </button>
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
                title="Trang đầu"
              >
                <span className="material-symbols-outlined text-base">first_page</span>
              </button>
              <button
                type="button"
                disabled={applied.page <= 0}
                onClick={() => setApplied((p) => ({ ...p, page: Math.max(0, p.page - 1) }))}
                className="flex h-8 w-8 items-center justify-center rounded-lg text-[#3d4947] transition hover:bg-[#deebe8] disabled:opacity-40"
                title="Trang trước"
              >
                <span className="material-symbols-outlined text-base">chevron_left</span>
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
                title="Trang sau"
              >
                <span className="material-symbols-outlined text-base">chevron_right</span>
              </button>
              <button
                type="button"
                disabled={applied.page + 1 >= totalPages}
                onClick={() => setApplied((p) => ({ ...p, page: totalPages - 1 }))}
                className="flex h-8 w-8 items-center justify-center rounded-lg text-[#3d4947] transition hover:bg-[#deebe8] disabled:opacity-40"
                title="Trang cuối"
              >
                <span className="material-symbols-outlined text-base">last_page</span>
              </button>
            </div>
          </div>
        ) : null}
      </div>

      {detailEntry ? (
        <AuditDetailModal entry={detailEntry} onClose={() => setDetailEntry(null)} />
      ) : null}
    </section>
  );
}
