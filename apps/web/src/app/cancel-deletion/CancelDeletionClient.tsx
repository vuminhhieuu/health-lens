'use client';

import React, { useEffect, useRef, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { Callout } from "@radix-ui/themes";
import { AlertTriangle, CheckCircle2, Loader } from "lucide-react";
import { useAccountDeletion } from '../../hooks/useAccountDeletion';

/**
 * Story 1.6 - AC #5: Cancel deletion page
 * Called from email link with cancellation token
 * User can cancel their account deletion request within 72h grace period
 */
export default function CancelDeletionClient() {
  const calledRef = useRef(false);
  const searchParams = useSearchParams();
  const router = useRouter();
  const token = searchParams.get("token");
  console.log("Cancel token:", token);

  const [status, setStatus] = useState<"loading" | "success" | "error">("loading");
  const [message, setMessage] = useState("");
  const { cancelDeletion } = useAccountDeletion();

  useEffect(() => {
    if (!token || calledRef.current) return;

    calledRef.current = true;

    const run = async () => {
      try {
        const result = await cancelDeletion(token);

        setStatus("success");
        setMessage(result.message);

      } catch (err: any) {
        if (err?.response?.status === 409) {
          setStatus("success");
          setMessage("Yêu cầu xóa đã được xử lý trước đó");
          return;
        }
        setStatus("error");

        const message =
          err?.response?.data?.detail ||
          err?.response?.data?.error ||
          err?.message ||
          "Không thể hủy yêu cầu xóa tài khoản";

        setMessage(message);
      }
    };

    run();
  }, [token]);

  return (
    <div className="flex-grow p-6 md:p-12 lg:p-16 max-w-7xl mx-auto bg-[#effcf9] min-h-screen text-[#121e1c]">
      {/* Breadcrumbs & Header */}
      <div className="mb-10">
        <nav className="flex text-sm text-[#6d7a77] mb-2">
          <span className="text-[#121e1c] font-medium">Hủy yêu cầu xóa</span>
        </nav>
        <h1 className="text-4xl font-extrabold tracking-tight text-[#121e1c]">
          Hủy yêu cầu xóa tài khoản
        </h1>
        <p className="text-[#6d7a77] mt-3 text-lg">
          Đang xử lý yêu cầu hủy...
        </p>
      </div>

      <div className="max-w-2xl">
        {status === "loading" && (
          <div className="bg-white rounded-3xl p-8 shadow-[0_8px_32px_rgba(18,30,28,0.04)] border border-[#bcc9c6]/20">
            <div className="flex flex-col items-center justify-center">
              <Loader className="animate-spin w-12 h-12 text-[#00685f] mb-4" />
              <p className="text-lg text-[#6d7a77]">Đang xử lý...</p>
            </div>
          </div>
        )}

        {status === "success" && (
          <div className="bg-white rounded-3xl p-8 shadow-[0_8px_32px_rgba(18,30,28,0.04)] border border-[#bcc9c6]/20 text-center">
            <div className="w-20 h-20 bg-[#e4f1ee] rounded-full flex items-center justify-center mx-auto mb-6">
              <CheckCircle2 size={40} className="text-[#00685f]" />
            </div>
            <h2 className="text-3xl font-bold text-[#121e1c] mb-4">
              Hủy yêu cầu thành công
            </h2>
            <Callout.Root
              color="green"
              mb="6"
              className="bg-[#e8f5e9] border border-[#2e7d32]/20 shadow-sm rounded-xl py-4 px-5"
            >
              <Callout.Icon>
                <CheckCircle2 size={20} className="text-[#2e7d32]" />
              </Callout.Icon>
              <Callout.Text className="text-[#2e7d32]">
                {message}
              </Callout.Text>
            </Callout.Root>
            <p className="text-[#6d7a77] mb-6">
              Tài khoản của bạn đã được khôi phục. Bạn có thể đăng nhập bình thường.
            </p>
            <a
              href="/login"
              className="inline-block bg-[#00685f] hover:bg-[#004d47] text-white font-bold py-3 px-6 rounded-xl transition-colors shadow-md hover:shadow-lg"
            >
              Quay lại trang đăng nhập
            </a>
          </div>
        )}

        {status === "error" && (
          <div className="bg-white rounded-3xl p-8 shadow-[0_8px_32px_rgba(18,30,28,0.04)] border border-[#bcc9c6]/20">
            <Callout.Root
              color="red"
              mb="6"
              className="bg-[#ffebee] border border-[#ba1a1a]/20 shadow-sm rounded-xl py-4 px-5"
            >
              <Callout.Icon>
                <AlertTriangle size={20} className="text-[#ba1a1a]" />
              </Callout.Icon>
              <Callout.Text className="text-[#ba1a1a] font-semibold">
                Không thể hủy yêu cầu
              </Callout.Text>
            </Callout.Root>
            <p className="text-[#6d7a77] mb-6 text-lg">{message}</p>
            <div className="flex gap-3">
              <a
                href="/login"
                className="flex-1 text-center bg-[#00685f] hover:bg-[#004d47] text-white font-bold py-3 px-6 rounded-xl transition-colors shadow-md hover:shadow-lg"
              >
                Quay lại trang đăng nhập
              </a>
              <a
                href="/"
                className="flex-1 text-center px-6 py-3 border-2 border-[#bcc9c6]/20 text-[#121e1c] font-bold rounded-xl hover:bg-[#e9f6f3] transition-colors"
              >
                Trang chủ
              </a>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}