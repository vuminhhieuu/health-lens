"use client";

import { Fragment, useCallback, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AlertCircle,
  CheckCircle2,
  Database,
  FileEdit,
  Loader2,
  Pencil,
  Plus,
  RefreshCcw,
  RotateCcw,
  Trash2,
  UploadCloud,
  X,
} from "lucide-react";
import Link from "next/link";

import { adminApiClient } from "@/lib/api/adminApiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { notify } from "@/lib/notify";
import { EmptyState, ErrorState, InlineFieldError, LoadingState } from "@/components/ui";
import {
  formatNoticeNewMetricMultiAdmin,
  NOTICE_NEW_METRIC_SINGLE_ADMIN,
} from "@/lib/admin/referenceDataNotices";

type ReferenceRange = {
  id: string;
  minValue: string;
  maxValue: string;
  attentionMin: string;
  attentionMax: string;
  gender: string | null;
  minAge: number | null;
  maxAge: number | null;
  status: string;
};

type PendingChangeSet = {
  changeSetId: string;
  operation: string;
  createdAt: string;
  proposedName: string;
  proposedDisplayNameVi: string;
  proposedUnit: string;
  proposedRangesCount: number;
  status: string;
};

type ReferenceMetric = {
  id: string;
  name: string;
  displayNameVi: string;
  unit: string;
  status: "active" | "draft" | "deactivated";
  rangesCount: number;
  ranges: ReferenceRange[];
  pendingChangeSet?: PendingChangeSet | null;
};

type MetricPayload = {
  name: string;
  displayNameVi: string;
  unit: string;
  ranges: Array<{
    minValue: number;
    maxValue: number;
    attentionMin: number;
    attentionMax: number;
    gender: string | null;
    minAge: number | null;
    maxAge: number | null;
  }>;
};

type MetricRangeForm = {
  minValue: string;
  maxValue: string;
  attentionMin: string;
  attentionMax: string;
  gender: string;
  minAge: string;
  maxAge: string;
};

type MetricFormState = {
  name: string;
  displayNameVi: string;
  unit: string;
  ranges: MetricRangeForm[];
};

type ConfirmState = {
  kind: "deactivate" | "reactivate";
  metric: ReferenceMetric;
};

const emptyRange = (): MetricRangeForm => ({
  minValue: "",
  maxValue: "",
  attentionMin: "",
  attentionMax: "",
  gender: "",
  minAge: "",
  maxAge: "",
});

const emptyForm = (): MetricFormState => ({
  name: "",
  displayNameVi: "",
  unit: "",
  ranges: [emptyRange()],
});

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
        errors?: Array<{ message?: string }>;
      };
    };
    const fieldErrors = response.data?.errors
      ?.map((item) => item.message)
      .filter((message): message is string => Boolean(message));

    return fieldErrors && fieldErrors.length > 0
      ? fieldErrors.join(" ")
      : response.data?.detail || "Không thể xử lý yêu cầu.";
  }
  return "Không thể kết nối tới máy chủ.";
}

function toFormState(metric?: ReferenceMetric): MetricFormState {
  if (!metric) {
    return emptyForm();
  }

  return {
    name: metric.name,
    displayNameVi: metric.displayNameVi,
    unit: metric.unit,
    ranges: metric.ranges.length > 0
      ? metric.ranges.map((range) => ({
        minValue: String(range.minValue ?? ""),
        maxValue: String(range.maxValue ?? ""),
        attentionMin: String(range.attentionMin ?? ""),
        attentionMax: String(range.attentionMax ?? ""),
        gender: range.gender ?? "",
        minAge: range.minAge === null ? "" : String(range.minAge),
        maxAge: range.maxAge === null ? "" : String(range.maxAge),
      }))
      : [emptyRange()],
  };
}

function buildPayload(form: MetricFormState): MetricPayload {
  return {
    name: form.name.trim(),
    displayNameVi: form.displayNameVi.trim(),
    unit: form.unit.trim(),
    ranges: form.ranges.map((range) => ({
      minValue: Number(range.minValue),
      maxValue: Number(range.maxValue),
      attentionMin: Number(range.attentionMin),
      attentionMax: Number(range.attentionMax),
      gender: range.gender || null,
      minAge: range.minAge === "" ? null : Number(range.minAge),
      maxAge: range.maxAge === "" ? null : Number(range.maxAge),
    })),
  };
}

export default function ReferenceDataPage() {
  const queryClient = useQueryClient();
  const [expandedMetricId, setExpandedMetricId] = useState<string | null>(null);
  const [mode, setMode] = useState<"create" | "edit">("create");
  const [selectedMetric, setSelectedMetric] = useState<ReferenceMetric | null>(null);
  const [form, setForm] = useState<MetricFormState>(emptyForm());
  const [formError, setFormError] = useState("");
  const [notice, setNotice] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [isEditorOpen, setIsEditorOpen] = useState(false);
  const [confirmState, setConfirmState] = useState<ConfirmState | null>(null);

  const { data: adminConfig } = useQuery<{ multiAdminMode: boolean }>({
    queryKey: ["admin-config"],
    queryFn: async () => {
      const response = await adminApiClient.get(API_ROUTES.ADMIN_REFERENCE_DATA.CONFIG);
      return response.data.data;
    },
    staleTime: 60_000,
  });

  const {
    data: metrics = [],
    isLoading,
    isFetching,
    isError,
    refetch,
  } = useQuery<ReferenceMetric[]>({
    queryKey: ["admin-reference-metrics"],
    queryFn: async () => {
      const response = await adminApiClient.get(API_ROUTES.ADMIN_REFERENCE_DATA.METRICS);
      return response.data.data;
    },
  });

  const groupedMetrics = useMemo(
    () => ({
      active: metrics.filter((metric) => metric.status === "active"),
      draft: metrics.filter((metric) => metric.status === "draft"),
      deactivated: metrics.filter((metric) => metric.status === "deactivated"),
    }),
    [metrics],
  );

  const reloadList = async () => {
    await queryClient.invalidateQueries({ queryKey: ["admin-reference-metrics"] });
  };

  const createMutation = useMutation({
    mutationFn: async (payload: MetricPayload) => {
      const response = await adminApiClient.post(API_ROUTES.ADMIN_REFERENCE_DATA.METRICS, payload);
      return response.data.data as ReferenceMetric;
    },
    onSuccess: async () => {
      const message = adminConfig?.multiAdminMode
        ? formatNoticeNewMetricMultiAdmin(1)
        : NOTICE_NEW_METRIC_SINGLE_ADMIN;
      setNotice({ type: "success", message });
      notify.success(message);
      closeEditor();
      await reloadList();
    },
    onError: (error) => {
      const message = parseApiError(error);
      setFormError(message);
      notify.error(message);
    },
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, payload }: { id: string; payload: MetricPayload }) => {
      const response = await adminApiClient.put(API_ROUTES.ADMIN_REFERENCE_DATA.METRIC_BY_ID(id), payload);
      return response.data.data as { message: string };
    },
    onSuccess: async (data) => {
      setNotice({ type: "success", message: data.message });
      notify.success(data.message);
      closeEditor();
      await reloadList();
    },
    onError: (error) => {
      const message = parseApiError(error);
      setFormError(message);
      notify.error(message);
    },
  });

  const deactivateMutation = useMutation({
    mutationFn: async (id: string) => {
      const response = await adminApiClient.delete(API_ROUTES.ADMIN_REFERENCE_DATA.METRIC_BY_ID(id));
      return response.data.data as { message: string };
    },
    onSuccess: async (data) => {
      setNotice({ type: "success", message: data.message });
      notify.success(data.message);
      setConfirmState(null);
      await reloadList();
    },
    onError: (error) => {
      const message = parseApiError(error);
      setNotice({ type: "error", message });
      notify.error(message);
      setConfirmState(null);
    },
  });

  const reactivateMutation = useMutation({
    mutationFn: async (id: string) => {
      const response = await adminApiClient.post(API_ROUTES.ADMIN_REFERENCE_DATA.REACTIVATE(id));
      return response.data.data as ReferenceMetric;
    },
    onSuccess: async () => {
      const message = "Đã kích hoạt lại chỉ số và toàn bộ ngưỡng liên quan.";
      setNotice({ type: "success", message });
      notify.success(message);
      setConfirmState(null);
      await reloadList();
    },
    onError: (error) => {
      const message = parseApiError(error);
      setNotice({ type: "error", message });
      notify.error(message);
      setConfirmState(null);
    },
  });

  const closeEditor = () => {
    setIsEditorOpen(false);
    setSelectedMetric(null);
    setMode("create");
    setForm(emptyForm());
    setFormError("");
  };

  const openCreateModal = () => {
    setMode("create");
    setSelectedMetric(null);
    setForm(emptyForm());
    setFormError("");
    setIsEditorOpen(true);
  };

  const openEditModal = (metric: ReferenceMetric) => {
    setMode("edit");
    setSelectedMetric(metric);
    setForm(toFormState(metric));
    setFormError("");
    setIsEditorOpen(true);
  };


  const handleConfirmAction = async () => {
    if (!confirmState) {
      return;
    }
    if (confirmState.kind === "deactivate") {
      await deactivateMutation.mutateAsync(confirmState.metric.id);
      return;
    }
    await reactivateMutation.mutateAsync(confirmState.metric.id);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 rounded-[28px] bg-white p-6 shadow-sm ring-1 ring-slate-200 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 className="flex items-center gap-3 text-2xl font-bold text-slate-900">
            <Database className="h-7 w-7 text-teal-600" />
            Dữ liệu tham chiếu
          </h1>
          <p className="mt-2 max-w-3xl text-sm text-slate-500">
            Quản lý chỉ số và ngưỡng tham chiếu cho hệ thống diễn giải.
            {adminConfig?.multiAdminMode
              ? " Mọi thay đổi sẽ được gửi vào hàng đợi phê duyệt trước khi áp dụng."
              : " Các thay đổi sẽ được áp dụng trực tiếp vào hệ thống."}
          </p>
        </div>

        <div className="flex gap-4">
          <div className="rounded-2xl bg-emerald-50 px-4 py-2 ring-1 ring-emerald-100">
            <div className="text-[10px] font-bold uppercase tracking-widest text-emerald-600">Đang áp dụng</div>
            <div className="text-xl font-bold text-emerald-700">{groupedMetrics.active.length}</div>
          </div>
          <div className="rounded-2xl bg-slate-100 px-4 py-2 ring-1 ring-slate-200">
            <div className="text-[10px] font-bold uppercase tracking-widest text-slate-500">Ngưng áp dụng</div>
            <div className="text-xl font-bold text-slate-600">{groupedMetrics.deactivated.length}</div>
          </div>
        </div>
      </div>

      {notice ? (
        <div
          className={`flex items-start gap-3 rounded-2xl border px-4 py-3 text-sm ${notice.type === "success"
            ? "border-emerald-200 bg-emerald-50 text-emerald-800"
            : "border-rose-200 bg-rose-50 text-rose-800"
            }`}
        >
          {notice.type === "success" ? (
            <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />
          ) : (
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          )}
          <span className="font-medium leading-relaxed">{notice.message}</span>
        </div>
      ) : null}

      <div className="rounded-[28px] bg-white p-6 shadow-sm ring-1 ring-slate-200">
        <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Danh mục chỉ số xét nghiệm</h2>
          </div>

          <div className="flex flex-wrap gap-3">
            <Link
              href="/admin/reference-data/import"
              className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-4 py-2 text-sm font-medium text-slate-600 transition hover:border-teal-200 hover:text-teal-700"
            >
              <UploadCloud className="h-4 w-4" />
              Nhập CSV/JSON
            </Link>
            <button
              type="button"
              onClick={() => void reloadList()}
              className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-4 py-2 text-sm font-medium text-slate-600 transition hover:border-teal-200 hover:text-teal-700"
            >
              <RefreshCcw className={`h-4 w-4 ${isFetching ? "animate-spin" : ""}`} />
              Làm mới
            </button>
            <button
              type="button"
              onClick={openCreateModal}
              className="inline-flex items-center gap-2 rounded-xl bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800"
            >
              <Plus className="h-4 w-4" />
              Thêm chỉ số
            </button>
          </div>
        </div>

        {isLoading ? (
          <LoadingState
            title="Đang tải danh sách chỉ số"
            description="Các chỉ số tham chiếu sẽ hiển thị ngay khi dữ liệu sẵn sàng."
            className="border-slate-200 bg-slate-50"
          />
        ) : isError ? (
          <ErrorState
            title="Không tải được danh sách chỉ số"
            description="Vui lòng thử lại để quản trị dữ liệu tham chiếu."
            actionLabel="Thử lại"
            onAction={() => void refetch()}
            className="border-slate-200 bg-slate-50"
          />
        ) : (
          <div className="space-y-6">
            <MetricSection
              title="Đang áp dụng"
              description="Các chỉ số đang áp dụng hiện được hệ thống dùng để diễn giải kết quả."
              metrics={groupedMetrics.active}
              expandedMetricId={expandedMetricId}
              onToggleExpanded={setExpandedMetricId}
              onEdit={openEditModal}
              onDeactivate={(metric) => setConfirmState({ kind: "deactivate", metric })}
            />

            <MetricSection
              title="Ngưng áp dụng"
              description="Các chỉ số đã ngưng áp dụng. Có thể kích hoạt lại để tiếp tục sử dụng."
              metrics={groupedMetrics.deactivated}
              expandedMetricId={expandedMetricId}
              onToggleExpanded={setExpandedMetricId}
              onEdit={null}
              onDeactivate={null}
              onReactivate={(metric) => setConfirmState({ kind: "reactivate", metric })}
            />
          </div>
        )}
      </div>

      <MetricEditorModal
        key={selectedMetric?.id || (isEditorOpen ? "create" : "closed")}
        open={isEditorOpen}
        mode={mode}
        metric={selectedMetric}
        initialForm={form}
        formError={formError}
        adminConfig={adminConfig}
        onClose={closeEditor}
        onSubmitPayload={async (payload) => {
          setFormError("");
          if (mode === "create") {
            await createMutation.mutateAsync(payload);
            return;
          }
          if (!selectedMetric) {
            setFormError("Không có chỉ số nào được chọn để chỉnh sửa.");
            return;
          }
          await updateMutation.mutateAsync({ id: selectedMetric.id, payload });
        }}
        isPending={createMutation.isPending || updateMutation.isPending}
      />

      <ActionModal
        open={Boolean(confirmState)}
        title={confirmState?.kind === "deactivate" ? "Ngưng áp dụng chỉ số?" : "Kích hoạt lại chỉ số?"}
        description={
          confirmState
            ? confirmState.kind === "deactivate"
              ? `Chỉ số "${confirmState.metric.displayNameVi}" sẽ được chuyển sang trạng thái ngưng áp dụng và không còn ảnh hưởng đến dữ liệu đang áp dụng.`
              : `Chỉ số "${confirmState.metric.displayNameVi}" sẽ được kích hoạt lại cùng toàn bộ ngưỡng liên quan.`
            : ""
        }
        confirmLabel={confirmState?.kind === "deactivate" ? "Ngưng áp dụng" : "Kích hoạt lại"}
        tone={confirmState?.kind === "deactivate" ? "danger" : "success"}
        isPending={deactivateMutation.isPending || reactivateMutation.isPending}
        onCancel={() => setConfirmState(null)}
        onConfirm={() => void handleConfirmAction()}
      />
    </div>
  );
}

function MetricSection({
  title,
  description,
  metrics,
  expandedMetricId,
  onToggleExpanded,
  onEdit,
  onDeactivate,
  onReactivate,
}: {
  title: string;
  description: string;
  metrics: ReferenceMetric[];
  expandedMetricId: string | null;
  onToggleExpanded: (metricId: string | null) => void;
  onEdit: ((metric: ReferenceMetric) => void) | null;
  onDeactivate: ((metric: ReferenceMetric) => void) | null;
  onReactivate?: (metric: ReferenceMetric) => void;
}) {
  return (
    <section className="rounded-3xl border border-slate-200">
      <div className="border-b border-slate-200 px-5 py-4">
        <h3 className="text-base font-semibold text-slate-900">{title}</h3>
        <p className="mt-1 text-sm text-slate-500">{description}</p>
      </div>

      {metrics.length === 0 ? (
        <div className="px-5 py-5">
          <EmptyState
            title="Chưa có chỉ số nào"
            description="Nhóm này chưa có chỉ số tham chiếu để hiển thị."
            className="min-h-48 border-slate-200 bg-slate-50"
          />
        </div>
      ) : (
        <div className="overflow-hidden">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-slate-500">
              <tr>
                <th className="px-5 py-3 font-medium">Chỉ số</th>
                <th className="px-5 py-3 font-medium">Đơn vị</th>
                <th className="px-5 py-3 font-medium">Số ngưỡng</th>
                <th className="px-5 py-3 font-medium">Trạng thái</th>
                <th className="px-5 py-3 font-medium text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 bg-white">
              {metrics.map((metric) => {
                const expanded = expandedMetricId === metric.id;
                return (
                  <Fragment key={metric.id}>
                    <tr>
                      <td className="px-5 py-4">
                        <button
                          type="button"
                          onClick={() => onToggleExpanded(expanded ? null : metric.id)}
                          className="text-left"
                        >
                          <div className="font-semibold text-slate-900">{metric.displayNameVi}</div>
                          <div className="text-xs text-slate-500">{metric.name}</div>
                        </button>
                      </td>
                      <td className="px-5 py-4 text-slate-600">{metric.unit}</td>
                      <td className="px-5 py-4 text-slate-600">{metric.rangesCount}</td>
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-2">
                          <span className={statusBadge(metric.status)}>{statusLabel(metric.status)}</span>
                          {metric.pendingChangeSet && metric.status === "draft" ? (
                            <span className="inline-flex items-center gap-1 rounded-full bg-indigo-100 px-2.5 py-1 text-xs font-semibold text-indigo-700">
                              <FileEdit className="h-3 w-3" />
                              Đang chờ duyệt
                            </span>
                          ) : null}
                        </div>
                      </td>
                      <td className="px-5 py-4">
                        <div className="flex flex-wrap justify-end gap-2">
                          {onEdit ? (
                            <button
                              type="button"
                              onClick={() => onEdit(metric)}
                              className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2 text-slate-600 transition hover:border-teal-200 hover:text-teal-700"
                            >
                              <Pencil className="h-4 w-4" />
                              {metric.status === "draft" ? "Sửa bản nháp" : "Chỉnh sửa"}
                            </button>
                          ) : null}

                          {onDeactivate ? (
                            <button
                              type="button"
                              onClick={() => onDeactivate(metric)}
                              className="inline-flex items-center gap-2 rounded-xl border border-rose-200 px-3 py-2 text-rose-600 transition hover:bg-rose-50"
                            >
                              <Trash2 className="h-4 w-4" />
                              Ngưng áp dụng
                            </button>
                          ) : null}

                          {onReactivate ? (
                            <button
                              type="button"
                              onClick={() => onReactivate(metric)}
                              className="inline-flex items-center gap-2 rounded-xl border border-emerald-200 px-3 py-2 text-emerald-700 transition hover:bg-emerald-50"
                            >
                              <RotateCcw className="h-4 w-4" />
                              Kích hoạt lại
                            </button>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                    {expanded ? (
                      <tr>
                        <td colSpan={5} className="bg-slate-50 px-5 py-4">
                          <div className="rounded-2xl border border-slate-200 bg-white p-4">
                            <div className="mb-3 text-sm font-semibold text-slate-900">
                              Danh sách ngưỡng tham chiếu
                            </div>
                            <div className="space-y-3">
                              {metric.ranges.map((range, index) => (
                                <div
                                  key={range.id}
                                  className="grid gap-3 rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 text-sm text-slate-600 md:grid-cols-5"
                                >
                                  <InfoCell label={`Ngưỡng ${index + 1}`} value={`${range.minValue} - ${range.maxValue}`} />
                                  <InfoCell label="Khoảng chú ý" value={`${range.attentionMin} - ${range.attentionMax}`} />
                                  <InfoCell label="Giới tính" value={range.gender === "male" ? "Nam" : range.gender === "female" ? "Nữ" : "Tất cả"} />
                                  <InfoCell label="Độ tuổi" value={`${range.minAge ?? 0} - ${range.maxAge ?? "không giới hạn"}`} />
                                  <InfoCell label="Trạng thái" value={statusLabel(range.status)} />
                                </div>
                              ))}
                            </div>
                          </div>
                        </td>
                      </tr>
                    ) : null}
                  </Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function MetricEditorModal({
  open,
  mode,
  metric,
  initialForm,
  formError,
  adminConfig,
  onClose,
  onSubmitPayload,
  isPending,
}: {
  open: boolean;
  mode: "create" | "edit";
  metric: ReferenceMetric | null;
  initialForm: MetricFormState;
  formError: string;
  adminConfig?: { multiAdminMode: boolean };
  onClose: () => void;
  onSubmitPayload: (payload: MetricPayload) => Promise<void>;
  isPending: boolean;
}) {
  const [form, setForm] = useState<MetricFormState>(initialForm);

  const onChange = useCallback(
    (field: keyof Omit<MetricFormState, "ranges">, value: string) => {
      setForm((current) => ({ ...current, [field]: value }));
    },
    [],
  );

  const onRangeChange = useCallback(
    (index: number, field: keyof MetricRangeForm, value: string) => {
      setForm((current) => ({
        ...current,
        ranges: current.ranges.map((range, i) =>
          i === index ? { ...range, [field]: value } : range
        ),
      }));
    },
    [],
  );

  const addRange = useCallback(() => {
    setForm((current) => ({
      ...current,
      ranges: [...current.ranges, emptyRange()],
    }));
  }, []);

  const removeRange = useCallback((index: number) => {
    setForm((current) => ({
      ...current,
      ranges: current.ranges.length === 1
        ? current.ranges
        : current.ranges.filter((_, i) => i !== index),
    }));
  }, []);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const payload = buildPayload(form);
    await onSubmitPayload(payload);
  };

  if (!open) {
    return null;
  }

  const isDraftEditing = mode === "edit" && metric?.status === "draft";
  const title = mode === "create"
    ? "Tạo chỉ số mới"
    : isDraftEditing
      ? "Cập nhật bản nháp"
      : "Tạo bản chỉnh sửa nháp";

  const description = mode === "create"
    ? (adminConfig?.multiAdminMode
      ? "Yêu cầu tạo chỉ số mới sẽ được gửi tới hàng đợi phê duyệt."
      : "Chỉ số mới sẽ được tạo và áp dụng ngay lập tức.")
    : isDraftEditing
      ? "Bạn đang chỉnh sửa trực tiếp bản nháp hiện có của chỉ số này."
      : "Bạn đang chỉnh sửa chỉ số đang áp dụng. Hệ thống sẽ tạo bản thay đổi nháp thay vì sửa trực tiếp.";

  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
      <div className="w-full max-w-5xl rounded-[32px] bg-white shadow-2xl">
        <div className="flex items-start justify-between border-b border-slate-200 px-6 py-5">
          <div>
            <h3 className="text-2xl font-bold text-slate-900">{title}</h3>
            <p className="mt-2 max-w-3xl text-sm text-slate-500">{description}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full p-2 text-slate-500 transition hover:bg-slate-100 hover:text-slate-700"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form
          className="max-h-[80vh] overflow-y-auto px-6 py-5"
          onSubmit={handleSubmit}
          aria-describedby={formError ? "reference-metric-form-error" : undefined}
        >
          <div className="grid gap-4 md:grid-cols-3">
            <Field label="Tên chỉ số (tiếng Anh)">
              <input
                value={form.name}
                onChange={(event) => onChange("name", event.target.value)}
                className={inputClassName}
                placeholder="Glucose"
                required
              />
            </Field>

            <Field label="Tên hiển thị tiếng Việt">
              <input
                value={form.displayNameVi}
                onChange={(event) => onChange("displayNameVi", event.target.value)}
                className={inputClassName}
                placeholder="Đường huyết"
                required
              />
            </Field>

            <Field label="Đơn vị">
              <input
                value={form.unit}
                onChange={(event) => onChange("unit", event.target.value)}
                className={inputClassName}
                placeholder="mmol/L"
                required
              />
            </Field>
          </div>

          <div className="mt-6 space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h4 className="text-base font-semibold text-slate-900">Ngưỡng tham chiếu</h4>
                <p className="mt-1 text-sm text-slate-500">
                  Bạn có thể khai báo nhiều ngưỡng cho từng giới tính hoặc nhóm tuổi khác nhau.
                </p>
              </div>
              <button
                type="button"
                onClick={addRange}
                className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-teal-200 hover:text-teal-700"
              >
                <Plus className="h-4 w-4" />
                Thêm ngưỡng
              </button>
            </div>

            {form.ranges.map((range, index) => (
              <div key={index} className="rounded-3xl border border-slate-200 p-4">
                <div className="mb-4 flex items-center justify-between">
                  <h5 className="font-semibold text-slate-900">Ngưỡng #{index + 1}</h5>
                  <button
                    type="button"
                    onClick={() => removeRange(index)}
                    disabled={form.ranges.length === 1}
                    className="rounded-xl border border-rose-200 px-3 py-2 text-sm font-medium text-rose-600 transition hover:bg-rose-50 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    Xóa ngưỡng
                  </button>
                </div>

                <div className="grid gap-4 lg:grid-cols-4">
                  <Field label="Min bình thường">
                    <input
                      type="number"
                      step="0.0001"
                      value={range.minValue}
                      onChange={(event) => onRangeChange(index, "minValue", event.target.value)}
                      className={inputClassName}
                      required
                    />
                  </Field>
                  <Field label="Max bình thường">
                    <input
                      type="number"
                      step="0.0001"
                      value={range.maxValue}
                      onChange={(event) => onRangeChange(index, "maxValue", event.target.value)}
                      className={inputClassName}
                      required
                    />
                  </Field>
                  <Field label="Min cảnh báo">
                    <input
                      type="number"
                      step="0.0001"
                      value={range.attentionMin}
                      onChange={(event) => onRangeChange(index, "attentionMin", event.target.value)}
                      className={inputClassName}
                      required
                    />
                  </Field>
                  <Field label="Max cảnh báo">
                    <input
                      type="number"
                      step="0.0001"
                      value={range.attentionMax}
                      onChange={(event) => onRangeChange(index, "attentionMax", event.target.value)}
                      className={inputClassName}
                      required
                    />
                  </Field>
                </div>

                <div className="mt-4 grid gap-4 md:grid-cols-3">
                  <Field label="Giới tính">
                    <select
                      value={range.gender}
                      onChange={(event) => onRangeChange(index, "gender", event.target.value)}
                      className={inputClassName}
                    >
                      <option value="">Tất cả</option>
                      <option value="male">Nam</option>
                      <option value="female">Nữ</option>
                    </select>
                  </Field>
                  <Field label="Tuổi tối thiểu">
                    <input
                      type="number"
                      value={range.minAge}
                      onChange={(event) => onRangeChange(index, "minAge", event.target.value)}
                      className={inputClassName}
                      placeholder="18"
                    />
                  </Field>
                  <Field label="Tuổi tối đa">
                    <input
                      type="number"
                      value={range.maxAge}
                      onChange={(event) => onRangeChange(index, "maxAge", event.target.value)}
                      className={inputClassName}
                      placeholder="65"
                    />
                  </Field>
                </div>
              </div>
            ))}
          </div>

          {formError ? (
            <InlineFieldError id="reference-metric-form-error" message={formError} />
          ) : null}

          <div className="mt-6 flex flex-col-reverse gap-3 border-t border-slate-200 pt-5 sm:flex-row sm:justify-end">
            <button
              type="button"
              onClick={onClose}
              disabled={isPending}
              className="rounded-2xl border border-slate-200 px-5 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isPending}
              className="inline-flex items-center justify-center gap-2 rounded-2xl bg-teal-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-teal-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : mode === "create" ? <Plus className="h-4 w-4" /> : <Pencil className="h-4 w-4" />}
              {mode === "create" ? "Lưu bản nháp" : isDraftEditing ? "Cập nhật bản nháp" : "Tạo bản chỉnh sửa nháp"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function ActionModal({
  open,
  title,
  description,
  confirmLabel,
  tone,
  isPending,
  onCancel,
  onConfirm,
}: {
  open: boolean;
  title?: string;
  description?: string;
  confirmLabel?: string;
  tone: "danger" | "success";
  isPending: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  if (!open) {
    return null;
  }

  const isDanger = tone === "danger";
  return (
    <div className="fixed inset-0 z-[80] flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
      <div className="w-full max-w-md rounded-3xl bg-white p-8 shadow-2xl">
        <div className="flex flex-col items-center text-center">
          <div className={`mb-5 flex h-16 w-16 items-center justify-center rounded-full ${isDanger ? "bg-rose-100 text-rose-700" : "bg-emerald-100 text-emerald-700"}`}>
            {isDanger ? <Trash2 className="h-7 w-7" /> : <RotateCcw className="h-7 w-7" />}
          </div>
          <h3 className={`mb-2 text-2xl font-bold ${isDanger ? "text-rose-800" : "text-emerald-800"}`}>
            {title}
          </h3>
          <p className="mb-8 text-slate-600">{description}</p>
          <div className="flex w-full flex-col gap-3">
            <button
              type="button"
              onClick={onConfirm}
              disabled={isPending}
              className={`inline-flex w-full items-center justify-center gap-2 rounded-2xl py-4 font-bold text-white transition disabled:cursor-not-allowed disabled:opacity-70 ${isDanger ? "bg-rose-700 hover:brightness-110" : "bg-emerald-600 hover:brightness-110"
                }`}
            >
              {isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
              {isPending ? "Đang xử lý..." : confirmLabel}
            </button>
            <button
              type="button"
              onClick={onCancel}
              disabled={isPending}
              className="w-full rounded-2xl border border-slate-200 bg-white py-4 font-bold text-slate-600 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-70"
            >
              Hủy
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block space-y-2">
      <span className="text-sm font-medium text-slate-700">{label}</span>
      {children}
    </label>
  );
}

function InfoCell({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-xs uppercase tracking-wide text-slate-400">{label}</div>
      <div className="mt-1 font-medium text-slate-700">{value}</div>
    </div>
  );
}


function statusLabel(status: string) {
  if (status === "active") return "Đang áp dụng";
  if (status === "pending") return "Chờ phê duyệt";
  return "Ngưng áp dụng";
}

function statusBadge(status: string) {
  if (status === "active") {
    return "inline-flex rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-700";
  }
  if (status === "pending") {
    return "inline-flex rounded-full bg-indigo-100 px-3 py-1 text-xs font-semibold text-indigo-700";
  }
  return "inline-flex rounded-full bg-slate-200 px-3 py-1 text-xs font-semibold text-slate-700";
}

const inputClassName =
  "h-11 w-full rounded-2xl border border-slate-200 px-4 text-sm text-slate-900 outline-none transition focus:border-teal-500 focus:ring-2 focus:ring-teal-100";
