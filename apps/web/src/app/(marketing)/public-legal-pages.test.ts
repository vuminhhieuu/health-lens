import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";

import { describe, expect, it } from "vitest";

import { PUBLIC_SITEMAP_PATHS } from "@/lib/seo/site";
import { privacyViContent } from "@/content/legal/privacy-vi";
import { termsViContent } from "@/content/legal/terms-vi";

const appDir = join(process.cwd(), "src/app");
const marketingDir = join(process.cwd(), "src/app/(marketing)");
const dashboardLayout = join(process.cwd(), "src/app/(dashboard)/layout.tsx");

describe("public legal pages (pae-5)", () => {
  it("exposes /privacy and /terms under marketing without dashboard auth gate", () => {
    expect(existsSync(join(marketingDir, "privacy/page.tsx"))).toBe(true);
    expect(existsSync(join(marketingDir, "terms/page.tsx"))).toBe(true);

    const privacyPage = readFileSync(join(marketingDir, "privacy/page.tsx"), "utf8");
    const termsPage = readFileSync(join(marketingDir, "terms/page.tsx"), "utf8");

    expect(privacyPage).toContain("LegalPage");
    expect(termsPage).toContain("LegalPage");
    expect(privacyPage).not.toContain("(dashboard)");
    expect(termsPage).not.toContain("(dashboard)");

    const dashboard = readFileSync(dashboardLayout, "utf8");
    expect(dashboard).toContain("useAuthBootstrap");
    expect(privacyPage).not.toContain("useAuthBootstrap");
    expect(termsPage).not.toContain("useAuthBootstrap");
  });

  it("maps /privacy and /terms to routable marketing pages (HTTP 200 smoke via seo-foundation + dev server)", () => {
    const routeFiles: Record<string, string> = {
      "/privacy": "(marketing)/privacy/page.tsx",
      "/terms": "(marketing)/terms/page.tsx",
    };

    for (const path of ["/privacy", "/terms"] as const) {
      expect(PUBLIC_SITEMAP_PATHS).toContain(path);
      expect(existsSync(join(appDir, routeFiles[path])), `missing routable page for ${path}`).toBe(
        true,
      );
    }

    const smokeScript = join(process.cwd(), "scripts/smoke-sitemap-urls.mjs");
    expect(existsSync(smokeScript)).toBe(true);
  });

  it("includes draft banner and NĐ 13/2023 aligned sections", () => {
    expect(privacyViContent.showDraftBanner).toBe(true);
    expect(termsViContent.showDraftBanner).toBe(true);

    const privacyText = JSON.stringify(privacyViContent);
    expect(privacyText).toContain("Nghị định 13/2023");
    expect(privacyText).toContain("AES-256");
    expect(privacyText).toContain("OCR");
    expect(privacyText).toContain("không chia sẻ");

    const termsText = JSON.stringify(termsViContent);
    expect(termsText).toContain("OCR");
    expect(termsText).toContain("tham khảo");
    expect(termsText).toContain("bác sĩ");
  });
});
