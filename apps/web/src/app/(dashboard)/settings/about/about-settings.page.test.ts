import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";

import { describe, expect, it } from "vitest";

describe("about settings page", () => {
  const pagePath = join(process.cwd(), "src/app/(dashboard)/settings/about/page.tsx");

  it("exists and shows version from appVersion helpers", () => {
    expect(existsSync(pagePath)).toBe(true);
    const source = readFileSync(pagePath, "utf8");

    expect(source).toContain("DashboardPageShell");
    expect(source).toContain("getAppVersionLabel");
    expect(source).toContain("APP_DISPLAY_NAME");
    expect(source).not.toContain("SettingsComingSoonPage");
    expect(source).toContain('active="about"');
  });

  it("links to public privacy, terms, and help support route", () => {
    const source = readFileSync(pagePath, "utf8");

    expect(source).toContain('href="/privacy"');
    expect(source).toContain('href="/terms"');
    expect(source).toContain("PUBLIC_SUPPORT_HREF");
    expect(source).toContain("openInNewTab");
    expect(source).toContain('target="_blank"');
    expect(source).toContain("SettingsDirectContactCard");
  });
});
