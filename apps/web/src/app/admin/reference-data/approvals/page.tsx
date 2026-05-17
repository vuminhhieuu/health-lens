"use client";

import { Fragment, useCallback, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AlertCircle,
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  Clock,
  FileEdit,
  Loader2,
  RefreshCcw,
  ShieldCheck,
  X,
  XCircle,
} from "lucide-react";

import { adminApiClient } from "@/lib/api/adminApiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { notify } from "@/lib/notify";

const APPROVAL_CHECKBOX_CLASS =
  "h-4 w-4 shrink-0 cursor-pointer rounded border border-slate-400 bg-white shadow-sm outline-none transition-colors checked:border-emerald-600 checked:bg-emerald-600 checked:text-white focus-visible:ring-2 focus-visible:ring-emerald-200 focus-visible:ring-offset-0 disabled:cursor-not-allowed disabled:opacity-50";

type ChangeSetDetail = {
  id: string;
  adminId: string;
  adminEmail?: string | null;
  adminName?: string | null;
  entityType: string;
  entityId: string | null;
  operation: string;
  changesJson: string;
  currentSnapshotJson: string | null;
  status: string;
  createdAt: string;
  approvedAt: string | null;
  reviewerId: string | null;
  reviewerEmail?: string | null;
  reviewerName?: string | null;
  rejectionReason: string | null;
};

type AdminIdentity = {
  id: string;
  email: string | null;
};

function parseApiError(error: unknown) {
  if (
    error &&
    typeof error === "object" &&
    "response" in error &&
    error.response &&
    typeof error.response === "object"
  ) {
    const response = error.response as {
      data?: {
        detail?: string;
        message?: string;
        errors?: Array<{ message?: string }>; 
      };
    };
    const fieldErrors = response.data?.errors
      ?.map((item) => item.message)
      .filter((message): message is string => Boolean(message));

    return fieldErrors && fieldErrors.length > 0
      ? fieldErrors.join(" ")
      : response.data?.detail || response.data?.message || "Không thể xử lý yêu cầu.";
  }
  return "Không thể kết nối tới máy chủ.";
}

type BulkChangeSetActionResult = {
  succeededIds: string[];
  failed: Array<{ id: string; message: string }>;
};

async function runBulkChangeSetRequests(
  changeSetIds: string[],
  request: (changeSetId: string) => Promise<unknown>,
): Promise<BulkChangeSetActionResult> {
  if (changeSetIds.length === 0) {
    return { succeededIds: [], failed: [] };
  }

  const settled = await Promise.allSettled(
    changeSetIds.map(async (changeSetId) => {
      await request(changeSetId);
      return changeSetId;
    }),
  );

  const succeededIds: string[] = [];
  const failed: BulkChangeSetActionResult["failed"] = [];

  settled.forEach((outcome, index) => {
    const id = changeSetIds[index]!;
    if (outcome.status === "fulfilled") {
      succeededIds.push(id);
    } else {
      failed.push({ id, message: parseApiError(outcome.reason) });
    }
  });

  return { succeededIds, failed };
}

function formatBulkChangeSetNotice(
  actionVerb: string,
  result: BulkChangeSetActionResult,
): { type: "success" | "error"; message: string } {
  const total = result.succeededIds.length + result.failed.length;
  if (result.failed.length === 0) {
    return {
      type: "success",
      message: `Đã ${actionVerb} ${result.succeededIds.length} tập dữ liệu thay đổi.`,
    };
  }
  const failureDetails = result.failed
    .map((item) => `${item.id.slice(0, 8)}…: ${item.message}`)
    .join("; ");
  if (result.succeededIds.length === 0) {
    return {
      type: "error",
      message: `Không thể ${actionVerb} ${total} tập dữ liệu thay đổi. ${failureDetails}`,
    };
  }
  return {
    type: "error",
    message: `Đã ${actionVerb} ${result.succeededIds.length}/${total} tập dữ liệu thay đổi. Thất bại: ${failureDetails}`,
  };
}

function getAdminIdentity(): AdminIdentity | null {
  if (typeof window === "undefined") {
    return null;
  }

  const token = sessionStorage.getItem("admin_access_token");
  if (!token) {
    return null;
  }

  const parts = token.split(".");
  if (parts.length < 2) {
    return null;
  }

  try {
    const payload = JSON.parse(atob(parts[1].replace(/-/g, "+").replace(/_/g, "/"))) as {
      sub?: string;
      email?: string;
    };
    if (!payload.sub) {
      return null;
    }
    return {
      id: payload.sub,
      email: payload.email ?? null,
    };
  } catch {
    return null;
  }
}

function formatDate(dateStr: string) {
  const date = new Date(dateStr);
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function operationLabel(op: string) {
  switch (op) {
    case "CREATE":
      return "Tạo mới";
    case "UPDATE":
      return "Cập nhật";
    case "DEACTIVATE":
      return "Ngưng áp dụng";
    default:
      return op;
  }
}

function operationBadgeClass(op: string) {
  switch (op) {
    case "CREATE":
      return "bg-emerald-100 text-emerald-700";
    case "UPDATE":
      return "bg-amber-100 text-amber-700";
    case "DEACTIVATE":
      return "bg-rose-100 text-rose-700";
    default:
      return "bg-slate-100 text-slate-700";
  }
}

type ParsedSnapshot = {
  name?: string;
  displayNameVi?: string;
  unit?: string;
  status?: string;
  ranges?: Array<{
    minValue?: number;
    maxValue?: number;
    attentionMin?: number;
    attentionMax?: number;
    gender?: string | null;
    minAge?: number | null;
    maxAge?: number | null;
  }>;
};

function tryParseSnapshot(json: string | null): ParsedSnapshot | null {
  if (!json) return null;
  try {
    return JSON.parse(json);
  } catch {
    return null;
  }
}

export default function ApprovalsPage() {
  const queryClient = useQueryClient();
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [rejectDialogId, setRejectDialogId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [bulkRejectOpen, setBulkRejectOpen] = useState(false);
  const [bulkRejectReason, setBulkRejectReason] = useState("");
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [notice, setNotice] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);
  const adminIdentity = useMemo(() => getAdminIdentity(), []);

  // Detect multi-admin mode
  const { data: adminConfig, isLoading: isConfigLoading } = useQuery<{ multiAdminMode: boolean }>({
    queryKey: ["admin-config"],
    queryFn: async () => {
      const response = await adminApiClient.get(
        API_ROUTES.ADMIN_REFERENCE_DATA.CONFIG
      );
      return response.data.data;
    },
    staleTime: 60_000,
  });

  const isMultiAdmin = adminConfig?.multiAdminMode ?? false;

  const {
    data: changeSets = [],
    isLoading,
    isFetching,
  } = useQuery<ChangeSetDetail[]>({
    queryKey: ["admin-pending-change-sets"],
    queryFn: async () => {
      const response = await adminApiClient.get(
        API_ROUTES.ADMIN_REFERENCE_DATA.CHANGE_SETS
      );
      return response.data.data;
    },
    enabled: isMultiAdmin,
  });

  const reloadList = useCallback(async () => {
    await queryClient.invalidateQueries({
      queryKey: ["admin-pending-change-sets"],
    });
  }, [queryClient]);

  const applyBulkChangeSetResult = useCallback(
    async (
      result: BulkChangeSetActionResult,
      actionVerb: string,
      options?: { onFullSuccess?: () => void },
    ) => {
      await reloadList();
      const nextNotice = formatBulkChangeSetNotice(actionVerb, result);
      setNotice(nextNotice);
      if (nextNotice.type === "success") {
        notify.success(nextNotice.message);
      } else {
        notify.error(nextNotice.message);
      }
      if (result.failed.length === 0) {
        setSelectedIds(new Set());
        options?.onFullSuccess?.();
      } else {
        setSelectedIds(new Set(result.failed.map((item) => item.id)));
      }
    },
    [reloadList],
  );

  const approveManyMutation = useMutation({
    mutationFn: async (changeSetIds: string[]) =>
      runBulkChangeSetRequests(changeSetIds, (changeSetId) =>
        adminApiClient.post(API_ROUTES.ADMIN_REFERENCE_DATA.APPROVE_CHANGE_SET(changeSetId)),
      ),
    onSuccess: async (result) => {
      await applyBulkChangeSetResult(result, "phê duyệt");
    },
    onError: async (error) => {
      const message = parseApiError(error);
      setNotice({ type: "error", message });
      notify.error(message);
      await reloadList();
    },
  });

  const rejectManyMutation = useMutation({
    mutationFn: async ({ changeSetIds, reason }: { changeSetIds: string[]; reason: string }) =>
      runBulkChangeSetRequests(changeSetIds, (changeSetId) =>
        adminApiClient.post(API_ROUTES.ADMIN_REFERENCE_DATA.REJECT_CHANGE_SET(changeSetId), {
          reason,
        }),
      ),
    onSuccess: async (result) => {
      await applyBulkChangeSetResult(result, "từ chối", {
        onFullSuccess: () => {
          setBulkRejectOpen(false);
          setBulkRejectReason("");
        },
      });
    },
    onError: async (error) => {
      const message = parseApiError(error);
      setNotice({ type: "error", message });
      notify.error(message);
      await reloadList();
    },
  });

  const rejectMutation = useMutation({
    mutationFn: async ({
      changeSetId,
      reason,
    }: {
      changeSetId: string;
      reason: string;
    }) => {
      const response = await adminApiClient.post(
        API_ROUTES.ADMIN_REFERENCE_DATA.REJECT_CHANGE_SET(changeSetId),
        { reason }
      );
      return response.data.data as { message: string };
    },
    onSuccess: async (data) => {
      setNotice({ type: "success", message: data.message });
      notify.success(data.message);
      setRejectDialogId(null);
      setRejectReason("");
      setSelectedIds(new Set());
      await reloadList();
    },
    onError: (error) => {
      const message = parseApiError(error);
      setNotice({ type: "error", message });
      notify.error(message);
    },
  });

  const pendingCount = changeSets.length;
  const approvingIdSet = useMemo(() => {
    const v = approveManyMutation.variables;
    if (!approveManyMutation.isPending || !v?.length) return null;
    return new Set(v);
  }, [approveManyMutation.isPending, approveManyMutation.variables]);

  const selectableIds = useMemo(
    () => changeSets.filter((cs) => adminIdentity?.id !== cs.adminId).map((cs) => cs.id),
    [adminIdentity?.id, changeSets],
  );

  const effectiveSelectedIds = useMemo(() => {
    const selectable = new Set(selectableIds);
    const next = new Set<string>();
    for (const id of selectedIds) {
      if (selectable.has(id)) {
        next.add(id);
      }
    }
    return next;
  }, [selectedIds, selectableIds]);

  const allSelected =
    selectableIds.length > 0 && effectiveSelectedIds.size === selectableIds.length;
  const selectAllAriaLabel =
    selectableIds.length === 0
      ? "Không có tập dữ liệu thay đổi nào có thể chọn"
      : allSelected
        ? "Bỏ chọn tất cả"
        : "Chọn tất cả";
  const selectedCount = effectiveSelectedIds.size;
  const showBulkToolbar = selectedCount >= 2;
  const isBulkActionPending = approveManyMutation.isPending || rejectManyMutation.isPending;

  if (isConfigLoading) {
    return (
      <div className="space-y-6">
        <div className="rounded-[28px] bg-white p-6 shadow-sm ring-1 ring-slate-200">
          <div className="h-8 w-64 animate-pulse rounded-lg bg-slate-200" />
          <div className="mt-6 h-48 animate-pulse rounded-2xl bg-slate-100" />
        </div>
      </div>
    );
  }

  // Single-admin mode: show informational page
  if (!isMultiAdmin) {
    return (
      <div className="space-y-6">
        <div className="rounded-[28px] bg-white p-6 shadow-sm ring-1 ring-slate-200">
          <h1 className="flex items-center gap-3 text-2xl font-bold text-slate-900">
            <ShieldCheck className="h-7 w-7 text-teal-600" />
            Phê duyệt thay đổi
          </h1>
          <div className="mt-6 flex flex-col items-center justify-center gap-4 rounded-2xl border border-dashed border-slate-200 px-6 py-12 text-center">
            <CheckCircle2 className="h-12 w-12 text-emerald-400" />
            <div>
              <p className="text-base font-semibold text-slate-700">
                Chế độ đơn quản trị viên
              </p>
              <p className="mt-2 max-w-lg text-sm text-slate-500">
                Hệ thống hiện chỉ có 1 quản trị viên. Mọi thay đổi sẽ được áp dụng
                trực tiếp tại trang{" "}
                <a href="/admin/reference-data" className="font-medium text-teal-600 hover:underline">
                  Dữ liệu tham chiếu
                </a>
                {" "}mà không cần gửi duyệt.
              </p>
              <p className="mt-3 max-w-lg text-xs text-slate-400">
                Khi có từ 2 tài khoản quản trị trở lên, trang này sẽ tự động chuyển sang
                chế độ phê duyệt chéo — người tạo thay đổi không thể tự phê duyệt.
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={`space-y-6${showBulkToolbar ? " pb-28" : ""}`}>
      {/* Header */}
      <div className="flex flex-col gap-4 rounded-[28px] bg-white p-6 shadow-sm ring-1 ring-slate-200 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 className="flex items-center gap-3 text-2xl font-bold text-slate-900">
            <ShieldCheck className="h-7 w-7 text-teal-600" />
            Phê duyệt thay đổi
          </h1>
          <p className="mt-2 max-w-3xl text-sm text-slate-500">
            Hàng đợi phê duyệt các bản thay đổi dữ liệu tham chiếu. Duyệt
            hoặc từ chối trước khi áp dụng vào sản xuất.
          </p>
        </div>

        <div className="flex w-full flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center sm:justify-end lg:w-auto">
          <div className="flex h-11 min-w-0 flex-1 items-center justify-center gap-2 rounded-2xl border border-amber-200 bg-amber-50 px-4 text-sm sm:flex-initial sm:justify-center">
            <Clock className="h-4 w-4 shrink-0 text-amber-600" />
            <span className="font-semibold text-amber-700">{pendingCount}</span>
            <span className="text-amber-600">đang chờ duyệt</span>
          </div>

          <button
            type="button"
            onClick={() => void reloadList()}
            className="inline-flex h-11 shrink-0 items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 text-sm font-medium text-slate-600 shadow-sm transition hover:border-teal-200 hover:text-teal-700 sm:min-w-[140px]"
          >
            <RefreshCcw className={`h-4 w-4 ${isFetching ? "animate-spin" : ""}`} />
            Làm mới
          </button>
        </div>
      </div>

      {/* Notice */}
      {notice ? (
        <div
          className={`flex items-start gap-3 rounded-2xl border px-4 py-3 text-sm ${
            notice.type === "success"
              ? "border-emerald-200 bg-emerald-50 text-emerald-800"
              : "border-rose-200 bg-rose-50 text-rose-800"
          }`}
        >
          {notice.type === "success" ? (
            <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />
          ) : (
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          )}
          <span>{notice.message}</span>
          <button
            type="button"
            onClick={() => setNotice(null)}
            className="ml-auto"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      ) : null}

      {/* Change Sets List */}
      <div className="rounded-[28px] bg-white p-6 shadow-sm ring-1 ring-slate-200">
        {isLoading ? (
          <div className="flex items-center justify-center gap-3 rounded-2xl border border-dashed border-slate-200 px-6 py-12 text-sm text-slate-500">
            <Loader2 className="h-5 w-5 animate-spin" />
            Đang tải danh sách thay đổi chờ duyệt...
          </div>
        ) : changeSets.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-3 rounded-2xl border border-dashed border-slate-200 px-6 py-12 text-center">
            <CheckCircle2 className="h-10 w-10 text-emerald-400" />
            <p className="text-sm font-medium text-slate-600">
              Không có thay đổi nào đang chờ duyệt
            </p>
            <p className="max-w-md text-xs text-slate-400">
              Khi admin tạo hoặc chỉnh sửa chỉ số, bản thay đổi sẽ xuất hiện
              ở đây để duyệt trước khi áp dụng.
            </p>
          </div>
        ) : (
          <div>
            <div className="overflow-hidden rounded-2xl border border-slate-200">
              <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead className="bg-slate-50 text-left text-slate-500">
                <tr>
                  <th className="px-3 py-3 text-left font-medium text-slate-600">
                    <button
                      type="button"
                      disabled={approveManyMutation.isPending || selectableIds.length === 0}
                      onClick={() =>
                        setSelectedIds(allSelected ? new Set() : new Set(selectableIds))
                      }
                      className="text-sm font-medium text-slate-600 underline-offset-2 hover:text-teal-700 hover:underline disabled:cursor-not-allowed disabled:opacity-50"
                      aria-label={selectAllAriaLabel}
                      title={
                        selectableIds.length === 0
                          ? "Không có tập dữ liệu thay đổi nào có thể chọn"
                          : allSelected
                            ? "Bỏ chọn tất cả"
                            : "Chọn tất cả"
                      }
                    >
                      Chọn
                    </button>
                  </th>
                  <th className="w-12 px-2 py-3 font-medium text-slate-600">
                    <span className="sr-only">Mở rộng</span>
                  </th>
                  <th className="px-4 py-3 font-medium">Loại</th>
                  <th className="px-4 py-3 font-medium">Thao tác</th>
                  <th className="px-4 py-3 font-medium">Tóm tắt thay đổi</th>
                  <th className="px-4 py-3 font-medium">Ngày tạo</th>
                  <th className="px-4 py-3 font-medium text-right text-slate-600">
                    Hành động
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 bg-white">
                {changeSets.map((cs) => {
                  const expanded = expandedId === cs.id;
                  const snapshot = tryParseSnapshot(cs.changesJson);
                  const isSelfChange = adminIdentity?.id === cs.adminId;
                  const canSelect = !isSelfChange;
                  const isSelected = effectiveSelectedIds.has(cs.id);
                  const rowApproveLoading = Boolean(approvingIdSet?.has(cs.id));
                  const approveLocked =
                    isSelfChange ||
                    (approveManyMutation.isPending && !approvingIdSet?.has(cs.id));
                  return (
                    <Fragment key={cs.id}>
                      <tr className="transition hover:bg-slate-50/50">
                        <td className="px-2 py-4 text-center">
                          <input
                            type="checkbox"
                            className={APPROVAL_CHECKBOX_CLASS}
                            checked={isSelected}
                            disabled={approveManyMutation.isPending || !canSelect}
                            onChange={(e) => {
                              const checked = e.target.checked;
                              setSelectedIds((current) => {
                                const next = new Set(current);
                                if (checked) next.add(cs.id);
                                else next.delete(cs.id);
                                return next;
                              });
                            }}
                            aria-label={
                              !canSelect
                                ? "Không thể chọn: do bạn tạo"
                                : isSelected
                                  ? "Bỏ chọn"
                                  : "Chọn dòng"
                            }
                            title={
                              !canSelect
                                ? "Bạn không thể chọn tập dữ liệu thay đổi do mình tạo."
                                : undefined
                            }
                          />
                        </td>
                        <td className="px-2 py-4">
                          <button
                            type="button"
                            onClick={() =>
                              setExpandedId(expanded ? null : cs.id)
                            }
                            className="text-slate-400 transition hover:text-slate-700"
                            title={expanded ? "Thu gọn" : "Mở rộng"}
                            aria-expanded={expanded}
                            aria-label={expanded ? "Thu gọn chi tiết" : "Mở rộng chi tiết"}
                          >
                            {expanded ? (
                              <ChevronDown className="h-4 w-4" />
                            ) : (
                              <ChevronRight className="h-4 w-4" />
                            )}
                          </button>
                        </td>
                        <td className="px-4 py-4">
                          <span className="inline-flex items-center gap-1.5 rounded-full bg-indigo-100 px-2.5 py-1 text-xs font-semibold text-indigo-700">
                            <FileEdit className="h-3 w-3" />
                            {cs.entityType}
                          </span>
                        </td>
                        <td className="px-4 py-4">
                          <span
                            className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${operationBadgeClass(cs.operation)}`}
                          >
                            {operationLabel(cs.operation)}
                          </span>
                        </td>
                        <td className="max-w-sm px-4 py-4">
                          {snapshot ? (
                            <div>
                              <div className="font-semibold text-slate-900">
                                {snapshot.displayNameVi || snapshot.name || "—"}
                              </div>
                              <div className="mt-0.5 text-xs text-slate-500">
                                {snapshot.name} · {snapshot.unit} ·{" "}
                                {snapshot.ranges?.length ?? 0} ngưỡng
                              </div>
                            </div>
                          ) : (
                            <span className="text-slate-400">
                              Không thể đọc dữ liệu
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-4 text-slate-600">
                          {formatDate(cs.createdAt)}
                        </td>
                        <td className="px-4 py-4">
                          <div className="flex flex-wrap justify-end gap-2">
                            <button
                              type="button"
                              disabled={approveLocked}
                              onClick={() => void approveManyMutation.mutateAsync([cs.id])}
                              title={
                                isSelfChange
                                  ? "Bạn không thể tự phê duyệt thay đổi do mình tạo."
                                  : "Phê duyệt"
                              }
                              aria-label="Phê duyệt"
                              className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-emerald-200 bg-white text-emerald-600 transition hover:bg-emerald-50 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                              {rowApproveLoading ? (
                                <Loader2 className="h-4 w-4 animate-spin" />
                              ) : (
                                <Check className="h-4 w-4" />
                              )}
                            </button>
                            <button
                              type="button"
                              disabled={rejectMutation.isPending || isSelfChange}
                              onClick={() => {
                                setRejectDialogId(cs.id);
                                setRejectReason("");
                              }}
                              title={
                                isSelfChange
                                  ? "Bạn không thể tự từ chối thay đổi do mình tạo."
                                  : "Từ chối"
                              }
                              aria-label="Từ chối"
                              className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-rose-200 bg-white text-rose-600 transition hover:bg-rose-50 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                              <XCircle className="h-4 w-4" />
                            </button>
                          </div>
                        </td>
                      </tr>

                      {/* Expanded Detail Row */}
                      {expanded ? (
                        <tr>
                          <td
                            colSpan={7}
                            className="bg-slate-50/60 px-6 py-5"
                          >
                            <ChangeSetDiffPanel
                              changeSet={cs}
                              snapshot={snapshot}
                            />
                          </td>
                        </tr>
                      ) : null}
                    </Fragment>
                  );
                })}
              </tbody>
              </table>
            </div>
          </div>
        )}
      </div>

      {/* Reject Dialog */}
      {rejectDialogId ? (
        <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
          <div className="w-full max-w-lg rounded-[28px] bg-white p-6 shadow-2xl">
            <div className="flex items-start justify-between">
              <div>
                <h3 className="text-xl font-bold text-slate-900">
                  Từ chối thay đổi
                </h3>
                <p className="mt-2 text-sm text-slate-500">
                  Vui lòng cung cấp lý do từ chối để admin tạo thay đổi có thể
                  sửa lại.
                </p>
              </div>
              <button
                type="button"
                onClick={() => {
                  setRejectDialogId(null);
                  setRejectReason("");
                }}
                className="rounded-full p-2 text-slate-500 transition hover:bg-slate-100 hover:text-slate-700"
                aria-label="Đóng hộp thoại từ chối"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="mt-5">
              <label className="mb-2 block text-sm font-medium text-slate-700">
                Lý do từ chối <span className="text-rose-500">*</span>
              </label>
              <textarea
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="Nhập lý do từ chối..."
                rows={4}
                className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 shadow-sm transition focus:border-teal-400 focus:outline-none focus:ring-2 focus:ring-teal-100"
              />
            </div>

            <div className="mt-5 flex justify-end gap-3">
              <button
                type="button"
                onClick={() => {
                  setRejectDialogId(null);
                  setRejectReason("");
                }}
                className="rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-medium text-slate-600 transition hover:bg-slate-50"
              >
                Hủy
              </button>
              <button
                type="button"
                disabled={
                  !rejectReason.trim() || rejectMutation.isPending
                }
                onClick={() =>
                  void rejectMutation.mutateAsync({
                    changeSetId: rejectDialogId,
                    reason: rejectReason.trim(),
                  })
                }
                className="inline-flex items-center gap-2 rounded-xl bg-rose-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-rose-500 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {rejectMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <XCircle className="h-4 w-4" />
                )}
                Xác nhận từ chối
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {bulkRejectOpen ? (
        <div className="fixed inset-0 z-[75] flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
          <div className="w-full max-w-lg rounded-[28px] bg-white p-6 shadow-2xl">
            <div className="flex items-start justify-between">
              <div>
                <h3 className="text-xl font-bold text-slate-900">Từ chối hàng loạt</h3>
                <p className="mt-2 text-sm text-slate-500">
                  Từ chối {selectedCount} đã chọn. 
                </p>
              </div>
              <button
                type="button"
                onClick={() => {
                  setBulkRejectOpen(false);
                  setBulkRejectReason("");
                }}
                className="rounded-full p-2 text-slate-500 transition hover:bg-slate-100 hover:text-slate-700"
                aria-label="Đóng"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="mt-5">
              <label className="mb-2 block text-sm font-medium text-slate-700">
                Lý do từ chối <span className="text-rose-500">*</span>
              </label>
              <textarea
                value={bulkRejectReason}
                onChange={(e) => setBulkRejectReason(e.target.value)}
                placeholder="Nhập lý do từ chối..."
                rows={4}
                className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 shadow-sm transition focus:border-teal-400 focus:outline-none focus:ring-2 focus:ring-teal-100"
              />
            </div>

            <div className="mt-5 flex justify-end gap-3">
              <button
                type="button"
                onClick={() => {
                  setBulkRejectOpen(false);
                  setBulkRejectReason("");
                }}
                className="rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-medium text-slate-600 transition hover:bg-slate-50"
              >
                Hủy
              </button>
              <button
                type="button"
                disabled={!bulkRejectReason.trim() || rejectManyMutation.isPending}
                onClick={() =>
                  void rejectManyMutation.mutateAsync({
                    changeSetIds: Array.from(effectiveSelectedIds),
                    reason: bulkRejectReason.trim(),
                  })
                }
                className="inline-flex items-center gap-2 rounded-xl bg-rose-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-rose-500 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {rejectManyMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <XCircle className="h-4 w-4" />
                )}
                Xác nhận từ chối
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {showBulkToolbar ? (
        <div className="fixed bottom-6 left-0 right-0 z-[60] flex justify-center px-4 md:left-64">
          <div className="flex w-full max-w-3xl flex-col gap-3 rounded-[24px] border border-slate-200 bg-white/95 p-4 shadow-2xl backdrop-blur-sm sm:flex-row sm:items-center sm:justify-between sm:gap-4">
            <div className="flex min-w-0 flex-1 flex-wrap items-center gap-4">
              <label className="flex cursor-pointer items-center gap-2.5 select-none">
                <input
                  ref={(el) => {
                    if (el) {
                      el.indeterminate =
                        selectedCount > 0 && !allSelected && selectableIds.length > 0;
                    }
                  }}
                  type="checkbox"
                  className={APPROVAL_CHECKBOX_CLASS}
                  checked={allSelected && selectableIds.length > 0}
                  disabled={isBulkActionPending || selectableIds.length === 0}
                  onChange={(e) => {
                    const checked = e.target.checked;
                    setSelectedIds(checked ? new Set(selectableIds) : new Set());
                  }}
                  aria-label={selectAllAriaLabel}
                  title={
                    selectableIds.length === 0
                      ? "Không có tập dữ liệu thay đổi nào có thể chọn"
                      : allSelected
                        ? "Bỏ chọn tất cả"
                        : "Chọn tất cả"
                  }
                />
                <span className="text-base font-semibold text-slate-800">Chọn tất cả</span>
              </label>
            </div>
            <div className="flex shrink-0 flex-wrap items-center justify-end gap-3">
              <p className="text-sm font-semibold whitespace-nowrap text-slate-900">
                Đã chọn {selectedCount} chỉ số
              </p>
              <button
                type="button"
                disabled={isBulkActionPending || selectedCount === 0}
                onClick={() => void approveManyMutation.mutateAsync(Array.from(effectiveSelectedIds))}
                title="Phê duyệt các dòng đã chọn"
                aria-label="Phê duyệt hàng loạt"
                className="inline-flex h-11 w-11 items-center justify-center rounded-2xl bg-emerald-600 text-white shadow-sm transition hover:bg-emerald-500 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {approveManyMutation.isPending ? (
                  <Loader2 className="h-5 w-5 animate-spin" />
                ) : (
                  <Check className="h-5 w-5" />
                )}
              </button>
              <button
                type="button"
                disabled={isBulkActionPending || selectedCount === 0}
                onClick={() => {
                  setBulkRejectOpen(true);
                  setBulkRejectReason("");
                }}
                title="Từ chối các dòng đã chọn"
                aria-label="Từ chối hàng loạt"
                className="inline-flex h-11 w-11 items-center justify-center rounded-2xl border border-rose-200 bg-white text-rose-600 shadow-sm transition hover:bg-rose-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <XCircle className="h-5 w-5" />
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function ChangeSetDiffPanel({
  changeSet,
  snapshot,
}: {
  changeSet: ChangeSetDetail;
  snapshot: ParsedSnapshot | null;
}) {
  const currentSnapshot = useMemo(
    () => tryParseSnapshot(changeSet.currentSnapshotJson),
    [changeSet.currentSnapshotJson]
  );
  const adminIdentity = useMemo(() => getAdminIdentity(), []);
  const createdByLabel = adminIdentity?.id === changeSet.adminId
    ? "Bạn"
    : changeSet.adminName || changeSet.adminEmail || changeSet.adminId;
  const reviewerLabel = changeSet.reviewerId
    ? (adminIdentity?.id === changeSet.reviewerId
        ? "Bạn"
        : changeSet.reviewerName || changeSet.reviewerEmail || changeSet.reviewerId)
    : "—";

  if (!snapshot) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-white p-4 text-sm text-slate-500">
        Không thể hiển thị chi tiết thay đổi — dữ liệu JSON không hợp lệ.
      </div>
    );
  }

  const isUpdate = changeSet.operation === "UPDATE";

  return (
    <div className="space-y-6">
      {/* Header Info */}
      <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h4 className="mb-4 text-sm font-bold text-slate-900 uppercase tracking-tight">
          Thông tin bản thay đổi
        </h4>
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
          <InfoBlock label="Mã bản thay đổi" value={changeSet.id} mono />
          <InfoBlock
            label="Mã định danh dữ liệu"
            value={changeSet.entityId ?? "—"}
            mono
          />
          <InfoBlock
            label="Loại thao tác"
            value={operationLabel(changeSet.operation)}
          />
          <InfoBlock label="Trạng thái hàng đợi" value={changeSet.status === 'pending' ? 'Chờ phê duyệt' : changeSet.status} />
          <InfoBlock label="Người tạo" value={createdByLabel} mono={createdByLabel !== "Bạn"} />
          <InfoBlock label="Người phê duyệt" value={reviewerLabel} mono={reviewerLabel !== "Bạn"} />
        </div>
      </div>

      <div className={`grid gap-6 ${isUpdate ? "md:grid-cols-2" : "grid-cols-1"}`}>
        {/* CURRENT DATA (for Updates) */}
        {isUpdate && currentSnapshot && (
          <div className="rounded-2xl border border-slate-200 bg-slate-50/50 p-6">
            <div className="mb-4 flex items-center justify-between">
              <h4 className="text-sm font-bold text-slate-500 uppercase tracking-widest">
                Phiên bản cũ (Hiện tại)
              </h4>
              <span className="rounded-full bg-slate-200 px-2 py-0.5 text-[10px] font-bold text-slate-600">PRODUCTION</span>
            </div>
            
            <div className="space-y-6">
              <div className="grid gap-4 sm:grid-cols-2">
                <InfoBlock label="Tên chỉ số" value={currentSnapshot.name ?? "—"} />
                <InfoBlock label="Đơn vị" value={currentSnapshot.unit ?? "—"} />
                <div className="sm:col-span-2">
                  <InfoBlock label="Tên hiển thị (VI)" value={currentSnapshot.displayNameVi ?? "—"} />
                </div>
              </div>

              {currentSnapshot.ranges && currentSnapshot.ranges.length > 0 && (
                <div className="space-y-3">
                  <h5 className="text-[10px] font-bold uppercase tracking-widest text-slate-400">
                    Ngưỡng tham chiếu ({currentSnapshot.ranges.length})
                  </h5>
                  <div className="space-y-2">
                    {currentSnapshot.ranges.map((range, idx) => (
                      <div key={idx} className="rounded-xl border border-slate-200 bg-white p-3 text-xs shadow-sm">
                        <div className="flex justify-between items-center mb-2">
                          <span className="font-bold text-slate-400"># {idx + 1}</span>
                          <span className="text-[10px] text-slate-400 uppercase">{range.gender === 'male' ? 'Nam' : range.gender === 'female' ? 'Nữ' : 'Tất cả'}</span>
                        </div>
                        <div className="grid grid-cols-2 gap-2">
                          <InfoCell label="Giá trị" value={`${range.minValue} – ${range.maxValue}`} />
                          <InfoCell label="Độ tuổi" value={`${range.minAge ?? 0} – ${range.maxAge ?? "∞"}`} />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {/* PROPOSED DATA */}
        <div className={`rounded-2xl border border-indigo-200 bg-indigo-50/30 p-6 shadow-sm ${!isUpdate ? "max-w-4xl mx-auto w-full" : ""}`}>
          <div className="mb-4 flex items-center justify-between">
            <h4 className="text-sm font-bold text-indigo-600 uppercase tracking-widest">
              Phiên bản mới (Đề xuất)
            </h4>
            <span className="rounded-full bg-indigo-600 px-2 py-0.5 text-[10px] font-bold text-white uppercase animate-pulse-short">PROPOSED</span>
          </div>

          <div className="space-y-6">
            <div className="grid gap-4 sm:grid-cols-2">
              <InfoBlock label="Tên chỉ số" value={snapshot.name ?? "—"} />
              <InfoBlock label="Đơn vị" value={snapshot.unit ?? "—"} />
              <div className="sm:col-span-2">
                <InfoBlock label="Tên hiển thị (VI)" value={snapshot.displayNameVi ?? "—"} />
              </div>
            </div>

            {snapshot.ranges && snapshot.ranges.length > 0 && (
              <div className="space-y-3">
                <h5 className="text-[10px] font-bold uppercase tracking-widest text-indigo-400">
                  Ngưỡng tham chiếu ({snapshot.ranges.length})
                </h5>
                <div className="space-y-2">
                  {snapshot.ranges.map((range, idx) => (
                    <div key={idx} className="rounded-xl border border-indigo-200 bg-white p-3 text-xs shadow-sm ring-1 ring-indigo-50">
                      <div className="flex justify-between items-center mb-2">
                        <span className="font-bold text-indigo-400"># {idx + 1}</span>
                        <span className="text-[10px] text-indigo-400 uppercase">{range.gender === 'male' ? 'Nam' : range.gender === 'female' ? 'Nữ' : 'Tất cả'}</span>
                      </div>
                      <div className="grid grid-cols-2 gap-2">
                        <InfoCell label="Giá trị" value={`${range.minValue} – ${range.maxValue}`} />
                        <InfoCell label="Độ tuổi" value={`${range.minAge ?? 0} – ${range.maxAge ?? "∞"}`} />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function InfoBlock({
  label,
  value,
  mono,
}: {
  label: string;
  value: string;
  mono?: boolean;
}) {
  return (
    <div>
      <div className="text-xs font-medium text-slate-400">{label}</div>
      <div
        className={`mt-1 text-sm text-slate-900 ${mono ? "font-mono text-xs break-all" : ""}`}
      >
        {value}
      </div>
    </div>
  );
}

function InfoCell({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-xs font-medium text-slate-400">{label}</div>
      <div className="mt-0.5 text-sm font-medium text-slate-700">{value}</div>
    </div>
  );
}
