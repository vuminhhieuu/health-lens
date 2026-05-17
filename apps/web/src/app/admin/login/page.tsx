"use client";

import { ShieldCheck, LogIn, KeyRound, QrCode, Copy, Check } from "lucide-react";
import { useState } from "react";
import { QRCodeCanvas } from "qrcode.react";

import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { notify } from "@/lib/notify";

type LoginStep = "credentials" | "totp" | "totp-method" | "totp-setup" | "totp-success";

interface AdminLoginState {
  accessToken: string;
  email: string;
}

export default function AdminLoginPage() {
  const [step, setStep] = useState<LoginStep>("credentials");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [totpCode, setTotpCode] = useState("");
  const [totpVerifyMethod, setTotpVerifyMethod] = useState<"app" | "backup">("app");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [secretCopied, setSecretCopied] = useState(false);
  const [adminState, setAdminState] = useState<AdminLoginState | null>(null);
  const [totpSetup, setTotpSetup] = useState<{
    secret: string;
    qrCodeUrl: string;
    backupCodes?: string[];
  } | null>(null);

  const handleCredentialsSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);

    try {
      const response = await apiClient.post(API_ROUTES.ADMIN_AUTH.LOGIN, {
        email,
        password,
      });

      const data = response.data.data;
      setAdminState({ accessToken: data.accessToken, email: data.email });

      if (data.totpSetupRequired) {
        setStep("totp-method");
      } else if (data.totpRequired) {
        setStep("totp");
      } else {
        handleLoginSuccess(data.accessToken);
      }
    } catch (err: unknown) {
      if (
        err &&
        typeof err === "object" &&
        "response" in err &&
        err.response &&
        typeof err.response === "object"
      ) {
        const resp = err.response as {
          status?: number;
          data?: { detail?: string };
        };
        if (resp.status === 429) {
          setError("Tài khoản bị khóa tạm thời. Vui lòng thử lại sau.");
        } else if (resp.status === 401) {
          setError("Email hoặc mật khẩu không đúng.");
        } else {
          setError("Đăng nhập thất bại. Vui lòng thử lại.");
        }
      } else {
        setError("Không thể kết nối đến máy chủ.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  const fetchTotpSetup = async (token: string) => {
    try {
      setIsLoading(true);
      const setupRes = await apiClient.post(
        API_ROUTES.ADMIN_AUTH.TOTP_SETUP,
        {},
        { headers: { Authorization: `Bearer ${token}` } },
      );
      setTotpSetup(setupRes.data.data);
    } catch (err) {
      const error = err as { response?: { data?: { detail?: string; message?: string } } };
      setError(
        error.response?.data?.detail ||
          error.response?.data?.message ||
          "Không thể khởi tạo MFA.",
      );
    } finally {
      setIsLoading(false);
    }
  };

  const handleTotpSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);

    try {
      // Re-login with TOTP code
      const response = await apiClient.post(API_ROUTES.ADMIN_AUTH.LOGIN, {
        email,
        password,
        totpCode,
      });

      const data = response.data.data;
      handleLoginSuccess(data.accessToken);
    } catch (err: unknown) {
      const error = err as { response?: { data?: { detail?: string; message?: string } } };
      setError(
        error.response?.data?.detail ||
          error.response?.data?.message ||
          "Xác thực thất bại. Vui lòng thử lại.",
      );
    } finally {
      setIsLoading(false);
    }
  };

  const handleTotpSetupVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);

    try {
      const response = await apiClient.post(
        API_ROUTES.ADMIN_AUTH.TOTP_VERIFY,
        { code: totpCode },
        {
          headers: {
            Authorization: `Bearer ${adminState?.accessToken}`,
          },
        },
      );

      const data = response.data.data;
      setAdminState({ ...adminState, accessToken: data.accessToken, email: adminState?.email || "" });
      setStep("totp-success");
    } catch (err: unknown) {
      const error = err as { response?: { data?: { detail?: string; message?: string } } };
      setError(
        error.response?.data?.detail ||
          error.response?.data?.message ||
          "Xác thực thiết lập thất bại.",
      );
    } finally {
      setIsLoading(false);
    }
  };

  const handleLoginSuccess = (token: string) => {
    // Store admin token in sessionStorage (not zustand — separate from user auth)
    if (typeof window !== "undefined") {
      sessionStorage.setItem("admin_access_token", token);
      window.location.href = "/admin";
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      {/* Header */}
      <header className="fixed top-0 z-50 w-full border-b border-slate-200 bg-white/80 backdrop-blur-md">
        <div className="mx-auto flex h-16 w-full items-center justify-between px-6">
          <div className="flex items-center gap-3">
            <ShieldCheck className="h-6 w-6 text-teal-600" />
            <span className="text-lg font-bold tracking-tight text-slate-900">
              HealthLens <span className="text-teal-600">Admin</span>
            </span>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex min-h-screen items-center justify-center pt-16 px-4 pb-12">
        <section className={`w-full ${step === 'totp-method' || step === 'totp-setup' ? 'max-w-[640px]' : 'max-w-md'} animate-in fade-in slide-in-from-bottom-4 duration-500`}>
          {step === "credentials" && (
            <div className="bg-white rounded-2xl p-8 shadow-sm border border-slate-200">
              <div className="mb-10 text-center">
                <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-teal-50">
                  <ShieldCheck className="h-8 w-8 text-teal-600" />
                </div>
                <h1 className="mb-2 text-2xl font-extrabold tracking-tight">
                  Quản trị viên
                </h1>
                <p className="text-sm text-slate-500">
                  Đăng nhập để truy cập hệ thống quản trị
                </p>
              </div>

              <form className="space-y-6" onSubmit={handleCredentialsSubmit} noValidate>
                <div className="space-y-2">
                  <label htmlFor="admin-email" className="ml-1 block text-sm font-semibold text-slate-700">
                    Email
                  </label>
                  <input
                    id="admin-email"
                    type="email"
                    autoComplete="email"
                    placeholder="admin@healthlens.vn"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value);
                      setError("");
                    }}
                    className="h-14 w-full rounded-lg border border-slate-200 bg-white shadow-sm px-4 text-base text-slate-900 outline-none transition placeholder:text-gray-500 focus:border-teal-500 focus:ring-1 focus:ring-teal-500"
                    required
                  />
                </div>

                <div className="space-y-2">
                  <label htmlFor="admin-password" className="ml-1 block text-sm font-semibold text-slate-700">
                    Mật khẩu
                  </label>
                  <input
                    id="admin-password"
                    type="password"
                    autoComplete="current-password"
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value);
                      setError("");
                    }}
                    className="h-14 w-full rounded-lg border border-slate-200 bg-white shadow-sm px-4 text-base text-slate-900 outline-none transition placeholder:text-gray-500 focus:border-teal-500 focus:ring-1 focus:ring-teal-500"
                    required
                  />
                </div>

                {error ? (
                  <p
                    id="admin-credentials-error"
                    role="alert"
                    className="text-center text-sm font-medium text-red-600"
                  >
                    {error}
                  </p>
                ) : null}

                <button
                  id="admin-login-submit"
                  type="submit"
                  disabled={isLoading || !email || !password}
                  className="flex h-14 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-br from-teal-600 to-teal-700 text-lg font-bold text-white shadow-lg shadow-teal-600/20 transition hover:brightness-110 disabled:opacity-60"
                >
                  {isLoading ? (
                    "Đang xử lý..."
                  ) : (
                    <>
                      <LogIn className="h-5 w-5" />
                      Tiếp tục
                    </>
                  )}
                </button>
              </form>
            </div>
          )}

          {step === "totp" && (
            <div className="bg-white rounded-2xl p-8 shadow-sm border border-slate-200">
              <div className="mb-10 text-center">
                <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-teal-50">
                  <KeyRound className="h-8 w-8 text-teal-600" />
                </div>
                <h1 className="mb-2 text-2xl font-extrabold tracking-tight">
                  {totpVerifyMethod === "app" ? "Xác thực hai yếu tố" : "Sử dụng mã dự phòng"}
                </h1>
                <p className="text-sm text-slate-500">
                  {totpVerifyMethod === "app" 
                    ? "Nhập mã từ ứng dụng Authenticator"
                    : "Nhập 1 trong 16 mã dự phòng gồm 10 ký tự của bạn"}
                </p>
              </div>

              <form className="space-y-6" onSubmit={handleTotpSubmit} noValidate>
                <div className="space-y-2">
                  <label htmlFor="admin-totp-code" className="ml-1 block text-sm font-semibold text-slate-700">
                    {totpVerifyMethod === "app" ? "Mã xác thực" : "Mã dự phòng"}
                  </label>
                  
                  {totpVerifyMethod === "app" ? (
                    <input
                      id="admin-totp-code"
                      type="text"
                      inputMode="numeric"
                      maxLength={6}
                      autoComplete="one-time-code"
                      placeholder="000 000"
                      value={totpCode}
                      onChange={(e) => {
                        setTotpCode(e.target.value.replace(/\D/g, "").slice(0, 6));
                        setError("");
                      }}
                      aria-describedby={error ? "admin-totp-error" : undefined}
                      aria-invalid={Boolean(error)}
                      className="h-16 w-full rounded-xl border border-slate-200 bg-slate-50 px-4 text-center text-3xl font-mono tracking-[0.5em] text-slate-900 outline-none transition placeholder:text-slate-300 focus:border-teal-500 focus:ring-1 focus:ring-teal-500 focus:bg-white"
                      required
                    />
                  ) : (
                    <input
                      id="admin-totp-code-backup"
                      type="text"
                      maxLength={11}
                      placeholder="HLABCD-00"
                      value={totpCode}
                      onChange={(e) => {
                        const val = e.target.value.toUpperCase().replace(/[^A-Z0-9-]/g, "");
                        setTotpCode(val.slice(0, 11));
                        setError("");
                      }}
                      aria-describedby={error ? "admin-totp-error" : undefined}
                      aria-invalid={Boolean(error)}
                      className="h-16 w-full rounded-xl border border-slate-200 bg-slate-50 px-4 text-center text-xl md:text-2xl font-mono tracking-[0.2em] md:tracking-[0.5em] text-slate-900 outline-none transition placeholder:text-slate-300 focus:border-teal-500 focus:ring-1 focus:ring-teal-500 focus:bg-white uppercase"
                      required
                    />
                  )}
                </div>

                {error ? (
                  <p
                    id="admin-totp-error"
                    role="alert"
                    className="text-center text-sm font-medium text-red-600"
                  >
                    {error}
                  </p>
                ) : null}

                <button
                  id="admin-totp-submit"
                  type="submit"
                  disabled={isLoading || (totpVerifyMethod === "app" ? totpCode.length !== 6 : (totpCode.length < 10))}
                  className="flex h-14 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-br from-teal-500 to-teal-600 text-lg font-bold text-white shadow-md transition hover:to-teal-700 hover:shadow-lg disabled:opacity-60"
                >
                  {isLoading ? "Đang xác thực..." : "Xác nhận"}
                </button>

                <div className="flex flex-col space-y-4 pt-4 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => {
                      setTotpCode("");
                      setError("");
                      setTotpVerifyMethod(totpVerifyMethod === "app" ? "backup" : "app");
                    }}
                    className="text-sm font-semibold text-teal-700 hover:text-teal-900 transition text-center w-full"
                  >
                    {totpVerifyMethod === "app" ? "Sử dụng mã dự phòng" : "Sử dụng ứng dụng Authenticator"}
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setStep("credentials");
                      setTotpCode("");
                      setError("");
                      setTotpVerifyMethod("app");
                    }}
                    className="w-full text-center text-sm font-semibold text-slate-500 hover:text-slate-800 transition"
                  >
                    ← Quay lại đăng nhập
                  </button>
                </div>
              </form>
            </div>
          )}

          {step === "totp-method" && (
            <>
              <div className="flex flex-col items-center mb-10 text-center">
                <h1 className="text-3xl font-extrabold text-teal-900 tracking-tight mb-2">
                  Xác Thực Hai Yếu Tố (MFA)
                </h1>
                <p className="text-slate-600 max-w-md">
                  Tăng cường bảo mật tài khoản quản trị viên của bạn bằng cách thiết lập lớp bảo vệ bổ sung.
                </p>
              </div>

              {/* Stepper */}
              <div className="flex items-center justify-between mb-12 relative px-4">
                <div className="absolute top-1/2 left-0 w-full h-[2px] bg-slate-200 -translate-y-1/2 -z-10"></div>
                <div className="absolute top-1/2 left-0 w-0 h-[2px] bg-teal-600 -translate-y-1/2 -z-10 transition-all duration-500"></div>
                {/* Step 1: Active */}
                <div className="flex flex-col items-center gap-2">
                  <div className="w-10 h-10 rounded-full bg-white border-4 border-teal-600 text-teal-600 flex items-center justify-center shadow-lg">
                    <span className="font-bold">1</span>
                  </div>
                  <span className="text-xs font-bold text-teal-900">Chọn phương thức</span>
                </div>
                {/* Step 2: Pending */}
                <div className="flex flex-col items-center gap-2">
                  <div className="w-10 h-10 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center">
                    <span className="font-bold">2</span>
                  </div>
                  <span className="text-xs font-medium text-slate-500">Thiết lập</span>
                </div>
                {/* Step 3: Pending */}
                <div className="flex flex-col items-center gap-2">
                  <div className="w-10 h-10 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center">
                    <span className="font-bold">3</span>
                  </div>
                  <span className="text-xs font-medium text-slate-500">Xác nhận</span>
                </div>
              </div>

              <div className="bg-white rounded-[24px] p-8 shadow-sm border border-slate-200 space-y-6">
                <div className="space-y-4">
                  <button
                    type="button"
                    onClick={() => {
                      if (adminState?.accessToken) {
                        setStep("totp-setup");
                        fetchTotpSetup(adminState.accessToken);
                      }
                    }}
                    className="flex w-full items-center gap-4 rounded-xl border border-teal-100 bg-white shadow-sm p-4 text-left transition hover:border-teal-500 hover:bg-slate-50 group"
                  >
                    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-teal-50 group-hover:bg-teal-100 transition">
                      <QrCode className="h-6 w-6 text-teal-600" />
                    </div>
                    <div>
                      <div className="font-bold text-teal-900">Ứng dụng xác thực</div>
                      <div className="text-xs text-slate-500">Google Authenticator, Authy...</div>
                    </div>
                  </button>
                  
                  <button
                    type="button"
                    disabled
                    className="flex w-full items-center gap-4 rounded-xl border border-slate-100 bg-slate-50 p-4 text-left opacity-50 cursor-not-allowed"
                  >
                    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-slate-200">
                      <ShieldCheck className="h-6 w-6 text-slate-400" />
                    </div>
                    <div>
                      <div className="font-bold text-slate-700">SMS / Email</div>
                      <div className="text-xs text-slate-500">Sắp ra mắt</div>
                    </div>
                  </button>
                </div>

                <div className="pt-4 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => {
                      setStep("credentials");
                      setError("");
                    }}
                    className="w-full text-center text-sm font-semibold text-teal-700 hover:text-teal-900 transition"
                  >
                    ← Quay lại đăng nhập
                  </button>
                </div>
              </div>
            </>
          )}

          {step === "totp-setup" && totpSetup && (
            <>
              <div className="flex flex-col items-center mb-10 text-center">
                <h1 className="text-3xl font-extrabold text-teal-900 tracking-tight mb-2">
                  Xác Thực Hai Yếu Tố (MFA)
                </h1>
                <p className="text-slate-600 max-w-md">
                  Tăng cường bảo mật tài khoản quản trị viên của bạn bằng cách thiết lập lớp bảo vệ bổ sung.
                </p>
              </div>

              {/* Stepper */}
              <div className="flex items-center justify-between mb-12 relative px-4 z-0">
                <div className="absolute top-1/2 left-0 w-full h-[2px] bg-slate-200 -translate-y-1/2 z-0"></div>
                <div className="absolute top-1/2 left-0 w-1/2 h-[2px] bg-teal-600 -translate-y-1/2 z-0 transition-all duration-500"></div>
                {/* Step 1: Completed */}
                <div className="flex flex-col items-center gap-2 relative z-10">
                  <div className="w-10 h-10 rounded-full bg-teal-600 text-white flex items-center justify-center shadow-md">
                    <Check className="h-5 w-5" />
                  </div>
                  <span className="text-xs font-semibold text-teal-600">Chọn phương thức</span>
                </div>
                {/* Step 2: Active */}
                <div className="flex flex-col items-center gap-2 relative z-10">
                  <div className="w-10 h-10 rounded-full bg-white border-4 border-teal-600 text-teal-600 flex items-center justify-center shadow-lg">
                    <span className="font-bold">2</span>
                  </div>
                  <span className="text-xs font-bold text-teal-900">Thiết lập</span>
                </div>
                {/* Step 3: Pending */}
                <div className="flex flex-col items-center gap-2 relative z-10">
                  <div className="w-10 h-10 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center">
                    <span className="font-bold">3</span>
                  </div>
                  <span className="text-xs font-medium text-slate-500">Xác nhận</span>
                </div>
              </div>

              <div className="bg-white rounded-[24px] p-8 shadow-sm border border-slate-200 space-y-10">
                {/* QR Code Section */}
                <div className="flex flex-col items-center text-center">
                  <h3 className="text-lg font-bold text-teal-900 mb-2">Quét mã QR bằng ứng dụng Authenticator</h3>
                  <p className="text-sm text-slate-600 mb-6">Sử dụng Google Authenticator, Microsoft Authenticator hoặc các ứng dụng tương đương.</p>
                  
                  <div className="p-4 bg-white border-8 border-slate-50 rounded-3xl shadow-inner">
                    <div className="bg-white flex items-center justify-center rounded-xl p-2">
                      <QRCodeCanvas
                        value={totpSetup.qrCodeUrl}
                        size={180}
                        level="M"
                        bgColor="#ffffff"
                        fgColor="#0f172a"
                        includeMargin={true}
                      />
                    </div>
                  </div>
                </div>

                {/* Manual Entry Section */}
                <div className="bg-slate-50 rounded-2xl p-6">
                  <p className="text-sm font-semibold text-teal-800 mb-3">Không thể quét? Nhập mã thủ công:</p>
                  <div className="flex items-center justify-between bg-white px-4 py-3 rounded-xl border border-teal-100/50">
                    <code className="font-mono text-teal-900 font-bold tracking-widest text-sm break-all">{totpSetup.secret}</code>
                    <button 
                      type="button"
                      onClick={() => {
                        navigator.clipboard.writeText(totpSetup.secret);
                        setSecretCopied(true);
                        setTimeout(() => setSecretCopied(false), 2000);
                      }}
                      className="flex shrink-0 items-center justify-center text-teal-600 hover:bg-teal-50 p-2 rounded-lg transition-colors ml-2"
                      title="Copy"
                    >
                      {secretCopied ? <Check className="h-5 w-5" /> : <Copy className="h-5 w-5" />}
                    </button>
                  </div>
                </div>

                {/* Verification Section */}
                <form className="space-y-4 pt-4 border-t border-slate-100" onSubmit={handleTotpSetupVerify} noValidate>
                  <label className="block text-sm font-bold text-teal-900">Nhập mã 6 chữ số từ ứng dụng</label>
                  <input
                    id="setup-totp-code"
                    type="text"
                    inputMode="numeric"
                    maxLength={6}
                    pattern="[0-9]*"
                    autoComplete="one-time-code"
                    placeholder="000 000"
                    value={totpCode}
                    onChange={(e) => {
                      setTotpCode(e.target.value.replace(/\D/g, "").slice(0, 6));
                      setError("");
                    }}
                    aria-describedby={error ? "admin-totp-setup-error" : undefined}
                    aria-invalid={Boolean(error)}
                    className="w-full h-14 bg-slate-50 border border-slate-200 rounded-xl text-center text-2xl font-mono tracking-[0.5em] focus:ring-2 focus:ring-teal-500 focus:bg-white transition-all text-teal-900 placeholder:text-slate-300"
                    required
                  />

                  {error ? (
                    <p
                      id="admin-totp-setup-error"
                      role="alert"
                      className="text-center text-sm font-medium text-red-600"
                    >
                      {error}
                    </p>
                  ) : null}

                  <div className="flex gap-4 pt-6">
                    <button
                      type="button"
                      onClick={() => {
                        setStep("totp-method");
                        setTotpCode("");
                        setError("");
                      }}
                      className="flex-1 h-12 bg-slate-100 text-teal-800 font-bold rounded-xl hover:bg-slate-200 transition-all"
                    >
                      Quay lại
                    </button>
                    <button
                      type="submit"
                      disabled={isLoading || totpCode.length !== 6}
                      className="flex-1 h-12 bg-gradient-to-br from-teal-500 to-teal-600 text-white font-bold rounded-xl shadow-md hover:shadow-lg hover:to-teal-700 active:scale-95 transition-all disabled:opacity-60"
                    >
                      {isLoading ? "Đang xử lý..." : "Xác nhận"}
                    </button>
                  </div>
                </form>
              </div>

              {/* Backup Codes Section */}
              <div className="bg-white rounded-[24px] p-8 shadow-sm border border-slate-200 mt-6 max-w-[640px] w-full">
                <div className="flex justify-between items-center mb-6">
                  <div>
                    <h3 className="text-xl font-extrabold text-teal-900 mb-1">Mã dự phòng</h3>
                    <p className="text-sm text-slate-500">
                      Lưu trữ 16 mã này ở nơi an toàn. Dùng khi bạn mất thiết bị xác thực. Mỗi mã chỉ dùng 1 lần.
                    </p>
                  </div>
                  <button
                    type="button"
                    className="p-2 text-teal-600 hover:bg-teal-50 rounded-lg transition"
                    onClick={async () => {
                      if (totpSetup?.backupCodes) {
                        try {
                          await navigator.clipboard.writeText(totpSetup.backupCodes.join('\n'));
                          notify.success("Đã sao chép mã dự phòng.");
                        } catch {
                          notify.error("Không thể sao chép mã dự phòng. Vui lòng thử lại.");
                        }
                      }
                    }}
                    title="Sao chép tất cả"
                  >
                    <Copy className="h-5 w-5" />
                  </button>
                </div>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  {totpSetup?.backupCodes?.map((code, i) => (
                    <div key={i} className="font-mono text-sm font-bold text-slate-700 bg-slate-50 border border-slate-100 p-2 rounded text-center tracking-widest">
                      {code}
                    </div>
                  ))}
                </div>
              </div>

              {/* Informational Tip */}
              <div className="mt-8 flex gap-4 p-4 rounded-2xl bg-teal-50/80 border border-teal-100/50 text-teal-800 shadow-sm max-w-[640px]">
                <ShieldCheck className="h-6 w-6 text-teal-600 shrink-0" />
                <p className="text-sm leading-relaxed">
                  <strong>Mẹo bảo mật:</strong> Đảm bảo rằng bạn không chia sẻ mã MFA này với bất kỳ ai. HealthLens sẽ không bao giờ yêu cầu bạn cung cấp mã này qua email hoặc điện thoại.
                </p>
              </div>
            </>
          )}

          {step === "totp-success" && (
            <div className="w-full animate-in fade-in zoom-in duration-500">
              <div className="flex flex-col items-center mb-10 text-center">
                <h1 className="text-3xl font-extrabold text-teal-900 tracking-tight mb-2">
                  Xác Thực Hai Yếu Tố (MFA)
                </h1>
                <p className="text-slate-600 max-w-md">
                  Thiết lập thành công! Tài khoản của bạn đã được bảo vệ.
                </p>
              </div>

              {/* Stepper */}
              <div className="flex items-center justify-between mb-12 relative px-4 z-0">
                <div className="absolute top-1/2 left-0 w-full h-[2px] bg-teal-600 -translate-y-1/2 z-0"></div>
                {/* Step 1: Completed */}
                <div className="flex flex-col items-center gap-2 relative z-10">
                  <div className="w-10 h-10 rounded-full bg-teal-600 text-white flex items-center justify-center shadow-md">
                    <Check className="h-5 w-5" />
                  </div>
                  <span className="text-xs font-semibold text-teal-600">Chọn phương thức</span>
                </div>
                {/* Step 2: Completed */}
                <div className="flex flex-col items-center gap-2 relative z-10">
                  <div className="w-10 h-10 rounded-full bg-teal-600 text-white flex items-center justify-center shadow-md">
                    <Check className="h-5 w-5" />
                  </div>
                  <span className="text-xs font-semibold text-teal-600">Thiết lập</span>
                </div>
                {/* Step 3: Completed */}
                <div className="flex flex-col items-center gap-2 relative z-10">
                  <div className="w-10 h-10 rounded-full bg-teal-600 text-white flex items-center justify-center shadow-md">
                    <Check className="h-5 w-5" />
                  </div>
                  <span className="text-xs font-bold text-teal-900">Xác nhận</span>
                </div>
              </div>

              <div className="bg-white rounded-[24px] p-10 shadow-lg border border-teal-100 flex flex-col items-center text-center">
                <div className="w-20 h-20 bg-teal-50 rounded-full flex items-center justify-center mb-6">
                  <ShieldCheck className="h-10 w-10 text-teal-600" />
                </div>
                <h2 className="text-2xl font-bold text-teal-900 mb-3">Xác thực thành công</h2>
                <p className="text-slate-600 mb-8 max-w-sm">
                  Từ bây giờ, bạn sẽ cần nhập mã từ ứng dụng Authenticator mỗi khi đăng nhập vào hệ thống quản trị.
                </p>
                <button
                  onClick={() => handleLoginSuccess(adminState?.accessToken || "")}
                  className="w-full h-14 bg-gradient-to-br from-teal-500 to-teal-600 text-white font-bold rounded-xl shadow-md hover:shadow-lg hover:to-teal-700 active:scale-95 transition-all"
                >
                  Vào trang quản trị
                </button>
              </div>
            </div>
          )}

          {/* Error message */}
          {error && step !== "credentials" && step !== "totp" && !(step === "totp-setup" && totpSetup) && (
            <p className="mt-4 text-center text-sm text-red-600">
              {error}
            </p>
          )}
        </section>
      </main>

      {/* Decorative */}
      <div className="pointer-events-none fixed bottom-0 right-0 hidden p-8 opacity-5 lg:block">
        <ShieldCheck className="h-56 w-56 text-teal-600" />
      </div>
    </div>
  );
}
