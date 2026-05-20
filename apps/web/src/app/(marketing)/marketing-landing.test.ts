import { readFileSync } from "node:fs";
import { join } from "node:path";

import { describe, expect, it } from "vitest";

const marketingDir = join(process.cwd(), "src/app/(marketing)");
const componentsDir = join(process.cwd(), "src/components/marketing");
const source = (dir: string, file: string) => readFileSync(join(dir, file), "utf8");

describe("public landing page (pae-3)", () => {
  it("defines required Vietnamese sections without fake patient metrics", () => {
    const marketingHome = source(marketingDir, "MarketingHome.tsx");

    expect(marketingHome).toContain("Một nơi an toàn để lưu trữ");
    expect(marketingHome).toContain("Cách HealthLens hoạt động");
    expect(marketingHome).toContain("Tin cậy và tuân thủ");
    expect(marketingHome).toContain("NĐ 13/2023/NĐ-CP");
    expect(marketingHome).toContain("MEDICAL_RECOMMENDATIONS_DISCLAIMER");
    expect(marketingHome).toContain("Điểm nổi bật");
    expect(marketingHome).toContain("LandingDashboardPreview");
    expect(marketingHome).toContain("MarketingSection");
    expect(marketingHome).toContain("marketingBand.mint");
    expect(marketingHome).toContain("marketingBand.white");
    expect(marketingHome).toContain("marketingBand.mintDeep");
    expect(marketingHome).not.toContain("LandingGuestClosingBand");
    expect(marketingHome).toContain("Theo dõi kết quả khám, dễ hiểu hơn");
    expect(marketingHome).not.toMatch(/HealthLens\s*—/);
  });

  it("uses dashboard-style preview and single hero CTA pattern", () => {
    const preview = source(componentsDir, "LandingDashboardPreview.tsx");
    const guestCtas = source(componentsDir, "LandingGuestCtas.tsx");

    expect(preview).toContain("Chào mừng quay lại!");
    expect(preview).toContain("Kết quả gần đây");
    expect(preview).toContain("không phải dữ liệu bệnh nhân thật");
    expect(preview).not.toMatch(/\d{2,3}\s*(mg\/dL|mmol|bpm|%)/i);

    expect(guestCtas).toContain("Bắt đầu miễn phí");
    expect(guestCtas).toContain("Đã có tài khoản?");
    expect(guestCtas).toContain('href="/register"');
    expect(guestCtas).toContain('href="/login"');
    expect(guestCtas).not.toContain("Đăng ký");
  });

  it("exposes support route and footer legal links", () => {
    const supportPage = source(marketingDir, "support/page.tsx");
    const footer = source(componentsDir, "MarketingFooter.tsx");

    expect(supportPage).toContain('permanentRedirect("/help")');
    expect(supportPage).toContain("export const metadata");
    expect(footer).toContain('href: "/privacy"');
    expect(footer).toContain('href: "/terms"');
    expect(footer).toContain('href: "/support"');
  });
});
