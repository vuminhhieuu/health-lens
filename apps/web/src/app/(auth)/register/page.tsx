"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { registerSchema } from "@healthlens/shared/schemas/auth";
import axios from "axios";
import { Eye, EyeOff, Info, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { AuthPageShell } from "@/components/auth/AuthPageShell";
import { InlineFieldError } from "@/components/ui/StateComponents";
import { apiBaseUrl } from "@/lib/api";
import { AUTH_PUBLIC_FOOTER_LINKS } from "@/lib/authPublicLinks";
import { messageCatalog, registerErrorMessage } from "@/lib/i18n/messages";

const POST_REGISTER_RETURN_URL_KEY = "post-register-return-url";

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

const registerPageSchema = registerSchema.extend({
  fullName: z.string().min(1, "Vui lòng nhập họ và tên"),
  birthDate: z.string().min(1, "Vui lòng chọn ngày sinh"),
  acceptedTerms: z.boolean().refine((value) => value, {
    message: "Bạn cần chấp nhận điều khoản sử dụng",
  }),
});

type RegisterPageInput = z.infer<typeof registerPageSchema>;

export default function RegisterPage() {
  const searchParams = typeof window !== "undefined" ? new URLSearchParams(window.location.search) : null;
  const inviteToken = searchParams?.get("inviteToken") ?? null;
  const returnUrl = typeof window !== "undefined" ? safeInternalReturnUrl(searchParams?.get("returnUrl") ?? null) : null;
  const inviteReturnUrl =
    inviteToken && !returnUrl
      ? `/health-record-invitations/accept?token=${encodeURIComponent(inviteToken)}`
      : returnUrl;
  const [successMessage, setSuccessMessage] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterPageInput>({
    resolver: zodResolver(registerPageSchema),
    mode: "onChange",
    defaultValues: {
      fullName: "",
      email: "",
      birthDate: "",
      password: "",
      confirmPassword: "",
      acceptedTerms: false,
    },
  });

  const onSubmit = async (data: RegisterPageInput) => {
    setSuccessMessage("");
    setSubmitError("");

    try {
      if (typeof window !== "undefined") {
        if (inviteReturnUrl) {
          window.localStorage.setItem(POST_REGISTER_RETURN_URL_KEY, inviteReturnUrl);
        } else {
          window.localStorage.removeItem(POST_REGISTER_RETURN_URL_KEY);
        }
      }

      await axios.post(`${apiBaseUrl}/api/v1/auth/register`, {
        fullName: data.fullName,
        email: data.email,
        birthDate: data.birthDate,
        password: data.password,
      });

      const message =
        inviteReturnUrl
          ? messageCatalog.auth.registerSuccessWithInvite
          : messageCatalog.auth.registerSuccess;
      setSuccessMessage(message);
    } catch (error: unknown) {
      const message = registerErrorMessage(error);
      setSubmitError(message);
    }
  };

  const termsLink = AUTH_PUBLIC_FOOTER_LINKS.find((link) => link.href === "/terms");

  return (
    <AuthPageShell
      footer
      cardClassName="w-full max-w-[500px] rounded-xl bg-white p-8 shadow-[0_8px_32px_rgba(18,30,28,0.08)] md:p-10"
      decoration={
        <div className="pointer-events-none fixed bottom-0 right-0 hidden p-8 opacity-10 lg:block">
          <ShieldCheck className="h-56 w-56 text-[#00685f]" />
        </div>
      }
    >
      <div className="mb-10 text-center">
            <h1 className="mb-2 text-3xl font-extrabold tracking-tight text-[#121e1c]">Tạo Tài Khoản</h1>
            <p className="text-base text-[#3d4947]">Bắt đầu quản lý sức khỏe của bạn</p>
            {inviteToken ? (
              <p className="mt-3 rounded-lg bg-[#eaf9f5] px-3 py-2 text-sm text-[#005049]">
                Bạn đang đăng ký từ lời mời chia sẻ hồ sơ gia đình.
              </p>
            ) : null}
          </div>

          <form className="space-y-6" onSubmit={handleSubmit(onSubmit)} noValidate>
            <div className="space-y-2">
              <label htmlFor="fullName" className="ml-1 block text-sm font-semibold text-[#3d4947]">
                Họ và tên
              </label>
              <input
                id="fullName"
                type="text"
                placeholder="Nguyễn Văn A"
                {...register("fullName")}
                aria-invalid={Boolean(errors.fullName)}
                aria-describedby={errors.fullName ? "register-full-name-error" : undefined}
                className="h-14 w-full rounded-t-lg border-b-2 border-transparent bg-[#d8e5e2] px-4 text-base text-[#121e1c] outline-none transition focus:border-[#00685f]"
              />
              <InlineFieldError id="register-full-name-error" message={errors.fullName?.message} />
            </div>

            <div className="space-y-2">
              <label htmlFor="email" className="ml-1 block text-sm font-semibold text-[#3d4947]">
                Email
              </label>
              <input
                id="email"
                type="email"
                placeholder="email@vi-du.com"
                {...register("email")}
                aria-invalid={Boolean(errors.email)}
                aria-describedby={errors.email ? "register-email-error" : undefined}
                className="h-14 w-full rounded-t-lg border-b-2 border-transparent bg-[#d8e5e2] px-4 text-base text-[#121e1c] outline-none transition focus:border-[#00685f]"
              />
              <InlineFieldError id="register-email-error" message={errors.email?.message} />
            </div>

            <div className="space-y-2">
              <label htmlFor="birthDate" className="ml-1 block text-sm font-semibold text-[#3d4947]">
                Ngày sinh
              </label>
              <input
                id="birthDate"
                type="date"
                {...register("birthDate")}
                aria-invalid={Boolean(errors.birthDate)}
                aria-describedby={errors.birthDate ? "register-birth-date-error" : undefined}
                className="h-14 w-full rounded-t-lg border-b-2 border-transparent bg-[#d8e5e2] px-4 text-base text-[#121e1c] outline-none transition focus:border-[#00685f]"
              />
              <InlineFieldError id="register-birth-date-error" message={errors.birthDate?.message} />
            </div>

            <div className="space-y-2">
              <label htmlFor="password" className="ml-1 block text-sm font-semibold text-[#3d4947]">
                Mật khẩu
              </label>
              <div className="relative">
                <input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  placeholder="••••••••"
                  {...register("password")}
                  aria-invalid={Boolean(errors.password)}
                  aria-describedby={
                    errors.password ? "register-password-error" : "register-password-hint"
                  }
                  className="h-14 w-full rounded-t-lg border-b-2 border-transparent bg-[#d8e5e2] px-4 pr-12 text-base text-[#121e1c] outline-none transition focus:border-[#00685f]"
                />
                <button
                  type="button"
                  aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full p-1 text-[#3d4947] transition hover:bg-[#c8d7d4]"
                >
                  {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                </button>
              </div>
              <p id="register-password-hint" className="ml-1 flex items-center gap-1 text-xs text-[#3d4947]">
                <Info className="h-3.5 w-3.5" aria-hidden="true" />
                Mật khẩu phải có ít nhất 8 ký tự
              </p>
              <InlineFieldError id="register-password-error" message={errors.password?.message} />
            </div>

            <div className="space-y-2">
              <label htmlFor="confirmPassword" className="ml-1 block text-sm font-semibold text-[#3d4947]">
                Xác nhận mật khẩu
              </label>
              <div className="relative">
                <input
                  id="confirmPassword"
                  type={showConfirmPassword ? "text" : "password"}
                  placeholder="••••••••"
                  {...register("confirmPassword")}
                  aria-invalid={Boolean(errors.confirmPassword)}
                  aria-describedby={errors.confirmPassword ? "register-confirm-password-error" : undefined}
                  className="h-14 w-full rounded-t-lg border-b-2 border-transparent bg-[#d8e5e2] px-4 pr-12 text-base text-[#121e1c] outline-none transition focus:border-[#00685f]"
                />
                <button
                  type="button"
                  aria-label={showConfirmPassword ? "Ẩn xác nhận mật khẩu" : "Hiện xác nhận mật khẩu"}
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full p-1 text-[#3d4947] transition hover:bg-[#c8d7d4]"
                >
                  {showConfirmPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                </button>
              </div>
              <InlineFieldError
                id="register-confirm-password-error"
                message={errors.confirmPassword?.message}
              />
            </div>

            <div className="flex items-start gap-3 py-1">
              <input
                id="acceptedTerms"
                type="checkbox"
                {...register("acceptedTerms")}
                aria-invalid={Boolean(errors.acceptedTerms)}
                aria-describedby={errors.acceptedTerms ? "register-accepted-terms-error" : undefined}
                className="mt-0.5 h-5 w-5 rounded border-[#6d7a77] text-[#00685f]"
              />
              <p className="text-sm leading-snug text-[#3d4947]">
                <label htmlFor="acceptedTerms" className="cursor-pointer">
                  Tôi đã đọc và chấp nhận
                </label>{" "}
                <Link
                  href={termsLink?.href ?? "/terms"}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="font-semibold text-[#00685f] hover:underline"
                >
                  Điều khoản sử dụng
                </Link>
              </p>
            </div>
            <InlineFieldError id="register-accepted-terms-error" message={errors.acceptedTerms?.message} />

            {successMessage ? (
              <p
                role="status"
                aria-live="polite"
                className="text-sm font-medium text-[#00685f]"
              >
                {successMessage}
              </p>
            ) : null}

            {submitError ? (
              <p
                role="alert"
                aria-live="assertive"
                className="text-sm font-medium text-[#ba1a1a]"
              >
                {submitError}
              </p>
            ) : null}

            <button
              type="submit"
              disabled={isSubmitting}
              className="h-14 w-full rounded-xl bg-gradient-to-br from-[#00685f] to-[#008378] text-lg font-bold text-white shadow-lg transition hover:brightness-110 disabled:opacity-60"
            >
              {isSubmitting ? "Đang xử lý..." : "Đăng Ký"}
            </button>
          </form>

          <div className="mt-8 border-t border-[#d8e5e2] pt-6 text-center">
            <p className="text-sm text-[#3d4947]">
              Đã có tài khoản?
              <a
                href={returnUrl ? `/login?returnUrl=${encodeURIComponent(returnUrl)}` : "/login"}
                className="ml-1 font-bold text-[#00685f] hover:underline"
              >
                Đăng Nhập
              </a>
            </p>
          </div>
    </AuthPageShell>
  );
}
