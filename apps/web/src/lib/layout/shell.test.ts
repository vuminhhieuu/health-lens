import { describe, expect, it } from "vitest";

import { marketingShell } from "@/lib/marketing/layout";
import {
  authenticatedContentShell,
  authenticatedHeaderInnerClassName,
  authenticatedHeaderSurfaceClassName,
} from "@/lib/layout/shell";

describe("authenticated layout shell (/home standard)", () => {
  it("uses px-6 on header surface and max-w-7xl content shell", () => {
    expect(authenticatedHeaderSurfaceClassName).toContain("px-6");
    expect(authenticatedContentShell).toContain("max-w-7xl");
    expect(authenticatedContentShell).toContain("px-6");
    expect(authenticatedHeaderInnerClassName).toContain("justify-between");
  });

  it("aliases marketingShell to the same content shell as /home", () => {
    expect(marketingShell).toBe(authenticatedContentShell);
  });
});
