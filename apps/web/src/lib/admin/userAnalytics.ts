/** Khớp `AnalyticsService.MAX_USER_ANALYTICS_MONTHS` (UTC). */
export const MAX_USER_ANALYTICS_MONTHS = 24;

export function utcAnalyticsDefaultFrom(): string {
  const now = new Date();
  return new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() - 5, 1))
    .toISOString()
    .slice(0, 10);
}

/** Ngày hiện tại theo lịch UTC (`yyyy-MM-dd`). */
export function utcAnalyticsToday(): string {
  const now = new Date();
  return new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()))
    .toISOString()
    .slice(0, 10);
}

export function utcAnalyticsDefaultTo(): string {
  return utcAnalyticsToday();
}

export function countUtcMonthsInclusive(fromIso: string, toIso: string): number {
  const from = new Date(`${fromIso}T00:00:00Z`);
  const to = new Date(`${toIso}T00:00:00Z`);
  if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime()) || from > to) {
    return 0;
  }
  return (
    (to.getUTCFullYear() - from.getUTCFullYear()) * 12 +
    (to.getUTCMonth() - from.getUTCMonth()) +
    1
  );
}

export function getUserAnalyticsRangeError(from: string, to: string): string | null {
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
  if (countUtcMonthsInclusive(from, to) > MAX_USER_ANALYTICS_MONTHS) {
    return `Chọn tối đa ${MAX_USER_ANALYTICS_MONTHS} tháng để xem biểu đồ.`;
  }
  return null;
}

export function isUserAnalyticsRangeValid(from: string, to: string): boolean {
  return getUserAnalyticsRangeError(from, to) === null;
}
