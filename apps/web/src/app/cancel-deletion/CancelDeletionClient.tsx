'use client';

import React, { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { AlertTriangle, CalendarCheck, CheckCircle2, HelpCircle, Info, Loader, Mail, RefreshCcw, Send, Timer, Undo2 } from "lucide-react";
import { AxiosError } from "axios";
import { useAccountDeletion } from '../../hooks/useAccountDeletion';

interface ApiErrorData {
  error?: string;
  detail?: string;
}

/** Chuẩn hóa token từ query (trình đọc mail đôi khi mã hóa ký tự thêm một lần). */
function normalizeCancellationToken(raw: string | null): string | null {
  if (raw == null) return null;
  const trimmed = raw.trim();
  if (!trimmed) return null;
  try {
    return decodeURIComponent(trimmed);
  } catch {
    return trimmed;
  }
}

const parseTimestamp = (value?: string | null) => {
  if (!value) return Number.NaN;
  const normalizedValue = value.trim().replace(/ (\d{2}:\d{2})$/, "+$1");
  return new Date(normalizedValue).getTime();
};

/**
 * Story 1.6 — UI tham chiếu Stitch (project 2069125245324220624):
 * Xác nhận hủy, thành công, liên kết không hợp lệ (không hiển thị mã lỗi kỹ thuật).
 */
export default function CancelDeletionClient() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = normalizeCancellationToken(searchParams.get("token"));
  const requestedAt = searchParams.get("requestedAt");
  const scheduledDeletionAt = searchParams.get("scheduledDeletionAt");

  const [status, setStatus] = useState<"ready" | "loading" | "success" | "error">("ready");
  const [message, setMessage] = useState("");
  const [resendEmail, setResendEmail] = useState("");
  const [confirmedEmail, setConfirmedEmail] = useState<string | null>(null);
  const [cancelledAt, setCancelledAt] = useState<string | null>(null);
  const [redirectSecondsLeft, setRedirectSecondsLeft] = useState(3);
  const [nowMs, setNowMs] = useState(() => Date.now());
  const { cancelDeletion } = useAccountDeletion();
  const successMessage =
    "Tài khoản của bạn đã được khôi phục và có thể đăng nhập bình thường.";

  const deletionTimestamp = useMemo(() => {
    const deletionTimestampFromParam = parseTimestamp(scheduledDeletionAt);
    if (!Number.isNaN(deletionTimestampFromParam)) return deletionTimestampFromParam;

    const requestedTimestamp = parseTimestamp(requestedAt);
    if (!Number.isNaN(requestedTimestamp)) return requestedTimestamp + 72 * 60 * 60 * 1000;

    return Number.NaN;
  }, [scheduledDeletionAt, requestedAt]);

  useEffect(() => {
    if (Number.isNaN(deletionTimestamp)) return;
    const timer = window.setInterval(() => {
      setNowMs(Date.now());
    }, 30000);
    return () => window.clearInterval(timer);
  }, [deletionTimestamp]);

  useEffect(() => {
    if (status !== "success") return;

    const countdownInterval = window.setInterval(() => {
      setRedirectSecondsLeft((current) => (current > 1 ? current - 1 : 1));
    }, 1000);

    const redirectTimeout = window.setTimeout(() => {
      router.replace("/login");
    }, 3000);

    return () => {
      window.clearInterval(countdownInterval);
      window.clearTimeout(redirectTimeout);
    };
  }, [status, router]);

  const cancelledAtDisplay = useMemo(() => {
    const parsed = parseTimestamp(cancelledAt);
    if (Number.isNaN(parsed)) return "--";
    return new Date(parsed).toLocaleString("vi-VN");
  }, [cancelledAt]);

  const remainingMs = useMemo<number | null>(() => {
    if (Number.isNaN(deletionTimestamp)) return null;
    return deletionTimestamp - nowMs;
  }, [deletionTimestamp, nowMs]);

  const isExpired = remainingMs !== null && remainingMs <= 0;

  const remainingTimeDisplay = useMemo(() => {
    if (remainingMs === null) return "72 giờ";
    if (remainingMs <= 0) return "0 phút";
    const oneHourMs = 60 * 60 * 1000;
    if (remainingMs >= oneHourMs) {
      const remainingHours = Math.ceil(remainingMs / oneHourMs);
      return `${remainingHours} giờ`;
    }

    const remainingMinutes = Math.ceil(remainingMs / (60 * 1000));
    return `${remainingMinutes} phút`;
  }, [remainingMs]);

  const maskedEmail = useMemo(() => {
    const email = confirmedEmail || searchParams.get("email");
    if (!email || !email.includes("@")) return "a...r@email.com";
    const [name, domain] = email.split("@");
    if (!name) return `...@${domain}`;
    const first = name[0];
    const last = name[name.length - 1];
    return `${first}...${last}@${domain}`;
  }, [confirmedEmail, searchParams]);

  const handleConfirmCancel = async () => {
    if (!token) {
      setStatus("error");
      setMessage(
        "Liên kết có thể đã hết hạn hoặc không đúng định dạng. Vui lòng kiểm tra email mới nhất từ HealthLens, hoặc đăng nhập nếu tài khoản vẫn hoạt động — và liên hệ privacy@healthlens.vn nếu bạn cần hỗ trợ.",
      );
      return;
    }
    if (isExpired) {
      setStatus("error");
      setMessage("Thời gian hủy yêu cầu đã hết. Vui lòng đăng nhập để kiểm tra trạng thái tài khoản.");
      return;
    }

    setStatus("loading");
    try {
      const result = await cancelDeletion(token);
      if (result?.email) setConfirmedEmail(result.email);
      setCancelledAt(result?.cancelledAt ?? new Date().toISOString());
      setRedirectSecondsLeft(3);
      setStatus("success");
    } catch (err) {
      const error = err as AxiosError<ApiErrorData>;
      if (error?.response?.status === 409) {
        setCancelledAt(new Date().toISOString());
        setRedirectSecondsLeft(3);
        setStatus("success");
        return;
      }
      setStatus("error");
      setMessage(
        error?.response?.data?.detail ||
        error?.response?.data?.error ||
        error?.message ||
        "Không thể hoàn tất hủy yêu cầu. Vui lòng thử lại hoặc liên hệ hỗ trợ.",
      );
    }
  };

  return (
    <div className="min-h-screen bg-[#effcf9] text-[#121e1c] antialiased">
      <main className="mx-auto flex min-h-screen max-w-xl items-center justify-center px-4 py-12">
        {status === "ready" && (
          <div className="w-full">
            <div className="overflow-hidden rounded-xl bg-white shadow-[0_8px_32px_rgba(18,30,28,0.06)]">
              <div className="flex flex-col items-center bg-[#00685f]/5 p-8 text-center">
                <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-[#008378] text-white shadow-lg shadow-[#00685f]/20">
                  <HelpCircle className="h-8 w-8" />
                </div>
                <h1 className="mb-1 text-2xl font-bold tracking-tight text-[#00685f]">
                  {isExpired ? "Liên kết đã hết hạn" : "Xác nhận hủy yêu cầu xóa"}
                </h1>
                <p className="text-sm text-[#3d4947]">
                  {isExpired
                    ? "Bạn đã quá thời gian 72 giờ để hủy yêu cầu xóa. Liên kết từ email không còn hiệu lực"
                    : "Yêu cầu xóa tài khoản của bạn đang trong thời gian chờ xử lý."}
                </p>
              </div>
              <div className="space-y-8 p-8">
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <div className="rounded-xl bg-[#e9f6f3] p-5">
                    <span className="mb-2 block text-xs font-semibold uppercase tracking-wider text-[#3d4947]">Tài khoản</span>
                    <div className="flex items-center gap-3">
                      <span className="text-[#00685f]">@</span>
                      <span className="font-medium text-[#121e1c]">{maskedEmail}</span>
                    </div>
                  </div>
                  <div className="rounded-xl bg-[#924628]/5 p-5">
                    <span className="mb-2 block text-xs font-semibold uppercase tracking-wider text-[#924628]">Thời gian còn lại</span>
                    <div className="flex items-center gap-3">
                      <Timer className="h-5 w-5 text-[#924628]" />
                      <span className="text-lg font-bold text-[#924628]">{remainingTimeDisplay}</span>
                    </div>
                  </div>
                </div>

                <div className="space-y-4">
                  <div className="flex items-start gap-4">
                    <div className="mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#00685f]/10">
                      <RefreshCcw className="h-4 w-4 text-[#00685f]" />
                    </div>
                    <p className="leading-relaxed text-[#3d4947]">
                      Việc hủy yêu cầu xóa sẽ khôi phục quyền truy cập đầy đủ vào{" "}
                      <span className="font-semibold text-[#00685f]">hồ sơ sức khỏe</span>, lịch sử thăm khám
                      và các dịch vụ cá nhân hóa khác trên HealthLens ngay lập tức.
                    </p>
                  </div>
                  <div className="h-px bg-[#d8e5e2]/40" />
                  <p className="text-center font-semibold text-[#121e1c]">
                    {isExpired ? "Liên kết này không còn hiệu lực để khôi phục tài khoản." : "Bạn có muốn hủy yêu cầu xóa không?"}
                  </p>
                </div>

                <div className="space-y-3">
                  <button
                    type="button"
                    onClick={handleConfirmCancel}
                    disabled={isExpired}
                    className={`flex h-12 w-full items-center justify-center gap-2 rounded-xl font-semibold text-white shadow-md transition-all ${isExpired
                      ? "cursor-not-allowed bg-[#bcc9c6]"
                      : "bg-gradient-to-r from-[#00685f] to-[#008378] hover:shadow-lg"
                      }`}
                  >
                    <Undo2 className="h-4 w-4" />
                    Hủy yêu cầu xóa (khôi phục tài khoản)
                  </button>
                  <Link
                    href="/login"
                    className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-[#deebe8] font-medium text-[#121e1c] transition-all hover:bg-[#d8e5e2]"
                  >
                    Giữ yêu cầu xóa
                  </Link>
                </div>

                <div className="flex items-start gap-2 pt-2">
                  <Info className="mt-0.5 h-4 w-4 shrink-0 text-[#3d4947]" />
                  <p className="text-[11px] leading-normal text-[#3d4947]">
                    Theo chính sách bảo mật, dữ liệu của bạn sẽ được giữ lại trong 72 giờ grace period trước khi bị xóa vĩnh viễn khỏi hệ thống của chúng tôi để đảm bảo quyền lợi khôi phục tài khoản. Hành động khôi phục sẽ tuân thủ các điều khoản dịch vụ hiện hành.
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {status === "loading" && (
          <div className="w-full rounded-2xl border border-[#bcc9c6]/20 bg-white p-10 shadow-[0_8px_32px_rgba(18,30,28,0.06)]">
            <div className="flex flex-col items-center justify-center gap-4">
              <Loader className="h-12 w-12 animate-spin text-[#00685f]" aria-hidden />
              <p className="text-sm text-[#6d7a77]">Đang hủy yêu cầu...</p>
            </div>
          </div>
        )}

        {status === "success" && (
          <div className="relative w-full max-w-lg overflow-hidden rounded-xl bg-white p-10 text-center shadow-[0_8px_32px_rgba(18,30,28,0.06)]">
            <div className="pointer-events-none absolute -right-24 -top-24 h-64 w-64 rounded-full bg-[#00685f]/5 blur-3xl" />
            <div className="pointer-events-none absolute -bottom-24 -left-24 h-64 w-64 rounded-full bg-[#3f6560]/5 blur-3xl" />

            <div className="relative mb-8">
              <div className="mx-auto mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-[#00685f]/10">
                <CheckCircle2 className="h-12 w-12 text-[#00685f]" aria-hidden />
              </div>
              <div className="absolute inset-0 -z-10 rounded-full bg-[#00685f]/10 blur-2xl" />
            </div>

            <h2 className="mb-4 text-[1.75rem] font-bold tracking-tight text-[#121e1c]">Yêu cầu xóa đã được hủy</h2>
            <p className="mb-8 text-lg leading-relaxed text-[#3d4947]">{successMessage}</p>

            <section className="mb-10 w-full rounded-xl bg-[#e9f6f3] p-6 text-left">
              <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-3">
                    <CalendarCheck className="h-5 w-5 text-[#00685f]" />
                    <span className="font-medium text-[#3d4947]">Thời điểm hủy:</span>
                  </div>
                  <span className="font-semibold text-[#121e1c]">{cancelledAtDisplay}</span>
                </div>
                <div className="h-px w-full bg-[#bcc9c6]/30" />
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-3">
                    <Mail className="h-5 w-5 text-[#00685f]" />
                    <span className="font-medium text-[#3d4947]">Email xác nhận:</span>
                  </div>
                  <span className="font-semibold text-[#121e1c]">{maskedEmail}</span>
                </div>
              </div>
            </section>

            <p className="mb-8 text-sm text-[#3d4947]">
              Hệ thống sẽ tự động chuyển bạn về trang đăng nhập sau <span className="font-semibold text-[#00685f]">{redirectSecondsLeft} giây</span>.
            </p>

            <div className="flex items-start gap-3 rounded-lg bg-[#ffdbce]/30 p-4 text-left">
              <Info className="mt-0.5 h-4 w-4 shrink-0 text-[#924628]" />
              <p className="text-[13px] leading-snug text-[#773215]">
                <span className="font-bold">Lưu ý:</span> Nếu không phải bạn thực hiện thao tác này, vui
                lòng liên hệ với đội ngũ hỗ trợ của chúng tôi ngay lập tức.
              </p>
            </div>
          </div>
        )}

        {status === "error" && (
          <div className="w-full">
            <div className="relative overflow-hidden rounded-xl border border-white/50 bg-white p-10 shadow-[0_8px_32px_rgba(18,30,28,0.06)]">
              <div className="pointer-events-none absolute -right-12 -top-12 h-32 w-32 rounded-full bg-[#00685f]/5 blur-3xl" />

              <div className="mb-6 flex flex-col items-center space-y-6 text-center">
                <div className="flex h-16 w-16 items-center justify-center rounded-full bg-[#c2ebe3] text-[#00685f]">
                  <AlertTriangle className="h-8 w-8" aria-hidden />
                </div>
                <div className="space-y-3">
                  <h2 className="text-2xl font-bold tracking-tight text-[#121e1c]">
                    Liên kết không hợp lệ hoặc đã hết hạn
                  </h2>
                  <p className="mx-auto max-w-md text-base leading-relaxed text-[#3d4947]">
                    {message || "Có vẻ như liên kết này không còn khả dụng. Điều này có thể xảy ra nếu yêu cầu đã được xử lý xong hoặc mã bảo mật đã hết thời gian hiệu lực."}
                  </p>
                </div>
              </div>

              <div className="space-y-6">
                <div className="space-y-2">
                  <label htmlFor="resend-email" className="ml-1 block text-sm font-medium text-[#3d4947]">
                    Để nhận liên kết mới, vui lòng nhập email của bạn:
                  </label>
                  <div className="group relative">
                    <input
                      id="resend-email"
                      type="email"
                      value={resendEmail}
                      onChange={(e) => setResendEmail(e.target.value)}
                      placeholder="example@healthlens.vn"
                      className="h-14 w-full rounded-xl border-none bg-[#d8e5e2] px-4 text-[#121e1c] outline-none transition-all placeholder:text-[#6d7a77] focus:ring-2 focus:ring-[#00685f]/20"
                    />
                    <div className="absolute bottom-0 left-0 h-0.5 w-full scale-x-0 bg-[#00685f] transition-transform duration-300 group-focus-within:scale-x-100" />
                  </div>
                </div>

                <div className="flex flex-col gap-3">
                  <button
                    type="button"
                    className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-[#00685f] to-[#008378] font-semibold text-white transition-all hover:shadow-lg hover:shadow-[#00685f]/20"
                  >
                    <span>Gửi lại email hủy yêu cầu</span>
                    <Send className="h-4 w-4" />
                  </button>
                  <Link
                    href="/login"
                    className="flex h-12 w-full items-center justify-center rounded-xl border-2 border-[#bcc9c6] font-semibold text-[#00685f] transition-all hover:bg-[#e9f6f3]"
                  >
                    Về trang đăng nhập
                  </Link>
                </div>
              </div>

              <div className="mt-10 rounded-xl border border-[#00685f]/5 bg-[#e9f6f3] p-6">
                <div className="mb-2 flex items-center gap-3">
                  <HelpCircle className="h-5 w-5 text-[#00685f]" />
                  <h3 className="font-semibold text-[#121e1c]">Bạn cần hỗ trợ trực tiếp?</h3>
                </div>
                <div className="flex flex-col gap-y-2 text-sm text-[#3d4947] md:flex-row md:items-center md:gap-x-6">
                  <div className="flex items-center gap-2">
                    <span className="font-medium">Hotline:</span>
                    <a href="tel:19001234" className="font-bold text-[#00685f] hover:underline">1900 1234</a>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="font-medium">Email:</span>
                    <a href="mailto:support@healthlens.vn" className="font-bold text-[#00685f] hover:underline">support@healthlens.vn</a>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>
      <footer className="py-8 text-center text-xs text-[#3d4947]/60">
        <p>© 2024 HealthLens Digital Health Solutions. Tất cả quyền được bảo lưu.</p>
      </footer>
    </div>
  );
}
