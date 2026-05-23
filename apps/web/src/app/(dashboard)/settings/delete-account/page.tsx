"use client";

import React, { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import type { ComponentProps } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  AlertTriangle,
  CheckCircle2,
  FileText,
  FileUp,
  FolderOpen,
  Info,
  Shield,
  ShieldCheck,
  Stethoscope,
  Trash2,
  UserRound,
} from "lucide-react";

import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { breadcrumbFromSettings } from "@/lib/layout/dashboardBreadcrumbTrails";
import { useAccountDeletion } from "@/hooks/useAccountDeletion";
import { useAuthStore } from "@/stores/authStore";

import { SettingsAccountSidebar } from "../_components/SettingsAccountSidebar";
import { SettingsDirectContactCard } from "../_components/SettingsDirectContactCard";
import { settingsCardClassName, settingsTipCardClassName } from "../_components/settingsStyles";

const passwordInputClassName =
  "h-12 w-full rounded-xl border-none bg-[#e9f6f3] px-4 text-sm font-medium text-[#121e1c] placeholder:text-[#9ba9a6] outline-none transition focus:ring-2 focus:ring-[#00685f]/20";

export default function DeleteAccountPage() {
  const router = useRouter();
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const {
    requestDeletion,
    isRequesting,
    error: hookError,
  } = useAccountDeletion();
  const [password, setPassword] = useState("");
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [hasReadConsequences, setHasReadConsequences] = useState(false);
  const [requestSent, setRequestSent] = useState(false);
  const [secondsLeft, setSecondsLeft] = useState(6);
  const redirectScheduledRef = useRef(false);

  useEffect(() => {
    if (!requestSent) return;

    const countdownInterval = window.setInterval(() => {
      setSecondsLeft((current) => (current > 1 ? current - 1 : 1));
    }, 1000);
    const redirectTimeout = window.setTimeout(() => {
      clearAuth();
      window.location.replace("/login?deleted=true");
    }, 6000);

    return () => {
      window.clearInterval(countdownInterval);
      window.clearTimeout(redirectTimeout);
    };
  }, [requestSent, clearAuth]);

  useEffect(() => {
    if (!requestSent) return;
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prevOverflow;
    };
  }, [requestSent]);

  const handleSubmit: NonNullable<ComponentProps<"form">["onSubmit"]> = async (
    e,
  ) => {
    e.preventDefault();
    if (!hasReadConsequences) {
      setPasswordError(
        "Vui lòng xác nhận bạn đã hiểu hậu quả của việc xóa tài khoản",
      );
      return;
    }
    if (!password.trim()) {
      setPasswordError("Vui lòng nhập mật khẩu");
      return;
    }
    setPasswordError(null);
    try {
      await requestDeletion(password);
      if (!redirectScheduledRef.current) {
        redirectScheduledRef.current = true;
        setSecondsLeft(6);
      }
      setPassword("");
      setRequestSent(true);
    } catch {
      // Hook provides `hookError` displayed by the UI
    }
  };

  return (
    <>
      <DashboardPageShell
        title="Xóa tài khoản"
        subtitle="Thực hiện quyền xóa dữ liệu theo Nghị định 13/2023/NĐ-CP. Hành động này không thể hoàn tác sau thời gian chờ."
        breadcrumbs={breadcrumbFromSettings("Xóa tài khoản")}
      >
        <div
          className={
            requestSent
              ? "pointer-events-none select-none opacity-0"
              : "grid grid-cols-1 items-start gap-8 lg:grid-cols-3"
          }
          aria-hidden={requestSent}
        >
          <div className="space-y-8 lg:col-span-2">
            <section className={`${settingsCardClassName} relative overflow-hidden`}>
              <div className="absolute top-0 right-0 -mt-16 -mr-16 h-32 w-32 rounded-bl-full bg-[#ba1a1a]/5" />

              <div className="relative mb-8 flex flex-col gap-6 md:flex-row md:items-start">
                <div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-2xl bg-[#fff8f7] text-[#ba1a1a] ring-4 ring-[#fff8f7] shadow-md">
                  <Trash2 className="h-10 w-10" aria-hidden="true" />
                </div>
                <div className="min-w-0 flex-grow">
                  <h2 className="text-2xl font-bold text-[#121e1c]">Yêu cầu xóa tài khoản</h2>
                  <p className="mt-2 text-sm leading-6 text-[#6d7a77]">
                    Chúng tôi rất tiếc khi thấy bạn rời đi. Hoàn thành các bước bên dưới để gửi yêu cầu
                    xóa dữ liệu. Tài khoản sẽ bị khóa đăng nhập ngay và xóa vĩnh viễn sau tối đa 72 giờ.
                  </p>
                </div>
              </div>

              <form onSubmit={handleSubmit} className="relative space-y-8">
                <div className="rounded-xl border-l-4 border-[#ba1a1a] bg-[#ffdad6]/30 p-6">
                  <div className="flex gap-4">
                    <AlertTriangle className="h-5 w-5 shrink-0 text-[#ba1a1a]" aria-hidden="true" />
                    <div>
                      <h3 className="mb-2 text-lg font-bold text-[#93000a]">Cảnh báo quan trọng</h3>
                      <ul className="list-disc space-y-2 pl-4 text-sm leading-relaxed text-[#93000a]">
                        <li>
                          Hành động này là <strong>vĩnh viễn</strong> và không thể hoàn tác.
                        </li>
                        <li>
                          Tất cả dữ liệu y tế, lịch sử khám bệnh và hồ sơ sẽ bị xóa sạch khỏi hệ thống.
                        </li>
                        <li>
                          Tài khoản của bạn sẽ bị tạm khóa trong{" "}
                          <strong>72 giờ</strong> trước khi bị xóa vĩnh viễn.
                        </li>
                      </ul>
                    </div>
                  </div>
                </div>

                <div className="space-y-4">
                  <h3 className="text-sm font-bold uppercase tracking-widest text-[#3d4947]">
                    Dữ liệu sẽ bị xóa bao gồm
                  </h3>
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div className="flex items-center gap-3 rounded-xl bg-[#e9f6f3] p-4">
                      <UserRound className="h-5 w-5 text-[#3f6560]" aria-hidden="true" />
                      <span className="text-sm font-medium">Thông tin định danh (PII)</span>
                    </div>
                    <div className="flex items-center gap-3 rounded-xl bg-[#e9f6f3] p-4">
                      <Stethoscope className="h-5 w-5 text-[#3f6560]" aria-hidden="true" />
                      <span className="text-sm font-medium">Hồ sơ bệnh án &amp; Vitals</span>
                    </div>
                    <div className="flex items-center gap-3 rounded-xl bg-[#e9f6f3] p-4">
                      <FileUp className="h-5 w-5 text-[#3f6560]" aria-hidden="true" />
                      <span className="text-sm font-medium">Tệp đính kèm &amp; Kết quả xét nghiệm</span>
                    </div>
                    <div className="flex items-center gap-3 rounded-xl bg-[#e9f6f3] p-4">
                      <ShieldCheck className="h-5 w-5 text-[#3f6560]" aria-hidden="true" />
                      <span className="text-sm font-medium">Nhật ký đồng thuận dữ liệu</span>
                    </div>
                  </div>
                </div>

                <label
                  htmlFor="delete-account-ack-consequences"
                  className="group flex cursor-pointer items-start gap-4 rounded-xl bg-[#e9f6f3] p-4 transition-colors hover:bg-[#d8ebe6]"
                >
                  <input
                    id="delete-account-ack-consequences"
                    type="checkbox"
                    checked={hasReadConsequences}
                    onChange={(e) => setHasReadConsequences(e.target.checked)}
                    className="mt-1 h-5 w-5 shrink-0 rounded border-[#6d7a77]"
                  />
                  <span className="text-sm leading-relaxed text-[#3d4947]">
                    Tôi đã hiểu và chấp nhận hậu quả của việc xóa tài khoản. Tôi xác nhận rằng HealthLens
                    đã thông báo đầy đủ về các quyền của tôi theo quy định pháp luật về bảo vệ dữ liệu cá
                    nhân.
                  </span>
                </label>

                <div className="space-y-6 border-t border-[#deebe8] pt-8">
                  <p className="text-sm font-bold uppercase tracking-wider text-[#00685f]">
                    Bước 2 — Xác nhận danh tính
                  </p>
                  <div>
                    <label
                      htmlFor="delete-account-password"
                      className="mb-2 block text-sm font-bold text-[#6d7a77]"
                    >
                      Mật khẩu hiện tại
                    </label>
                    <input
                      id="delete-account-password"
                      type="password"
                      autoComplete="current-password"
                      value={password}
                      onChange={(e) => {
                        setPassword(e.target.value);
                        if (passwordError) setPasswordError(null);
                      }}
                      placeholder="Nhập mật khẩu của bạn"
                      className={passwordInputClassName}
                    />
                  </div>

                  {(hookError || passwordError) && (
                    <div
                      role="alert"
                      aria-live="assertive"
                      className="flex items-center gap-3 rounded-xl border border-[#ba1a1a]/20 bg-[#ffebee] p-4 text-[#ba1a1a]"
                    >
                      <AlertTriangle className="h-5 w-5 shrink-0" />
                      <p className="text-sm font-semibold leading-5">
                        {hookError || passwordError}
                      </p>
                    </div>
                  )}

                  <div className="flex flex-col gap-4 sm:flex-row">
                    <button
                      type="submit"
                      disabled={isRequesting}
                      className="inline-flex min-h-12 flex-1 items-center justify-center gap-2 rounded-xl bg-[#ba1a1a] text-sm font-bold text-white shadow-md transition hover:opacity-90 disabled:opacity-60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#ba1a1a]"
                    >
                      <Trash2 className="h-5 w-5" aria-hidden="true" />
                      {isRequesting ? "Đang xử lý..." : "Gửi yêu cầu xóa"}
                    </button>
                    <button
                      type="button"
                      onClick={() => router.back()}
                      className="inline-flex min-h-12 flex-1 items-center justify-center rounded-xl bg-[#deebe8] text-sm font-bold text-[#3d4947] transition hover:bg-[#d8e5e2] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
                    >
                      Hủy bỏ
                    </button>
                  </div>
                </div>
              </form>
            </section>
          </div>

          <aside className="space-y-8">
            <SettingsAccountSidebar />
            <section className={settingsTipCardClassName}>
              <div className="relative z-10">
                <div className="mb-6 flex items-center gap-3">
                  <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-white/50 backdrop-blur-md">
                    <Info className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
                  </div>
                  <h3 className="text-xl font-bold text-[#121e1c]">Thời gian chờ 72 giờ</h3>
                </div>
                <p className="text-sm leading-relaxed text-[#274d48]">
                  Sau khi gửi yêu cầu, bạn vẫn có thể hủy xóa trong thời gian chờ qua liên kết trong email
                  xác nhận. Đăng nhập sẽ bị khóa ngay để bảo vệ dữ liệu trong lúc xử lý.
                </p>
              </div>
              <div
                className="pointer-events-none absolute -bottom-10 -right-10 h-40 w-40 rounded-full bg-[#00685f]/10 blur-3xl"
                aria-hidden="true"
              />
            </section>
            <SettingsDirectContactCard
              title="Cần hỗ trợ?"
              description="Liên hệ đội ngũ nếu bạn có thắc mắc về quyền xóa dữ liệu hoặc quy trình chờ 72 giờ."
            />
          </aside>
        </div>
      </DashboardPageShell>

      {requestSent &&
        typeof document !== "undefined" &&
        createPortal(
          <div
            className="fixed inset-0 z-[200] flex cursor-default flex-col items-center justify-start overflow-y-auto bg-[#121e1c]/45 p-4 pb-[max(1rem,env(safe-area-inset-bottom))] backdrop-blur-md sm:justify-center sm:p-6"
            role="dialog"
            aria-modal="true"
            aria-labelledby="deletion-sent-title"
          >
            <div className="mx-auto flex w-full max-w-3xl flex-col justify-center py-8 sm:min-h-0 sm:py-0">
              <div className="rounded-xl border border-[#bcc9c6]/15 bg-white p-8 text-center shadow-[0_8px_32px_rgba(18,30,28,0.12)] md:p-10">
                <Link
                  href="/"
                  className="mx-auto mb-4 flex items-center justify-center gap-2"
                >
                  <Shield className="h-6 w-6 text-[#00685f]" aria-hidden />
                  <span className="text-lg font-extrabold tracking-tight text-[#00685f]">
                    HealthLens
                  </span>
                </Link>
                <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-[#e9f6f3]">
                  <CheckCircle2 className="h-12 w-12 text-[#00685f]" />
                </div>

                <h2
                  id="deletion-sent-title"
                  className="mb-4 text-3xl font-extrabold tracking-tight text-[#121e1c]"
                >
                  Yêu cầu xóa dữ liệu đã được gửi
                </h2>
                <p className="mx-auto mb-8 max-w-2xl leading-relaxed text-[#3d4947]">
                  Chúng tôi đã tiếp nhận yêu cầu của bạn. Theo{" "}
                  <strong>Nghị định 13/2023/NĐ-CP</strong> về bảo vệ dữ liệu cá nhân, toàn bộ dữ liệu
                  của bạn sẽ được xóa vĩnh viễn khỏi hệ thống trong tối đa{" "}
                  <span className="font-bold text-[#00685f]">72 giờ</span>.
                </p>

                <div className="mb-6 rounded-xl bg-[#e9f6f3] p-6 text-left">
                  <h3 className="mb-4 text-sm font-bold uppercase tracking-wider text-[#00685f]">
                    Dữ liệu sẽ bị xóa bỏ
                  </h3>
                  <ul className="grid grid-cols-1 gap-3 text-sm text-[#3d4947] md:grid-cols-2">
                    <li className="flex items-center gap-3">
                      <UserRound className="h-4 w-4 text-[#00685f]" />
                      Thông tin định danh (PII)
                    </li>
                    <li className="flex items-center gap-3">
                      <FolderOpen className="h-4 w-4 text-[#00685f]" />
                      Hồ sơ sức khỏe cá nhân
                    </li>
                    <li className="flex items-center gap-3">
                      <FileUp className="h-4 w-4 text-[#00685f]" />
                      Tệp tin đính kèm (PDF/Ảnh)
                    </li>
                    <li className="flex items-center gap-3">
                      <ShieldCheck className="h-4 w-4 text-[#00685f]" />
                      Nhật ký đồng thuận
                    </li>
                    <li className="flex items-center gap-3">
                      <FileText className="h-4 w-4 text-[#00685f]" />
                      Nhật ký hoạt động
                    </li>
                  </ul>
                </div>

                <div className="mb-8 flex gap-3 rounded-lg border-l-4 border-[#6d7a77] bg-[#d8e5e2] p-4 text-left">
                  <Info className="mt-0.5 h-5 w-5 shrink-0 text-[#3d4947]" />
                  <p className="text-sm text-[#3d4947]">
                    Tài khoản của bạn sẽ bị <strong>vô hiệu hóa đăng nhập</strong> ngay lập tức trong
                    thời gian chờ xử lý để đảm bảo tính toàn vẹn của tiến trình xóa.
                  </p>
                </div>

                <p className="text-sm text-[#3d4947]">
                  Phiên làm việc sẽ được khóa và bạn được chuyển về đăng nhập sau{" "}
                  <span className="font-semibold text-[#00685f]">{secondsLeft} giây</span>.
                </p>
              </div>
            </div>
          </div>,
          document.body,
        )}
    </>
  );
}
