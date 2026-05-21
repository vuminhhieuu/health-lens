"use client";

import Link from "next/link";
import { formatDistanceToNow } from "date-fns";
import { vi } from "date-fns/locale";
import { ChevronRight, Mail } from "lucide-react";

import type { NotificationInboxItem } from "@healthlens/shared/types";

import { notificationDestination } from "./notificationDestination";

export type NotificationInboxItemRowVariant = "compact" | "full";

type NotificationInboxItemRowProps = {
  item: NotificationInboxItem;
  variant?: NotificationInboxItemRowVariant;
  onNavigate?: () => void;
  onMarkRead?: (itemId: string) => void;
};

function formatRelativeTime(createdAt: string) {
  const parsed = new Date(createdAt);
  if (Number.isNaN(parsed.getTime())) {
    return "";
  }
  return formatDistanceToNow(parsed, { addSuffix: true, locale: vi });
}

export function NotificationInboxItemRow({
  item,
  variant = "compact",
  onNavigate,
  onMarkRead,
}: NotificationInboxItemRowProps) {
  const href = notificationDestination(item);
  const relativeTime = formatRelativeTime(item.createdAt);
  const isUnread = !item.read;
  const marksReadOnDestination = href.includes("inboxRead=");

  const handleNavigate = () => {
    if (isUnread && onMarkRead && !marksReadOnDestination) {
      onMarkRead(item.id);
    }
    onNavigate?.();
  };

  if (variant === "full") {
    return (
      <Link
        href={href}
        onClick={handleNavigate}
        className={`flex flex-col gap-4 rounded-3xl border p-5 transition-colors sm:flex-row sm:items-center sm:justify-between ${
          isUnread
            ? "border-[#00685f]/20 bg-[#e9f6f3]/80 hover:border-[#00685f]/35 hover:bg-[#e9f6f3]"
            : "border-[#bcc9c6]/30 bg-white hover:bg-[#f6fbfa]"
        }`}
      >
        <div className="flex min-w-0 gap-4">
          <div
            className={`relative flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl shadow-sm ${
              isUnread ? "bg-white text-[#00685f]" : "bg-[#f0f5f4] text-[#6d7a77]"
            }`}
          >
            <Mail className="h-6 w-6" aria-hidden="true" />
            {isUnread ? (
              <span
                className="absolute -right-0.5 -top-0.5 h-2.5 w-2.5 rounded-full bg-[#00685f] ring-2 ring-white"
                aria-hidden="true"
              />
            ) : null}
          </div>
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <p
                className={
                  isUnread ? "font-black text-[#121e1c]" : "font-semibold text-[#6d7a77]"
                }
              >
                {item.title}
              </p>
              {isUnread ? (
                <span className="rounded-full bg-[#00685f]/10 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-[#00685f]">
                  Chưa đọc
                </span>
              ) : (
                <span className="text-[10px] font-bold uppercase tracking-wide text-[#6d7a77]">
                  Đã đọc
                </span>
              )}
            </div>
            <p
              className={`mt-1 text-sm ${isUnread ? "font-medium text-[#3d4947]" : "text-[#6d7a77]"}`}
            >
              {item.body}
            </p>
            {relativeTime ? (
              <p className="mt-1 text-xs text-[#6d7a77]">{relativeTime}</p>
            ) : null}
          </div>
        </div>
        <span
          className={`inline-flex shrink-0 items-center justify-center gap-1 self-start rounded-2xl px-5 py-2.5 text-sm font-bold sm:self-center ${
            isUnread
              ? "bg-[#008378] text-white"
              : "border border-[#bcc9c6]/40 bg-white text-[#6d7a77]"
          }`}
        >
          Xem
          <ChevronRight className="h-4 w-4" aria-hidden="true" />
        </span>
      </Link>
    );
  }

  return (
    <Link
      href={href}
      onClick={handleNavigate}
      className={`flex gap-3 rounded-xl px-3 py-2.5 transition-colors ${
        isUnread ? "hover:bg-[#e9f6f3]" : "opacity-80 hover:bg-[#f6fbfa]"
      }`}
    >
      <div
        className={`relative flex h-9 w-9 shrink-0 items-center justify-center rounded-xl ${
          isUnread ? "bg-[#e9f6f3] text-[#00685f]" : "bg-[#f0f5f4] text-[#6d7a77]"
        }`}
      >
        <Mail className="h-4 w-4" aria-hidden="true" />
        {isUnread ? (
          <span
            className="absolute -right-0.5 -top-0.5 h-2 w-2 rounded-full bg-[#00685f] ring-2 ring-white"
            aria-hidden="true"
          />
        ) : null}
      </div>
      <div className="min-w-0 flex-1">
        <p
          className={`truncate text-sm ${isUnread ? "font-bold text-[#121e1c]" : "font-medium text-[#6d7a77]"}`}
        >
          {item.title}
        </p>
        <p className={`truncate text-xs ${isUnread ? "text-[#3d4947]" : "text-[#6d7a77]"}`}>
          {item.body}
        </p>
        {relativeTime ? (
          <p className="mt-0.5 text-[10px] font-medium text-[#6d7a77]">{relativeTime}</p>
        ) : null}
      </div>
      <ChevronRight className="mt-1 h-4 w-4 shrink-0 text-[#6d7a77]" aria-hidden="true" />
    </Link>
  );
}
