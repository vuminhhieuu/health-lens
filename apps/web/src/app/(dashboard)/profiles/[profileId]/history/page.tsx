"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import {
  AlertTriangle,
  Calendar,
  Loader2,
  Activity,
  Search,
  Filter,
  ShieldPlus,
  TrendingUp,
  Trash2,
  Eye,
  ChevronRight,
} from "lucide-react";

import { ApiPaths } from "@healthlens/shared/constants";

import { UploadButton } from "@/components/features/upload/UploadButton";
import { apiClient } from "@/lib/api/apiClient";
import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { DeleteRecordModal } from "@/components/features/health-records/DeleteRecordModal";

type HistoryItem = {
  id: string;
  status?: string | null;
  examDate: string | null;
  testType: string;
  overallStatus: "normal" | "attention" | "abnormal" | string;
  abnormalCount: number;
  hospitalName: string | null;
  sourceType: string | null;
  createdAt: string;
  canDelete?: boolean;
};

type HistoryPagination = {
  page: number;
  limit: number;
  total: number;
  totalPages: number;
};

type HistoryPageResponse = {
  data: HistoryItem[];
  pagination: HistoryPagination;
};

type Profile = {
  id: string;
  displayName: string;
};

type SharedProfile = {
  profileId: string;
  accessLevel: "view" | "edit" | string;
};

const PAGE_SIZE = 20;

export default function ProfileHistoryPage() {
  const params = useParams<{ profileId: string }>();
  const router = useRouter();
  const searchParams = useSearchParams();
  const profileId = params.profileId;
  const loadMoreRef = useRef<HTMLDivElement | null>(null);
  const historyQueryKey = ["profile-history", profileId] as const;
  const [searchTerm, setSearchTerm] = useState("");
  const [periodFilter, setPeriodFilter] = useState("all");
  const [testTypeFilter, setTestTypeFilter] = useState("all");
  const [statusFilter, setStatusFilter] = useState("all");
  const [deleteTarget, setDeleteTarget] = useState<HistoryItem | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const { data: profiles = [] } = useQuery({
    queryKey: ["profiles-for-history-breadcrumb"],
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.PROFILES.BASE);
      return (response.data?.data ?? []) as Profile[];
    },
  });
  const { data: sharedProfiles = [] } = useQuery({
    queryKey: ["shared-profiles"],
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.SHARED_PROFILES.LIST);
      return (response.data?.data ?? []) as SharedProfile[];
    },
  });
  const historyQuery = useInfiniteQuery({
    queryKey: historyQueryKey,
    enabled: Boolean(profileId),
    initialPageParam: 0,
    queryFn: async ({ pageParam }) => {
      const response = await apiClient.get(
        ApiPaths.PROFILES.HEALTH_RECORDS(profileId),
        {
          params: { page: pageParam, limit: PAGE_SIZE },
        },
      );
      return {
        data: (response.data?.data ?? []) as HistoryItem[],
        pagination: response.data?.pagination as HistoryPagination,
      } satisfies HistoryPageResponse;
    },
    getNextPageParam: (lastPage) => {
      const nextPage = lastPage.pagination.page + 1;
      return nextPage < lastPage.pagination.totalPages ? nextPage : undefined;
    },
  });
  const { fetchNextPage, hasNextPage, isFetchingNextPage } = historyQuery;
  const deleteMutation = useMutation({
    mutationFn: async (recordId: string) => {
      await apiClient.delete(ApiPaths.HEALTH_RECORDS.DELETE(recordId));
      return recordId;
    },
    onSuccess: (recordId) => {
      queryClient.setQueryData(
        historyQueryKey,
        (oldData: typeof historyQuery.data) => {
          if (!oldData) return oldData;
          const didRemoveRecord = oldData.pages.some((page) =>
            page.data.some((item) => item.id === recordId),
          );
          if (!didRemoveRecord) return oldData;
          const nextTotal = Math.max(
            0,
            (oldData.pages[0]?.pagination.total ?? 0) - 1,
          );
          return {
            ...oldData,
            pages: oldData.pages.map((page) => {
              const nextTotalPages =
                page.pagination.limit > 0
                  ? Math.max(1, Math.ceil(nextTotal / page.pagination.limit))
                  : page.pagination.totalPages;
              return {
                ...page,
                data: page.data.filter((item) => item.id !== recordId),
                pagination: {
                  ...page.pagination,
                  total: nextTotal,
                  totalPages: nextTotalPages,
                },
              };
            }),
          };
        },
      );
      setDeleteError(null);
      setDeleteTarget(null);
    },
    onError: () => {
      setDeleteError("Xóa kết quả thất bại. Vui lòng thử lại.");
    },
  });
  useEffect(() => {
    if (!loadMoreRef.current) return;
    const node = loadMoreRef.current;
    const observer = new IntersectionObserver(
      (entries) => {
        const firstEntry = entries[0];
        if (firstEntry?.isIntersecting && hasNextPage && !isFetchingNextPage) {
          void fetchNextPage();
        }
      },
      { rootMargin: "200px" },
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, [fetchNextPage, hasNextPage, isFetchingNextPage]);

  const allItems = useMemo(
    () => historyQuery.data?.pages.flatMap((page) => page.data) ?? [],
    [historyQuery.data?.pages],
  );
  const currentProfileName = useMemo(
    () =>
      profiles.find((profile) => profile.id === profileId)?.displayName ??
      searchParams.get("displayName")?.trim() ??
      "Hồ sơ",
    [profiles, profileId, searchParams],
  );
  const canUpload = useMemo(() => {
    const isOwnedProfile = profiles.some((profile) => profile.id === profileId);
    if (isOwnedProfile) {
      return true;
    }
    const sharedProfile = sharedProfiles.find((profile) => profile.profileId === profileId);
    return sharedProfile?.accessLevel === "edit";
  }, [profileId, profiles, sharedProfiles]);
  const availableTestTypes = useMemo(() => {
    const uniqueValues = new Set(
      allItems
        .map((item) => item.testType?.trim())
        .filter((value): value is string => Boolean(value)),
    );
    return Array.from(uniqueValues).sort((a, b) => a.localeCompare(b, "vi"));
  }, [allItems]);
  const filteredItems = useMemo(() => {
    const normalizedSearch = searchTerm.trim().toLowerCase();

    return allItems.filter((item) => {
      const matchesSearch =
        normalizedSearch.length === 0 ||
        item.testType?.toLowerCase().includes(normalizedSearch) ||
        item.hospitalName?.toLowerCase().includes(normalizedSearch);
      if (!matchesSearch) return false;

      if (testTypeFilter !== "all" && item.testType !== testTypeFilter) {
        return false;
      }

      if (statusFilter !== "all") {
        const itemStatus = resolveHistoryStatus(item);
        if (itemStatus !== statusFilter) {
          return false;
        }
      }

      if (periodFilter === "all") {
        return true;
      }

      if (!item.examDate) {
        return false;
      }

      const examDate = new Date(item.examDate);
      if (Number.isNaN(examDate.getTime())) {
        return false;
      }

      const now = new Date();
      const threshold = new Date(now);
      switch (periodFilter) {
        case "7d":
          threshold.setDate(now.getDate() - 7);
          break;
        case "30d":
          threshold.setDate(now.getDate() - 30);
          break;
        case "90d":
          threshold.setDate(now.getDate() - 90);
          break;
        case "12m":
          threshold.setMonth(now.getMonth() - 12);
          break;
        default:
          return true;
      }

      return examDate >= threshold;
    });
  }, [allItems, periodFilter, searchTerm, statusFilter, testTypeFilter]);

  if (historyQuery.isLoading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center bg-[#effcf9]">
        <Loader2 className="h-8 w-8 animate-spin text-[#00685f]" />
      </div>
    );
  }

  return (
    <DashboardPageShell
      title="Lịch sử khám bệnh"
      subtitle="Xem diễn tiến sức khỏe theo thời gian, mới nhất ở trên cùng."
      breadcrumbs={[
        { label: "Kết quả khám", href: "/health-records" },
        { label: currentProfileName },
      ]}
    >
      {allItems.length === 0 ? (
        <section className="mt-3 rounded-3xl border-2 border-dashed border-[#b7d8d1] bg-white px-12 py-14 text-center shadow-sm">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-[#effcf9] text-[#00685f]">
            <Activity className="h-7 w-7" />
          </div>
          <p className="mb-5 text-[#4e6360]">Hồ sơ này chưa có kết quả nào.</p>
          {canUpload ? (
            <div className="mx-auto w-fit">
              <UploadButton
                profileId={profileId}
                label="Thêm kết quả đầu tiên"
                size="compact"
              />
            </div>
          ) : null}
        </section>
      ) : (
        <section className="mt-3 space-y-4">
          <article className="rounded-2xl border border-[#b7d8d1] bg-white p-6 shadow-sm">
            <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <h2 className="text-2xl font-bold text-[#005049]">
                  Kết quả Sức khỏe
                </h2>
                <p className="mt-1 text-sm text-[#6d7a77]">
                  Quản lý và theo dõi tất cả các kết quả xét nghiệm y tế của
                  bạn.
                </p>
              </div>
              {canUpload ? <UploadButton profileId={profileId} /> : null}
            </div>

            <div className="mb-4 grid grid-cols-1 divide-y divide-[#e8eeec] overflow-hidden rounded-2xl border border-[#dde6e3] bg-[#f8fbfa] lg:grid-cols-4 lg:divide-x lg:divide-y-0">
              <label className="flex min-h-13 items-center gap-2.5 px-5 text-sm font-medium text-[#7b8a87]">
                <Search className="h-3.75 w-3.75" />
                <input
                  value={searchTerm}
                  onChange={(event) => setSearchTerm(event.target.value)}
                  placeholder="Tìm kiếm kết quả..."
                  className="w-full bg-transparent text-sm text-[#4e6360] placeholder:text-[#9ba9a6] outline-none"
                />
              </label>
              <label className="flex min-h-13 items-center gap-2.5 px-5 text-sm font-medium text-[#7b8a87]">
                <Calendar className="h-3.75 w-3.75" />
                <select
                  value={periodFilter}
                  onChange={(event) => setPeriodFilter(event.target.value)}
                  className="w-full bg-transparent text-sm text-[#4e6360] outline-none"
                >
                  <option value="all">Khoảng thời gian</option>
                  <option value="7d">7 ngày gần đây</option>
                  <option value="30d">30 ngày gần đây</option>
                  <option value="90d">90 ngày gần đây</option>
                  <option value="12m">12 tháng gần đây</option>
                </select>
              </label>
              <label className="flex min-h-13 items-center gap-2.5 px-5 text-sm font-medium text-[#7b8a87]">
                <Filter className="h-3.75 w-3.75" />
                <select
                  value={testTypeFilter}
                  onChange={(event) => setTestTypeFilter(event.target.value)}
                  className="w-full bg-transparent text-sm text-[#4e6360] outline-none"
                >
                  <option value="all">Tất cả loại xét nghiệm</option>
                  {availableTestTypes.map((testType) => (
                    <option key={testType} value={testType}>
                      {testType}
                    </option>
                  ))}
                </select>
              </label>
              <label className="flex min-h-13 items-center gap-2.5 px-5 text-sm font-medium text-[#7b8a87]">
                <Filter className="h-3.75 w-3.75" />
                <select
                  value={statusFilter}
                  onChange={(event) => setStatusFilter(event.target.value)}
                  className="w-full bg-transparent text-sm text-[#4e6360] outline-none"
                >
                  <option value="all">Tất cả trạng thái</option>
                  <option value="normal">Bình thường</option>
                  <option value="attention">Cần chú ý</option>
                  <option value="abnormal">Bất thường</option>
                  <option value="unverified">Chưa xác thực</option>
                  <option value="error">Lỗi</option>
                </select>
              </label>
            </div>

            <div className="overflow-hidden rounded-xl border border-[#e0ece9]">
              <div className="grid grid-cols-[1fr_1.6fr_1.4fr_1fr_0.8fr] bg-[#f1f6f5] px-4 py-3 text-xs font-bold uppercase tracking-wide text-[#6d7a77]">
                <span>Ngày xét nghiệm</span>
                <span>Loại xét nghiệm</span>
                <span>Cơ sở y tế</span>
                <span>Trạng thái</span>
                <span className="text-right">Hành động</span>
              </div>

              {filteredItems.length > 0 ? (
                filteredItems.map((item) => (
                  <div
                    key={item.id}
                    className="grid grid-cols-[1fr_1.6fr_1.4fr_1fr_0.8fr] items-center gap-2 border-t border-[#edf4f2] px-4 py-4 text-sm"
                  >
                    <span className="text-[#3d4947]">
                      {item.examDate || "Chưa có ngày khám"}
                    </span>
                    <div className="space-y-1">
                      <p className="font-semibold text-[#005049]">
                        {item.testType || "Phiếu khám bệnh"}
                      </p>
                      <p className="inline-flex items-center gap-1 text-xs text-[#6d7a77]">
                        <AlertTriangle className="h-3.5 w-3.5" />
                        Bất thường: {item.abnormalCount}
                      </p>
                    </div>
                    <span className="text-[#6d7a77]">
                      {item.hospitalName?.trim() || "Chưa cập nhật"}
                    </span>
                    <span
                      className={`w-fit rounded-full px-2.5 py-1 text-xs font-semibold ${statusStyle(resolveHistoryStatus(item))}`}
                    >
                      {statusLabel(resolveHistoryStatus(item))}
                    </span>
                    <div className="flex items-center justify-end gap-3">
                      {item.canDelete ? (
                        <button
                          type="button"
                          onClick={() => setDeleteTarget(item)}
                          className="inline-flex items-center rounded-md p-1.5 text-red-600 transition hover:bg-red-50"
                          aria-label="Xóa kết quả"
                          title="Xóa kết quả"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      ) : null}
                      <button
                        type="button"
                        onClick={() =>
                          router.push(`/health-records/review/${item.id}`)
                        }
                        className="inline-flex items-center rounded-md p-1.5 text-[#0c9f94] transition hover:bg-[#eaf9f5]"
                        aria-label="Xem chi tiết"
                        title="Xem chi tiết"
                      >
                        <Eye className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                ))
              ) : (
                <div className="border-t border-[#edf4f2] px-4 py-10 text-center text-sm text-[#6d7a77]">
                  Không có kết quả phù hợp với bộ lọc hiện tại.
                </div>
              )}
            </div>

            <div className="mt-4 flex flex-col gap-3 text-sm text-[#6d7a77] sm:flex-row sm:items-center sm:justify-between">
              <span>
                Đang hiển thị {filteredItems.length} trong số {allItems.length}{" "}
                kết quả đã tải
              </span>
              {hasNextPage ? (
                <button
                  type="button"
                  onClick={() => void fetchNextPage()}
                  disabled={isFetchingNextPage}
                  className="inline-flex items-center gap-2 rounded-lg border border-[#d7e5e1] px-3 py-1.5 font-medium text-[#245b55] transition hover:bg-[#f4faf8] disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {isFetchingNextPage ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Đang tải thêm
                    </>
                  ) : (
                    "Tải thêm"
                  )}
                </button>
              ) : (
                <span className="rounded-lg border border-[#d7e5e1] px-3 py-1.5 text-[#6d7a77]">
                  Đã tải tất cả kết quả
                </span>
              )}
            </div>
            {deleteError ? (
              <p className="mt-3 text-sm text-[#ba1a1a]">{deleteError}</p>
            ) : null}
          </article>

          <div className="grid grid-cols-1 gap-4 xl:grid-cols-[2fr_1fr]">
            <article className="relative overflow-hidden rounded-2xl border border-[#d5e7e3] bg-[#eaf9f5] p-7 shadow-sm">
              <span className="inline-flex items-center rounded-full bg-[#d0efe8] px-4 py-1.5 text-xs font-bold uppercase tracking-wide text-[#1b6c63]">
                Lời khuyên hôm nay
              </span>
              <h3 className="mt-5 text-[36px] leading-tight font-bold text-[#1f5750]">
                Duy trì theo dõi chỉ số đường huyết định kỳ
              </h3>
              <p className="mt-3 max-w-2xl text-[28px] leading-relaxed text-[#41716b]">
                Dựa trên kết quả gần nhất, chúng tôi khuyên bạn nên duy trì chế
                độ ăn ít tinh bột và tái khám sau 3 tháng.
              </p>
              <button
                type="button"
                className="mt-7 rounded-xl bg-[#0c9f94] px-6 py-3 text-base font-bold text-white shadow-sm transition hover:bg-[#0a8f85]"
              >
                Xem kế hoạch chi tiết
              </button>
              <div className="pointer-events-none absolute bottom-7 right-7 text-[#cbeee7]">
                <TrendingUp className="h-28 w-28" />
              </div>
            </article>

            <article className="rounded-2xl border border-[#e5edeb] bg-white p-6 text-center shadow-sm">
              <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-xl bg-[#eaf9f5] text-[#159b90]">
                <ShieldPlus className="h-8 w-8" />
              </div>
              <h3 className="text-2xl font-bold text-[#253c39]">
                Kết quả mới nhất?
              </h3>
              <p className="mt-4 text-lg leading-relaxed text-[#5e7471]">
                Bạn có thêm kết quả xét nghiệm từ bệnh viện khác? Hãy cập nhật
                để HealthLens phân tích toàn diện hơn.
              </p>
              <button
                type="button"
                className="mt-5 inline-flex items-center gap-2 text-base font-semibold text-[#0c9f94] hover:underline"
              >
                Tìm hiểu cách tải lên
                <ChevronRight className="h-4 w-4" />
              </button>
            </article>
          </div>

          <div
            ref={loadMoreRef}
            className="flex h-12 items-center justify-center"
          >
            {isFetchingNextPage ? (
              <Loader2 className="h-5 w-5 animate-spin text-[#00685f]" />
            ) : hasNextPage ? (
              <span className="text-sm text-[#6d7a77]">
                Kéo xuống để tải thêm
              </span>
            ) : null}
          </div>
        </section>
      )}
      <DeleteRecordModal
        open={Boolean(deleteTarget)}
        title="Xác nhận xóa kết quả?"
        description={
          deleteTarget
            ? `Bạn có chắc muốn xóa kết quả ${deleteTarget.testType || "Phiếu khám bệnh"}? Kết quả sẽ bị ẩn khỏi lịch sử ngay bây giờ và file ảnh/PDF gốc sẽ bị xóa vĩnh viễn sau 30 ngày.`
            : undefined
        }
        onCancel={() => {
          setDeleteError(null);
          setDeleteTarget(null);
        }}
        onConfirm={() => {
          if (!deleteTarget) return;
          deleteMutation.mutate(deleteTarget.id);
        }}
        isPending={deleteMutation.isPending}
      />
    </DashboardPageShell>
  );
}

function statusStyle(status: string): string {
  switch (status) {
    case "abnormal":
      return "bg-red-100 text-red-700";
    case "attention":
      return "bg-amber-100 text-amber-700";
    case "normal":
      return "bg-emerald-100 text-emerald-700";
    case "error":
    case "failed":
    case "ocr_failed":
      return "bg-rose-100 text-rose-700";
    case "unverified":
    case "pending":
    case "review_required":
      return "bg-slate-100 text-slate-700";
    default:
      return "bg-slate-100 text-slate-700";
  }
}

function statusLabel(status: string): string {
  switch (status) {
    case "abnormal":
      return "Bất thường";
    case "attention":
      return "Cần chú ý";
    case "normal":
      return "Bình thường";
    case "error":
    case "failed":
    case "ocr_failed":
      return "Lỗi";
    case "unverified":
    case "pending":
    case "review_required":
      return "Chưa xác thực";
    default:
      return "Chưa xác thực";
  }
}

function resolveHistoryStatus(item: HistoryItem): string {
  const fallbackStatus = item as unknown as {
    recordStatus?: string | null;
    verificationStatus?: string | null;
  };
  const recordStatus = (
    item.status ??
    fallbackStatus.recordStatus ??
    fallbackStatus.verificationStatus
  )?.toLowerCase();
  if (recordStatus === "done") {
    return item.overallStatus ?? "normal";
  }
  if (
    recordStatus === "review_required" ||
    recordStatus === "processing" ||
    recordStatus === "pending"
  ) {
    return "unverified";
  }
  if (
    recordStatus === "ocr_failed" ||
    recordStatus === "failed" ||
    recordStatus === "error"
  ) {
    return "error";
  }
  // Fallback for old API payloads that don't include record status.
  const looksLikeIncompleteRecord = !item.examDate;
  if (looksLikeIncompleteRecord) {
    return item.sourceType === "ocr_partial" ? "unverified" : "error";
  }
  return item.overallStatus ?? "unverified";
}
