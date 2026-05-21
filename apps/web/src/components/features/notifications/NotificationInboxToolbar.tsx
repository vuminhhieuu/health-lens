"use client";

type NotificationInboxToolbarProps = {
  unreadCount: number;
  onMarkAllRead: () => void;
  isMarkingAllRead?: boolean;
  compact?: boolean;
};

export function NotificationInboxToolbar({
  unreadCount,
  onMarkAllRead,
  isMarkingAllRead = false,
  compact = false,
}: NotificationInboxToolbarProps) {
  if (unreadCount === 0) {
    return null;
  }

  return (
    <div
      className={`flex items-center justify-between gap-3 ${compact ? "px-1 pb-2" : "mb-4"}`}
    >
      <p
        className={`font-medium text-[#6d7a77] ${compact ? "text-[10px]" : "text-xs"}`}
      >
        {unreadCount} chưa đọc
      </p>
      <button
        type="button"
        onClick={onMarkAllRead}
        disabled={isMarkingAllRead}
        className={`font-bold text-[#00685f] transition-colors hover:text-[#005049] disabled:opacity-60 ${
          compact ? "text-[10px]" : "text-xs"
        }`}
      >
        Đánh dấu tất cả đã đọc
      </button>
    </div>
  );
}
