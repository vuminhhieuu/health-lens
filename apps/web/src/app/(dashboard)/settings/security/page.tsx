"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import {
  AlertCircle,
  CheckCircle2,
  Copy,
  KeyRound,
  Lock,
  QrCode,
  Shield,
  ShieldCheck,
} from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useState, type HTMLAttributes } from "react";
import { useForm, type UseFormRegisterReturn } from "react-hook-form";
import { QRCodeCanvas } from "qrcode.react";
import { z } from "zod";

import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { InlineFieldError } from "@/components/ui/StateComponents";
import { breadcrumbFromSettings } from "@/lib/layout/dashboardBreadcrumbTrails";
import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { getApiErrorPayload } from "@/lib/i18n/messages";
import { notify } from "@/lib/notify";
import { useAuthStore } from "@/stores/authStore";

import { SettingsAccountNav } from "../_components/SettingsAccountNav";
import { SettingsDirectContactCard } from "../_components/SettingsDirectContactCard";
import { settingsCardClassName, settingsTipCardClassName } from "../_components/settingsStyles";

const verifyCodeSchema = z.object({
  code: z.string().min(6, "Nhập mã 6 chữ số hoặc mã dự phòng"),
});

const disableSchema = z.object({
  password: z.string().min(1, "Nhập mật khẩu"),
  code: z.string().min(6, "Nhập mã xác thực"),
});

type VerifyCodeInput = z.infer<typeof verifyCodeSchema>;
type DisableInput = z.infer<typeof disableSchema>;

type TotpStatus = {
  enabled: boolean;
  pendingVerification: boolean;
};

type SetupData = {
  secret: string;
  otpauthUri: string;
  backupCodes: string[];
};

type SetupStep = "idle" | "setup" | "backup";

const inputClassName =
  "h-12 w-full rounded-xl border-none bg-[#e9f6f3] px-4 pl-11 text-[#121e1c] font-medium outline-none transition focus:ring-2 focus:ring-[#00685f]/20";

const labelClassName = "text-sm font-bold text-[#6d7a77]";

const primaryButtonClassName =
  "inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-[#00685f] to-[#008378] px-8 py-3 text-sm font-bold text-white shadow-lg shadow-[#00685f]/20 transition active:scale-95 hover:brightness-105 disabled:opacity-60";

const secondaryButtonClassName =
  "inline-flex min-h-12 items-center justify-center gap-2 rounded-xl border-2 border-[#bcc9c6]/40 px-6 py-3 text-sm font-bold text-[#3d4947] transition hover:bg-[#e9f6f3]";

type SecurityFieldProps = {
  id: string;
  label: string;
  type?: string;
  inputMode?: HTMLAttributes<HTMLInputElement>["inputMode"];
  autoComplete?: string;
  placeholder?: string;
  error?: string;
  registration: UseFormRegisterReturn;
  icon: typeof Lock;
};

function SecurityField({
  id,
  label,
  type = "text",
  inputMode,
  autoComplete,
  placeholder,
  error,
  registration,
  icon: Icon,
}: SecurityFieldProps) {
  const errorId = `${id}-error`;
  const hasError = Boolean(error);

  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={id} className={labelClassName}>
        {label}
      </label>
      <div className="relative">
        <Icon
          className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-[#6d7a77]"
          aria-hidden="true"
        />
        <input
          id={id}
          type={type}
          inputMode={inputMode}
          autoComplete={autoComplete}
          placeholder={placeholder}
          className={inputClassName}
          {...registration}
          aria-invalid={hasError}
          aria-describedby={hasError ? errorId : undefined}
        />
      </div>
      <InlineFieldError id={errorId} message={error} />
    </div>
  );
}

function TotpStatusBadge({
  enabled,
  pending,
}: {
  enabled: boolean;
  pending: boolean;
}) {
  if (enabled) {
    return (
      <span className="inline-flex items-center gap-2 rounded-full bg-[#e9f6f3] px-4 py-1.5 text-xs font-bold uppercase tracking-wide text-[#00685f] ring-1 ring-[#00685f]/15">
        <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
        Đang bật
      </span>
    );
  }
  if (pending) {
    return (
      <span className="inline-flex items-center gap-2 rounded-full bg-[#fff4e8] px-4 py-1.5 text-xs font-bold uppercase tracking-wide text-[#924628] ring-1 ring-[#924628]/15">
        <AlertCircle className="h-4 w-4" aria-hidden="true" />
        Chưa hoàn tất
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-2 rounded-full bg-[#f6fbfa] px-4 py-1.5 text-xs font-bold uppercase tracking-wide text-[#6d7a77] ring-1 ring-[#bcc9c6]/30">
      Chưa bật
    </span>
  );
}

export default function SecuritySettingsPage() {
  const [status, setStatus] = useState<TotpStatus | null>(null);
  const [statusError, setStatusError] = useState("");
  const [loadingStatus, setLoadingStatus] = useState(true);
  const [setupStep, setSetupStep] = useState<SetupStep>("idle");
  const [setupData, setSetupData] = useState<SetupData | null>(null);
  const [submitError, setSubmitError] = useState("");
  const [isStartingSetup, setIsStartingSetup] = useState(false);
  const user = useAuthStore((state) => state.user);
  const isAdmin = user?.role === "ROLE_ADMIN";

  const verifyForm = useForm<VerifyCodeInput>({
    resolver: zodResolver(verifyCodeSchema),
    defaultValues: { code: "" },
  });

  const disableForm = useForm<DisableInput>({
    resolver: zodResolver(disableSchema),
    defaultValues: { password: "", code: "" },
  });

  const loadStatus = useCallback(async () => {
    setLoadingStatus(true);
    setStatusError("");
    try {
      const res = await apiClient.get<{ data: TotpStatus }>(API_ROUTES.USERS.ME_TOTP);
      setStatus(res.data.data);
    } catch {
      setStatus(null);
      setStatusError("Không thể tải trạng thái 2FA. Vui lòng thử lại.");
    } finally {
      setLoadingStatus(false);
    }
  }, []);

  useEffect(() => {
    void loadStatus();
  }, [loadStatus]);

  const startSetup = async () => {
    if (isAdmin) {
      return;
    }
    setSubmitError("");
    setIsStartingSetup(true);
    try {
      const res = await apiClient.post<{ data: SetupData }>(API_ROUTES.USERS.ME_TOTP_SETUP, {});
      setSetupData(res.data.data);
      setSetupStep("setup");
    } catch (error: unknown) {
      const detail = getApiErrorPayload(error)?.detail;
      setSubmitError(
        detail ?? "Không thể khởi tạo xác thực hai yếu tố. Vui lòng thử lại.",
      );
    } finally {
      setIsStartingSetup(false);
    }
  };

  const onVerifySetup = async (data: VerifyCodeInput) => {
    setSubmitError("");
    try {
      await apiClient.post(API_ROUTES.USERS.ME_TOTP_VERIFY, { code: data.code });
      notify.success("Đã bật xác thực hai yếu tố.");
      setSetupStep("idle");
      setSetupData(null);
      verifyForm.reset();
      await loadStatus();
    } catch {
      setSubmitError("Mã xác thực không hợp lệ hoặc đã hết hạn.");
    }
  };

  const onDisable = async (data: DisableInput) => {
    setSubmitError("");
    try {
      await apiClient.delete(API_ROUTES.USERS.ME_TOTP, { data });
      notify.success("Đã tắt xác thực hai yếu tố.");
      disableForm.reset();
      setSetupStep("idle");
      await loadStatus();
    } catch {
      setSubmitError("Không thể tắt 2FA. Kiểm tra mật khẩu và mã xác thực.");
    }
  };

  const copyBackupCodes = async () => {
    if (!setupData?.backupCodes?.length) return;
    try {
      await navigator.clipboard.writeText(setupData.backupCodes.join("\n"));
      notify.success("Đã sao chép mã dự phòng.");
    } catch {
      notify.error("Không thể sao chép mã dự phòng. Vui lòng thử lại.");
    }
  };

  const isEnabled = status?.enabled === true;
  const isPending = status?.pendingVerification === true;

  return (
    <DashboardPageShell
      title="Bảo mật & 2FA"
      subtitle="Bảo vệ tài khoản bằng xác thực hai yếu tố và các thiết lập liên quan."
      breadcrumbs={breadcrumbFromSettings("Bảo mật & 2FA")}
    >
      <div className="grid grid-cols-1 items-start gap-8 lg:grid-cols-3">
        <div className="space-y-8 lg:col-span-2">
          <section className={`${settingsCardClassName} relative overflow-hidden`}>
            <div className="absolute top-0 right-0 -mt-16 -mr-16 h-32 w-32 rounded-bl-full bg-[#00685f]/5" />

            <div className="relative mb-8 flex flex-col gap-6 md:flex-row md:items-center">
              <div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-2xl bg-[#e9f6f3] text-[#00685f] ring-4 ring-[#e9f6f3] shadow-md">
                <ShieldCheck className="h-10 w-10" aria-hidden="true" />
              </div>
              <div className="min-w-0 flex-grow">
                <div className="flex flex-wrap items-center gap-3">
                  <h2 className="text-2xl font-bold text-[#121e1c]">Xác thực hai yếu tố (2FA)</h2>
                  {!loadingStatus && !statusError ? (
                    <TotpStatusBadge enabled={isEnabled} pending={isPending} />
                  ) : null}
                </div>
                <p className="mt-2 text-sm leading-6 text-[#6d7a77]">
                  Mỗi lần đăng nhập, bạn nhập mật khẩu rồi mã 6 chữ số từ ứng dụng xác thực trên điện
                  thoại — giống ngân hàng hay email công việc của bạn.
                </p>
              </div>
            </div>

            {loadingStatus ? (
              <p className="text-sm text-[#6d7a77]">Đang tải trạng thái…</p>
            ) : statusError ? (
              <div className="space-y-4 rounded-xl bg-[#fff8f7] p-4 ring-1 ring-[#ba1a1a]/15">
                <p role="alert" className="text-sm font-medium text-[#ba1a1a]">
                  {statusError}
                </p>
                <button type="button" onClick={() => void loadStatus()} className={secondaryButtonClassName}>
                  Thử lại
                </button>
              </div>
            ) : isAdmin ? (
              <div className="space-y-4 rounded-xl bg-[#fff4e8] px-4 py-4 ring-1 ring-[#924628]/15">
                <p className="text-sm leading-6 text-[#924628]">
                  Tài khoản quản trị dùng xác thực hai yếu tố riêng khi đăng nhập khu vực Admin — không
                  thiết lập tại trang cài đặt người dùng này.
                </p>
                <Link
                  href="/admin/login"
                  className="inline-flex text-sm font-bold text-[#00685f] underline-offset-2 hover:underline"
                >
                  Mở trang đăng nhập quản trị
                </Link>
              </div>
            ) : isEnabled ? (
              <div className="space-y-6">
                <p className="rounded-xl bg-[#e9f6f3] px-4 py-3 text-sm leading-6 text-[#00685f]">
                  2FA đang bật. Khi đăng nhập, bạn sẽ cần mã từ ứng dụng xác thực hoặc một mã dự phòng
                  chưa dùng.
                </p>
                <form className="space-y-6" onSubmit={disableForm.handleSubmit(onDisable)} noValidate>
                  <p className="text-sm font-bold text-[#3d4947]">Tắt xác thực hai yếu tố</p>
                  <p className="text-sm leading-6 text-[#6d7a77]">
                    Cần mật khẩu hiện tại và mã TOTP (hoặc mã dự phòng) để xác nhận.
                  </p>
                  <SecurityField
                    id="disable-password"
                    label="Mật khẩu *"
                    type="password"
                    autoComplete="current-password"
                    placeholder="Mật khẩu đang dùng"
                    error={disableForm.formState.errors.password?.message}
                    registration={disableForm.register("password")}
                    icon={Lock}
                  />
                  <SecurityField
                    id="disable-code"
                    label="Mã xác thực *"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    placeholder="Mã 6 chữ số hoặc mã dự phòng"
                    error={disableForm.formState.errors.code?.message}
                    registration={disableForm.register("code")}
                    icon={ShieldCheck}
                  />
                  <div className="flex flex-col-reverse justify-end gap-4 border-t border-[#bcc9c6]/20 pt-6 sm:flex-row">
                    <button
                      type="submit"
                      disabled={disableForm.formState.isSubmitting}
                      className="rounded-xl border-2 border-[#ba1a1a]/30 px-8 py-3 text-sm font-bold text-[#ba1a1a] transition hover:bg-[#ba1a1a]/5 disabled:opacity-60"
                    >
                      {disableForm.formState.isSubmitting ? "Đang xử lý…" : "Tắt xác thực hai yếu tố"}
                    </button>
                  </div>
                </form>
              </div>
            ) : setupStep === "setup" && setupData ? (
              <div className="space-y-6">
                <ol className="space-y-3 text-sm leading-6 text-[#3d4947]">
                  <li className="flex gap-3 rounded-xl bg-[#e9f6f3] p-4">
                    <span className="font-bold text-[#00685f]">1.</span>
                    <span>Quét mã QR bằng Google Authenticator, Authy hoặc ứng dụng tương tự.</span>
                  </li>
                  <li className="flex gap-3 rounded-xl bg-[#e9f6f3] p-4">
                    <span className="font-bold text-[#00685f]">2.</span>
                    <span>Nhập mã 6 chữ số từ ứng dụng để xác nhận.</span>
                  </li>
                  <li className="flex gap-3 rounded-xl bg-[#e9f6f3] p-4">
                    <span className="font-bold text-[#00685f]">3.</span>
                    <span>Lưu mã dự phòng ở nơi an toàn — chỉ hiển thị một lần.</span>
                  </li>
                </ol>

                <div className="flex flex-col items-center gap-6 rounded-2xl bg-[#f6fbfa] p-6 sm:flex-row sm:items-start">
                  <div className="rounded-xl bg-white p-4 shadow-sm ring-1 ring-[#bcc9c6]/20">
                    <QRCodeCanvas value={setupData.otpauthUri} size={168} />
                  </div>
                  <div className="min-w-0 text-sm text-[#3d4947]">
                    <p className="font-bold text-[#121e1c]">Không quét được?</p>
                    <p className="mt-2 leading-6">Nhập khóa thủ công vào ứng dụng xác thực:</p>
                    <p className="mt-2 break-all rounded-lg bg-white px-3 py-2 font-mono text-xs ring-1 ring-[#bcc9c6]/25">
                      {setupData.secret}
                    </p>
                  </div>
                </div>

                <form className="space-y-6" onSubmit={verifyForm.handleSubmit(onVerifySetup)} noValidate>
                  <SecurityField
                    id="verify-code"
                    label="Mã xác thực *"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    placeholder="000000"
                    error={verifyForm.formState.errors.code?.message}
                    registration={verifyForm.register("code")}
                    icon={ShieldCheck}
                  />
                  <div className="flex flex-col-reverse justify-end gap-4 border-t border-[#bcc9c6]/20 pt-6 sm:flex-row">
                    <button
                      type="button"
                      className={secondaryButtonClassName}
                      onClick={() => setSetupStep("backup")}
                    >
                      Xem mã dự phòng
                    </button>
                    <button
                      type="submit"
                      disabled={verifyForm.formState.isSubmitting}
                      className={primaryButtonClassName}
                    >
                      {verifyForm.formState.isSubmitting ? "Đang xác nhận…" : "Xác nhận và bật 2FA"}
                    </button>
                  </div>
                </form>
              </div>
            ) : setupStep === "backup" && setupData ? (
              <div className="space-y-6">
                <div className="rounded-xl border border-[#ba1a1a]/20 bg-[#fff8f7] px-4 py-3">
                  <p className="text-sm font-semibold text-[#ba1a1a]">
                    Lưu các mã dự phòng ở nơi an toàn — chỉ hiển thị một lần.
                  </p>
                </div>
                <ul className="grid grid-cols-2 gap-2 font-mono text-sm sm:grid-cols-3">
                  {setupData.backupCodes.map((code) => (
                    <li
                      key={code}
                      className="rounded-xl bg-[#e9f6f3] px-3 py-2 text-center font-bold text-[#121e1c]"
                    >
                      {code}
                    </li>
                  ))}
                </ul>
                <div className="flex flex-wrap gap-3">
                  <button type="button" onClick={copyBackupCodes} className={secondaryButtonClassName}>
                    <Copy className="h-4 w-4" aria-hidden="true" />
                    Sao chép tất cả
                  </button>
                  <button
                    type="button"
                    className="text-sm font-bold text-[#00685f] underline-offset-2 hover:underline"
                    onClick={() => setSetupStep("setup")}
                  >
                    Quay lại nhập mã TOTP
                  </button>
                </div>
              </div>
            ) : (
              <div className="space-y-6">
                {isPending ? (
                  <p className="rounded-xl bg-[#fff4e8] px-4 py-3 text-sm leading-6 text-[#924628]">
                    Bạn đã bắt đầu thiết lập nhưng chưa xác nhận. Tiếp tục để hoàn tất trước khi đăng
                    nhập yêu cầu mã xác thực.
                  </p>
                ) : (
                  <p className="text-sm leading-6 text-[#3d4947]">
                    Khuyến nghị bật 2FA nếu bạn lưu kết quả khám hoặc dữ liệu nhạy cảm trên HealthLens.
                  </p>
                )}
                <button
                  type="button"
                  onClick={() => void startSetup()}
                  disabled={isStartingSetup}
                  className={primaryButtonClassName}
                >
                  <QrCode className="h-5 w-5" aria-hidden="true" />
                  {isStartingSetup
                    ? "Đang khởi tạo…"
                    : isPending
                      ? "Tiếp tục thiết lập 2FA"
                      : "Bật xác thực hai yếu tố"}
                </button>
              </div>
            )}

            {submitError ? (
              <p role="alert" className="mt-6 text-sm font-medium text-[#ba1a1a]">
                {submitError}
              </p>
            ) : null}
          </section>

          {isEnabled ? (
            <section className={settingsCardClassName}>
              <div className="mb-6 flex items-center gap-3">
                <ShieldCheck className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
                <h2 className="text-xl font-bold text-[#121e1c]">Sau khi bật 2FA</h2>
              </div>
              <ul className="space-y-4 text-sm leading-6 text-[#3d4947]">
                <li className="flex gap-3 rounded-xl bg-[#e9f6f3] p-4">
                  <span className="font-bold text-[#00685f]">1.</span>
                  <span>
                    Mỗi lần đăng nhập trên thiết bị mới, bạn nhập mật khẩu rồi mã từ ứng dụng xác thực.
                  </span>
                </li>
                <li className="flex gap-3 rounded-xl bg-[#e9f6f3] p-4">
                  <span className="font-bold text-[#00685f]">2.</span>
                  <span>
                    Mất điện thoại? Dùng <strong>mã dự phòng</strong> đã lưu khi thiết lập (mỗi mã dùng
                    một lần).
                  </span>
                </li>
                <li className="flex gap-3 rounded-xl bg-[#e9f6f3] p-4">
                  <span className="font-bold text-[#00685f]">3.</span>
                  <span>
                    Đổi điện thoại mới? Tắt 2FA tại đây rồi bật lại để quét mã QR mới — cần mật khẩu và
                    mã hiện tại.
                  </span>
                </li>
              </ul>
            </section>
          ) : null}
        </div>

        <aside className="space-y-8">
          <section className={settingsCardClassName}>
            <div className="mb-8 flex items-center gap-3">
              <Shield className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
              <h2 className="text-xl font-bold text-[#121e1c]">Bảo mật & tài khoản</h2>
            </div>
            <SettingsAccountNav active="security" />
          </section>

          <section className={settingsTipCardClassName}>
            <div className="relative z-10">
              <div className="mb-6 flex items-center gap-3">
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-white/50 backdrop-blur-md">
                  <ShieldCheck className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
                </div>
                <h3 className="text-xl font-bold text-[#121e1c]">Mẹo bảo mật</h3>
              </div>
              <ul className="space-y-3 text-sm leading-relaxed text-[#274d48]">
                <li className="rounded-xl bg-white/45 px-4 py-3">
                  Lưu mã dự phòng ngoài ứng dụng HealthLens — ví giấy hoặc két an toàn.
                </li>
                <li className="rounded-xl bg-white/45 px-4 py-3">
                  Không chụp màn hình mã QR hoặc gửi mã dự phòng qua chat.
                </li>
                <li className="rounded-xl bg-white/45 px-4 py-3">
                  Mất ứng dụng xác thực? Liên hệ hỗ trợ kèm thông tin tài khoản đã xác thực email.
                </li>
              </ul>
            </div>
            <div
              className="pointer-events-none absolute -bottom-10 -right-10 h-40 w-40 rounded-full bg-[#00685f]/10 blur-3xl"
              aria-hidden="true"
            />
          </section>

          <section className={settingsCardClassName}>
            <div className="mb-6 flex items-center gap-3">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-[#e9f6f3]">
                <KeyRound className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
              </div>
              <h3 className="text-xl font-bold text-[#121e1c]">Đổi mật khẩu</h3>
            </div>
            <p className="text-sm leading-relaxed text-[#3d4947]">
              Cập nhật mật khẩu đăng nhập mà không cần thoát khỏi cài đặt.
            </p>
            <Link
              href="/settings/change-password"
              className="mt-5 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-[#00685f] px-4 text-sm font-bold text-white shadow-lg shadow-[#00685f]/20 transition hover:bg-[#008378] active:scale-[0.98]"
            >
              <KeyRound className="h-4 w-4" aria-hidden="true" />
              Đến trang đổi mật khẩu
            </Link>
          </section>

          <SettingsDirectContactCard
            title="Cần hỗ trợ bảo mật?"
            description="Liên hệ đội ngũ nếu bạn mất quyền truy cập ứng dụng xác thực hoặc mã dự phòng."
          />
        </aside>
      </div>
    </DashboardPageShell>
  );
}
