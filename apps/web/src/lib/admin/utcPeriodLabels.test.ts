import { describe, expect, it } from "vitest";

import {
  formatUtcIsoWeekAxisLabel,
  formatUtcWeekBucketTooltip,
  utcIsoWeekNumber,
} from "@/lib/admin/utcPeriodLabels";

describe("utcPeriodLabels", () => {
  it("uses ISO week numbers for UTC Monday periodStart (not locale week-of-year)", () => {
    expect(utcIsoWeekNumber("2026-03-02")).toBe(10);
    expect(formatUtcIsoWeekAxisLabel("2026-03-02")).toBe("T10");
  });

  it("handles ISO week-year boundary Mondays", () => {
    expect(utcIsoWeekNumber("2025-12-29")).toBe(1);
    expect(formatUtcIsoWeekAxisLabel("2025-12-29")).toBe("T1");
  });

  it("formats week tooltip from UTC period start", () => {
    expect(formatUtcWeekBucketTooltip("2026-03-02")).toBe(
      "Tuần bắt đầu 02/03/2026 (UTC)",
    );
  });
});
