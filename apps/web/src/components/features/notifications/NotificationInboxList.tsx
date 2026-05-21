"use client";

import { Loader2 } from "lucide-react";

import type { NotificationInboxItem } from "@healthlens/shared/types";

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
};

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
}: NotificationInboxListProps) {
  const compact = variant === "compact";

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

  if (items.length === 0) {
    return (
      <p className="py-16 text-center text-sm font-medium text-[#6d7a77]">{emptyMessage}</p>
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
    </div>
  );
}
