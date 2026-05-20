import { afterEach, describe, expect, it } from "vitest";

import { DEFAULT_SITE_URL, getSiteUrl, normalizeSiteUrl } from "@/lib/seo/site";

const originalEnv = { ...process.env };

afterEach(() => {
  process.env = { ...originalEnv };
});

describe("normalizeSiteUrl", () => {
  it("adds https when scheme is missing", () => {
    expect(normalizeSiteUrl("healthlens.vn")).toBe("https://healthlens.vn");
  });

  it("preserves explicit https origin", () => {
    expect(normalizeSiteUrl("https://healthlens.vn/")).toBe("https://healthlens.vn");
  });

  it("preserves http for local origins", () => {
    expect(normalizeSiteUrl("http://localhost:3000")).toBe("http://localhost:3000");
  });

  it("returns null for invalid values", () => {
    expect(normalizeSiteUrl("not a valid url !!!")).toBeNull();
    expect(normalizeSiteUrl("")).toBeNull();
  });
});

describe("getSiteUrl", () => {
  it("normalizes NEXT_PUBLIC_SITE_URL without scheme", () => {
    process.env.NEXT_PUBLIC_SITE_URL = "healthlens.vn";
    process.env.NODE_ENV = "production";
    expect(getSiteUrl()).toBe("https://healthlens.vn");
  });

  it("falls back to localhost in development when env is invalid", () => {
    process.env.NEXT_PUBLIC_SITE_URL = "%%%";
    process.env.NODE_ENV = "development";
    expect(getSiteUrl()).toBe("http://localhost:3000");
  });

  it("falls back to DEFAULT_SITE_URL in production when env is invalid", () => {
    process.env.NEXT_PUBLIC_SITE_URL = "%%%";
    process.env.NODE_ENV = "production";
    expect(getSiteUrl()).toBe(DEFAULT_SITE_URL);
  });
});
