import type { Metadata } from "next";
import { Suspense } from "react";

import { LoadingState } from "@/components/ui";

import VerifyEmailClient from "./VerifyEmailClient";

export const metadata: Metadata = {
  referrer: "no-referrer",
};

export default function VerifyEmailPage() {
  return (
    <Suspense
      fallback={
        <main className="flex min-h-screen items-center justify-center bg-[#effcf9] px-4 py-10">
          <LoadingState
            title="Đang tải trang xác thực"
            description="HealthLens đang chuẩn bị kiểm tra liên kết xác thực email."
            className="w-full max-w-xl border-[#b7d8d1] bg-white"
          />
        </main>
      }
    >
      <VerifyEmailClient />
    </Suspense>
  );
}
