#!/usr/bin/env node
/**
 * Smoke: every PUBLIC_SITEMAP_PATHS URL returns HTTP 200.
 * Prerequisite: `pnpm dev` (or `pnpm start`) running.
 *
 * Usage: node scripts/smoke-sitemap-urls.mjs
 * Env: NEXT_PUBLIC_SITE_URL (default http://localhost:3000)
 */

const DEFAULT_BASE = "http://localhost:3000";
const PATHS = ["/", "/privacy", "/terms", "/help"];

const base = (process.env.NEXT_PUBLIC_SITE_URL ?? DEFAULT_BASE).replace(/\/$/, "");

async function check(path) {
  const url = path === "/" ? `${base}/` : `${base}${path}`;
  const res = await fetch(url, { redirect: "follow" });
  if (!res.ok) {
    throw new Error(`${url} → ${res.status} ${res.statusText}`);
  }
  console.log(`OK ${res.status} ${url}`);
}

async function main() {
  console.log(`Checking sitemap URLs at ${base} …`);
  for (const path of PATHS) {
    await check(path);
  }
  console.log("All sitemap URLs returned 200.");
}

main().catch((err) => {
  console.error(err.message ?? err);
  process.exit(1);
});
