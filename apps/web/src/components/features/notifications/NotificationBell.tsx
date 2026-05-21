"use client";

import { useEffect, useId, useRef, useState } from "react";
import { Bell } from "lucide-react";

import { useNotificationInbox } from "@/hooks/useNotificationInbox";

import { NotificationDropdown } from "./NotificationDropdown";

export function NotificationBell() {
  const panelId = useId();
  const [isOpen, setIsOpen] = useState(false);
  const bellRef = useRef<HTMLButtonElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const {
    items,
    unreadCount,
    isLoading,
    isError,
    refetch,
    markAsRead,
    markAllAsRead,
    isMarkingAllRead,
  } = useNotificationInbox();

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    function handleClickOutside(event: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isOpen]);

  const togglePanel = () => {
    setIsOpen((value) => !value);
  };

  const closePanel = () => {
    setIsOpen(false);
    bellRef.current?.focus();
  };

  const badgeLabel =
    unreadCount > 0
      ? `${unreadCount} thông báo chưa đọc`
      : "Không có thông báo chưa đọc";

  return (
    <div className="relative" ref={containerRef}>
      <button
        ref={bellRef}
        type="button"
        title="Mở thông báo"
        aria-label="Mở thông báo"
        aria-expanded={isOpen}
        aria-haspopup="dialog"
        aria-controls={panelId}
        onClick={togglePanel}
        className="relative rounded-full p-2 text-[#3d4947] transition-colors hover:bg-[#e9f6f3] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 ? (
          <span
            className="absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-[#924628] px-1 text-[10px] font-black leading-none text-white"
            aria-hidden="true"
          >
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        ) : null}
        <span className="sr-only">{badgeLabel}</span>
      </button>

      <NotificationDropdown
        panelId={panelId}
        isOpen={isOpen}
        onClose={closePanel}
        items={items}
        unreadCount={unreadCount}
        isLoading={isLoading}
        isError={isError}
        onRetry={() => {
          void refetch();
        }}
        onMarkRead={markAsRead}
        onMarkAllRead={markAllAsRead}
        isMarkingAllRead={isMarkingAllRead}
      />
    </div>
  );
}
