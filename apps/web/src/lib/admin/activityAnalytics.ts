import { utcAnalyticsToday } from "@/lib/admin/userAnalytics";

/** Khớp `AnalyticsService.MAX_ACTIVITY_DAYS` (UTC). */
export const MAX_ACTIVITY_DAYS = 90;

/** Mặc định 7 ngày lịch UTC (khớp `AnalyticsService.defaultActivityFrom`). */
export function utcActivityDefaultFrom(): string {
  const today = utcAnalyticsToday();
  const anchor = new Date(`${today}T00:00:00Z`);
  anchor.setUTCDate(anchor.getUTCDate() - 6);
  return anchor.toISOString().slice(0, 10);
}

export function utcActivityDefaultTo(): string {
  return utcAnalyticsToday();
}

/** Số ngày lịch UTC inclusive (from → to), khớp `validateActivityRange` trên API. */
export function countUtcDaysInclusive(fromIso: string, toIso: string): number {
  const from = new Date(`${fromIso}T00:00:00Z`);
  const to = new Date(`${toIso}T00:00:00Z`);
  if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime()) || from > to) {
    return 0;
  }
  return Math.floor((to.getTime() - from.getTime()) / 86_400_000) + 1;
}

export function getActivityRangeError(from: string, to: string): string | null {
  const today = utcAnalyticsToday();
  if (!from || !to) {
    return "Chọn đầy đủ ngày bắt đầu và kết thúc.";
  }
  if (from > today || to > today) {
    return "Không được chọn ngày trong tương lai (UTC).";
  }
  if (from > to) {
    return "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.";
  }
  if (countUtcDaysInclusive(from, to) > MAX_ACTIVITY_DAYS) {
    return `Khoảng thời gian tối đa là ${MAX_ACTIVITY_DAYS} ngày.`;
  }
  return null;
}

export function isActivityRangeValid(from: string, to: string): boolean {
  return getActivityRangeError(from, to) === null;
}
