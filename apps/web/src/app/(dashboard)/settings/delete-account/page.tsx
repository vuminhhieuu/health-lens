"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { Callout } from "@radix-ui/themes";
import {
  AlertTriangle,
  ArrowLeftRight,
  CheckCircle2,
  CircleHelp,
  Database,
  FileClock,
  FileSpreadsheet,
  LogOut,
  ShieldAlert,
  HeartPulse,
  Paperclip,
  ShieldCheck,
  Stethoscope,
  Trash2,
  UserRound,
  FileUp,
} from "lucide-react";

import { useAccountDeletion } from "@/hooks/useAccountDeletion";
import { useAuthStore } from "@/stores/authStore";

/**
 * Story 1.6 — UI theo Stitch (project 2069125245324220624):
 * - Màn “Yêu cầu xóa tài khoản”: bước 1 xác nhận + bước 2 mật khẩu
 * - Màn “Yêu cầu xóa đã được gửi”: success sau API
 */
export default function DeleteAccountPage() {
  const router = useRouter();
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const { requestDeletion, isRequesting, error: hookError, requestSuccess } = useAccountDeletion();
  const [password, setPassword] = useState("");
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [hasReadConsequences, setHasReadConsequences] = useState(false);
  const [deletionLink, setDeletionLink] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!hasReadConsequences) {
      setPasswordError("Vui lòng xác nhận bạn đã hiểu hậu quả của việc xóa tài khoản");
      return;
    }
    if (!password.trim()) {
      setPasswordError("Vui lòng nhập mật khẩu");
      return;
    }
    setPasswordError(null);
    try {
      const result = await requestDeletion(password);
      setDeletionLink(result.cancellationLink);
      setPassword("");
      setTimeout(() => {
        clearAuth();
        router.push("/login?deleted=true");
      }, 4000);
    } catch (err) {
      console.error("Deletion request failed", err);
    }
  };

  if (requestSuccess && deletionLink) {
    return (
      <div className="min-h-screen bg-[radial-gradient(circle_at_top_left,#effcf9_0%,#e4f1ee_100%)] p-6 text-[#121e1c]">
        <main className="mx-auto w-full max-w-[640px]">
          <div className="mb-8 flex justify-center">
            <div className="flex items-center gap-2">
              <HeartPulse className="h-8 w-8 text-[#00685f]" />
              <span className="text-2xl font-extrabold tracking-tight text-[#00685f]">HealthLens</span>
            </div>
          </div>

          <div className="overflow-hidden rounded-xl bg-white shadow-[0_8px_32px_rgba(18,30,28,0.06)]">
            <div className="flex flex-col items-center p-10 text-center">
              <div className="mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-[#e9f6f3]">
                <CheckCircle2 className="h-12 w-12 text-[#00685f]" aria-hidden />
              </div>

              <h1 className="mb-4 text-3xl font-extrabold tracking-tight">Yêu cầu xóa dữ liệu đã được gửi</h1>
              <p className="mb-8 max-w-lg leading-relaxed text-[#3d4947]">
                Chúng tôi đã tiếp nhận yêu cầu của bạn. Theo <span className="font-semibold">Nghị định 13/2023/NĐ-CP</span> về
                Bảo vệ dữ liệu cá nhân, toàn bộ dữ liệu của bạn sẽ được xóa vĩnh viễn khỏi hệ thống trong tối đa{" "}
                <span className="font-bold text-[#00685f]">72 giờ</span>.
              </p>

              <div className="mb-6 w-full rounded-xl bg-[#e9f6f3] p-6 text-left">
                <h3 className="mb-4 flex items-center gap-2 text-sm font-bold uppercase tracking-wider text-[#00685f]">
                  <Database className="h-4 w-4" />
                  Dữ liệu sẽ bị xóa bỏ
                </h3>
                <ul className="grid grid-cols-1 gap-3 md:grid-cols-2">
                  <li className="flex items-center gap-3 text-sm text-[#3d4947]">
                    <UserRound className="h-4 w-4 text-[#00685f]" />
                    Thông tin định danh (PII)
                  </li>
                  <li className="flex items-center gap-3 text-sm text-[#3d4947]">
                    <FileSpreadsheet className="h-4 w-4 text-[#00685f]" />
                    Hồ sơ sức khỏe cá nhân
                  </li>
                  <li className="flex items-center gap-3 text-sm text-[#3d4947]">
                    <Paperclip className="h-4 w-4 text-[#00685f]" />
                    Tệp tin đính kèm (PDF/Ảnh)
                  </li>
                  <li className="flex items-center gap-3 text-sm text-[#3d4947]">
                    <ShieldCheck className="h-4 w-4 text-[#00685f]" />
                    Nhật ký đồng ý (Consent logs)
                  </li>
                  <li className="flex items-center gap-3 text-sm text-[#3d4947]">
                    <FileClock className="h-4 w-4 text-[#00685f]" />
                    Nhật ký hoạt động (Audit logs)
                  </li>
                </ul>
              </div>

              <div className="mb-10 flex w-full gap-4 rounded-lg border-l-4 border-[#6d7a77] bg-[#d8e5e2] p-4 text-left">
                <ShieldAlert className="h-5 w-5 shrink-0 text-[#6d7a77]" />
                <p className="text-sm text-[#3d4947]">
                  Tài khoản của bạn sẽ bị <span className="font-bold">vô hiệu hóa đăng nhập</span> ngay lập tức trong thời gian chờ xử
                  lý để đảm bảo tính toàn vẹn của tiến trình xóa.
                </p>
              </div>

              <div className="flex w-full flex-col gap-3">
                <button
                  type="button"
                  onClick={() => {
                    clearAuth();
                    router.push("/login?deleted=true");
                  }}
                  className="flex h-14 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-[#00685f] to-[#008378] text-lg font-bold text-white transition-all hover:shadow-lg"
                >
                  <LogOut className="h-5 w-5" />
                  Đăng xuất ngay
                </button>
                <button
                  type="button"
                  onClick={() => router.push("/login")}
                  className="flex h-14 w-full items-center justify-center gap-2 rounded-xl border-2 border-[#bcc9c6] text-lg font-bold text-[#00685f] transition-all hover:bg-[#e9f6f3]"
                >
                  <ArrowLeftRight className="h-5 w-5" />
                  Quay về trang đăng nhập
                </button>
              </div>

              <div className="mt-8">
                <p className="text-sm text-[#3d4947]">
                  Đổi ý?{" "}
                  <a
                    href={deletionLink}
                    className="font-semibold text-[#00685f] underline underline-offset-4 transition-colors hover:text-[#008378]"
                  >
                    Hủy yêu cầu xóa qua liên kết trong email
                  </a>
                </p>
              </div>
            </div>
          </div>

          <footer className="mt-12 text-center">
            <p className="text-xs font-medium uppercase tracking-[0.2em] text-[#6d7a77]">
              HealthLens Security Compliance — 2024
            </p>
          </footer>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#effcf9] text-[#121e1c]">
      <header className="sticky top-0 z-50 flex h-16 w-full items-center justify-between bg-teal-50/80 px-6 backdrop-blur-md md:px-8">
        <div className="flex items-center gap-2">
          <HeartPulse className="h-6 w-6 text-[#00685f]" />
          <span className="text-xl font-bold tracking-tight text-teal-900">HealthLens</span>
        </div>
        <button
          type="button"
          className="flex items-center gap-2 rounded-full p-2 text-teal-600 transition-colors hover:bg-teal-100/50"
        >
          <CircleHelp className="h-5 w-5" />
          <span className="text-sm font-medium">Trợ giúp</span>
        </button>
      </header>

      <main className="flex flex-1 items-center justify-center p-6 md:p-12">
        <section className="w-full max-w-3xl rounded-xl border border-[#bcc9c6]/10 bg-white p-8 shadow-[0_8px_32px_rgba(18,30,28,0.06)] md:p-12">
          <div className="mb-10 text-center">
            <h1 className="mb-3 text-3xl font-bold tracking-tight text-[#121e1c]">Yêu cầu xóa tài khoản</h1>
            <p className="mx-auto max-w-lg leading-relaxed text-[#3d4947]">
              Chúng tôi rất tiếc khi thấy bạn rời đi. Vui lòng hoàn thành các bước bên dưới để thực
              hiện quyền xóa dữ liệu theo Nghị định 13/2023/NĐ-CP.
            </p>
          </div>

          <div className="mb-10 flex justify-center gap-3">
            <div className="h-1.5 w-16 rounded-full bg-[#00685f]" />
            <div className="h-1.5 w-16 rounded-full bg-[#deebe8]" />
          </div>

          <form onSubmit={handleSubmit} className="space-y-8">
            <div className="rounded-xl border-l-4 border-[#ba1a1a] bg-[#ffdad6]/30 p-6">
              <div className="flex gap-4">
                <AlertTriangle className="h-5 w-5 text-[#ba1a1a]" />
                <div>
                  <h2 className="mb-2 text-lg font-bold text-[#93000a]">Cảnh báo quan trọng</h2>
                  <ul className="list-disc space-y-2 pl-4 text-sm leading-relaxed text-[#93000a]">
                    <li>
                      Hành động này là <strong>vĩnh viễn</strong> và không thể hoàn tác.
                    </li>
                    <li>Tất cả dữ liệu y tế, lịch sử khám bệnh và hồ sơ sẽ bị xóa sạch khỏi hệ thống.</li>
                    <li>
                      Tài khoản của bạn sẽ bị tạm khóa trong <strong>72 giờ</strong> trước khi bị xóa
                      vĩnh viễn.
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
                  <span className="text-sm font-medium">Thông tin định danh (PII)</span>
                </div>
                <div className="flex items-center gap-3 rounded-xl bg-[#e9f6f3] p-4">
                  <Stethoscope className="h-5 w-5 text-[#3f6560]" />
                  <span className="text-sm font-medium">Hồ sơ bệnh án &amp; Vitals</span>
                </div>
                <div className="flex items-center gap-3 rounded-xl bg-[#e9f6f3] p-4">
                  <FileUp className="h-5 w-5 text-[#3f6560]" />
                  <span className="text-sm font-medium">Tệp đính kèm &amp; Kết quả xét nghiệm</span>
                </div>
                <div className="flex items-center gap-3 rounded-xl bg-[#e9f6f3] p-4">
                  <ShieldCheck className="h-5 w-5 text-[#3f6560]" />
                  <span className="text-sm font-medium">Nhật ký đồng thuận dữ liệu</span>
                </div>
              </div>
            </div>

            <label className="group flex cursor-pointer items-start gap-4 rounded-xl bg-[#d8e5e2] p-4 transition-colors hover:bg-[#d0ddda]">
              <input
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

            <div className="space-y-6 border-t border-[#deebe8] pt-10">
              <div>
                <h3 className="mb-2 text-lg font-bold text-[#121e1c]">Xác nhận danh tính</h3>
                <p className="mb-6 text-sm text-[#3d4947]">Vui lòng nhập mật khẩu của bạn để tiếp tục.</p>
                <input
                  id="password"
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
                <Callout.Root className="rounded-xl border border-[#ba1a1a]/20 bg-[#ffebee]">
                  <Callout.Icon>
                    <AlertTriangle size={18} className="text-[#ba1a1a]" />
                  </Callout.Icon>
                  <Callout.Text className="text-[#ba1a1a]">{hookError || passwordError}</Callout.Text>
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

          <div className="mt-8 flex flex-wrap justify-center gap-6 text-xs font-medium uppercase tracking-widest text-[#3d4947]/60">
            <a className="transition-colors hover:text-[#00685f]" href="#">
              Chính sách bảo mật
            </a>
            <a className="transition-colors hover:text-[#00685f]" href="#">
              Điều khoản dịch vụ
            </a>
            <a className="transition-colors hover:text-[#00685f]" href="#">
              Liên hệ hỗ trợ
            </a>
          </div>
        </section>
      </main>

      <footer className="flex w-full flex-col items-center justify-between gap-6 border-t border-teal-100 bg-teal-50 px-12 py-10 md:flex-row">
        <div className="flex flex-col gap-2">
          <p className="text-xs font-medium uppercase tracking-widest text-teal-900">
            © 2024 HealthLens. HIPAA Compliant &amp; Secure.
          </p>
          <p className="text-[10px] uppercase tracking-widest text-teal-500">
            Tuân thủ Nghị định 13/2023/NĐ-CP về Bảo vệ dữ liệu cá nhân
          </p>
        </div>
        <div className="flex flex-wrap gap-8">
          <a className="text-xs uppercase tracking-widest text-teal-500 opacity-80 transition-opacity hover:opacity-100 hover:underline" href="#">
            Privacy Policy
          </a>
          <a className="text-xs uppercase tracking-widest text-teal-500 opacity-80 transition-opacity hover:opacity-100 hover:underline" href="#">
            Terms of Service
          </a>
          <a className="text-xs uppercase tracking-widest text-teal-500 opacity-80 transition-opacity hover:opacity-100 hover:underline" href="#">
            Data Rights
          </a>
          <a className="text-xs uppercase tracking-widest text-teal-500 opacity-80 transition-opacity hover:opacity-100 hover:underline" href="#">
            Contact Support
          </a>
        </div>
      </footer>
    </div>
  );
}
