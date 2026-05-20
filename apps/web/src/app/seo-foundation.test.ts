import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";

import { describe, expect, it } from "vitest";

import robots from "./robots";
import sitemap from "./sitemap";
import { PUBLIC_SITEMAP_PATHS, ROBOTS_DISALLOW_PREFIXES } from "@/lib/seo/site";
import { landingMetadata } from "@/lib/seo/metadata";

const appDir = join(process.cwd(), "src/app");
const publicDir = join(process.cwd(), "public");
const source = (path: string) => readFileSync(join(appDir, path), "utf8");

describe("SEO foundation (pae-4)", () => {
  it("lists public marketing URLs in sitemap.ts", () => {
    const entries = sitemap();
    const urls = entries.map((entry) => entry.url);

    for (const path of PUBLIC_SITEMAP_PATHS) {
      expect(urls.some((url) => url.endsWith(path === "/" ? "/" : path))).toBe(true);
    }
  });

  it("disallows dashboard, admin, and auth paths in robots.ts", () => {
    const rules = robots().rules;
    const ruleList = Array.isArray(rules) ? rules : [rules];
    const disallow = ruleList.flatMap((rule) => rule.disallow ?? []);

    for (const path of ROBOTS_DISALLOW_PREFIXES) {
      expect(disallow).toContain(path);
    }
    expect(disallow).toContain("/home");
    expect(disallow).toContain("/admin");
  });

  it("exposes Vietnamese landing metadata with openGraph locale vi_VN", () => {
    const landingPage = source("(marketing)/page.tsx");

    expect(landingPage).toContain("landingMetadata");
    expect(landingMetadata.openGraph?.locale).toBe("vi_VN");
    expect(landingMetadata.title).toBeTruthy();
    expect(landingMetadata.description).toBeTruthy();
    expect(landingMetadata.openGraph?.title).toBeTruthy();
    expect(landingMetadata.openGraph?.description).toBeTruthy();
  });

  it("ships HealthLens 3D favicon and app icons in public/", () => {
    expect(existsSync(join(publicDir, "favicon.ico"))).toBe(true);
    expect(existsSync(join(publicDir, "brand/healthlens-icon-3d-1024.png"))).toBe(true);
    expect(existsSync(join(publicDir, "apple-touch-icon.png"))).toBe(true);
    expect(existsSync(join(publicDir, "icon-192.png"))).toBe(true);
    expect(existsSync(join(publicDir, "icon-512.png"))).toBe(true);
    expect(existsSync(join(appDir, "favicon.ico"))).toBe(true);
  });

  it("maps each sitemap path to an App Router page (smoke: routable)", () => {
    const routeFiles: Record<string, string> = {
      "/": "(marketing)/page.tsx",
      "/privacy": "(marketing)/privacy/page.tsx",
      "/terms": "(marketing)/terms/page.tsx",
      "/help": "(marketing)/help/page.tsx",
    };

    for (const path of PUBLIC_SITEMAP_PATHS) {
      const file = routeFiles[path];
      expect(existsSync(join(appDir, file)), `missing page for ${path}`).toBe(true);
    }
  });

  it("documents HTTP 200 smoke for sitemap URLs (run scripts/smoke-sitemap-urls.mjs with dev server)", () => {
    const scriptPath = join(process.cwd(), "scripts/smoke-sitemap-urls.mjs");
    expect(existsSync(scriptPath)).toBe(true);
  });
});
