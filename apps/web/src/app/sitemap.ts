import type { MetadataRoute } from "next";

import { getSiteUrl, PUBLIC_SITEMAP_PATHS } from "@/lib/seo/site";

export default function sitemap(): MetadataRoute.Sitemap {
  const siteUrl = getSiteUrl();
  const lastModified = new Date();

  return PUBLIC_SITEMAP_PATHS.map((path) => ({
    url: path === "/" ? `${siteUrl}/` : `${siteUrl}${path}`,
    lastModified,
    changeFrequency: path === "/" ? "weekly" : "monthly",
    priority: path === "/" ? 1 : 0.6,
  }));
}
