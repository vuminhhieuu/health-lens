import type { Metadata } from "next";
import { permanentRedirect } from "next/navigation";

export const metadata: Metadata = {
  title: "Hỗ trợ — HealthLens",
  description: "Liên kết tới trang trợ giúp và hướng dẫn sử dụng HealthLens.",
  robots: { index: false, follow: true },
};

export default function SupportPage() {
  permanentRedirect("/help");
}
