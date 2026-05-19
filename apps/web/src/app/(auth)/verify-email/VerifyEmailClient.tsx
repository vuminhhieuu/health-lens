"use client";

import axios from "axios";
import { ArrowRight, CheckCircle2, Home, XCircle } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { LoadingState } from "@/components/ui";
import { API_ROUTES } from "@/lib/api/routes";
import { apiClient } from "@/lib/api/apiClient";

type VerifyStatus = "loading" | "success" | "error";
type VerifyViewState = {
  status: VerifyStatus;
  title: string;
  message: string;
};

const POST_REGISTER_RETURN_URL_KEY = "post-register-return-url";
const GENERIC_VERIFY_FAILURE_MESSAGE =
  "Liên kết xác thực không còn dùng được. Vui lòng đăng nhập lại hoặc liên hệ hỗ trợ nếu bạn cần xác thực email.";
const EXPIRED_OR_USED_VERIFY_FAILURE_MESSAGE =
  "Liên kết xác thực đã hết hạn hoặc đã được sử dụng. Vui lòng đăng nhập lại hoặc liên hệ hỗ trợ nếu tài khoản của bạn vẫn cần xác thực email.";
const VERIFY_RATE_LIMIT_MESSAGE =
  "Bạn đã thử xác thực quá nhiều lần. Vui lòng chờ một lúc rồi mở lại liên kết xác thực.";

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readStringProperty(value: unknown, key: string) {
  if (!isRecord(value)) {
    return null;
  }
  const property = value[key];
  return typeof property === "string" ? property : null;
}

function parseVerifyError(error: unknown): Pick<VerifyViewState, "message"> {
  if (!axios.isAxiosError(error)) {
    return {
      message: GENERIC_VERIFY_FAILURE_MESSAGE,
    };
  }

  const data = error.response?.data;
  const properties = isRecord(data) ? data.properties : null;
  const errorCode = readStringProperty(data, "errorCode") ?? readStringProperty(properties, "errorCode");
  const detail = readStringProperty(data, "detail");
  const status = error.response?.status;
  const normalizedDetail = detail?.toLowerCase() ?? "";
  const isRateLimited = status === 429 || errorCode === "RATE_LIMITED";
  const expiredOrUsedToken =
    status === 400 ||
    status === 410 ||
    errorCode === "VALIDATION_ERROR" ||
    errorCode === "AUTH_003" ||
    errorCode === "AUTH_004" ||
    normalizedDetail.includes("hết hạn") ||
    normalizedDetail.includes("không thể xác thực");

  if (isRateLimited) {
    return {
      message: VERIFY_RATE_LIMIT_MESSAGE,
    };
  }

  if (expiredOrUsedToken) {
    return {
      message: EXPIRED_OR_USED_VERIFY_FAILURE_MESSAGE,
    };
  }

  return {
    message: GENERIC_VERIFY_FAILURE_MESSAGE,
  };
}

function safeInternalReturnUrl(value: string | null) {
  if (!value || !value.startsWith("/") || value.startsWith("//")) {
    return null;
  }

  try {
    const parsed = new URL(value, window.location.origin);
    if (parsed.origin !== window.location.origin) {
      return null;
    }
    return `${parsed.pathname}${parsed.search}${parsed.hash}`;
  } catch {
    return null;
  }
}

export default function VerifyEmailClient() {
  const params = useSearchParams();
  const token = params.get("token");
  const processedToken = useRef<string | null | undefined>(undefined);
  const activeRequest = useRef<AbortController | null>(null);
  const [loginHref] = useState(() => {
    if (typeof window === "undefined") {
      return "/login";
    }
    const storedReturnUrl = safeInternalReturnUrl(window.localStorage.getItem(POST_REGISTER_RETURN_URL_KEY));
    return storedReturnUrl ? `/login?returnUrl=${encodeURIComponent(storedReturnUrl)}` : "/login";
  });

  const [viewState, setViewState] = useState<VerifyViewState>({
    status: "loading",
    title: "Đang xác thực email...",
    message: "Hệ thống đang kiểm tra liên kết xác thực của bạn.",
  });

  useEffect(() => {
    return () => {
      activeRequest.current?.abort();
    };
  }, []);

  useEffect(() => {
    if (!token) {
      if (processedToken.current === undefined) {
        processedToken.current = null;
        const showMissingToken = async () => {
          setViewState({
            status: "error",
            title: "Không thể xác thực email",
            message: GENERIC_VERIFY_FAILURE_MESSAGE,
          });
        };
        void showMissingToken();
      }
      return;
    }

    if (processedToken.current === token) {
      return;
    }

    processedToken.current = token;
    activeRequest.current?.abort();
    const abortController = new AbortController();
    activeRequest.current = abortController;

    const verify = async () => {
      setViewState({
        status: "loading",
        title: "Đang xác thực email...",
        message: "Hệ thống đang kiểm tra liên kết xác thực của bạn.",
      });

      if (typeof window !== "undefined") {
        window.history.replaceState(null, "", window.location.pathname);
      }

      try {
        await apiClient.post(API_ROUTES.AUTH.VERIFY_EMAIL, { token }, { signal: abortController.signal });
        if (abortController.signal.aborted) {
          return;
        }
        setViewState({
          status: "success",
          title: "Xác thực email thành công",
          message: "Email của bạn đã được xác thực. Bạn có thể đăng nhập để tiếp tục.",
        });
      } catch (error: unknown) {
        if (abortController.signal.aborted || axios.isCancel(error)) {
          return;
        }

        const parsedError = parseVerifyError(error);
        setViewState({
          status: "error",
          title: "Không thể xác thực email",
          message: parsedError.message,
        });
      }

      if (activeRequest.current === abortController) {
        activeRequest.current = null;
      }
    };

    verify();
  }, [token]);

  if (viewState.status === "loading") {
    return (
      <main className="flex min-h-screen items-center justify-center bg-gradient-to-br from-[#effcf9] via-[#e9f6f3] to-[#d8e5e2] px-4 py-10">
        <LoadingState
          title="Đang xác thực email"
          description={viewState.message}
          className="w-full max-w-xl border-[#b7d8d1] bg-white"
        />
      </main>
    );
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-gradient-to-br from-[#effcf9] via-[#e9f6f3] to-[#d8e5e2] px-4 py-10">
      <section className="w-full max-w-xl rounded-2xl bg-white p-8 text-center shadow-[0_8px_32px_rgba(18,30,28,0.08)] md:p-10">
        <div
          role={viewState.status === "error" ? "alert" : "status"}
          aria-live={viewState.status === "error" ? "assertive" : "polite"}
        >
          {viewState.status === "success" ? (
            <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-[#e9f6f3]">
              <CheckCircle2 className="h-9 w-9 text-[#10B981]" />
            </div>
          ) : null}
          {viewState.status === "error" ? (
            <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-[#fef2f2]">
              <XCircle className="h-9 w-9 text-[#ef4444]" />
            </div>
          ) : null}

          <h1 className="mb-3 text-3xl font-bold tracking-tight text-[#121e1c]">{viewState.title}</h1>
          <p className="mx-auto max-w-md text-base leading-relaxed text-[#3d4947]">{viewState.message}</p>
        </div>

        <div className="mt-8 flex flex-col items-center gap-4">
          <Link
            href={loginHref}
            className="inline-flex h-12 items-center justify-center gap-2 rounded-xl bg-[#10B981] px-7 text-sm font-semibold text-white transition hover:brightness-110"
          >
            Đến trang đăng nhập
            <ArrowRight className="h-4 w-4" />
          </Link>

          <Link
            href="/"
            className="inline-flex items-center gap-2 text-sm font-medium text-[#3d4947] transition hover:text-[#00685f]"
          >
            <Home className="h-4 w-4" />
            Quay lại trang chủ
          </Link>
        </div>
      </section>
    </main>
  );
}
