import type { Metadata } from "next";

import { getSiteUrl, SITE_NAME } from "@/lib/seo/site";

const defaultDescription =
  "Nền tảng đọc và theo dõi kết quả sức khỏe cho người dùng Việt Nam.";

export const landingTitle = "HealthLens — Theo dõi kết quả khám, dễ hiểu hơn";

export const landingDescription =
  "HealthLens hỗ trợ bạn đọc kết quả xét nghiệm bằng ngôn ngữ dễ hiểu, tổ chức dữ liệu theo từng hồ sơ và chuẩn bị thông tin trước khi trao đổi với bác sĩ.";

export function createRootMetadata(): Metadata {
  const siteUrl = getSiteUrl();

  return {
    metadataBase: new URL(siteUrl),
    title: {
      default: SITE_NAME,
      template: `%s | ${SITE_NAME}`,
    },
    description: defaultDescription,
    icons: {
      icon: [
        { url: "/favicon.ico", sizes: "16x16" },
        { url: "/icon-192.png", sizes: "192x192", type: "image/png" },
        { url: "/icon-512.png", sizes: "512x512", type: "image/png" },
      ],
      apple: [{ url: "/apple-touch-icon.png", sizes: "180x180", type: "image/png" }],
    },
    openGraph: {
      type: "website",
      locale: "vi_VN",
      siteName: SITE_NAME,
      title: SITE_NAME,
      description: defaultDescription,
    },
  };
}

export const landingMetadata: Metadata = {
  title: landingTitle,
  description: landingDescription,
  openGraph: {
    title: landingTitle,
    description: landingDescription,
    locale: "vi_VN",
    type: "website",
    siteName: SITE_NAME,
  },
};
