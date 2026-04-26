"use client";

import { AlertTriangle, Camera, PencilLine, ShieldCheck } from "lucide-react";

type OcrFailureScreenProps = {
  hasPartialMetrics: boolean;
  onRetry: () => void;
  onManualInput: () => void;
  onKeepPartial: () => void;
  isKeepingPartial?: boolean;
};

const CAMERA_TIPS = [
  "Đặt giấy phẳng",
  "Ánh sáng đều",
  "Tránh bóng đổ",
  "Xoay ngang nếu cần",
];

export function OcrFailureScreen({
  hasPartialMetrics,
  onRetry,
  onManualInput,
  onKeepPartial,
  isKeepingPartial = false,
}: OcrFailureScreenProps) {
  return (
    <div className="mx-auto flex min-h-screen w-full max-w-3xl flex-col justify-center gap-6 bg-[#effcf9] px-6 py-10">
      <div className="rounded-3xl border border-[#f2b8b5] bg-white p-8 shadow-sm">
        <div className="mb-6 flex items-center gap-3">
          <AlertTriangle className="h-7 w-7 text-[#ba1a1a]" />
          <div>
            <h1 className="text-2xl font-bold text-[#ba1a1a]">không trích xuất được đầy đủ</h1>
            <p className="text-sm text-[#6d4a49]">Bạn có thể chọn một trong các cách xử lý bên dưới.</p>
          </div>
        </div>

        <div className="space-y-3">
          <button
            type="button"
            onClick={onRetry}
            className="flex w-full items-start gap-3 rounded-2xl border border-[#b7d8d1] p-4 text-left transition hover:bg-[#effcf9]"
          >
            <Camera className="mt-0.5 h-5 w-5 text-[#00685f]" />
            <div>
              <p className="font-semibold text-[#005049]">Chụp lại rõ hơn</p>
              <p className="text-sm text-[#4e6360]">Quay lại trang tải tệp với hướng dẫn chụp ảnh rõ nét.</p>
            </div>
          </button>

          <button
            type="button"
            onClick={onManualInput}
            className="flex w-full items-start gap-3 rounded-2xl border border-[#b7d8d1] p-4 text-left transition hover:bg-[#effcf9]"
          >
            <PencilLine className="mt-0.5 h-5 w-5 text-[#00685f]" />
            <div>
              <p className="font-semibold text-[#005049]">Nhập thủ công</p>
              <p className="text-sm text-[#4e6360]">Mở chế độ chỉnh sửa để tự nhập từng chỉ số.</p>
            </div>
          </button>

          {hasPartialMetrics ? (
            <button
              type="button"
              onClick={onKeepPartial}
              disabled={isKeepingPartial}
              className="flex w-full items-start gap-3 rounded-2xl border border-[#b7d8d1] p-4 text-left transition hover:bg-[#effcf9] disabled:cursor-not-allowed disabled:opacity-70"
            >
              <ShieldCheck className="mt-0.5 h-5 w-5 text-[#00685f]" />
              <div>
                <p className="font-semibold text-[#005049]">Giữ những gì có</p>
                <p className="text-sm text-[#4e6360]">Lưu các chỉ số đã trích xuất và tiếp tục xử lý sau.</p>
              </div>
            </button>
          ) : null}
        </div>
      </div>

      <div className="rounded-2xl border border-[#b7d8d1] bg-white p-5">
        <p className="mb-2 text-sm font-semibold text-[#005049]">Tips chụp ảnh để trích xuất ổn định:</p>
        <ul className="space-y-1 text-sm text-[#4e6360]">
          {CAMERA_TIPS.map((tip) => (
            <li key={tip}>- {tip}</li>
          ))}
        </ul>
      </div>
    </div>
  );
}
