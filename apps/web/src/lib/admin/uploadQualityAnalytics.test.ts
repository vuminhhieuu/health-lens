import { describe, expect, it } from "vitest";

import {
  getUploadQualityRangeError,
  isUploadQualityRangeValid,
  utcUploadQualityDefaultFrom,
  utcUploadQualityDefaultTo,
} from "@/lib/admin/uploadQualityAnalytics";
import { utcAnalyticsToday } from "@/lib/admin/userAnalytics";

describe("uploadQualityAnalytics", () => {
  it("default range spans 7 UTC calendar days", () => {
    const from = utcUploadQualityDefaultFrom();
    const to = utcUploadQualityDefaultTo();
    const fromDate = new Date(`${from}T00:00:00Z`);
    const toDate = new Date(`${to}T00:00:00Z`);
    const diffDays = Math.round((toDate.getTime() - fromDate.getTime()) / 86_400_000);
    expect(diffDays).toBe(6);
    expect(to).toBe(utcAnalyticsToday());
  });

  it("rejects future end date in UTC", () => {
    const today = utcAnalyticsToday();
    const future = new Date(`${today}T00:00:00Z`);
    future.setUTCDate(future.getUTCDate() + 3);
    const futureIso = future.toISOString().slice(0, 10);
    expect(isUploadQualityRangeValid(today, futureIso)).toBe(false);
    expect(getUploadQualityRangeError(today, futureIso)).toMatch(/tương lai/i);
  });

  it("accepts valid inclusive UTC range", () => {
    const to = utcAnalyticsToday();
    const fromDate = new Date(`${to}T00:00:00Z`);
    fromDate.setUTCDate(fromDate.getUTCDate() - 14);
    const from = fromDate.toISOString().slice(0, 10);
    expect(isUploadQualityRangeValid(from, to)).toBe(true);
    expect(getUploadQualityRangeError(from, to)).toBeNull();
  });
});
