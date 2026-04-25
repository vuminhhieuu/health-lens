"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { forgotPasswordSchema } from "@healthlens/shared/schemas/auth";
import { CircleHelp, Mail, ArrowLeft, CheckCircle2, LockKeyhole, ArrowRight } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";

type ForgotPasswordInput = z.infer<typeof forgotPasswordSchema>;

export default function ForgotPasswordPage() {
  const [submitError, setSubmitError] = useState("");
  const [isSuccess, setIsSuccess] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordInput>({
    resolver: zodResolver(forgotPasswordSchema),
    mode: "onChange",
    defaultValues: {
      email: "",
    },
  });

  const onSubmit = async (data: ForgotPasswordInput) => {
    setSubmitError("");

    try {
      await apiClient.post(API_ROUTES.AUTH.FORGOT_PASSWORD, {
        email: data.email,
      });
      setIsSuccess(true);
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
          const retryAfter = resp.data?.retryAfterSeconds ?? 3600;
          const minutes = Math.ceil(retryAfter / 60);
          setSubmitError(
            `Bạn đã gửi yêu cầu quá nhiều. Vui lòng thử lại sau ${minutes} phút.`,
          );
        } else {
          setSubmitError("Đã có lỗi xảy ra. Vui lòng thử lại sau.");
        }
      } else {
        setSubmitError("Không thể kết nối đến máy chủ. Vui lòng thử lại.");
      }
    }
  };

  if (isSuccess) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#effcf9] via-[#e9f6f3] to-[#d8e5e2] text-[#121e1c] selection:bg-[#00685f]/10 selection:text-[#00685f]">
        {/* Header */}
        <header className="fixed top-0 z-50 w-full border-b border-[#d8e5e2] bg-[#effcf9]/80 backdrop-blur-md">
          <div className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between px-6">
            <Link href="/" className="text-2xl font-bold tracking-tight text-[#005049]">
              HealthLens
            </Link>
            <button
               type="button"
               aria-label="Trợ giúp"
               className="rounded-full p-2 text-[#3f6560] transition hover:bg-[#d8e5e2]"
            >
              <CircleHelp className="h-5 w-5" />
            </button>
          </div>
        </header>

        <main className="mx-auto flex min-h-screen w-full max-w-7xl items-center justify-center px-4 pb-12 pt-28">
          <section className="w-full max-w-[500px] transform transition-all duration-500 hover:scale-[1.01]">
            <div className="relative overflow-hidden rounded-[40px] bg-white p-10 shadow-[0_20px_50px_rgba(18,30,28,0.12)] border border-[#f0f4f3]">
              <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-[#effcf9] to-transparent rounded-bl-full opacity-50" />
              
              <div className="relative text-center">
                <div className="mb-8 flex justify-center">
                  <div className="relative">
                    <div className="absolute inset-0 bg-[#00685f]/20 rounded-full blur-2xl animate-pulse" />
                    <div className="relative h-24 w-24 rounded-full bg-gradient-to-br from-[#effcf9] to-[#dff6f1] flex items-center justify-center shadow-inner border border-white">
                      <CheckCircle2 className="h-12 w-12 text-[#00685f]" />
                    </div>
                  </div>
                </div>

                <h1 className="mb-4 text-3xl font-black tracking-tight text-[#121e1c]">
                  Kiểm tra email của bạn
                </h1>
                <p className="mb-8 text-lg leading-relaxed text-[#515b59]">
                  Chúng tôi đã gửi hướng dẫn đến email của bạn. Vui lòng kiểm tra hộp thư.
                </p>

                <div className="mb-10 rounded-2xl bg-[#effcf9] p-6 text-center shadow-inner border border-[#dff6f1]">
                  <p className="text-base italic text-[#3d4947]">
                    Không nhận được email? Hãy kiểm tra thư mục thư rác hoặc{" "}
                    <button
                      onClick={() => setIsSuccess(false)}
                      className="font-bold text-[#00685f] hover:underline"
                    >
                      gửi lại yêu cầu
                    </button>
                    .
                  </p>
                </div>

                <div className="space-y-4">
                  <Link
                    href="/login"
                    className="group flex h-16 w-full items-center justify-center gap-3 rounded-2xl bg-[#121e1c] text-lg font-bold text-white shadow-xl transition-all duration-300 hover:bg-[#263532] hover:-translate-y-1 active:translate-y-0"
                  >
                    <span>Quay lại đăng nhập</span>
                    <ArrowRight className="h-5 w-5 transition-transform group-hover:translate-x-1" />
                  </Link>
                </div>
              </div>
            </div>
          </section>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#effcf9] via-[#e9f6f3] to-[#d8e5e2] text-[#121e1c] selection:bg-[#00685f]/10 selection:text-[#00685f]">
      {/* Header */}
      <header className="fixed top-0 z-50 w-full border-b border-[#d8e5e2] bg-[#effcf9]/80 backdrop-blur-md">
        <div className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between px-6">
          <Link href="/" className="text-2xl font-bold tracking-tight text-[#005049]">
            HealthLens
          </Link>
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
      <main className="mx-auto flex min-h-screen w-full max-w-7xl items-center justify-center px-4 pb-12 pt-28">
        <section className="w-full max-w-[500px] transform transition-all duration-500">
          <div className="relative overflow-hidden rounded-[40px] bg-white p-10 shadow-[0_20px_50px_rgba(18,30,28,0.12)] border border-[#f0f4f3]">
            {/* Context Back Link */}
            <Link
              href="/login"
              className="group mb-10 inline-flex items-center gap-2 text-sm font-bold text-[#00685f] transition-all hover:translate-x-[-4px]"
            >
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-[#effcf9] group-hover:bg-[#00685f] group-hover:text-white transition-colors duration-300">
                <ArrowLeft className="h-4 w-4" />
              </div>
              <span>Quay lại đăng nhập</span>
            </Link>

            {/* Header Content */}
            <div className="mb-10 text-center">
              <div className="mb-6 flex justify-center">
                <div className="h-20 w-20 rounded-[24px] bg-gradient-to-br from-[#effcf9] to-[#dff6f1] flex items-center justify-center shadow-sm border border-white">
                  <LockKeyhole className="h-10 w-10 text-[#00685f]" />
                </div>
              </div>
              <h1 className="mb-3 text-4xl font-black tracking-tight text-[#121e1c]">
                Quên mật khẩu?
              </h1>
              <p className="text-lg text-[#515b59]">
                Nhập email của bạn để nhận hướng dẫn khôi phục mật khẩu.
              </p>
            </div>

            {/* Form */}
            <form
              className="space-y-6"
              onSubmit={handleSubmit(onSubmit)}
              noValidate
            >
              <div className="space-y-2">
                <label
                  htmlFor="forgot-email"
                  className="ml-1 block text-xs font-black uppercase tracking-widest text-[#515b59]/60"
                >
                  Email đăng ký
                </label>
                <div className="relative group">
                  <input
                    id="forgot-email"
                    type="email"
                    placeholder="example@email.com"
                    {...register("email")}
                    className="h-16 w-full rounded-2xl bg-[#f0f4f3]/50 border-2 border-transparent px-6 pl-14 text-lg font-medium text-[#121e1c] outline-none transition-all duration-300 focus:bg-white focus:border-[#00685f] focus:shadow-[0_0_20px_rgba(0,104,95,0.1)]"
                  />
                  <Mail className="absolute left-5 top-1/2 h-6 w-6 -translate-y-1/2 text-[#515b59]/40 group-focus-within:text-[#00685f] transition-colors" />
                </div>
                {errors.email ? (
                  <p className="ml-1 text-sm font-bold text-[#ba1a1a]">
                    {errors.email.message}
                  </p>
                ) : null}
              </div>

              {/* Submit Button */}
              <button
                id="forgot-submit"
                type="submit"
                disabled={isSubmitting}
                className="group relative flex h-16 w-full items-center justify-center overflow-hidden rounded-2xl bg-gradient-to-r from-[#00685f] to-[#008378] text-lg font-black text-white shadow-[0_10px_25px_rgba(0,104,95,0.3)] transition-all duration-300 hover:scale-[1.02] active:scale-[0.98] disabled:opacity-50 disabled:scale-100"
              >
                <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/20 to-white/0 translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-700 pointer-events-none" />
                <span className="relative flex items-center gap-2">
                  {isSubmitting ? "Đang xử lý..." : "Gửi yêu cầu"}
                  <ArrowRight className="h-5 w-5 transition-transform group-hover:translate-x-1" />
                </span>
              </button>
            </form>

            {submitError ? (
              <div className="mt-6 flex items-center gap-2 justify-center p-3 rounded-xl bg-[#ba1a1a]/5 text-[#ba1a1a] text-sm font-bold">
                <p>{submitError}</p>
              </div>
            ) : null}

            {/* Additional Links */}
            <div className="mt-10 flex items-center justify-between border-t border-[#f0f4f3] pt-6">
              <Link href="/privacy" className="text-xs font-bold text-[#515b59] hover:text-[#00685f] transition-colors uppercase tracking-widest">
                Riêng tư
              </Link>
              <div className="h-1 w-1 rounded-full bg-[#f0f4f3]" />
              <Link href="/terms" className="text-xs font-bold text-[#515b59] hover:text-[#00685f] transition-colors uppercase tracking-widest">
                Điều khoản
              </Link>
              <div className="h-1 w-1 rounded-full bg-[#f0f4f3]" />
              <Link href="/contact" className="text-xs font-bold text-[#515b59] hover:text-[#00685f] transition-colors uppercase tracking-widest">
                Hỗ trợ
              </Link>
            </div>
          </div>
        </section>
      </main>

      {/* Decoration background blobs */}
      <div className="fixed -bottom-20 -left-20 w-80 h-80 bg-[#00685f]/5 rounded-full blur-3xl pointer-events-none" />
      <div className="fixed -top-20 -right-20 w-80 h-80 bg-[#008378]/5 rounded-full blur-3xl pointer-events-none" />
    </div>
  );
}
