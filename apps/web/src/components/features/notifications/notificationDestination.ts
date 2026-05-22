import type {
  NotificationInboxItem,
  NotificationInboxItemType,
} from "@healthlens/shared/types";

function hubPath(base: string, itemId?: string) {
  if (!itemId) {
    return base;
  }
  return `${base}?inboxRead=${encodeURIComponent(itemId)}`;
}

/** In-app inbox taps land on hub pages; read state is applied via `inboxRead` query. */
export function notificationDestination(
  itemOrType: NotificationInboxItem | NotificationInboxItemType,
): string {
  const type = typeof itemOrType === "string" ? itemOrType : itemOrType.type;
  const itemId = typeof itemOrType === "string" ? undefined : itemOrType.id;

  switch (type) {
    case "PROFILE_INVITATION":
      return hubPath("/profiles", itemId);
    case "HEALTH_RECORD_INVITATION":
      return hubPath("/health-records", itemId);
    case "REMINDER_UPCOMING":
      if (typeof itemOrType !== "string" && itemOrType.actionUrl) {
        return itemOrType.actionUrl;
      }
      return "/follow-up-reminders";
    default:
      return "/settings/notifications";
  }
}
