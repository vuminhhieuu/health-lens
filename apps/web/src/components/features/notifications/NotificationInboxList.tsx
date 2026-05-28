"use client";

import { ChevronLeft, ChevronRight, Loader2 } from "lucide-react";

import type { NotificationInboxItem, PaginationMeta } from "@healthlens/shared/types";

import {
  NotificationInboxItemRow,
  type NotificationInboxItemRowVariant,
} from "./NotificationInboxItemRow";
import { NotificationInboxToolbar } from "./NotificationInboxToolbar";

type NotificationInboxListProps = {
  items: NotificationInboxItem[];
  isLoading: boolean;
  isError: boolean;
  onRetry: () => void;
  unreadCount: number;
  onMarkRead?: (itemId: string) => void;
  onMarkAllRead?: () => void;
  isMarkingAllRead?: boolean;
  onItemNavigate?: () => void;
  emptyMessage?: string;
  variant?: NotificationInboxItemRowVariant;
  showToolbar?: boolean;
  pagination?: PaginationMeta;
  onPageChange?: (page: number) => void;
  onPageSizeChange?: (limit: number) => void;
  isPaginationDisabled?: boolean;
};

const PAGE_SIZE_OPTIONS = [5, 10, 15, 20] as const;

export function NotificationInboxList({
  items,
  isLoading,
  isError,
  onRetry,
  unreadCount,
  onMarkRead,
  onMarkAllRead,
  isMarkingAllRead = false,
  onItemNavigate,
  emptyMessage = "Không có thông báo",
  variant = "full",
  showToolbar = true,
  pagination,
  onPageChange,
  onPageSizeChange,
  isPaginationDisabled = false,
}: NotificationInboxListProps) {
  const compact = variant === "compact";
  const showPagination = variant === "full" && pagination && onPageChange && pagination.total > 0;
  const isFirstPage = pagination ? pagination.page <= 0 : true;
  const isLastPage = pagination ? pagination.page + 1 >= pagination.totalPages : true;
  const hasItems = items.length > 0;
  const rangeStart = pagination && hasItems ? pagination.page * pagination.limit + 1 : 0;
  const rangeEnd = pagination && hasItems ? Math.min(pagination.total, rangeStart + items.length - 1) : 0;

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center gap-3 py-16 text-[#6d7a77]">
        <Loader2 className="h-8 w-8 animate-spin text-[#00685f]" aria-hidden="true" />
        <p className="text-sm font-medium">Đang tải thông báo...</p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6]/30 px-4 py-6 text-center">
        <p className="text-sm font-medium text-[#ba1a1a]">Không thể tải thông báo.</p>
        <button
          type="button"
          onClick={onRetry}
          className="mt-4 rounded-xl bg-[#00685f] px-4 py-2 text-sm font-bold text-white"
        >
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <div>
      {showToolbar && onMarkAllRead ? (
        <NotificationInboxToolbar
          unreadCount={unreadCount}
          onMarkAllRead={onMarkAllRead}
          isMarkingAllRead={isMarkingAllRead}
          compact={compact}
        />
      ) : null}

      {hasItems ? (
        <ul className="flex flex-col gap-3">
          {items.map((item) => (
            <li key={item.id}>
              <NotificationInboxItemRow
                item={item}
                variant={variant}
                onNavigate={onItemNavigate}
                onMarkRead={onMarkRead}
              />
            </li>
          ))}
        </ul>
      ) : (
        <p className="py-16 text-center text-sm font-medium text-[#6d7a77]">{emptyMessage}</p>
      )}

      {showPagination ? (
        <div className="mt-4 flex flex-col items-center justify-end gap-4 border-t border-[#e9f6f3] pt-4 text-sm font-medium text-[#3d4947] sm:flex-row sm:gap-8">
          <div className="flex items-center gap-2">
            <label htmlFor="notification-page-size">Số dòng:</label>
            <select
              id="notification-page-size"
              value={pagination.limit}
              onChange={(event) => onPageSizeChange?.(Number(event.target.value))}
              disabled={isPaginationDisabled || !onPageSizeChange}
              className="rounded-lg border border-transparent bg-transparent px-2 py-1 font-semibold text-[#121e1c] transition hover:bg-[#e9f6f3] focus:border-[#00685f] focus:outline-none disabled:opacity-60"
              aria-label="Chọn số dòng mỗi trang"
            >
              {PAGE_SIZE_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </div>
          <span className="min-w-32 text-center font-semibold text-[#121e1c]">
            {hasItems
              ? `${rangeStart.toLocaleString("vi-VN")}-${rangeEnd.toLocaleString("vi-VN")}`
              : "0"}{" "}
            trên{" "}
            {pagination.total.toLocaleString("vi-VN")} thông báo
          </span>
          <div className="flex items-center gap-3">
            <button
              type="button"
              disabled={isPaginationDisabled || isFirstPage}
              onClick={() => onPageChange(Math.max(0, pagination.page - 1))}
              className="inline-flex h-9 w-9 items-center justify-center rounded-full text-[#3d4947] transition hover:bg-[#deebe8] disabled:text-[#9aa5a2] disabled:opacity-60"
              aria-label="Trang trước"
              title="Trang trước"
            >
              <ChevronLeft className="h-6 w-6" aria-hidden="true" />
            </button>
            <button
              type="button"
              disabled={isPaginationDisabled || isLastPage}
              onClick={() => onPageChange(pagination.page + 1)}
              className="inline-flex h-9 w-9 items-center justify-center rounded-full text-[#3d4947] transition hover:bg-[#deebe8] disabled:text-[#9aa5a2] disabled:opacity-60"
              aria-label="Trang sau"
              title="Trang sau"
            >
              <ChevronRight className="h-6 w-6" aria-hidden="true" />
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
