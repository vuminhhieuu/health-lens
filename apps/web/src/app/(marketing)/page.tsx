import type { Metadata } from "next";

import { landingMetadata } from "@/lib/seo/metadata";

import { MarketingHome } from "./MarketingHome";

export const metadata: Metadata = landingMetadata;

export default function Home() {
  return <MarketingHome />;
}
