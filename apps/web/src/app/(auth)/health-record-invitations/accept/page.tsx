"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import axios from "axios";
import { Loader2 } from "lucide-react";
import { ApiPaths } from "@healthlens/shared/constants";
import { apiClient } from "@/lib/api/apiClient";
import { invitationErrorMessage, messageCatalog } from "@/lib/i18n/messages";

function loginReturnUrlForToken(token: string): string {
  const returnUrl = `/health-record-invitations/accept?token=${encodeURIComponent(token)}`;
  return `/login?returnUrl=${encodeURIComponent(returnUrl)}`;
}

function invitationLoginRedirectGuardKey(token: string): string {
  return `health-record-invitation-login-redirect:${token}`;
}

type AcceptResult = {
  outcome: "accepted" | "require-login" | "expired" | string;
  redirectUrl: string;
  recordId: string;
  profileId: string;
};

function LoadingState() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-[#effcf9] px-6">
      <div className="rounded-2xl bg-white px-6 py-5 shadow-sm">
        <div className="flex items-center gap-3 text-[#005049]">
          <Loader2 className="h-5 w-5 animate-spin" />
          <p className="text-sm font-semibold">Đang xử lý lời mời chia sẻ kết quả...</p>
        </div>
      </div>
    </main>
  );
}

function AcceptHealthRecordInvitationContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      return;
    }

    const run = async () => {
      try {
        const response = await apiClient.post(ApiPaths.HEALTH_RECORD_INVITATIONS.ACCEPT(token));
        const result = response.data?.data as AcceptResult | undefined;
        if (!result?.redirectUrl) {
          setError(messageCatalog.sharing.invitationProcessingFailed);
          return;
        }
        window.location.replace(result.redirectUrl);
      } catch (err: unknown) {
        if (axios.isAxiosError(err)) {
          const status = err.response?.status;
          if (status === 403) {
            setError(invitationErrorMessage(err));
            return;
          }
          if (status === 404) {
            setError(invitationErrorMessage(err));
            return;
          }
          if (status === 401) {
            const guardKey = invitationLoginRedirectGuardKey(token);
            if (sessionStorage.getItem(guardKey) === "1") {
              setError(messageCatalog.sharing.loginInvalidOrServerError);
              return;
            }
            sessionStorage.setItem(guardKey, "1");
            window.location.replace(loginReturnUrlForToken(token));
            return;
          }
          if (status !== undefined && status >= 500) {
            setError(invitationErrorMessage(err));
            return;
          }
          setError(invitationErrorMessage(err));
          return;
        }
        setError(messageCatalog.sharing.networkError);
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

  return <LoadingState />;
}

export default function AcceptHealthRecordInvitationPage() {
  return (
    <Suspense fallback={<LoadingState />}>
      <AcceptHealthRecordInvitationContent />
    </Suspense>
  );
}
