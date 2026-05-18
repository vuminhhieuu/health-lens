"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { Camera, CheckCircle2, PencilLine, ShieldCheck } from "lucide-react";

type OcrFailureScreenProps = {
  hasPartialMetrics: boolean;
  ocrFailureReason?: string | null;
  onRetry: () => void;
  isRetryUploading?: boolean;
  retryUploadError?: string | null;
  onManualInput: () => void;
  onKeepPartial: () => void;
  isKeepingPartial?: boolean;
  originalDocumentPreview?: ReactNode;
};

const CAMERA_TIPS = [
  "Đặt giấy phẳng",
  "Ánh sáng đều",
  "Tránh bóng đổ",
  "Xoay ngang nếu cần",
];

export function OcrFailureScreen({
  hasPartialMetrics,
  ocrFailureReason,
  onRetry,
  isRetryUploading = false,
  retryUploadError = null,
  onManualInput,
  onKeepPartial,
  isKeepingPartial = false,
  originalDocumentPreview,
}: OcrFailureScreenProps) {
  const failureReason = ocrFailureReason?.trim() || "Không thể nhận diện dữ liệu từ tệp đã tải lên.";

  return (
    <div className="w-full">
      <section className="grid gap-6 lg:grid-cols-12 lg:items-start">
        <div className="order-2 space-y-6 lg:order-1 lg:sticky lg:top-28 lg:col-span-5 xl:col-span-4">
          {originalDocumentPreview ?? (
            <div className="flex aspect-square items-center justify-center rounded-3xl border border-[#c8ddd8] bg-gradient-to-b from-[#e4f1ee] to-[#dbe8e5] p-6">
              <div className="rounded-full border border-[#bfd4cf] bg-white/70 px-3 py-1 text-xs font-medium text-[#45615c]">
                Ảnh tải lên gốc
              </div>
            </div>
          )}

          <div className="overflow-hidden rounded-3xl border border-[#f2d4d2] bg-white shadow-sm">
            <div className="border-b border-[#f0e3e2] px-5 py-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-[#8c4b45]">Cảnh báo OCR</p>
              <p className="mt-1 text-sm font-semibold text-[#2d3f3c]">{failureReason}</p>
            </div>
            <div className="bg-[#f6f9f8] px-5 py-3 text-xs italic text-[#5d7470]">
              Vui lòng thử tải tệp rõ nét hơn hoặc chuyển sang nhập thủ công.
            </div>
          </div>
        </div>

        <div className="order-1 space-y-6 lg:order-2 lg:col-span-7 xl:col-span-8">
          <header className="space-y-3">
            <h1 className="text-3xl font-extrabold tracking-tight text-[#11322e] sm:text-4xl">
              Không thể nhận diện dữ liệu
            </h1>
            <p className="max-w-3xl text-sm text-[#48635f] sm:text-base">
              Hệ thống gặp khó khăn khi đọc tệp của bạn. Hãy chọn cách xử lý phù hợp để tiếp tục xác nhận kết quả xét
              nghiệm.
            </p>
          </header>

          <div className="grid gap-4 md:grid-cols-2">
            <button
              type="button"
              onClick={onRetry}
              disabled={isRetryUploading}
              className="group flex h-full flex-col rounded-3xl border border-[#c2dbd6] bg-white p-6 text-left shadow-sm transition hover:-translate-y-0.5 hover:border-[#8ec3ba]"
            >
              <div className="mb-5 inline-flex h-12 w-12 items-center justify-center rounded-full bg-[#e6f7f4]">
                <Camera className="h-6 w-6 text-[#007267]" />
              </div>
              <p className="text-lg font-bold text-[#163a35]">Chụp lại ảnh hoặc chọn tệp khác</p>
              <p className="mt-2 flex-1 text-sm text-[#4f6965]">
                Đảm bảo ảnh rõ nét, đủ ánh sáng và không bị lóa để hệ thống tự động xử lý.
              </p>
              <span className="mt-6 inline-flex items-center justify-center rounded-xl bg-[#007267] px-4 py-2 text-sm font-semibold text-white">
                {isRetryUploading ? "Đang tải lên..." : "Chọn tệp mới"}
              </span>
            </button>

            <button
              type="button"
              onClick={onManualInput}
              className="group flex h-full flex-col rounded-3xl border border-[#c2dbd6] bg-white p-6 text-left shadow-sm transition hover:-translate-y-0.5 hover:border-[#8ec3ba]"
            >
              <div className="mb-5 inline-flex h-12 w-12 items-center justify-center rounded-full bg-[#e8f1ff]">
                <PencilLine className="h-6 w-6 text-[#2764a8]" />
              </div>
              <p className="text-lg font-bold text-[#163a35]">Tự nhập kết quả xét nghiệm</p>
              <p className="mt-2 flex-1 text-sm text-[#4f6965]">
                Tự điền chỉ số từ bản cứng. Trang review sẽ mở chế độ chỉnh sửa để bạn cập nhật ngay.
              </p>
              <span className="mt-6 inline-flex items-center justify-center rounded-xl border border-[#7ea9dc] px-4 py-2 text-sm font-semibold text-[#2764a8]">
                Nhập thủ công
              </span>
            </button>
          </div>

          {retryUploadError ? (
            <div className="rounded-2xl border border-[#f2b8b5] bg-[#ffefee] px-4 py-3 text-sm font-medium text-[#ba1a1a]">
              {retryUploadError}
            </div>
          ) : null}

          {hasPartialMetrics ? (
            <button
              type="button"
              onClick={onKeepPartial}
              disabled={isKeepingPartial}
              className="flex w-full items-start gap-3 rounded-3xl border border-[#c2dbd6] bg-white p-5 text-left shadow-sm transition hover:border-[#8ec3ba] disabled:cursor-not-allowed disabled:opacity-70"
            >
              <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-[#007267]" />
              <div>
                <p className="font-semibold text-[#163a35]">Giữ lại phần dữ liệu đã trích xuất</p>
                <p className="mt-1 text-sm text-[#4f6965]">
                  Lưu các chỉ số OCR đã đọc được và tiếp tục bổ sung sau. Lựa chọn này không làm mất dữ liệu hiện có.
                </p>
              </div>
            </button>
          ) : null}

          <div className="rounded-3xl border border-[#c8ddd8] bg-white p-5">
            <p className="mb-4 text-sm font-semibold text-[#0e4f48]">Mẹo để OCR hoạt động tốt hơn</p>
            <ul className="grid gap-2 sm:grid-cols-2">
              {CAMERA_TIPS.map((tip) => (
                <li key={tip} className="flex items-start gap-2 text-sm text-[#48635f]">
                  <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-[#0d8a7a]" />
                  <span>{tip}</span>
                </li>
              ))}
            </ul>
          </div>

          <div className="rounded-3xl bg-[#007267] px-6 py-7 text-white">
            <p className="text-xl font-bold">Cần hỗ trợ trực tiếp?</p>
            <p className="mt-2 text-sm text-white/90">
              Nếu bạn vẫn gặp sự cố khi tải lên, đội ngũ HealthLens sẵn sàng hỗ trợ để bạn tiếp tục nhanh hơn.
            </p>
            <div className="mt-5 flex flex-wrap gap-3">
              <a
                href="mailto:support@healthlens.vn?subject=HealthLens%20-%20Can%20ho%20tro%20tai%20len%20ket%20qua%20kham"
                aria-label="Chat với hỗ trợ"
                className="inline-flex rounded-full bg-white px-4 py-2 text-sm font-semibold text-[#007267] transition hover:bg-[#f6fbfa]"
              >
                Chat với hỗ trợ
              </a>
              <Link
                href="/help"
                aria-label="Xem hướng dẫn"
                className="inline-flex rounded-full border border-white/60 px-4 py-2 text-sm font-medium text-white transition hover:bg-white/10"
              >
                Xem hướng dẫn
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
