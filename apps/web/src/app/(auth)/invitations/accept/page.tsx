"use client";

import { useEffect, useState } from "react";

import axios from "axios";
import { Loader2 } from "lucide-react";
import { ApiPaths } from "@healthlens/shared/constants";
import { apiClient } from "@/lib/api/apiClient";


function problemDetailMessage(data: unknown): string | undefined {
  if (typeof data !== "object" || data === null || !("detail" in data)) {
    return undefined;
  }
  const detail = (data as { detail: unknown }).detail;
  return typeof detail === "string" ? detail : undefined;
}

function loginReturnUrlForToken(token: string): string {
  const returnUrl = `/invitations/accept?token=${encodeURIComponent(token)}`;
  return `/login?returnUrl=${encodeURIComponent(returnUrl)}`;
}

type AcceptResult = {
  outcome: "accepted" | "require-register" | "expired" | string;
  redirectUrl: string;
  profileId: string;
};

export default function AcceptInvitationPage() {

  const [error, setError] = useState<string | null>(null);
  const token =
    typeof window !== "undefined" ? new URLSearchParams(window.location.search).get("token") : null;

  useEffect(() => {
    if (!token) {
      return;
    }

    const run = async () => {
      try {
        const response = await apiClient.post(ApiPaths.INVITATIONS.ACCEPT(token));
        const result = response.data?.data as AcceptResult | undefined;
        if (!result?.redirectUrl) {
          setError("Không thể xử lý lời mời. Vui lòng thử lại.");
          return;
        }

        // Full navigation so dashboard loads with fresh auth/session (same-tab deep link from email).
        window.location.replace(result.redirectUrl);
      } catch (err: unknown) {
        if (axios.isAxiosError(err)) {
          const status = err.response?.status;
          const detail = problemDetailMessage(err.response?.data);
          if (status === 403) {
            setError(detail ?? "Email đăng nhập không khớp với lời mời.");
            return;
          }
          if (status === 404) {
            setError(detail ?? "Liên kết mời không hợp lệ hoặc không còn dùng được.");
            return;
          }
          if (status === 401) {
            window.location.replace(loginReturnUrlForToken(token));
            return;
          }
          if (status !== undefined && status >= 500) {
            setError("Hệ thống đang gặp sự cố. Vui lòng thử lại sau.");
            return;
          }
          setError(detail ?? "Không thể xử lý lời mời. Vui lòng thử lại.");
          return;
        }
        setError("Không thể kết nối. Vui lòng kiểm tra mạng và thử lại.");
      }
    };

    void run();

  }, [token]);

  if (!token) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-[#effcf9] px-6">
        <p className="rounded-xl bg-white px-5 py-4 text-sm text-[#ba1a1a] shadow-sm">Liên kết mời không hợp lệ.</p>
      </main>
    );
  }

  if (error) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-[#effcf9] px-6">
        <p className="rounded-xl bg-white px-5 py-4 text-sm text-[#ba1a1a] shadow-sm">{error}</p>
      </main>
    );
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-[#effcf9] px-6">
      <div className="rounded-2xl bg-white px-6 py-5 shadow-sm">
        <div className="flex items-center gap-3 text-[#005049]">
          <Loader2 className="h-5 w-5 animate-spin" />
          <p className="text-sm font-semibold">Đang xử lý lời mời...</p>
        </div>
      </div>
    </main>
  );
}
