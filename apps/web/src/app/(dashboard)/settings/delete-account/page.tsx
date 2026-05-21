"use client";

import React, { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import type { ComponentProps } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Callout } from "@radix-ui/themes";
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

import { useAccountDeletion } from "@/hooks/useAccountDeletion";
import { authenticatedContentShell } from "@/lib/layout/shell";
import { useAuthStore } from "@/stores/authStore";

/**
 * Story 1.6 — UI theo Stitch (project 2069125245324220624):
 * - Màn “Yêu cầu xóa tài khoản”: bước 1 xác nhận + bước 2 mật khẩu
 * - Sau khi gửi thành công: chuyển thẳng tới đăng nhập
 */
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
      // Hook provides `hookError` displayed by the UI; avoid noisy console.error in production
    }
  };

  return (
    <div className="relative flex min-h-screen flex-col bg-[#effcf9] text-[#121e1c]">
      <div className="sticky top-16 z-30 border-b border-[#bcc9c6]/25 bg-[#effcf9]/95 backdrop-blur">
        <nav
          className={`${authenticatedContentShell} py-3 text-sm font-medium text-[#6d7a77]`}
          aria-label="Breadcrumb"
        >
          <div className="flex flex-wrap items-center gap-1">
            <span className="inline-flex items-center gap-1">
              <Link href="/home" className="hover:text-[#00685f] hover:underline">
                Trang chủ
              </Link>
              <span>/</span>
            </span>
            <span className="inline-flex items-center gap-1">
              <Link href="/settings" className="hover:text-[#00685f] hover:underline">
                Cài đặt
              </Link>
              <span>/</span>
            </span>
            <span className="inline-flex items-center gap-1">
              <span className="text-[#3d4947]">Xóa tài khoản</span>
            </span>
          </div>
        </nav>
      </div>

      <main
        className={
          requestSent
            ? "pointer-events-none flex min-h-screen flex-1 select-none flex-col px-4 py-8 opacity-0 md:px-8 md:py-12 lg:px-12"
            : "flex w-full flex-1 flex-col px-4 py-8 md:px-8 md:py-12 lg:px-12"
        }
        aria-hidden={requestSent}
      >
        <section className="w-full flex-1 rounded-xl border border-[#bcc9c6]/10 bg-white p-8 shadow-[0_8px_32px_rgba(18,30,28,0.06)] md:p-12">
          {!requestSent && (
            <>
              <div className="mb-10 text-center">
                <h1 className="mb-3 text-3xl font-bold tracking-tight text-[#121e1c]">
                  Yêu cầu xóa tài khoản
                </h1>
                <p className="mx-auto max-w-lg leading-relaxed text-[#3d4947]">
                  Chúng tôi rất tiếc khi thấy bạn rời đi. Vui lòng hoàn thành
                  các bước bên dưới để thực hiện quyền xóa dữ liệu theo Nghị
                  định 13/2023/NĐ-CP.
                </p>
              </div>

              <div className="mb-10 flex justify-center gap-3">
                <div className="h-1.5 w-16 rounded-full bg-[#00685f]" />
                <div className="h-1.5 w-16 rounded-full bg-[#deebe8]" />
              </div>
            </>
          )}

          {!requestSent ? (
            <form onSubmit={handleSubmit} className="space-y-8">
              <div className="rounded-xl border-l-4 border-[#ba1a1a] bg-[#ffdad6]/30 p-6">
                <div className="flex gap-4">
                  <AlertTriangle className="h-5 w-5 text-[#ba1a1a]" />
                  <div>
                    <h2 className="mb-2 text-lg font-bold text-[#93000a]">
                      Cảnh báo quan trọng
                    </h2>
                    <ul className="list-disc space-y-2 pl-4 text-sm leading-relaxed text-[#93000a]">
                      <li>
                        Hành động này là <strong>vĩnh viễn</strong> và không thể
                        hoàn tác.
                      </li>
                      <li>
                        Tất cả dữ liệu y tế, lịch sử khám bệnh và hồ sơ sẽ bị
                        xóa sạch khỏi hệ thống.
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
                  Dữ liệu sẽ bị xóa bao gồm:
                </h3>
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <div className="flex items-center gap-3 rounded-xl bg-[#e9f6f3] p-4">
                    <UserRound className="h-5 w-5 text-[#3f6560]" />
                    <span className="text-sm font-medium">
                      Thông tin định danh (PII)
                    </span>
                  </div>
                  <div className="flex items-center gap-3 rounded-xl bg-[#e9f6f3] p-4">
                    <Stethoscope className="h-5 w-5 text-[#3f6560]" />
                    <span className="text-sm font-medium">
                      Hồ sơ bệnh án &amp; Vitals
                    </span>
                  </div>
                  <div className="flex items-center gap-3 rounded-xl bg-[#e9f6f3] p-4">
                    <FileUp className="h-5 w-5 text-[#3f6560]" />
                    <span className="text-sm font-medium">
                      Tệp đính kèm &amp; Kết quả xét nghiệm
                    </span>
                  </div>
                  <div className="flex items-center gap-3 rounded-xl bg-[#e9f6f3] p-4">
                    <ShieldCheck className="h-5 w-5 text-[#3f6560]" />
                    <span className="text-sm font-medium">
                      Nhật ký đồng thuận dữ liệu
                    </span>
                  </div>
                </div>
              </div>

              <label
                htmlFor="delete-account-ack-consequences"
                className="group flex cursor-pointer items-start gap-4 rounded-xl bg-[#d8e5e2] p-4 transition-colors hover:bg-[#d0ddda]"
              >
                <input
                  id="delete-account-ack-consequences"
                  type="checkbox"
                  checked={hasReadConsequences}
                  onChange={(e) => setHasReadConsequences(e.target.checked)}
                  className="mt-1 h-5 w-5 shrink-0 rounded border-[#6d7a77]"
                />
                <span className="text-sm leading-relaxed text-[#3d4947]">
                  Tôi đã hiểu và chấp nhận hậu quả của việc xóa tài khoản. Tôi
                  xác nhận rằng HealthLens đã thông báo đầy đủ về các quyền của
                  tôi theo quy định pháp luật về bảo vệ dữ liệu cá nhân.
                </span>
              </label>

              <div className="space-y-6 border-t border-[#deebe8] pt-10">
                <div>
                  <h3 className="mb-2 text-lg font-bold text-[#121e1c]">
                    Xác nhận danh tính
                  </h3>
                  <label htmlFor="delete-account-password" className="mb-2 block text-sm font-bold text-[#121e1c]">
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
                    className="h-14 w-full rounded-xl border-none bg-[#d8e5e2] px-5 text-[#121e1c] placeholder:text-[#bcc9c6] focus:outline-none focus:ring-2 focus:ring-[#00685f]"
                  />
                </div>

                {(hookError || passwordError) && (
                  <Callout.Root
                    role="alert"
                    aria-live="assertive"
                    className="rounded-xl border border-[#ba1a1a]/20 bg-[#ffebee]"
                  >
                    <Callout.Icon>
                      <AlertTriangle size={18} className="text-[#ba1a1a]" />
                    </Callout.Icon>
                    <Callout.Text className="text-[#ba1a1a]">
                      {hookError || passwordError}
                    </Callout.Text>
                  </Callout.Root>
                )}

                <div className="flex flex-col gap-4 pt-4 md:flex-row">
                  <button
                    type="submit"
                    disabled={isRequesting}
                    className="flex h-12 flex-1 items-center justify-center gap-2 rounded-xl bg-[#ba1a1a] font-bold text-white shadow-md transition-all hover:opacity-90 disabled:opacity-60"
                  >
                    <Trash2 className="h-5 w-5" />
                    {isRequesting ? "Đang xử lý..." : "Gửi yêu cầu xóa"}
                  </button>
                  <button
                    type="button"
                    onClick={() => router.back()}
                    className="h-12 flex-1 rounded-xl bg-[#deebe8] font-bold text-[#3d4947] transition-all hover:bg-[#d8e5e2]"
                  >
                    Hủy bỏ
                  </button>
                </div>
              </div>
            </form>
          ) : (
            <div className="min-h-[50vh]" aria-hidden />
          )}
        </section>
      </main>

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
                  <strong>Nghị định 13/2023/NĐ-CP</strong> về bảo vệ dữ liệu cá
                  nhân, toàn bộ dữ liệu của bạn sẽ được xóa vĩnh viễn khỏi hệ
                  thống trong tối đa{" "}
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
                    Tài khoản của bạn sẽ bị{" "}
                    <strong>vô hiệu hóa đăng nhập</strong> ngay lập tức trong
                    thời gian chờ xử lý để đảm bảo tính toàn vẹn của tiến trình
                    xóa.
                  </p>
                </div>

                <p className="text-sm text-[#3d4947]">
                  Phiên làm việc sẽ được khóa và bạn được chuyển về đăng nhập
                  sau{" "}
                  <span className="font-semibold text-[#00685f]">
                    {secondsLeft} giây
                  </span>
                  .
                </p>
              </div>
            </div>
          </div>,
          document.body,
        )}
    </div>
  );
}
