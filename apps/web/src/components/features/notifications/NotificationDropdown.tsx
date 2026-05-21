"use client";

import Link from "next/link";
import { useEffect, useId } from "react";
import { Loader2 } from "lucide-react";

import type { NotificationInboxItem } from "@healthlens/shared/types";

import { NotificationInboxItemRow } from "./NotificationInboxItemRow";
import { NotificationInboxToolbar } from "./NotificationInboxToolbar";

const DROPDOWN_PREVIEW_LIMIT = 5;

type NotificationDropdownProps = {
  panelId: string;
  isOpen: boolean;
  onClose: () => void;
  items: NotificationInboxItem[];
  unreadCount: number;
  isLoading: boolean;
  isError: boolean;
  onRetry: () => void;
  onMarkRead: (itemId: string) => void;
  onMarkAllRead: () => void;
  isMarkingAllRead?: boolean;
};

export function NotificationDropdown({
  panelId,
  isOpen,
  onClose,
  items,
  unreadCount,
  isLoading,
  isError,
  onRetry,
  onMarkRead,
  onMarkAllRead,
  isMarkingAllRead = false,
}: NotificationDropdownProps) {
  const titleId = useId();

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) {
    return null;
  }

  const previewItems = items.slice(0, DROPDOWN_PREVIEW_LIMIT);

  return (
    <div
      id={panelId}
      role="dialog"
      aria-modal="false"
      aria-labelledby={titleId}
      className="animate-in fade-in zoom-in-95 absolute right-0 z-[60] mt-2 w-[min(360px,calc(100vw-24px))] origin-top-right overflow-hidden rounded-2xl border border-[#bcc9c6]/25 bg-white shadow-xl duration-100"
    >
      <div className="border-b border-[#bcc9c6]/20 px-4 py-3">
        <h2 id={titleId} className="text-sm font-black text-[#121e1c]">
          Thông báo
        </h2>
        {!isLoading && !isError && items.length > 0 ? (
          <NotificationInboxToolbar
            unreadCount={unreadCount}
            onMarkAllRead={onMarkAllRead}
            isMarkingAllRead={isMarkingAllRead}
            compact
          />
        ) : null}
      </div>

      <div className="max-h-[min(320px,50vh)] overflow-y-auto px-2 py-2">
        {isLoading ? (
          <div className="flex flex-col items-center gap-2 py-8 text-[#6d7a77]">
            <Loader2 className="h-6 w-6 animate-spin text-[#00685f]" aria-hidden="true" />
            <p className="text-xs font-medium">Đang tải thông báo...</p>
          </div>
        ) : null}

        {!isLoading && isError ? (
          <div className="px-2 py-4 text-center">
            <p className="text-xs font-medium text-[#ba1a1a]">Không thể tải thông báo.</p>
            <button
              type="button"
              onClick={onRetry}
              className="mt-3 rounded-xl bg-[#00685f] px-3 py-1.5 text-xs font-bold text-white"
            >
              Thử lại
            </button>
          </div>
        ) : null}

        {!isLoading && !isError && items.length === 0 ? (
          <p className="px-2 py-10 text-center text-sm font-medium text-[#6d7a77]">
            Không có thông báo
          </p>
        ) : null}

        {!isLoading && !isError && items.length > 0 && unreadCount === 0 ? (
          <p className="px-2 pb-2 text-center text-[10px] font-medium text-[#6d7a77]">
            Tất cả thông báo đã được xem
          </p>
        ) : null}

        {!isLoading && !isError && previewItems.length > 0 ? (
          <ul className="flex flex-col">
            {previewItems.map((item) => (
              <li key={item.id}>
                <NotificationInboxItemRow
                  item={item}
                  variant="compact"
                  onNavigate={onClose}
                  onMarkRead={onMarkRead}
                />
              </li>
            ))}
          </ul>
        ) : null}
      </div>

      <div className="border-t border-[#bcc9c6]/20 px-4 py-2.5">
        <Link
          href="/settings/notifications"
          onClick={onClose}
          className="block rounded-lg py-1.5 text-center text-xs font-bold text-[#00685f] transition-colors hover:bg-[#e9f6f3]"
        >
          Xem tất cả
        </Link>
      </div>
    </div>
  );
}
