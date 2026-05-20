import type { ReactNode } from "react";

import { MarketingFooter } from "@/components/marketing/MarketingFooter";
import { MarketingHeader } from "@/components/marketing/MarketingHeader";

export default function MarketingLayout({
  children,
}: Readonly<{
  children: ReactNode;
}>) {
  return (
    <div className="flex min-h-screen flex-col bg-[#f6fbfa] text-[#121e1c]">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-[100] focus:rounded-lg focus:bg-[#00685f] focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-white"
      >
        Bỏ qua đến nội dung chính
      </a>
      <MarketingHeader />
      <div className="flex flex-1 flex-col">{children}</div>
      <MarketingFooter />
    </div>
  );
}
