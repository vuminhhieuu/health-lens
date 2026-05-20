import { utcAnalyticsToday } from "@/lib/admin/userAnalytics";

/** Mặc định 7 ngày lịch UTC cho chất lượng upload. */
export function utcUploadQualityDefaultFrom(): string {
  const today = utcAnalyticsToday();
  const anchor = new Date(`${today}T00:00:00Z`);
  anchor.setUTCDate(anchor.getUTCDate() - 6);
  return anchor.toISOString().slice(0, 10);
}

export function utcUploadQualityDefaultTo(): string {
  return utcAnalyticsToday();
}

export function getUploadQualityRangeError(from: string, to: string): string | null {
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
  return null;
}

export function isUploadQualityRangeValid(from: string, to: string): boolean {
  return getUploadQualityRangeError(from, to) === null;
}
