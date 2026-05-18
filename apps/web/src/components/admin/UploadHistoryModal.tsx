"use client";

import { useQuery } from "@tanstack/react-query";
import { format } from "date-fns";
import { vi } from "date-fns/locale";
import { ChevronLeft, ChevronRight, History, X } from "lucide-react";

import { adminApiClient } from "@/lib/api/adminApiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { EmptyState, ErrorState, LoadingState } from "@/components/ui";

export type UploadHistoryFilter = {
  status?: "done" | "ocr_failed";
  failureReason?: string;
};

type UploadHistoryItem = {
  id: string;
  userId: string;
  userEmail: string;
  userFullName: string;
  profileId: string;
  profileDisplayName: string;
  status: string;
  failureReason: string | null;
  createdAt: string;
  hospitalName: string | null;
  recordType: string | null;
};

type UploadHistoryPageResponse = {
  items: UploadHistoryItem[];
  page: number;
  limit: number;
  total: number;
  totalPages: number;
};

const FAILURE_LABELS: Record<string, string> = {
  timeout: "Timeout",
  low_confidence: "Độ tin cậy thấp",
  api_error: "Lỗi API",
  invalid_file: "Tệp không hợp lệ",
};

const FILTER_TITLES: Record<string, string> = {
  all: "Tất cả lượt tải lên",
  done: "Tải lên thành công",
  ocr_failed: "Tải lên thất bại",
};

type UploadHistoryModalProps = {
  open: boolean;
  onClose: () => void;
  from: string;
  to: string;
  filter: UploadHistoryFilter;
  page: number;
  onPageChange: (page: number) => void;
};

export function UploadHistoryModal({
  open,
  onClose,
  from,
  to,
  filter,
  page,
  onPageChange,
}: UploadHistoryModalProps) {
  const filterKey = filter.status ?? "all";
  const subtitle =
    filter.failureReason != null
      ? `${FILTER_TITLES[filterKey] ?? "Tải lên"} · ${FAILURE_LABELS[filter.failureReason] ?? filter.failureReason}`
      : (FILTER_TITLES[filterKey] ?? "Tải lên");

  const { data, isLoading, isError, refetch, isFetching } = useQuery({
    queryKey: ["admin-upload-history", from, to, filter.status, filter.failureReason, page],
    queryFn: async () => {
      const response = await adminApiClient.get<{ data: UploadHistoryPageResponse }>(
        API_ROUTES.ADMIN_ANALYTICS.UPLOAD_HISTORY,
        {
          params: {
            from,
            to,
            page,
            limit: 20,
            ...(filter.status ? { status: filter.status } : {}),
            ...(filter.failureReason ? { failureReason: filter.failureReason } : {}),
          },
        },
      );
      return response.data.data;
    },
    enabled: open,
  });

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-100 flex items-center justify-center bg-black/40 p-6 sm:p-8"
      role="presentation"
      onClick={onClose}
    >
      <div
        className="flex w-full max-w-2xl max-h-[min(90vh,860px)] flex-col rounded-2xl bg-white shadow-xl"
        role="dialog"
        aria-modal="true"
        aria-labelledby="upload-history-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-6 py-5 shrink-0">
          <div>
            <h2
              id="upload-history-title"
              className="text-lg font-bold text-slate-900 flex items-center gap-2"
            >
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-teal-50">
                <History className="h-5 w-5 text-teal-600" />
              </span>
              Lịch sử upload
            </h2>
            <p className="text-sm text-slate-500 mt-1 pl-11">{subtitle}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 shrink-0"
            aria-label="Đóng"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-6 sm:px-8 py-5 min-h-0">
          {isLoading ? (
            <LoadingState title="Đang tải lịch sử upload" className="min-h-64 border-slate-200 bg-slate-50" />
          ) : isError ? (
            <ErrorState
              title="Không tải được lịch sử"
              description="Vui lòng thử lại để xem các lượt upload."
              actionLabel="Thử lại"
              onAction={() => void refetch()}
              className="min-h-64 border-slate-200 bg-slate-50"
            />
          ) : data && data.items.length === 0 ? (
            <EmptyState
              title="Không có bản ghi"
              description="Không có lượt upload nào trong khoảng đã chọn."
              className="min-h-64 border-slate-200 bg-slate-50"
            />
          ) : data ? (
            <ul className="space-y-3">
              {data.items.map((item) => (
                <li
                  key={item.id}
                  className="rounded-xl border border-slate-200 bg-white p-4 space-y-2 shadow-sm"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <p className="font-semibold text-slate-900 truncate">{item.userFullName}</p>
                      <p className="text-xs text-slate-500 truncate">{item.userEmail}</p>
                    </div>
                    <StatusBadge status={item.status} />
                  </div>
                  <div className="text-sm text-slate-600 space-y-1">
                    <p>
                      Hồ sơ:{" "}
                      <span className="font-semibold text-slate-800">{item.profileDisplayName}</span>
                    </p>
                    <p>
                      Thời gian:{" "}
                      <span className="font-semibold text-slate-800">
                        {format(new Date(item.createdAt), "dd/MM/yyyy HH:mm", { locale: vi })}
                      </span>
                    </p>
                    {item.hospitalName && (
                      <p>
                        Cơ sở: <span className="text-slate-800">{item.hospitalName}</span>
                      </p>
                    )}
                    {item.recordType && (
                      <p>
                        Loại: <span className="text-slate-800">{item.recordType}</span>
                      </p>
                    )}
                    {item.failureReason && (
                      <p className="text-red-600">
                        Nguyên nhân:{" "}
                        <span className="font-semibold">
                          {FAILURE_LABELS[item.failureReason] ?? item.failureReason}
                        </span>
                      </p>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          ) : null}

          {isFetching && !isLoading && (
            <p className="text-center text-xs text-slate-400 mt-4">Đang cập nhật…</p>
          )}
        </div>

        {data && data.totalPages > 1 && (
          <div className="border-t border-slate-200 px-6 py-4 flex items-center justify-between shrink-0 rounded-b-2xl bg-white">
            <p className="text-sm text-slate-500">
              Trang {data.page + 1} / {data.totalPages} · {data.total} bản ghi
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                disabled={page <= 0}
                onClick={() => onPageChange(page - 1)}
                className="flex items-center gap-1 rounded-lg border border-slate-200 px-3 py-1.5 text-sm disabled:opacity-40 hover:bg-slate-50"
              >
                <ChevronLeft className="h-4 w-4" />
                Trước
              </button>
              <button
                type="button"
                disabled={page >= data.totalPages - 1}
                onClick={() => onPageChange(page + 1)}
                className="flex items-center gap-1 rounded-lg border border-slate-200 px-3 py-1.5 text-sm disabled:opacity-40 hover:bg-slate-50"
              >
                Sau
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  if (status === "done") {
    return (
      <span className="shrink-0 rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-semibold text-emerald-800">
        Thành công
      </span>
    );
  }
  return (
    <span className="shrink-0 rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-semibold text-red-800">
      Thất bại
    </span>
  );
}
