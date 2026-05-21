"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import axios from "axios";
import { ApiPaths } from "@healthlens/shared/constants";
import { apiClient } from "@/lib/api/apiClient";
import { invitationErrorMessage, messageCatalog } from "@/lib/i18n/messages";
import {
  InvitationErrorState,
  InvitationLoadingState,
} from "@/components/auth/InvitationFlowShell";

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
      <InvitationErrorState
        title="Liên kết mời không hợp lệ"
        description="Vui lòng kiểm tra lại email hoặc yêu cầu người gửi gửi lại lời mời."
      />
    );
  }

  if (error) {
    return <InvitationErrorState title="Không thể xử lý lời mời" description={error} />;
  }

  return (
    <InvitationLoadingState
      title="Đang xử lý lời mời"
      description="Hệ thống đang xác thực và mở quyền xem kết quả được chia sẻ."
    />
  );
}

export default function AcceptHealthRecordInvitationPage() {
  return (
    <Suspense
      fallback={
        <InvitationLoadingState
          title="Đang xử lý lời mời"
          description="Hệ thống đang xác thực và mở quyền xem kết quả được chia sẻ."
        />
      }
    >
      <AcceptHealthRecordInvitationContent />
    </Suspense>
  );
}
