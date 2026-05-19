"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import axios from "axios";
import { AlertTriangle, Loader2, CircleHelp } from "lucide-react";
import { ApiPaths } from "@healthlens/shared/constants";
import { apiClient } from "@/lib/api/apiClient";
import { logoutCurrentUser } from "@/lib/auth/logout";
import { tryRestoreSession } from "@/lib/auth/restoreSession";
import { useAuthHydrated } from "@/hooks/useAuthHydrated";
import {
  appendInvitationLoginRedirectAttemptedParam,
  clearLoginRedirectGuard,
  hasInvitationLoginRedirectAttemptedParam,
  isLoginRedirectGuardActive,
  setLoginRedirectGuard,
} from "@/lib/browser/loginRedirectGuard";
import { readSessionStorage, writeSessionStorage } from "@/lib/browser/sessionStorage";
import { invitationErrorMessage, messageCatalog } from "@/lib/i18n/messages";
import {
  parseAcceptProfileInvitationResult,
  type AcceptProfileInvitationResult,
} from "@/lib/sharing/acceptInvitationResult";
import {
  clearProfileInvitationFlowStorage,
  PENDING_PROFILE_INVITATION_TOKEN_KEY,
  profileInvitationLoginRedirectGuardKey,
} from "@/lib/sharing/profileInvitationStorage";

type InvitationErrorView =
  | { kind: "email_mismatch"; message: string; token: string }
  | { kind: "generic"; message: string };

function scrubInvitationTokenFromUrl() {
  if (typeof window !== "undefined") {
    window.history.replaceState(null, "", window.location.pathname);
  }
}

function stashInvitationToken(token: string) {
  writeSessionStorage(PENDING_PROFILE_INVITATION_TOKEN_KEY, token);
}

function readStashedInvitationToken(): string | null {
  return readSessionStorage(PENDING_PROFILE_INVITATION_TOKEN_KEY);
}

function loginReturnUrlForToken(token: string, storageGuardPersisted: boolean): string {
  let returnUrl = `/invitations/accept?token=${encodeURIComponent(token)}`;
  if (!storageGuardPersisted) {
    returnUrl = appendInvitationLoginRedirectAttemptedParam(returnUrl);
  }
  return `/login?returnUrl=${encodeURIComponent(returnUrl)}`;
}

async function postAcceptInvitation(token: string): Promise<AcceptProfileInvitationResult | null> {
  const response = await apiClient.post(ApiPaths.INVITATIONS.ACCEPT(token));
  return parseAcceptProfileInvitationResult(response.data?.data);
}

function LoadingState() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-[#effcf9] px-6">
      <div className="w-full max-w-lg rounded-3xl border border-white/70 bg-white px-8 py-10 shadow-[0_20px_60px_rgba(0,79,73,0.08)]">
        <div className="flex flex-col items-center text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-[#e4f3f1] text-[#008378]">
            <Loader2 className="h-8 w-8 animate-spin" />
          </div>
          <h1 className="mt-6 text-2xl font-semibold text-[#1c2b2a]">Đang xử lý lời mời</h1>
          <p className="mt-3 max-w-md text-sm leading-6 text-[#5f6e6c]">
            Vui lòng chờ trong giây lát, hệ thống đang xác thực và mở lời mời cho bạn.
          </p>
        </div>
      </div>
    </main>
  );
}

function ErrorState({
  errorView,
  onSwitchAccount,
  isSwitchingAccount,
}: {
  errorView: InvitationErrorView;
  onSwitchAccount?: () => void;
  isSwitchingAccount?: boolean;
}) {
  const isEmailMismatch = errorView.kind === "email_mismatch";

  return (
    <main className="flex min-h-screen items-center justify-center bg-[#effcf9] px-6 py-10">
      <div className="w-full max-w-xl rounded-3xl border border-white/70 bg-white px-8 py-10 shadow-[0_20px_60px_rgba(0,79,73,0.08)]">
        <div className="flex flex-col items-center text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-[#d5f0ed] text-[#00766f]">
            {isEmailMismatch ? <AlertTriangle className="h-8 w-8" /> : <CircleHelp className="h-8 w-8" />}
          </div>

          <h1 className="mt-6 text-2xl font-semibold text-[#1c2b2a]">
            {isEmailMismatch ? "Email đăng nhập không khớp với lời mời" : "Không thể xử lý lời mời"}
          </h1>

          <p className="mt-3 max-w-lg text-sm leading-6 text-[#5f6e6c]">{errorView.message}</p>
        </div>

        {isEmailMismatch ? (
          <div className="mt-6 rounded-2xl border border-[#d8e7e4] bg-[#f7fbfa] px-5 py-4 text-left text-sm leading-6 text-[#425352]">
            {messageCatalog.sharing.invitationEmailMismatchHint}
          </div>
        ) : null}

        {isEmailMismatch && onSwitchAccount ? (
          <button
            type="button"
            onClick={onSwitchAccount}
            disabled={isSwitchingAccount}
            className="mt-6 inline-flex w-full items-center justify-center rounded-full bg-[#008378] px-6 py-3 text-sm font-semibold text-white shadow-[0_12px_24px_rgba(0,131,120,0.22)] transition hover:bg-[#00796f] disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSwitchingAccount ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Đang chuyển tài khoản...
              </>
            ) : (
              messageCatalog.sharing.invitationSwitchAccount
            )}
          </button>
        ) : null}
      </div>
    </main>
  );
}

function AcceptInvitationContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const authHydrated = useAuthHydrated();
  const tokenFromUrl = searchParams.get("token");
  const processedToken = useRef<string | null | undefined>(undefined);
  const loginRedirectAttempted = useRef(false);
  const [errorView, setErrorView] = useState<InvitationErrorView | null>(null);
  const [isSwitchingAccount, setIsSwitchingAccount] = useState(false);

  const switchAccountForInvitation = async (token: string) => {
    setIsSwitchingAccount(true);
    try {
      await logoutCurrentUser();
      clearLoginRedirectGuard(profileInvitationLoginRedirectGuardKey(token));
      loginRedirectAttempted.current = false;
      processedToken.current = undefined;
      stashInvitationToken(token);
      router.replace(loginReturnUrlForToken(token, true));
    } finally {
      setIsSwitchingAccount(false);
    }
  };

  useEffect(() => {
    if (!authHydrated) {
      return;
    }

    if (!tokenFromUrl) {
      if (processedToken.current === undefined) {
        processedToken.current = null;
        clearProfileInvitationFlowStorage();
      }
      return;
    }

    if (processedToken.current === tokenFromUrl) {
      return;
    }

    processedToken.current = tokenFromUrl;
    const token = tokenFromUrl;
    setErrorView(null);
    stashInvitationToken(token);
    scrubInvitationTokenFromUrl();

    const showTerminalError = (view: InvitationErrorView) => {
      clearProfileInvitationFlowStorage(token);
      setErrorView(view);
    };

    const redirectToLogin = () => {
      const guardKey = profileInvitationLoginRedirectGuardKey(token);
      if (
        isLoginRedirectGuardActive(guardKey) ||
        loginRedirectAttempted.current ||
        hasInvitationLoginRedirectAttemptedParam(searchParams)
      ) {
        showTerminalError({
          kind: "generic",
          message: messageCatalog.sharing.loginInvalidOrServerError,
        });
        return;
      }
      loginRedirectAttempted.current = true;
      const storageGuardPersisted = setLoginRedirectGuard(guardKey);
      router.replace(loginReturnUrlForToken(token, storageGuardPersisted));
    };

    const handleResult = (result: AcceptProfileInvitationResult) => {
      if (result.outcome === "expired") {
        showTerminalError({
          kind: "generic",
          message: messageCatalog.sharing.invitationExpired,
        });
        return;
      }
      if (result.outcome === "require-login") {
        redirectToLogin();
        return;
      }
      clearProfileInvitationFlowStorage(token);
      window.location.replace(result.redirectUrl);
    };

    const acceptWithSessionRecovery = async (): Promise<AcceptProfileInvitationResult | null> => {
      await tryRestoreSession();
      return postAcceptInvitation(token);
    };

    const run = async () => {
      try {
        const result = await acceptWithSessionRecovery();
        if (!result) {
          showTerminalError({
            kind: "generic",
            message: messageCatalog.sharing.invitationProcessingFailed,
          });
          return;
        }
        handleResult(result);
      } catch (err: unknown) {
        if (axios.isAxiosError(err)) {
          const status = err.response?.status;

          if (status === 401) {
            const restored = await tryRestoreSession();
            if (restored) {
              try {
                const retryResult = await postAcceptInvitation(token);
                if (retryResult) {
                  handleResult(retryResult);
                  return;
                }
              } catch {
                // Fall through to login redirect.
              }
            }
            redirectToLogin();
            return;
          }

          if (status === 403) {
            showTerminalError({
              kind: "email_mismatch",
              message: invitationErrorMessage(err),
              token,
            });
            return;
          }

          if (status === 404) {
            showTerminalError({
              kind: "generic",
              message: invitationErrorMessage(err),
            });
            return;
          }
          if (status !== undefined && status >= 500) {
            showTerminalError({
              kind: "generic",
              message: invitationErrorMessage(err),
            });
            return;
          }
          showTerminalError({
            kind: "generic",
            message: invitationErrorMessage(err),
          });
          return;
        }
        showTerminalError({
          kind: "generic",
          message: messageCatalog.sharing.networkError,
        });
      }
    };

    void run();
  }, [authHydrated, router, searchParams, tokenFromUrl]);

  if (errorView) {
    const switchToken =
      errorView.kind === "email_mismatch" ? errorView.token : readStashedInvitationToken();

    return (
      <ErrorState
        errorView={errorView}
        onSwitchAccount={
          errorView.kind === "email_mismatch" && switchToken
            ? () => void switchAccountForInvitation(switchToken)
            : undefined
        }
        isSwitchingAccount={isSwitchingAccount}
      />
    );
  }

  if (authHydrated && !tokenFromUrl && processedToken.current === null) {
    return (
      <ErrorState
        errorView={{
          kind: "generic",
          message: messageCatalog.sharing.invitationInvalid,
        }}
      />
    );
  }

  return <LoadingState />;
}

export default function AcceptInvitationPage() {
  return (
    <Suspense fallback={<LoadingState />}>
      <AcceptInvitationContent />
    </Suspense>
  );
}
