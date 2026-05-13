"use client";
import { useMemo, useRef, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  AlertCircle,
  CheckCircle2,
  FileUp,
  Loader2,
  Paperclip,
  UploadCloud,
  X,
} from "lucide-react";

import { adminApiClient } from "@/lib/api/adminApiClient";
import { API_ROUTES, API_TIMEOUT } from "@/lib/api/routes";
import {
  formatNoticeNewMetricMultiAdmin,
  NOTICE_NEW_METRIC_SINGLE_ADMIN,
} from "@/lib/admin/referenceDataNotices";

type PreviewRow = {
  line: number;
  metricName: string;
  displayNameVi: string;
  unit: string;
  minValue: number;
  maxValue: number;
  gender: string | null;
  minAge: number | null;
  maxAge: number | null;
};

type ErrorRow = {
  line: number;
  error: string;
};

type PreviewResponse = {
  importId: string;
  validRows: PreviewRow[];
  errorRows: ErrorRow[];
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
      status?: number;
      data?: { detail?: string; title?: string; message?: string };
    };
    const d = response.data;
    if (d?.detail) return d.detail;
    if (d?.title && d?.message) return `${d.title}: ${d.message}`;
    if (d?.title) return d.title;
    if (d?.message) return d.message;
    if (response.status === 413) return "File quá lớn so với giới hạn máy chủ.";
    return "Không thể xử lý yêu cầu.";
  }
  if (error && typeof error === "object" && "message" in error && typeof (error as Error).message === "string") {
    return (error as Error).message;
  }
  return "Không thể kết nối tới máy chủ.";
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  const kb = bytes / 1024;
  if (kb < 1024) return `${kb.toFixed(1)} KB`;
  return `${(kb / 1024).toFixed(1)} MB`;
}

export default function ReferenceDataImportPage() {
  const queryClient = useQueryClient();
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<PreviewResponse | null>(null);
  const [notice, setNotice] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const activePreviewTokenRef = useRef(0);
  const [isDragging, setIsDragging] = useState(false);

  const bumpPreviewToken = () => {
    activePreviewTokenRef.current += 1;
    return activePreviewTokenRef.current;
  };

  const stats = useMemo(
    () => ({ validCount: preview?.validRows.length ?? 0, errorCount: preview?.errorRows.length ?? 0 }),
    [preview],
  );

  const previewMutation = useMutation({
    mutationFn: async ({ targetFile, token }: { targetFile: File; token: number }) => {
      const formData = new FormData();
      formData.append("file", targetFile);
      const response = await adminApiClient.post(
        API_ROUTES.ADMIN_REFERENCE_DATA.IMPORT_PREVIEW,
        formData,
        { timeout: API_TIMEOUT.UPLOAD },
      );
      return {
        token,
        preview: response.data.data as PreviewResponse,
      };
    },
    onSuccess: ({ token, preview }) => {
      if (token !== activePreviewTokenRef.current) {
        return;
      }
      setPreview(preview);
      setNotice(null);
    },
    onError: (error, variables) => {
      if (variables.token !== activePreviewTokenRef.current) {
        return;
      }
      setPreview(null);
      setNotice({ type: "error", message: parseApiError(error) });
    },
  });

  const confirmMutation = useMutation({
    mutationFn: async (importId: string) => {
      const response = await adminApiClient.post(API_ROUTES.ADMIN_REFERENCE_DATA.IMPORT_CONFIRM, { importId });
      return response.data.data as {
        draftChangeSetCount: number;
        message: string;
        changeSetIds: string[];
      };
    },
    onSuccess: async (data) => {
      const configRes = await adminApiClient.get(API_ROUTES.ADMIN_REFERENCE_DATA.CONFIG);
      const multiAdminMode = Boolean(configRes.data?.data?.multiAdminMode);

      const changeSetIds = data.changeSetIds ?? [];
      if (multiAdminMode) {
        await Promise.all(
          changeSetIds.map((id) =>
            adminApiClient.post(API_ROUTES.ADMIN_REFERENCE_DATA.SUBMIT_CHANGE_SET(id)),
          ),
        );
      } else {
        await Promise.all(
          changeSetIds.map((id) =>
            adminApiClient.post(API_ROUTES.ADMIN_REFERENCE_DATA.PUBLISH_CHANGE_SET(id)),
          ),
        );
      }

      bumpPreviewToken();
      setPreview(null);
      setFile(null);
      if (inputRef.current) inputRef.current.value = "";

      setNotice({
        type: "success",
        message: multiAdminMode
          ? formatNoticeNewMetricMultiAdmin(data.draftChangeSetCount)
          : NOTICE_NEW_METRIC_SINGLE_ADMIN,
      });
      void queryClient.invalidateQueries({ queryKey: ["admin-reference-metrics"] });
    },
    onError: (error) => {
      setNotice({ type: "error", message: parseApiError(error) });
    },
  });

  const validateFile = (nextFile: File) => {
    const ext = nextFile.name.split(".").pop()?.toLowerCase();
    if (!ext || !["csv", "json"].includes(ext)) {
      return "Chỉ hỗ trợ file .csv hoặc .json.";
    }
    if (nextFile.size > 5 * 1024 * 1024) {
      return "File vượt quá 5MB. Vui lòng chọn file nhỏ hơn.";
    }
    return null;
  };

  const handleFileChange = async (nextFile: File | null) => {
    if (!nextFile) return;
    const error = validateFile(nextFile);
    if (error) {
      bumpPreviewToken();
      setNotice({ type: "error", message: error });
      setFile(null);
      setPreview(null);
      if (inputRef.current) inputRef.current.value = "";
      return;
    }
    const token = bumpPreviewToken();
    setFile(nextFile);
    setPreview(null);
    setNotice(null);
    await previewMutation.mutateAsync({ targetFile: nextFile, token });
  };

  const clearFile = () => {
    bumpPreviewToken();
    setFile(null);
    setPreview(null);
    setNotice(null);
    if (inputRef.current) inputRef.current.value = "";
  };

  return (
    <div className="space-y-6">
      <div className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div className="max-w-2xl">
            <div className="inline-flex items-center gap-2 rounded-full bg-teal-50 px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-teal-700">
              <UploadCloud className="h-3.5 w-3.5" />
              Nhập dữ liệu tham chiếu
            </div>
            <h1 className="mt-4 text-3xl font-bold tracking-tight text-slate-900">
              Nhập dữ liệu tham chiếu
            </h1>
            <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-500">
             Nhập file CSV/JSON để hệ thống phân tích, xác thưc và hiển thị dữ liệu trước khi áp dụng. Sau khi xác nhận, dữ liệu sẽ được đưa vào hàng chờ phê duyệt.
            </p>
          </div>
        </div>
      </div>

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
        </div>
      ) : null}

      <div className="rounded-[28px] border border-slate-200 bg-white p-4 shadow-sm lg:p-5">
        <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_360px]">
          <label
            className={`group flex min-h-[180px] cursor-pointer flex-col items-center justify-center rounded-[24px] border-2 border-dashed bg-slate-50 px-6 py-10 text-center transition hover:bg-teal-50/50 ${isDragging
              ? "border-teal-400 ring-2 ring-teal-100"
              : "border-slate-200 hover:border-teal-300"
              }`}
            onDragEnter={() => setIsDragging(true)}
            onDragOver={(event) => {
              event.preventDefault();
              setIsDragging(true);
            }}
            onDragLeave={() => setIsDragging(false)}
            onDrop={(event) => {
              event.preventDefault();
              setIsDragging(false);
              const dropped = event.dataTransfer.files?.[0] ?? null;
              void handleFileChange(dropped);
            }}
          >
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-white shadow-sm ring-1 ring-slate-200 transition group-hover:ring-teal-200">
              <FileUp className="h-7 w-7 text-teal-600" />
            </div>
            <div className="mt-5 text-base font-semibold text-slate-800">
              Kéo thả file vào đây hoặc bấm để chọn
            </div>
            <div className="mt-2 text-sm text-slate-500">
              Hỗ trợ .csv / .json — tối đa 5MB
            </div>
            <input
              ref={inputRef}
              type="file"
              accept=".csv,.json,application/json,text/csv"
              className="hidden"
              onChange={(event) => {
                const nextFile = event.target.files?.[0] ?? null;
                void handleFileChange(nextFile);
              }}
            />
          </label>

          <div className="flex flex-col rounded-[24px] border border-slate-200 bg-white p-4">
            <div className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">
              Kết quả
            </div>

            <div className="mt-4 grid grid-cols-2 gap-3">
              <div className="rounded-2xl border border-emerald-100 bg-emerald-50 p-4">
                <div className="text-xs font-semibold uppercase tracking-wide text-emerald-700">Hợp lệ</div>
                <div className="mt-1 text-3xl font-bold text-emerald-700">{stats.validCount}</div>
              </div>
              <div className="rounded-2xl border border-rose-100 bg-rose-50 p-4">
                <div className="text-xs font-semibold uppercase tracking-wide text-rose-700">Lỗi</div>
                <div className="mt-1 text-3xl font-bold text-rose-700">{stats.errorCount}</div>
              </div>
            </div>

            <button
              type="button"
              disabled={confirmMutation.isPending || !preview || preview.validRows.length === 0}
              onClick={() => preview && void confirmMutation.mutateAsync(preview.importId)}
              className="mt-4 inline-flex items-center justify-center gap-2 rounded-2xl bg-teal-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-teal-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {confirmMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
              Xác nhận nhập dữ liệu
            </button>
          </div>
        </div>

        {file ? (
          <div className="mt-4 flex flex-col gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white text-teal-600 ring-1 ring-slate-200">
                <Paperclip className="h-4 w-4" />
              </div>
              <div>
                <div className="text-sm font-semibold text-slate-800">{file.name}</div>
                <div className="text-xs text-slate-500">{formatFileSize(file.size)}</div>
              </div>
            </div>

            <div className="flex gap-2 self-start sm:self-auto">
              <button
                type="button"
                onClick={clearFile}
                className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-600 transition hover:border-rose-200 hover:text-rose-600"
              >
                <X className="h-4 w-4" />
                Bỏ chọn
              </button>
            </div>
          </div>
        ) : null}
      </div>

      {preview ? (
        <div className="grid gap-6 lg:grid-cols-2">
          <div className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-900">Dữ liệu hợp lệ</h2>
              <span className="inline-flex h-7 min-w-7 items-center justify-center rounded-full bg-emerald-100 px-2 text-xs font-bold text-emerald-700">
                {preview.validRows.length}
              </span>
            </div>

            <div className="mt-4 overflow-x-auto rounded-2xl border border-slate-200">
              <table className="min-w-full divide-y divide-slate-200 text-sm">
                <thead className="bg-slate-50 text-left text-slate-500">
                  <tr>
                    <th className="px-4 py-3 font-medium">Chỉ số</th>
                    <th className="px-4 py-3 font-medium">Đơn vị</th>
                    <th className="px-4 py-3 font-medium">Ngưỡng min</th>
                    <th className="px-4 py-3 font-medium">Ngưỡng max</th>
                    <th className="px-4 py-3 font-medium">Giới tính</th>
                    <th className="px-4 py-3 font-medium">Tuổi</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 bg-white">
                  {preview.validRows.map((row) => (
                    <tr key={`${row.line}-${row.metricName}-${row.gender ?? "all"}-${row.minAge ?? 0}-${row.maxAge ?? "inf"}`}>
                      <td className="px-4 py-3">
                        <div className="font-semibold text-slate-900">{row.displayNameVi}</div>
                        <div className="text-xs text-slate-500">{row.metricName}</div>
                      </td>
                      <td className="px-4 py-3 text-slate-700">{row.unit}</td>
                      <td className="px-4 py-3 text-slate-700">{row.minValue}</td>
                      <td className="px-4 py-3 text-slate-700">{row.maxValue}</td>
                      <td className="px-4 py-3 text-slate-700">
                        {row.gender === "male" ? "Nam" : row.gender === "female" ? "Nữ" : "Tất cả"}
                      </td>
                      <td className="px-4 py-3 text-slate-700">
                        {row.minAge ?? 0} - {row.maxAge ?? "∞"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-900">Dữ liệu lỗi</h2>
              <span className="inline-flex h-7 min-w-7 items-center justify-center rounded-full bg-rose-100 px-2 text-xs font-bold text-rose-700">
                {preview.errorRows.length}
              </span>
            </div>

            {preview.errorRows.length === 0 ? (
              <div className="mt-4 flex min-h-[180px] items-center justify-center rounded-2xl border border-slate-200 bg-slate-50 text-sm text-slate-500">
                Không có lỗi.
              </div>
            ) : (
              <div className="mt-4 overflow-x-auto rounded-2xl border border-slate-200">
                <table className="min-w-full divide-y divide-slate-200 text-sm">
                  <thead className="bg-slate-50 text-left text-slate-500">
                    <tr>
                      <th className="px-4 py-3 font-medium">Line</th>
                      <th className="px-4 py-3 font-medium">Lý do</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200 bg-white">
                    {preview.errorRows.map((row) => (
                      <tr key={`${row.line}-${row.error}`}>
                        <td className="px-4 py-3 text-slate-700">{row.line}</td>
                        <td className="px-4 py-3 text-rose-700">{row.error}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      ) : null}
    </div>
  );
}
