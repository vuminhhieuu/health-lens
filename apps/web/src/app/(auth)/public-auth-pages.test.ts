import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";

import { describe, expect, it } from "vitest";

import { authPublicFooterHrefs } from "@/lib/authPublicLinks";

const authDir = join(process.cwd(), "src/app/(auth)");
const marketingDir = join(process.cwd(), "src/app/(marketing)");
const componentsDir = join(process.cwd(), "src/components/auth");

const source = (dir: string, file: string) => readFileSync(join(dir, file), "utf8");

const publicAuthPages = [
  "login/page.tsx",
  "register/page.tsx",
  "forgot-password/page.tsx",
  "reset-password/page.tsx",
] as const;

describe("public auth pages link consistency (story 2-6)", () => {
  it("exposes public legal/help routes for footer targets", () => {
    for (const href of authPublicFooterHrefs()) {
      const pagePath =
        href === "/privacy"
          ? join(marketingDir, "privacy/page.tsx")
          : href === "/terms"
            ? join(marketingDir, "terms/page.tsx")
            : join(marketingDir, "help/page.tsx");
      expect(existsSync(pagePath), `missing page for ${href}`).toBe(true);
    }
  });

  it.each(publicAuthPages)("%s does not use placeholder or missing footer links", (pageFile) => {
    const page = source(authDir, pageFile);

    expect(page).not.toContain('href="#"');
    expect(page).not.toContain('href="/contact"');
    expect(page).toContain("AuthPageShell");
    expect(page).toMatch(/<AuthPageShell[\s\S]*?\bfooter\b/);
  });

  it("login page wires shared footer via AuthPageShell", () => {
    const login = source(authDir, "login/page.tsx");
    const footer = source(componentsDir, "AuthPageFooter.tsx");

    expect(login).toContain("AuthPageShell");
    expect(footer).toContain("AUTH_PUBLIC_FOOTER_LINKS");
    expect(footer).toContain("{link.label}");
  });

  it("auth header help navigates to public /help", () => {
    const header = source(componentsDir, "AuthPageHeader.tsx");

    expect(header).toContain("AUTH_PUBLIC_HELP_HREF");
    expect(header).not.toMatch(/aria-label="Trợ giúp"[\s\S]*type="button"/);
    expect(header).toContain('aria-label="Trợ giúp"');
  });

  it("forgot-password avoids decorative blobs and oversized card radius", () => {
    const forgot = source(authDir, "forgot-password/page.tsx");

    expect(forgot).not.toContain("rounded-[40px]");
    expect(forgot).not.toContain("blur-3xl");
    expect(forgot).not.toContain("hover:scale");
    expect(forgot).toContain('role="status"');
    expect(forgot).toContain('role="alert"');
    expect(forgot).toContain("aria-live");
  });
});
