"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { Callout } from "@radix-ui/themes";
import { AlertTriangle, CheckCircle2, Lock } from "lucide-react";

import { useAccountDeletion } from "@/hooks/useAccountDeletion";

export default function DeleteAccountPage() {
    const router = useRouter();
    const {
        requestDeletion,
        isRequesting,
        isCancelling,
        error: hookError,
        requestSuccess
    } = useAccountDeletion();
    const [password, setPassword] = useState("");
    const [passwordError, setPasswordError] = useState<string | null>(null);
    const [showPasswordField, setShowPasswordField] = useState(false);
    const [deletionLink, setDeletionLink] = useState<string | null>(null);

    const handleConfirmWarning = () => {
        setShowPasswordField(true);
        setPassword("");
        setPasswordError(null);
    };

    const handleCancel = () => {
        setShowPasswordField(false);
        setPassword("");
        setPasswordError(null);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        // Validate password
        if (!password.trim()) {
            setPasswordError("Vui lòng nhập mật khẩu");
            return;
        }

        setPasswordError(null);

        try {
            const result = await requestDeletion(password);
            setDeletionLink(result.cancellationLink);
            setPassword("");
            // Optionally redirect after a delay
            setTimeout(() => {
                // Logout the user after successful deletion request
                router.push("/login?deleted=true");
            }, 3000);
        } catch (err) {
            console.error("Deletion request failed", err);
        }
    };

    if (requestSuccess && deletionLink) {
        return (
            <div className="flex-grow p-6 md:p-12 lg:p-16 max-w-7xl mx-auto bg-[#effcf9] min-h-screen text-[#121e1c]">
                <div className="max-w-2xl mx-auto">
                    <div className="bg-white rounded-3xl p-8 shadow-[0_8px_32px_rgba(18,30,28,0.04)] border border-[#bcc9c6]/20 text-center">
                        <div className="w-20 h-20 bg-[#e4f1ee] rounded-full flex items-center justify-center mx-auto mb-6">
                            <CheckCircle2 size={40} className="text-[#00685f]" />
                        </div>
                        <h2 className="text-3xl font-bold text-[#121e1c] mb-4">
                            Yêu cầu xóa đã được gửi
                        </h2>
                        <p className="text-[#6d7a77] mb-4 text-lg">
                            Dữ liệu của bạn sẽ bị xóa sau 72 giờ.
                        </p>
                        <p className="text-[#6d7a77] mb-6">
                            Kiểm tra email của bạn để xem danh sách dữ liệu sẽ bị xóa và link
                            hủy yêu cầu nếu cần thiết.
                        </p>
                        <p className="text-sm text-[#6d7a77]">
                            Bạn sẽ được đăng xuất trong giây lát...
                        </p>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="flex-grow p-6 md:p-12 lg:p-16 max-w-7xl mx-auto bg-[#effcf9] min-h-screen text-[#121e1c]">
            {/* Breadcrumbs & Header */}
            <div className="mb-10">
                <nav className="flex text-sm text-[#6d7a77] mb-2">
                    <span className="hover:text-[#00685f] cursor-pointer transition-colors">
                        Dashboard
                    </span>
                    <span className="mx-2">/</span>
                    <span className="hover:text-[#00685f] cursor-pointer transition-colors">
                        Cài đặt
                    </span>
                    <span className="mx-2">/</span>
                    <span className="text-[#121e1c] font-medium">Xóa tài khoản</span>
                </nav>
                <h1 className="text-4xl font-extrabold tracking-tight text-[#121e1c]">
                    Xóa tài khoản
                </h1>
                <p className="text-[#6d7a77] mt-3 text-lg">
                    Quản lý việc xóa tài khoản và dữ liệu cá nhân của bạn theo NĐ 13/2023
                </p>
            </div>

            <div className="max-w-2xl">
                {!showPasswordField ? (
                    <>
                        {/* Warning Section */}
                        <Callout.Root
                            color="red"
                            mb="6"
                            className="bg-[#ffebee] border border-[#ba1a1a]/20 shadow-sm rounded-xl py-4 px-5"
                        >
                            <Callout.Icon>
                                <AlertTriangle size={20} className="text-[#ba1a1a]" />
                            </Callout.Icon>
                            <Callout.Text className="text-[#ba1a1a] font-semibold">
                                Cảnh báo: Hành động này không thể hoàn tác
                            </Callout.Text>
                        </Callout.Root>

                        {/* Main Content Card */}
                        <div className="bg-white rounded-3xl p-8 shadow-[0_8px_32px_rgba(18,30,28,0.04)] border border-[#bcc9c6]/20">
                            {/* Data to be deleted */}
                            <div className="mb-8">
                                <h3 className="text-xl font-bold text-[#121e1c] mb-4">
                                    Dữ liệu sẽ bị xóa vĩnh viễn:
                                </h3>
                                <ul className="space-y-3 text-[#6d7a77]">
                                    <li className="flex items-start">
                                        <svg
                                            className="w-5 h-5 text-[#ba1a1a] mr-3 mt-0.5 flex-shrink-0"
                                            fill="currentColor"
                                            viewBox="0 0 20 20"
                                        >
                                            <path
                                                fillRule="evenodd"
                                                d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                                                clipRule="evenodd"
                                            />
                                        </svg>
                                        <span>Thông tin cá nhân (họ tên, ngày sinh, giới tính)</span>
                                    </li>
                                    <li className="flex items-start">
                                        <svg
                                            className="w-5 h-5 text-[#ba1a1a] mr-3 mt-0.5 flex-shrink-0"
                                            fill="currentColor"
                                            viewBox="0 0 20 20"
                                        >
                                            <path
                                                fillRule="evenodd"
                                                d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                                                clipRule="evenodd"
                                            />
                                        </svg>
                                        <span>
                                            Tất cả hồ sơ sức khỏe, kết quả xét nghiệm và chẩn đoán
                                        </span>
                                    </li>
                                    <li className="flex items-start">
                                        <svg
                                            className="w-5 h-5 text-[#ba1a1a] mr-3 mt-0.5 flex-shrink-0"
                                            fill="currentColor"
                                            viewBox="0 0 20 20"
                                        >
                                            <path
                                                fillRule="evenodd"
                                                d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                                                clipRule="evenodd"
                                            />
                                        </svg>
                                        <span>Tất cả tệp PDF, ảnh và tài liệu được tải lên</span>
                                    </li>
                                    <li className="flex items-start">
                                        <svg
                                            className="w-5 h-5 text-[#ba1a1a] mr-3 mt-0.5 flex-shrink-0"
                                            fill="currentColor"
                                            viewBox="0 0 20 20"
                                        >
                                            <path
                                                fillRule="evenodd"
                                                d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                                                clipRule="evenodd"
                                            />
                                        </svg>
                                        <span>Nhật ký đồng ý, lịch sử kiểm toán và audit logs</span>
                                    </li>
                                </ul>
                            </div>

                            {/* Grace period info */}
                            <Callout.Root
                                color="blue"
                                mb="6"
                                className="bg-[#e3f2fd] border border-[#1976d2]/20 shadow-sm rounded-xl py-4 px-5"
                            >
                                <Callout.Text className="text-[#1565c0] font-semibold">
                                    ⏱️ Thời gian chờ 72 giờ: Bạn có 72 giờ để hủy yêu cầu xóa.
                                    Hãy kiểm tra email để tìm link hủy yêu cầu nếu cần thay đổi ý
                                    kiến.
                                </Callout.Text>
                            </Callout.Root>

                            {/* Confirm button */}
                            <button
                                onClick={handleConfirmWarning}
                                className="w-full bg-[#ba1a1a] hover:bg-[#a01818] text-white font-bold py-3 rounded-xl transition-colors shadow-md hover:shadow-lg"
                            >
                                Tôi hiểu, tiếp tục xóa tài khoản
                            </button>
                        </div>
                    </>
                ) : (
                    <>
                        {/* Password verification Card */}
                        <div className="bg-white rounded-3xl p-8 shadow-[0_8px_32px_rgba(18,30,28,0.04)] border border-[#bcc9c6]/20">
                            <div className="mb-8">
                                <h3 className="text-2xl font-bold text-[#121e1c] mb-3">
                                    Xác nhận mật khẩu
                                </h3>
                                <p className="text-[#6d7a77]">
                                    Nhập mật khẩu của bạn để xác nhận yêu cầu xóa tài khoản. Bước
                                    này là bắt buộc để bảo vệ tài khoản của bạn.
                                </p>
                            </div>

                            {(hookError || passwordError) && (
                                <Callout.Root
                                    color="red"
                                    mb="6"
                                    className="bg-[#ffebee] border border-[#ba1a1a]/20 shadow-sm rounded-xl py-4 px-5"
                                >
                                    <Callout.Icon>
                                        <AlertTriangle size={20} className="text-[#ba1a1a]" />
                                    </Callout.Icon>
                                    <Callout.Text className="text-[#ba1a1a]">
                                        {hookError || passwordError}
                                    </Callout.Text>
                                </Callout.Root>
                            )}

                            <form onSubmit={handleSubmit}>
                                <div className="mb-8">
                                    <label
                                        htmlFor="password"
                                        className="block text-sm font-bold text-[#6d7a77] mb-3"
                                    >
                                        Mật khẩu *
                                    </label>
                                    <div className="relative">
                                        <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-[#6d7a77] pointer-events-none" />
                                        <input
                                            id="password"
                                            type="password"
                                            value={password}
                                            onChange={(e) => {
                                                setPassword(e.target.value);
                                                if (passwordError) setPasswordError(null);
                                            }}
                                            placeholder="Nhập mật khẩu của bạn"
                                            className={`w-full pl-12 pr-4 py-3 rounded-xl border-2 focus:outline-none transition-colors font-medium text-[#121e1c] ${passwordError
                                                ? "border-[#ba1a1a] bg-[#ffebee] focus:border-[#ba1a1a]"
                                                : "border-[#bcc9c6]/20 bg-[#e9f6f3] focus:border-[#00685f]"
                                                }`}
                                            required
                                            disabled={isRequesting}
                                        />
                                    </div>
                                </div>

                                <div className="flex gap-3">
                                    <button
                                        type="button"
                                        onClick={handleCancel}
                                        disabled={isCancelling}
                                        className="flex-1 px-6 py-3 border-2 border-[#bcc9c6]/20 text-[#121e1c] font-bold rounded-xl hover:bg-[#e9f6f3] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        Hủy
                                    </button>
                                    <button
                                        type="submit"
                                        disabled={isRequesting || !password.trim()}
                                        className="flex-1 px-6 py-3 bg-[#ba1a1a] hover:bg-[#a01818] text-white font-bold rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-md hover:shadow-lg"
                                    >
                                        {isRequesting ? "Đang xử lý..." : "Xóa tài khoản"}
                                    </button>
                                </div>

                                <p className="text-xs text-[#6d7a77] text-center mt-6">
                                    Dữ liệu của bạn sẽ bị xóa sau 72 giờ. Kiểm tra email để hủy
                                    yêu cầu.
                                </p>
                            </form>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}
