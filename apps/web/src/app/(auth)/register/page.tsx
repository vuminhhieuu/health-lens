"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { registerSchema } from "@healthlens/shared/schemas/auth";
import axios from "axios";
import { CalendarDays, CircleHelp, Info, ShieldCheck } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { apiBaseUrl } from "@/lib/api";

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
  const returnUrl = searchParams?.get("returnUrl") ?? null;
  const [successMessage, setSuccessMessage] = useState("");
  const [submitError, setSubmitError] = useState("");

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
      await axios.post(`${apiBaseUrl}/api/v1/auth/register`, {
        fullName: data.fullName,
        email: data.email,
        birthDate: data.birthDate,
        password: data.password,
      });

      setSuccessMessage("Tài khoản đã tạo. Vui lòng kiểm tra email của bạn để xác thực.");
    } catch {
      setSubmitError("Đăng ký thất bại. Vui lòng thử lại.");
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#effcf9] via-[#e9f6f3] to-[#d8e5e2] text-[#121e1c]">
      <header className="fixed top-0 z-50 w-full border-b border-[#d8e5e2] bg-[#effcf9]/80 backdrop-blur-md">
        <div className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between px-6">
          <p className="text-2xl font-bold tracking-tight text-[#005049]">HealthLens</p>
          <button
            type="button"
            aria-label="Trợ giúp"
            className="rounded-full p-2 text-[#3f6560] transition hover:bg-[#d8e5e2]"
          >
            <CircleHelp className="h-5 w-5" />
          </button>
        </div>
      </header>

      <main className="mx-auto flex min-h-screen w-full max-w-7xl items-center justify-center px-4 pb-12 pt-24">
        <section className="w-full max-w-[500px] rounded-xl bg-white p-8 shadow-[0_8px_32px_rgba(18,30,28,0.08)] md:p-10">
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
                className="h-14 w-full rounded-t-lg border-b-2 border-transparent bg-[#d8e5e2] px-4 text-base text-[#121e1c] outline-none transition focus:border-[#00685f]"
              />
              {errors.fullName ? <p className="text-sm text-[#ba1a1a]">{errors.fullName.message}</p> : null}
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
                className="h-14 w-full rounded-t-lg border-b-2 border-transparent bg-[#d8e5e2] px-4 text-base text-[#121e1c] outline-none transition focus:border-[#00685f]"
              />
              {errors.email ? <p className="text-sm text-[#ba1a1a]">{errors.email.message}</p> : null}
            </div>

            <div className="space-y-2">
              <label htmlFor="birthDate" className="ml-1 block text-sm font-semibold text-[#3d4947]">
                Ngày sinh
              </label>
              <div className="relative">
                <input
                  id="birthDate"
                  type="date"
                  {...register("birthDate")}
                  className="h-14 w-full rounded-t-lg border-b-2 border-transparent bg-[#d8e5e2] px-4 pr-12 text-base text-[#121e1c] outline-none transition focus:border-[#00685f]"
                />
                <CalendarDays className="pointer-events-none absolute right-4 top-1/2 h-5 w-5 -translate-y-1/2 text-[#3d4947]" />
              </div>
              {errors.birthDate ? <p className="text-sm text-[#ba1a1a]">{errors.birthDate.message}</p> : null}
            </div>

            <div className="space-y-2">
              <label htmlFor="password" className="ml-1 block text-sm font-semibold text-[#3d4947]">
                Mật khẩu
              </label>
              <input
                id="password"
                type="password"
                placeholder="••••••••"
                {...register("password")}
                className="h-14 w-full rounded-t-lg border-b-2 border-transparent bg-[#d8e5e2] px-4 text-base text-[#121e1c] outline-none transition focus:border-[#00685f]"
              />
              <p className="ml-1 flex items-center gap-1 text-xs text-[#3d4947]">
                <Info className="h-3.5 w-3.5" />
                Mật khẩu phải có ít nhất 8 ký tự
              </p>
              {errors.password ? <p className="text-sm text-[#ba1a1a]">{errors.password.message}</p> : null}
            </div>

            <div className="space-y-2">
              <label htmlFor="confirmPassword" className="ml-1 block text-sm font-semibold text-[#3d4947]">
                Xác nhận mật khẩu
              </label>
              <input
                id="confirmPassword"
                type="password"
                placeholder="••••••••"
                {...register("confirmPassword")}
                className="h-14 w-full rounded-t-lg border-b-2 border-transparent bg-[#d8e5e2] px-4 text-base text-[#121e1c] outline-none transition focus:border-[#00685f]"
              />
              {errors.confirmPassword ? <p className="text-sm text-[#ba1a1a]">{errors.confirmPassword.message}</p> : null}
            </div>

            <label htmlFor="acceptedTerms" className="flex cursor-pointer items-start gap-3 py-1">
              <input
                id="acceptedTerms"
                type="checkbox"
                {...register("acceptedTerms")}
                className="mt-0.5 h-5 w-5 rounded border-[#6d7a77] text-[#00685f]"
              />
              <span className="text-sm leading-snug text-[#3d4947]">
                Tôi đã đọc và chấp nhận{" "}
                <button type="button" className="font-semibold text-[#00685f] hover:underline">
                  Điều khoản sử dụng
                </button>
              </span>
            </label>
            {errors.acceptedTerms ? <p className="text-sm text-[#ba1a1a]">{errors.acceptedTerms.message}</p> : null}

            <button
              type="submit"
              disabled={isSubmitting}
              className="h-14 w-full rounded-xl bg-gradient-to-br from-[#00685f] to-[#008378] text-lg font-bold text-white shadow-lg transition hover:brightness-110 disabled:opacity-60"
            >
              {isSubmitting ? "Đang xử lý..." : "Đăng Ký"}
            </button>
          </form>

          {successMessage ? <p className="mt-4 text-sm text-[#00685f]">{successMessage}</p> : null}
          {submitError ? <p className="mt-4 text-sm text-[#ba1a1a]">{submitError}</p> : null}

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
        </section>
      </main>

      <div className="pointer-events-none fixed bottom-0 right-0 hidden p-8 opacity-10 lg:block">
        <ShieldCheck className="h-56 w-56 text-[#00685f]" />
      </div>
    </div>
  );
}
