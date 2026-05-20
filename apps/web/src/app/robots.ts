import type { MetadataRoute } from "next";

import { getSiteUrl, ROBOTS_DISALLOW_PREFIXES } from "@/lib/seo/site";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: [...ROBOTS_DISALLOW_PREFIXES],
    },
    sitemap: `${getSiteUrl()}/sitemap.xml`,
  };
}
