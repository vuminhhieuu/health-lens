#!/usr/bin/env node
/**
 * Smoke: every URL in /sitemap.xml returns HTTP 200.
 * Prerequisite: `pnpm dev` (or `pnpm start`) running.
 *
 * Usage: node scripts/smoke-sitemap-urls.mjs
 * Env: NEXT_PUBLIC_SITE_URL (default http://localhost:3000)
 */

const DEFAULT_BASE = "http://localhost:3000";

function normalizeBase(raw) {
  const trimmed = raw.trim().replace(/\/$/, "");
  if (!trimmed) return DEFAULT_BASE;
  const withScheme = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
  try {
    const url = new URL(withScheme);
    return `${url.protocol}//${url.host}`;
  } catch {
    return DEFAULT_BASE;
  }
}

function parseSitemapLocs(xml) {
  return [...xml.matchAll(/<loc>([^<]+)<\/loc>/gi)].map((match) => match[1].trim());
}

async function fetchSitemapUrls(base) {
  const res = await fetch(`${base}/sitemap.xml`);
  if (!res.ok) {
    throw new Error(`Failed to fetch sitemap.xml: ${res.status} ${res.statusText}`);
  }
  const xml = await res.text();
  const urls = parseSitemapLocs(xml);
  if (urls.length === 0) {
    throw new Error("sitemap.xml contains no <loc> entries");
  }
  return urls;
}

async function check(url) {
  const res = await fetch(url, { redirect: "follow" });
  if (!res.ok) {
    throw new Error(`${url} → ${res.status} ${res.statusText}`);
  }
  console.log(`OK ${res.status} ${url}`);
}

async function main() {
  const base = normalizeBase(process.env.NEXT_PUBLIC_SITE_URL ?? DEFAULT_BASE);
  console.log(`Fetching sitemap from ${base}/sitemap.xml …`);
  const urls = await fetchSitemapUrls(base);
  console.log(`Checking ${urls.length} URL(s) …`);
  for (const url of urls) {
    await check(url);
  }
  console.log("All sitemap URLs returned 200.");
}

main().catch((err) => {
  console.error(err.message ?? err);
  process.exit(1);
});
