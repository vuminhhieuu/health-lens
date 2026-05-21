"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { changePasswordSchema, type ChangePasswordInput } from "@healthlens/shared/schemas/auth";
import { Eye, EyeOff, KeyRound, Lock, Mail, Shield, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { useForm, type UseFormRegisterReturn } from "react-hook-form";

import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { changePasswordErrorMessage } from "@/lib/i18n/messages";
import { notify } from "@/lib/notify";

import { SettingsAccountNav } from "../_components/SettingsAccountNav";
import { SettingsDirectContactCard } from "../_components/SettingsDirectContactCard";
import { settingsCardClassName, settingsTipCardClassName } from "../_components/settingsStyles";

const inputClassName =
  "h-12 w-full rounded-xl border-none bg-[#e9f6f3] px-4 pl-11 pr-12 text-[#121e1c] font-medium outline-none transition focus:ring-2 focus:ring-[#00685f]/20";

const labelClassName = "text-sm font-bold text-[#6d7a77]";

type PasswordFieldProps = {
  id: string;
  label: string;
  autoComplete: string;
  visible: boolean;
  onToggleVisible: () => void;
  error?: string;
  registration: UseFormRegisterReturn;
};

function PasswordField({
  id,
  label,
  autoComplete,
  visible,
  onToggleVisible,
  error,
  registration,
}: PasswordFieldProps) {
  const errorId = `${id}-error`;
  const hasError = Boolean(error);

  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={id} className={labelClassName}>
        {label}
      </label>
      <div className="relative">
        <Lock
          className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-[#6d7a77]"
          aria-hidden="true"
        />
        <input
          id={id}
          type={visible ? "text" : "password"}
          autoComplete={autoComplete}
          className={inputClassName}
          {...registration}
          aria-invalid={hasError}
          aria-describedby={hasError ? errorId : undefined}
        />
        <button
          type="button"
          className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full p-1 text-[#6d7a77] transition hover:bg-[#d8e5e2]"
          onClick={onToggleVisible}
          aria-label={visible ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
        >
          {visible ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
        </button>
      </div>
      {hasError ? (
        <p id={errorId} role="alert" className="text-sm text-[#ba1a1a]">
          {error}
        </p>
      ) : null}
    </div>
  );
}

export default function ChangePasswordPage() {
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting, isValid },
  } = useForm<ChangePasswordInput>({
    resolver: zodResolver(changePasswordSchema),
    mode: "onChange",
    defaultValues: {
      currentPassword: "",
      newPassword: "",
      confirmPassword: "",
    },
  });

  const onSubmit = async (data: ChangePasswordInput) => {
    try {
      await apiClient.post(API_ROUTES.AUTH.CHANGE_PASSWORD, {
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      });
      notify.success("Mật khẩu đã được cập nhật thành công");
      reset();
      setShowCurrent(false);
      setShowNew(false);
      setShowConfirm(false);
    } catch (error: unknown) {
      notify.error(changePasswordErrorMessage(error));
    }
  };

  const handleCancel = () => {
    reset();
    setShowCurrent(false);
    setShowNew(false);
    setShowConfirm(false);
  };

  return (
    <DashboardPageShell
      title="Bảo mật"
      subtitle="Đổi mật khẩu và quản lý các thiết lập bảo vệ tài khoản."
      breadcrumbs={[
        { label: "Trang chủ", href: "/home" },
        { label: "Cài đặt", href: "/settings" },
        { label: "Đổi mật khẩu" },
      ]}
    >
      <div className="grid grid-cols-1 items-start gap-8 lg:grid-cols-3">
        {/* Cột chính (2/3) — form đổi mật khẩu */}
        <div className="space-y-8 lg:col-span-2">
          <section className={`${settingsCardClassName} relative overflow-hidden`}>
            <div className="absolute top-0 right-0 -mt-16 -mr-16 h-32 w-32 rounded-bl-full bg-[#00685f]/5" />

            <div className="relative mb-10 flex flex-col gap-6 md:flex-row md:items-center">
              <div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-2xl bg-[#e9f6f3] text-[#00685f] ring-4 ring-[#e9f6f3] shadow-md">
                <KeyRound className="h-10 w-10" aria-hidden="true" />
              </div>
              <div className="min-w-0 flex-grow">
                <h2 className="text-2xl font-bold text-[#121e1c]">Đổi mật khẩu</h2>
                <p className="mt-2 text-sm leading-6 text-[#6d7a77]">
                  Nhập mật khẩu hiện tại và mật khẩu mới. Bạn không cần thoát ra email đặt lại mật khẩu
                  nếu vẫn nhớ mật khẩu đang dùng.
                </p>
              </div>
            </div>

            <form className="relative space-y-6" onSubmit={handleSubmit(onSubmit)} noValidate>
              <PasswordField
                id="currentPassword"
                label="Mật khẩu hiện tại *"
                autoComplete="current-password"
                visible={showCurrent}
                onToggleVisible={() => setShowCurrent((v) => !v)}
                error={errors.currentPassword?.message}
                registration={register("currentPassword")}
              />

              <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
                <PasswordField
                  id="newPassword"
                  label="Mật khẩu mới *"
                  autoComplete="new-password"
                  visible={showNew}
                  onToggleVisible={() => setShowNew((v) => !v)}
                  error={errors.newPassword?.message}
                  registration={register("newPassword")}
                />
                <PasswordField
                  id="confirmPassword"
                  label="Xác nhận mật khẩu mới *"
                  autoComplete="new-password"
                  visible={showConfirm}
                  onToggleVisible={() => setShowConfirm((v) => !v)}
                  error={errors.confirmPassword?.message}
                  registration={register("confirmPassword")}
                />
              </div>

              <div className="flex flex-col-reverse justify-end gap-4 border-t border-[#bcc9c6]/20 pt-6 sm:flex-row">
                <button
                  type="button"
                  onClick={handleCancel}
                  className="rounded-xl px-8 py-3 text-sm font-bold text-[#3d4947] transition hover:bg-[#e9f6f3]"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting || !isValid}
                  className="rounded-xl bg-gradient-to-r from-[#00685f] to-[#008378] px-8 py-3 text-sm font-bold text-white shadow-lg shadow-[#00685f]/20 transition active:scale-95 hover:brightness-105 disabled:opacity-60"
                >
                  {isSubmitting ? "Đang lưu..." : "Lưu mật khẩu mới"}
                </button>
              </div>
            </form>
          </section>

          {/* Card phụ dưới form — “điều gì xảy ra sau khi lưu” (tương tự block Thông tin sức khỏe trên profile) */}
          <section className={settingsCardClassName}>
            <div className="mb-6 flex items-center gap-3">
              <ShieldCheck className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
              <h2 className="text-xl font-bold text-[#121e1c]">Sau khi lưu mật khẩu mới</h2>
            </div>
            <ul className="space-y-4 text-sm leading-6 text-[#3d4947]">
              <li className="flex gap-3 rounded-xl bg-[#e9f6f3] p-4">
                <span className="font-bold text-[#00685f]">1.</span>
                <span>
                  Phiên đăng nhập trên <strong>thiết bị khác</strong> có thể bị đăng xuất — bạn cần đăng
                  nhập lại bằng mật khẩu mới.
                </span>
              </li>
              <li className="flex gap-3 rounded-xl bg-[#e9f6f3] p-4">
                <span className="font-bold text-[#00685f]">2.</span>
                <span>
                  Trên <strong>thiết bị này</strong>, bạn vẫn tiếp tục dùng được cho đến khi phiên hết
                  hạn hoặc bạn đăng xuất.
                </span>
              </li>
              <li className="flex gap-3 rounded-xl bg-[#e9f6f3] p-4">
                <span className="font-bold text-[#00685f]">3.</span>
                <span>
                  Nếu bạn dùng trình quản lý mật khẩu, hãy cập nhật mật khẩu đã lưu cho HealthLens.
                </span>
              </li>
            </ul>
          </section>
        </div>

        {/* Cột phụ (1/3) — định hướng, mẹo, hỗ trợ */}
        <div className="space-y-8">
          {/* 1. Lối tắt bảo mật (đối xứng “Cài đặt tài khoản” trên profile) */}
          <section className={settingsCardClassName}>
            <div className="mb-8 flex items-center gap-3">
              <Shield className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
              <h2 className="text-xl font-bold text-[#121e1c]">Bảo mật & tài khoản</h2>
            </div>
            <SettingsAccountNav active="change-password" />
          </section>

          <section className={settingsTipCardClassName}>
            <div className="relative z-10">
              <div className="mb-6 flex items-center gap-3">
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-white/50 backdrop-blur-md">
                  <ShieldCheck className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
                </div>
                <h3 className="text-xl font-bold text-[#121e1c]">Mẹo mật khẩu mạnh</h3>
              </div>
              <ul className="space-y-3 text-sm leading-relaxed text-[#274d48]">
                <li className="rounded-xl bg-white/45 px-4 py-3">
                  Dùng cụm dễ nhớ nhưng khó đoán (ví dụ câu + số + ký tự đặc biệt).
                </li>
                <li className="rounded-xl bg-white/45 px-4 py-3">
                  Không dùng lại mật khẩu email hoặc mạng xã hội.
                </li>
                <li className="rounded-xl bg-white/45 px-4 py-3">
                  Đổi mật khẩu nếu nghi ngờ thiết bị bị lộ.
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
                <Mail className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
              </div>
              <h3 className="text-xl font-bold text-[#121e1c]">Không nhớ mật khẩu?</h3>
            </div>
            <p className="text-sm leading-relaxed text-[#3d4947]">
              Nếu bạn không nhớ mật khẩu hiện tại, hãy đặt lại qua email thay vì thử nhiều lần.
            </p>
            <Link
              href="/forgot-password"
              className="mt-5 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-[#00685f] px-4 text-sm font-bold text-white shadow-lg shadow-[#00685f]/20 transition hover:bg-[#008378] active:scale-[0.98]"
            >
              <Mail className="h-4 w-4" aria-hidden="true" />
              Đặt lại qua email
            </Link>
          </section>

          <SettingsDirectContactCard
            title="Cần hỗ trợ?"
            description="Liên hệ đội ngũ nếu bạn gặp khó khăn khi đổi mật khẩu."
          />
        </div>
      </div>
    </DashboardPageShell>
  );
}
