import type { Metadata } from "next";
import { Suspense } from "react";

import VerifyEmailClient from "./VerifyEmailClient";

export const metadata: Metadata = {
  referrer: "no-referrer",
};

export default function VerifyEmailPage() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-[#effcf9]" />}>
      <VerifyEmailClient />
    </Suspense>
  );
}
