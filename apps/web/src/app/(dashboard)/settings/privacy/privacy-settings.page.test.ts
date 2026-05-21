import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";

import { describe, expect, it } from "vitest";

describe("privacy settings page", () => {
  const pagePath = join(
    process.cwd(),
    "src/app/(dashboard)/settings/privacy/page.tsx",
  );

  it("exists and wires consent status API read-only", () => {
    expect(existsSync(pagePath)).toBe(true);
    const source = readFileSync(pagePath, "utf8");

    expect(source).toContain("DashboardPageShell");
    expect(source).toContain('label: "Riêng tư"');
    expect(source).toContain("API_ROUTES.CONSENT.ME");
    expect(source).not.toContain("apiClient.post");
    expect(source).not.toContain("ConsentModal");
    expect(source).toContain("useAuthStore");
    expect(source).toContain("ConsentMetricTile");
    expect(source).toContain("Đang hiệu lực");
    expect(source).toContain("SettingsAccountNav");
    expect(source).toContain('active="privacy"');
    expect(source).toContain("lg:grid-cols-3");
    expect(source).toContain("settingsCardClassName");
  });

  it("opens legal pages in a new tab and links delete-account prominently", () => {
    const source = readFileSync(pagePath, "utf8");

    expect(source).toContain('href="/privacy"');
    expect(source).toContain('href="/terms"');
    expect(source).toContain('target="_blank"');
    expect(source).toContain('rel="noopener noreferrer"');
    expect(source).toContain('href="/settings/delete-account"');
    expect(source).toContain("(mở trong tab mới)");
    expect(source).toContain("Thử lại");
  });
});
