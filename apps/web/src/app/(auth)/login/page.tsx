"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { loginSchema } from "@healthlens/shared/schemas/auth";
import { CircleHelp, Eye, EyeOff, LogIn, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState, Suspense } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { ApiPaths } from "@healthlens/shared/constants";

import { syncActiveConsentVersion } from "@/lib/consent/syncActiveConsentVersion";
import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { useAuthStore } from "@/stores/authStore";

type LoginInput = z.infer<typeof loginSchema>;

export default function LoginPage() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-[#effcf9]" />}>
      <LoginContent />
    </Suspense>
  );
}

function LoginContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const setAuth = useAuthStore((s) => s.setAuth);
  const setConsentState = useAuthStore((s) => s.setConsentState);

  const [submitError, setSubmitError] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginInput>({
    resolver: zodResolver(loginSchema),
    mode: "onChange",
    defaultValues: {
      email: "",
      password: "",
    },
  });

  const onSubmit = async (data: LoginInput) => {
    setSubmitError("");

    try {
      const response = await apiClient.post(API_ROUTES.AUTH.LOGIN, {
        email: data.email,
        password: data.password,
      });

      const { accessToken, user } = response.data.data;

      setAuth(
        { id: String(user.id), email: user.email, role: user.role },
        accessToken,
      );

      try {
        await syncActiveConsentVersion().catch(() => {});
        const consentRes = await apiClient.get<{
          consentGiven?: boolean;
          consentVersion?: string | null;
        }>(ApiPaths.CONSENT.ME);
        const body = consentRes.data;
        const cg = body?.consentGiven ?? false;
        const cv =
          body?.consentVersion === undefined || body?.consentVersion === null
            ? null
            : String(body.consentVersion);
        setConsentState(cg, cv);
      } catch {
        // Keep consent reset from setAuth until next refresh/bootstrap.
      }

      const returnUrl = searchParams.get("returnUrl") || "/";
      router.push(returnUrl);
    } catch (error: unknown) {
      if (
        error &&
        typeof error === "object" &&
        "response" in error &&
        error.response &&
        typeof error.response === "object"
      ) {
        const resp = error.response as {
          status?: number;
          data?: { detail?: string; retryAfterSeconds?: number };
        };
        if (resp.status === 429) {
          const retryAfter = resp.data?.retryAfterSeconds ?? 900;
          const minutes = Math.ceil(retryAfter / 60);
          setSubmitError(
            `Tài khoản bị khóa tạm thời. Thử lại sau ${minutes} phút.`,
          );
        } else if (resp.status === 401) {
          setSubmitError("Email hoặc mật khẩu không đúng.");
        } else {
          setSubmitError("Đăng nhập thất bại. Vui lòng thử lại.");
        }
      } else {
        setSubmitError("Không thể kết nối đến máy chủ. Vui lòng thử lại.");
      }
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#effcf9] via-[#e9f6f3] to-[#d8e5e2] text-[#121e1c]">
      {/* Header */}
      <header className="fixed top-0 z-50 w-full border-b border-[#d8e5e2] bg-[#effcf9]/80 backdrop-blur-md">
        <div className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between px-6">
          <p className="text-2xl font-bold tracking-tight text-[#005049]">
            HealthLens
          </p>
          <button
            type="button"
            aria-label="Trợ giúp"
            className="rounded-full p-2 text-[#3f6560] transition hover:bg-[#d8e5e2]"
          >
            <CircleHelp className="h-5 w-5" />
          </button>
        </div>
      </header>

      {/* Main */}
      <main className="mx-auto flex min-h-screen w-full max-w-7xl items-center justify-center px-4 pb-12 pt-24">
        <section className="w-full max-w-[460px] rounded-xl bg-white p-8 shadow-[0_8px_32px_rgba(18,30,28,0.08)] md:p-10">
          {/* Title */}
          <div className="mb-10 text-center">
            <h1 className="mb-2 text-3xl font-extrabold tracking-tight text-[#121e1c]">
              Đăng Nhập
            </h1>
            <p className="text-base text-[#3d4947]">
              Quản lý sức khỏe của bạn
            </p>
          </div>

          {/* Form */}
          <form
            className="space-y-6"
            onSubmit={handleSubmit(onSubmit)}
            noValidate
          >
            {/* Email */}
            <div className="space-y-2">
              <label
                htmlFor="login-email"
                className="ml-1 block text-sm font-semibold text-[#3d4947]"
              >
                Email
              </label>
              <input
                id="login-email"
                type="email"
                autoComplete="email"
                placeholder="email@vi-du.com"
                {...register("email")}
                className="h-14 w-full rounded-t-lg border-b-2 border-transparent bg-[#d8e5e2] px-4 text-base text-[#121e1c] outline-none transition focus:border-[#00685f]"
              />
              {errors.email ? (
                <p className="text-sm text-[#ba1a1a]">
                  {errors.email.message}
                </p>
              ) : null}
            </div>

            {/* Password */}
            <div className="space-y-2">
              <label
                htmlFor="login-password"
                className="ml-1 block text-sm font-semibold text-[#3d4947]"
              >
                Mật khẩu
              </label>
              <div className="relative">
                <input
                  id="login-password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  placeholder="••••••••"
                  {...register("password")}
                  className="h-14 w-full rounded-t-lg border-b-2 border-transparent bg-[#d8e5e2] px-4 pr-12 text-base text-[#121e1c] outline-none transition focus:border-[#00685f]"
                />
                <button
                  type="button"
                  aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full p-1 text-[#3d4947] transition hover:bg-[#c8d7d4]"
                >
                  {showPassword ? (
                    <EyeOff className="h-5 w-5" />
                  ) : (
                    <Eye className="h-5 w-5" />
                  )}
                </button>
              </div>
              {errors.password ? (
                <p className="text-sm text-[#ba1a1a]">
                  {errors.password.message}
                </p>
              ) : null}
            </div>

            {/* Forgot password */}
            <div className="text-right">
              <Link
                href="/forgot-password"
                className="text-sm font-semibold text-[#00685f] transition hover:underline"
              >
                Quên mật khẩu?
              </Link>
            </div>

            {/* Submit */}
            <button
              id="login-submit"
              type="submit"
              disabled={isSubmitting}
              className="flex h-14 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-br from-[#00685f] to-[#008378] text-lg font-bold text-white shadow-lg transition hover:brightness-110 disabled:opacity-60"
            >
              {isSubmitting ? (
                "Đang xử lý..."
              ) : (
                <>
                  <LogIn className="h-5 w-5" />
                  Đăng Nhập
                </>
              )}
            </button>
          </form>

          {/* Error message */}
          {submitError ? (
            <p className="mt-4 text-center text-sm text-[#ba1a1a]">
              {submitError}
            </p>
          ) : null}

          {/* Register link */}
          <div className="mt-8 border-t border-[#d8e5e2] pt-6 text-center">
            <p className="text-sm text-[#3d4947]">
              Chưa có tài khoản?
              <Link
                href="/register"
                className="ml-1 font-bold text-[#00685f] hover:underline"
              >
                Đăng Ký
              </Link>
            </p>
          </div>

          {/* Footer links (Stitch reference) */}
          <div className="mt-6 flex flex-wrap items-center justify-center gap-4 text-xs text-[#6d7a77]">
            <Link href="#" className="transition hover:text-[#00685f]">
              Quy định bảo mật
            </Link>
            <span className="text-[#d8e5e2]">|</span>
            <Link href="#" className="transition hover:text-[#00685f]">
              Điều khoản sử dụng
            </Link>
            <span className="text-[#d8e5e2]">|</span>
            <Link href="#" className="transition hover:text-[#00685f]">
              Trợ giúp
            </Link>
          </div>
        </section>
      </main>

      {/* Decorative background icon */}
      <div className="pointer-events-none fixed bottom-0 right-0 hidden p-8 opacity-10 lg:block">
        <ShieldCheck className="h-56 w-56 text-[#00685f]" />
      </div>
    </div>
  );
}
