'use client';

import React, { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { AlertTriangle, CalendarCheck, CheckCircle2, HelpCircle, Info, Loader, Mail, RefreshCcw, Timer, Undo2 } from "lucide-react";
import { AxiosError } from "axios";
import { useAccountDeletion } from '../../hooks/useAccountDeletion';

interface ApiErrorData {
    error?: string;
    detail?: string;
    title?: string;
    errorCode?: string;
    retryAfterSeconds?: number;
    properties?: {
        errorCode?: string;
        retryAfterSeconds?: number;
    };
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

const consumedTokenStorageKey = "healthlens.cancelDeletion.consumedTokens";

async function tokenFingerprint(token: string): Promise<string | null> {
    if (typeof window === "undefined" || !window.crypto?.subtle) {
        return null;
    }

    try {
        const tokenBytes = new TextEncoder().encode(token);
        const digest = await window.crypto.subtle.digest("SHA-256", tokenBytes);
        return Array.from(new Uint8Array(digest))
            .map((byte) => byte.toString(16).padStart(2, "0"))
            .join("");
    } catch {
        return null;
    }
}

function readConsumedTokenFingerprints(): string[] {
    if (typeof window === "undefined") return [];

    try {
        const raw = window.localStorage.getItem(consumedTokenStorageKey);
        const parsed = raw ? JSON.parse(raw) : [];
        return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === "string") : [];
    } catch {
        return [];
    }
}

async function isTokenConsumedLocally(token: string): Promise<boolean> {
    const fingerprint = await tokenFingerprint(token);
    if (!fingerprint) return false;
    return readConsumedTokenFingerprints().includes(fingerprint);
}

function rememberConsumedToken(token: string): void {
    if (typeof window === "undefined") return;

    void tokenFingerprint(token)
        .then((fingerprint) => {
            if (!fingerprint) return;

            const fingerprints = readConsumedTokenFingerprints();
            if (!fingerprints.includes(fingerprint)) {
                fingerprints.push(fingerprint);
                window.localStorage.setItem(consumedTokenStorageKey, JSON.stringify(fingerprints.slice(-20)));
            }
        })
        .catch(() => undefined);
}

type CancellationStatus = "ready" | "loading" | "success" | "error";
type CancellationErrorState =
    | "missing-token"
    | "invalid-token"
    | "replayed"
    | "unauthorized"
    | "forbidden"
    | "rate-limited"
    | "server-error"
    | "network"
    | "unknown";

function getErrorState(error: AxiosError<ApiErrorData>): CancellationErrorState {
    const status = error?.response?.status;
    if (status === 409) return "replayed";
    if (status === 401) return "unauthorized";
    if (status === 403) return "forbidden";
    if (status === 429) return "rate-limited";
    if (status !== undefined && status >= 500) return "server-error";
    if (!status) return "network";
    return "invalid-token";
}

const errorContent: Record<CancellationErrorState, { title: string; message: string }> = {
    "missing-token": {
        title: "Liên kết thiếu mã bảo mật",
        message: "Liên kết hủy yêu cầu không có mã bảo mật hợp lệ. Vui lòng mở lại email mới nhất từ HealthLens hoặc liên hệ hỗ trợ.",
    },
    "invalid-token": {
        title: "Liên kết không hợp lệ hoặc đã hết hạn",
        message: "Liên kết hủy yêu cầu không hợp lệ hoặc đã hết hiệu lực. Vui lòng kiểm tra email mới nhất từ HealthLens.",
    },
    replayed: {
        title: "Yêu cầu đã được xử lý",
        message: "Yêu cầu xóa này đã được hủy, hoàn tất, hoặc không còn ở trạng thái có thể hủy. Vui lòng đăng nhập để kiểm tra trạng thái tài khoản.",
    },
    unauthorized: {
        title: "Mã hủy đã hết hiệu lực",
        message: "Hệ thống không còn chấp nhận mã hủy này. Nếu bạn vẫn cần hỗ trợ khôi phục tài khoản, vui lòng liên hệ HealthLens ngay.",
    },
    forbidden: {
        title: "Không thể khôi phục bằng liên kết này",
        message: "Tài khoản không còn ở trạng thái chờ xóa nên liên kết email không thể dùng để khôi phục.",
    },
    "rate-limited": {
        title: "Thao tác tạm thời bị giới hạn",
        message: "Bạn đã thử quá nhiều lần. Vui lòng thử lại sau ít phút để bảo vệ tài khoản.",
    },
    "server-error": {
        title: "Chưa thể xử lý yêu cầu",
        message: "Hệ thống đang gặp sự cố khi xử lý liên kết hủy xóa. Vui lòng thử lại sau hoặc liên hệ hỗ trợ.",
    },
    network: {
        title: "Không thể kết nối",
        message: "Không thể kết nối đến máy chủ. Vui lòng kiểm tra mạng và thử lại.",
    },
    unknown: {
        title: "Không thể hoàn tất hủy yêu cầu",
        message: "Không thể hoàn tất hủy yêu cầu. Vui lòng thử lại hoặc liên hệ hỗ trợ.",
    },
};

/**
 * Story 1.6 — UI tham chiếu Stitch (project 2069125245324220624):
 * Xác nhận hủy, thành công, liên kết không hợp lệ (không hiển thị mã lỗi kỹ thuật).
 */
export default function CancelDeletionClient() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const [token] = useState(() => normalizeCancellationToken(searchParams.get("token")));
    const [requestedAt] = useState(() => searchParams.get("requestedAt"));
    const [scheduledDeletionAt] = useState(() => searchParams.get("scheduledDeletionAt"));

    const [status, setStatus] = useState<CancellationStatus>("ready");
    const [errorState, setErrorState] = useState<CancellationErrorState>("unknown");
    const [message, setMessage] = useState("");
    const [confirmedEmail, setConfirmedEmail] = useState<string | null>(null);
    const [cancelledAt, setCancelledAt] = useState<string | null>(null);
    const [redirectSecondsLeft, setRedirectSecondsLeft] = useState(3);
    const [nowMs, setNowMs] = useState(() => Date.now());
    const submitInFlightRef = useRef(false);
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
        const email = confirmedEmail;
        if (!email || !email.includes("@")) return "Email sẽ được xác nhận";
        const [name, domain] = email.split("@");
        if (!name) return `...@${domain}`;
        const first = name[0];
        const last = name[name.length - 1];
        return `${first}...${last}@${domain}`;
    }, [confirmedEmail]);

    useEffect(() => {
        if (typeof window === "undefined") return;
        if (!window.location.search) return;
        window.history.replaceState(window.history.state, "", window.location.pathname);
    }, []);

    useEffect(() => {
        if (token) return;
        setErrorState("missing-token");
        setMessage(errorContent["missing-token"].message);
        setStatus("error");
    }, [token]);

    useEffect(() => {
        if (!token) return;

        let cancelled = false;
        void isTokenConsumedLocally(token).then((consumed) => {
            if (cancelled || !consumed) return;
            setErrorState("replayed");
            setMessage(errorContent.replayed.message);
            setStatus("error");
        });

        return () => {
            cancelled = true;
        };
    }, [token]);

    const handleConfirmCancel = async () => {
        if (submitInFlightRef.current) return;
        if (!token) {
            setErrorState("missing-token");
            setStatus("error");
            setMessage(errorContent["missing-token"].message);
            return;
        }

        submitInFlightRef.current = true;
        setStatus("loading");
        try {
            const result = await cancelDeletion(token);
            rememberConsumedToken(token);
            if (result?.email) setConfirmedEmail(result.email);
            setCancelledAt(result?.cancelledAt ?? new Date().toISOString());
            setRedirectSecondsLeft(3);
            setStatus("success");
        } catch (err) {
            const error = err as AxiosError<ApiErrorData>;
            const nextErrorState = getErrorState(error);
            if (nextErrorState === "replayed") {
                rememberConsumedToken(token);
            }
            setErrorState(nextErrorState);
            setStatus("error");
            setMessage(errorContent[nextErrorState].message);
        } finally {
            submitInFlightRef.current = false;
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
                                    Xác nhận hủy yêu cầu xóa
                                </h1>
                                <p className="text-sm text-[#3d4947]">
                                    Hệ thống sẽ xác minh hiệu lực liên kết trước khi khôi phục tài khoản.
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
                                        Bạn có muốn hủy yêu cầu xóa không?
                                    </p>
                                </div>

                                <div className="space-y-3">
                                    <button
                                        type="button"
                                        onClick={handleConfirmCancel}
                                        className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-[#00685f] to-[#008378] font-semibold text-white shadow-md transition-all hover:shadow-lg disabled:cursor-not-allowed disabled:bg-[#bcc9c6] disabled:from-[#bcc9c6] disabled:to-[#bcc9c6]"
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
                                        {errorContent[errorState].title}
                                    </h2>
                                    <p className="mx-auto max-w-md text-base leading-relaxed text-[#3d4947]">
                                        {message || errorContent[errorState].message}
                                    </p>
                                </div>
                            </div>

                            <div className="space-y-6">
                                <div className="rounded-lg border border-[#bcc9c6]/60 bg-[#f7fbfa] p-4 text-sm text-[#3d4947]">
                                    Liên kết hủy yêu cầu được gửi kèm trong email xác nhận ban đầu. Nếu bạn cần hỗ trợ thêm,
                                    vui lòng liên hệ đội ngũ HealthLens theo thông tin bên dưới.
                                </div>
                                <Link
                                    href="/login"
                                    className="flex h-12 w-full items-center justify-center rounded-xl border-2 border-[#bcc9c6] font-semibold text-[#00685f] transition-all hover:bg-[#e9f6f3]"
                                >
                                    Về trang đăng nhập
                                </Link>
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
