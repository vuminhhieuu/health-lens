"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { resetPasswordSchema } from "@healthlens/shared/schemas/auth";
import { CheckCircle2, Eye, EyeOff, Lock, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { AuthPageShell } from "@/components/auth/AuthPageShell";
import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { getApiErrorStatus, messageCatalog } from "@/lib/i18n/messages";
import { notify } from "@/lib/notify";

type ResetPasswordInput = z.infer<typeof resetPasswordSchema>;

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-[#effcf9]" />}>
      <ResetPasswordContent />
    </Suspense>
  );
}

function ResetPasswordContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token") || "";

  const [submitError, setSubmitError] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordInput>({
    resolver: zodResolver(resetPasswordSchema),
    mode: "onChange",
    defaultValues: {
      token: token,
      newPassword: "",
      confirmPassword: "",
    },
  });

  const onSubmit = async (data: ResetPasswordInput) => {
    setSubmitError("");

    if (!data.token) {
      const message = messageCatalog.auth.resetTokenInvalid;
      setSubmitError(message);
      notify.error(message);
      return;
    }

    try {
      await apiClient.post(API_ROUTES.AUTH.RESET_PASSWORD, {
        token: data.token,
        newPassword: data.newPassword,
      });
      setIsSuccess(true);
      notify.success(messageCatalog.auth.resetPasswordSuccess);
      setTimeout(() => {
        router.push("/login");
      }, 3000);
    } catch (error: unknown) {
      const status = getApiErrorStatus(error);
      const message =
        status === 400 || status === 401
          ? messageCatalog.auth.resetTokenInvalid
          : status
            ? messageCatalog.auth.genericRetry
            : messageCatalog.auth.networkError;
      setSubmitError(message);
      notify.error(message);
    }
  };

  const shellDecoration = (
    <div className="pointer-events-none fixed bottom-0 right-0 hidden p-8 opacity-10 lg:block">
      <ShieldCheck className="h-56 w-56 text-[#00685f]" />
    </div>
  );

  if (!token && !isSuccess) {
    return (
      <AuthPageShell footer decoration={shellDecoration}>
        <div className="text-center">
          <h1 className="mb-4 text-2xl font-bold text-[#ba1a1a]">Lỗi Truy Cập</h1>
          <p className="mb-6 text-[#3d4947]">
            Liên kết đặt lại mật khẩu của bạn không hợp lệ hoặc đã hết hạn.
          </p>
          <Link href="/forgot-password" className="font-bold text-[#00685f] hover:underline">
            Yêu cầu liên kết mới
          </Link>
        </div>
      </AuthPageShell>
    );
  }

  if (isSuccess) {
    return (
      <AuthPageShell footer decoration={shellDecoration}>
        <div className="text-center">
          <div
            role="status"
            aria-live="polite"
            className="mb-6 flex flex-col items-center gap-4"
          >
            <div className="rounded-full bg-[#effcf9] p-4">
              <CheckCircle2 className="h-12 w-12 text-[#00685f]" aria-hidden />
            </div>
            <h1 className="text-2xl font-extrabold tracking-tight text-[#121e1c]">
              Mật khẩu đã được đặt lại
            </h1>
            <p className="text-[#3d4947]">
              Mật khẩu của bạn đã được cập nhật thành công. Đang chuyển hướng bạn về trang đăng
              nhập...
            </p>
          </div>
          <Link
            href="/login"
            className="flex h-14 w-full items-center justify-center rounded-xl bg-gradient-to-br from-[#00685f] to-[#008378] text-lg font-bold text-white shadow-lg transition hover:brightness-110"
          >
            Về trang Đăng Nhập
          </Link>
        </div>
      </AuthPageShell>
    );
  }

  return (
    <AuthPageShell footer decoration={shellDecoration}>
      <div className="mb-10 text-center">
        <h1 className="mb-2 text-3xl font-extrabold tracking-tight text-[#121e1c]">
          Đặt Lại Mật Khẩu
        </h1>
        <p className="text-base text-[#3d4947]">Nhập mật khẩu mới cho tài khoản của bạn</p>
      </div>

      <form className="space-y-6" onSubmit={handleSubmit(onSubmit)} noValidate>
        <input type="hidden" {...register("token")} />

        <div className="space-y-2">
          <label htmlFor="reset-password" className="ml-1 block text-sm font-semibold text-[#3d4947]">
            Mật khẩu mới
          </label>
          <div className="relative">
            <input
              id="reset-password"
              type={showPassword ? "text" : "password"}
              autoComplete="new-password"
              placeholder="••••••••"
              {...register("newPassword")}
              className="h-14 w-full rounded-t-lg border-b-2 border-transparent bg-[#d8e5e2] px-4 pl-12 pr-12 text-base text-[#121e1c] outline-none transition focus:border-[#00685f]"
            />
            <Lock className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-[#3d4947]" />
            <button
              type="button"
              aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full p-1 text-[#3d4947] transition hover:bg-[#c8d7d4]"
            >
              {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
            </button>
          </div>
          {errors.newPassword ? (
            <p className="text-sm text-[#ba1a1a]">{errors.newPassword.message}</p>
          ) : null}
        </div>

        <div className="space-y-2">
          <label
            htmlFor="reset-confirm-password"
            className="ml-1 block text-sm font-semibold text-[#3d4947]"
          >
            Xác nhận mật khẩu mới
          </label>
          <div className="relative">
            <input
              id="reset-confirm-password"
              type={showPassword ? "text" : "password"}
              autoComplete="new-password"
              placeholder="••••••••"
              {...register("confirmPassword")}
              className="h-14 w-full rounded-t-lg border-b-2 border-transparent bg-[#d8e5e2] px-4 pl-12 pr-12 text-base text-[#121e1c] outline-none transition focus:border-[#00685f]"
            />
            <Lock className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-[#3d4947]" />
          </div>
          {errors.confirmPassword ? (
            <p className="text-sm text-[#ba1a1a]">{errors.confirmPassword.message}</p>
          ) : null}
        </div>

        {submitError ? (
          <p role="alert" className="text-center text-sm font-medium text-[#ba1a1a]">
            {submitError}
          </p>
        ) : null}

        <button
          id="reset-submit"
          type="submit"
          disabled={isSubmitting}
          className="flex h-14 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-br from-[#00685f] to-[#008378] text-lg font-bold text-white shadow-lg transition hover:brightness-110 disabled:opacity-60"
        >
          {isSubmitting ? "Đang xử lý..." : "Cập nhật mật khẩu"}
        </button>
      </form>
    </AuthPageShell>
  );
}
