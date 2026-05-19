"use client";

import { ArrowRight, CheckCircle2, Home, Loader2, XCircle } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { API_ROUTES } from "@/lib/api/routes";
import { apiClient } from "@/lib/api/apiClient";

type VerifyStatus = "loading" | "success" | "error";

const POST_REGISTER_RETURN_URL_KEY = "post-register-return-url";
const GENERIC_VERIFY_FAILURE_MESSAGE =
  "Liên kết xác thực không còn dùng được. Vui lòng đăng nhập lại hoặc liên hệ hỗ trợ nếu bạn cần xác thực email.";

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
  const hasStarted = useRef(false);
  const [loginHref] = useState(() => {
    if (typeof window === "undefined") {
      return "/login";
    }
    const storedReturnUrl = safeInternalReturnUrl(window.localStorage.getItem(POST_REGISTER_RETURN_URL_KEY));
    return storedReturnUrl ? `/login?returnUrl=${encodeURIComponent(storedReturnUrl)}` : "/login";
  });

  const [status, setStatus] = useState<VerifyStatus>("loading");
  const [title, setTitle] = useState("Đang xác thực email...");
  const [message, setMessage] = useState("Hệ thống đang kiểm tra liên kết xác thực của bạn.");

  useEffect(() => {
    if (hasStarted.current) {
      return;
    }
    hasStarted.current = true;

    const verify = async () => {
      if (!token) {
        setStatus("error");
        setTitle("Không thể xác thực email");
        setMessage(GENERIC_VERIFY_FAILURE_MESSAGE);
        return;
      }

      if (typeof window !== "undefined") {
        window.history.replaceState(null, "", window.location.pathname);
      }

      try {
        await apiClient.post(API_ROUTES.AUTH.VERIFY_EMAIL, { token });
        setStatus("success");
        setTitle("Xác thực email thành công");
        setMessage("Email của bạn đã được xác thực. Bạn có thể đăng nhập để tiếp tục.");
      } catch {
        setStatus("error");
        setTitle("Không thể xác thực email");
        setMessage(GENERIC_VERIFY_FAILURE_MESSAGE);
      }
    };

    verify();
  }, [token]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-gradient-to-br from-[#effcf9] via-[#e9f6f3] to-[#d8e5e2] px-4 py-10">
      <section className="w-full max-w-xl rounded-2xl bg-white p-8 text-center shadow-[0_8px_32px_rgba(18,30,28,0.08)] md:p-10">
        {status === "loading" ? (
          <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-[#e9f6f3]">
            <Loader2 className="h-9 w-9 animate-spin text-[#10B981]" />
          </div>
        ) : null}
        {status === "success" ? (
          <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-[#e9f6f3]">
            <CheckCircle2 className="h-9 w-9 text-[#10B981]" />
          </div>
        ) : null}
        {status === "error" ? (
          <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-[#fef2f2]">
            <XCircle className="h-9 w-9 text-[#ef4444]" />
          </div>
        ) : null}

        <h1 className="mb-3 text-3xl font-bold tracking-tight text-[#121e1c]">{title}</h1>
        <p className="mx-auto mb-8 max-w-md text-base leading-relaxed text-[#3d4947]">{message}</p>

        <div className="flex flex-col items-center gap-4">
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
