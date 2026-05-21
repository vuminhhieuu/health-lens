"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { forgotPasswordSchema } from "@healthlens/shared/schemas/auth";
import { CheckCircle2 } from "lucide-react";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { AuthPageShell } from "@/components/auth/AuthPageShell";
import {
  forgotPasswordCooldownSeconds,
  getForgotPasswordCooldownRemainingMs,
  markForgotPasswordRequestSent,
} from "@/lib/auth/forgotPasswordCooldown";
import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { getApiErrorPayload, getApiErrorStatus, messageCatalog, retryAfterMinutes } from "@/lib/i18n/messages";
import { notify } from "@/lib/notify";

type ForgotPasswordInput = z.infer<typeof forgotPasswordSchema>;

export default function ForgotPasswordPage() {
  const [submitError, setSubmitError] = useState("");
  const [isSuccess, setIsSuccess] = useState(false);
  const [submittedEmail, setSubmittedEmail] = useState("");
  const [shouldFocusEmail, setShouldFocusEmail] = useState(false);
  const [cooldownRemainingMs, setCooldownRemainingMs] = useState(0);
  const emailInputRef = useRef<HTMLInputElement | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordInput>({
    resolver: zodResolver(forgotPasswordSchema),
    mode: "onChange",
    defaultValues: {
      email: "",
    },
  });

  const emailField = register("email");

  useEffect(() => {
    if (!isSuccess && shouldFocusEmail) {
      emailInputRef.current?.focus();
      setShouldFocusEmail(false);
    }
  }, [isSuccess, shouldFocusEmail]);

  useEffect(() => {
    const syncCooldown = () => setCooldownRemainingMs(getForgotPasswordCooldownRemainingMs());
    syncCooldown();
    const intervalId = window.setInterval(syncCooldown, 1000);
    return () => window.clearInterval(intervalId);
  }, [isSuccess]);

  const cooldownSeconds = forgotPasswordCooldownSeconds(cooldownRemainingMs);
  const isCooldownActive = cooldownRemainingMs > 0;

  const onSubmit = async (data: ForgotPasswordInput) => {
    const remainingMs = getForgotPasswordCooldownRemainingMs();
    if (remainingMs > 0) {
      const message = messageCatalog.auth.forgotPasswordCooldown(
        forgotPasswordCooldownSeconds(remainingMs),
      );
      setSubmitError(message);
      notify.error(message);
      return;
    }

    setSubmitError("");
    markForgotPasswordRequestSent();

    try {
      await apiClient.post(API_ROUTES.AUTH.FORGOT_PASSWORD, {
        email: data.email,
      });
      setSubmittedEmail(data.email);
      setIsSuccess(true);
      notify.success(messageCatalog.auth.forgotPasswordSent);
    } catch (error: unknown) {
      const status = getApiErrorStatus(error);
      const payload = getApiErrorPayload(error);
      const message =
        status === 429
          ? messageCatalog.auth.forgotPasswordRateLimited(retryAfterMinutes(payload, 3600))
          : status
            ? messageCatalog.auth.genericRetry
            : messageCatalog.auth.networkError;
      setSubmitError(message);
      notify.error(message);
    }
  };

  const handleResend = () => {
    if (submittedEmail) {
      setValue("email", submittedEmail);
    }
    setIsSuccess(false);
    setSubmitError("");
    setShouldFocusEmail(true);
  };

  return (
    <AuthPageShell footer>
      <div className="mb-10 text-center">
        <h1 className="mb-2 text-3xl font-extrabold tracking-tight text-[#121e1c]">
          {isSuccess ? "Kiểm tra email của bạn" : "Quên mật khẩu?"}
        </h1>
        <p className="text-base text-[#3d4947]">
          {isSuccess
            ? "Vui lòng kiểm tra hộp thư và thư mục thư rác."
            : "Nhập email của bạn để nhận hướng dẫn khôi phục mật khẩu."}
        </p>
      </div>

      {isSuccess ? (
        <div className="space-y-6">
          <div
            role="status"
            aria-live="polite"
            className="flex flex-col items-center gap-4 text-center"
          >
            <div className="rounded-full bg-[#effcf9] p-4">
              <CheckCircle2 className="h-12 w-12 text-[#00685f]" aria-hidden />
            </div>
            <p className="text-sm leading-relaxed text-[#3d4947]">
              {messageCatalog.auth.forgotPasswordSent}
            </p>
          </div>

          <p className="rounded-lg border border-[#bcc9c6]/30 bg-[#f6fbfa] px-4 py-3 text-center text-sm leading-relaxed text-[#3d4947]">
            Không nhận được email? Hãy kiểm tra thư mục thư rác hoặc{" "}
            <button
              type="button"
              onClick={handleResend}
              className="font-semibold text-[#00685f] hover:underline"
            >
              gửi lại yêu cầu
            </button>
            .
          </p>

          <Link
            href="/login"
            className="flex h-14 w-full items-center justify-center rounded-xl bg-gradient-to-br from-[#00685f] to-[#008378] text-lg font-bold text-white shadow-lg transition hover:brightness-110"
          >
            Quay lại đăng nhập
          </Link>
        </div>
      ) : (
        <>
          <form className="space-y-6" onSubmit={handleSubmit(onSubmit)} noValidate>
            {isSubmitting ? (
              <p role="status" aria-live="polite" className="sr-only">
                Đang gửi yêu cầu khôi phục mật khẩu
              </p>
            ) : null}

            <div className="space-y-2">
              <label
                htmlFor="forgot-email"
                className="ml-1 block text-sm font-semibold text-[#3d4947]"
              >
                Email
              </label>
              <input
                id="forgot-email"
                type="email"
                autoComplete="email"
                placeholder="email@vi-du.com"
                name={emailField.name}
                onChange={emailField.onChange}
                onBlur={emailField.onBlur}
                ref={(element) => {
                  emailField.ref(element);
                  emailInputRef.current = element;
                }}
                className="h-14 w-full rounded-t-lg border-b-2 border-transparent bg-[#d8e5e2] px-4 text-base text-[#121e1c] outline-none transition focus:border-[#00685f]"
              />
              {errors.email ? (
                <p className="text-sm text-[#ba1a1a]">{errors.email.message}</p>
              ) : null}
            </div>

            {submitError || isCooldownActive ? (
              <p id="forgot-submit-error" role="alert" className="text-center text-sm font-medium text-[#ba1a1a]">
                {submitError ||
                  messageCatalog.auth.forgotPasswordCooldown(cooldownSeconds)}
              </p>
            ) : null}

            <button
              id="forgot-submit"
              type="submit"
              disabled={isSubmitting || isCooldownActive}
              className="flex h-14 w-full items-center justify-center rounded-xl bg-gradient-to-br from-[#00685f] to-[#008378] text-lg font-bold text-white shadow-lg transition hover:brightness-110 disabled:opacity-60"
            >
              {isSubmitting
                ? "Đang xử lý..."
                : isCooldownActive
                  ? `Thử lại sau ${cooldownSeconds} giây`
                  : "Gửi yêu cầu"}
            </button>
          </form>

          <div className="mt-8 border-t border-[#d8e5e2] pt-6 text-center">
            <p className="text-sm text-[#3d4947]">
              Nhớ mật khẩu?
              <Link href="/login" className="ml-1 font-bold text-[#00685f] hover:underline">
                Đăng nhập
              </Link>
            </p>
          </div>
        </>
      )}
    </AuthPageShell>
  );
}
