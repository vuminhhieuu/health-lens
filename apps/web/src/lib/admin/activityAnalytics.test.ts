import { describe, expect, it, vi } from "vitest";

vi.mock("@/lib/admin/userAnalytics", () => ({
  utcAnalyticsToday: () => "2026-05-20",
}));

import {
  countUtcDaysInclusive,
  getActivityRangeError,
  isActivityRangeValid,
  utcActivityDefaultFrom,
  utcActivityDefaultTo,
} from "@/lib/admin/activityAnalytics";

describe("activityAnalytics", () => {
  it("returns UTC defaults aligned with backend window", () => {
    expect(utcActivityDefaultTo()).toBe("2026-05-20");
    expect(utcActivityDefaultFrom()).toBe("2026-05-14");
  });

  it("counts UTC days inclusive and handles invalid ranges", () => {
    expect(countUtcDaysInclusive("2026-05-14", "2026-05-20")).toBe(7);
    expect(countUtcDaysInclusive("2026-05-20", "2026-05-14")).toBe(0);
    expect(countUtcDaysInclusive("invalid", "2026-05-20")).toBe(0);
  });

  it("validates range with matching API constraints", () => {
    expect(getActivityRangeError("", "2026-05-20")).toContain("Chọn đầy đủ");
    expect(getActivityRangeError("2026-05-21", "2026-05-21")).toContain("tương lai");
    expect(getActivityRangeError("2026-05-20", "2026-05-19")).toContain("bắt đầu");
    expect(getActivityRangeError("2026-01-01", "2026-05-20")).toContain("90 ngày");
    expect(getActivityRangeError("2026-05-14", "2026-05-20")).toBeNull();
    expect(isActivityRangeValid("2026-05-14", "2026-05-20")).toBe(true);
    expect(isActivityRangeValid("2026-01-01", "2026-05-20")).toBe(false);
  });
});
