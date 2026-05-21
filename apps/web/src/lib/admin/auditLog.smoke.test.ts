import { describe, expect, it } from "vitest";

import {
  auditHasActiveFilters,
  buildAuditLogUrl,
  buildTraceOnlyFilters,
  resolveInitialAuditState,
} from "./auditLog";

/** Story 6.4 UX smoke — URL/bookmark helpers (automated proxy for manual checklist #2). */
describe("auditLog URL smoke (6.4)", () => {
  it("reference scope preserves filter query for bookmark", () => {
    const params = new URLSearchParams(
      "actorEmail=test%40x.com&from=2026-05-01&to=2026-05-10&action=LOGIN",
    );
    const { viewScope, filters } = resolveInitialAuditState(params);
    expect(viewScope).toBe("reference");
    expect(filters.actorEmail).toBe("test@x.com");
    expect(buildAuditLogUrl(viewScope, filters)).toContain("actorEmail=test%40x.com");
    expect(buildAuditLogUrl(viewScope, filters)).toContain("from=2026-05-01");
  });

  it("all scope adds view=all", () => {
    const params = new URLSearchParams("view=all&correlationId=corr-abc");
    const { viewScope, filters } = resolveInitialAuditState(params);
    expect(viewScope).toBe("all");
    expect(filters.correlationId).toBe("corr-abc");
    expect(buildAuditLogUrl(viewScope, filters)).toContain("view=all");
    expect(buildAuditLogUrl(viewScope, filters)).toContain("correlationId=corr-abc");
  });

  it("trace-only filters clear other fields", () => {
    const trace = buildTraceOnlyFilters("  corr-1  ");
    expect(trace.correlationId).toBe("corr-1");
    expect(trace.actorEmail).toBe("");
    expect(auditHasActiveFilters(trace, "all")).toBe(true);
  });

  it("active filters include correlationId", () => {
    expect(
      auditHasActiveFilters(
        {
          resourceType: "",
          resourceId: "",
          actorEmail: "",
          action: "",
          correlationId: "x",
          from: "",
          to: "",
        },
        "all",
      ),
    ).toBe(true);
  });
});
