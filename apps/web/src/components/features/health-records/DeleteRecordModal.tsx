"use client";

import { useId } from "react";
import { Loader2, Trash2 } from "lucide-react";

type DeleteRecordModalProps = {
  open: boolean;
  title?: string;
  description?: string;
  onCancel: () => void;
  onConfirm: () => void;
  isPending?: boolean;
  confirmLabel?: string;
  pendingLabel?: string;
};

const DEFAULT_TITLE = "Xác nhận xóa kết quả?";
const DEFAULT_DESCRIPTION =
  "Kết quả sẽ bị ẩn khỏi lịch sử ngay bây giờ. File ảnh/PDF gốc sẽ bị xóa vĩnh viễn sau 30 ngày.";

export function DeleteRecordModal({
  open,
  title = DEFAULT_TITLE,
  description = DEFAULT_DESCRIPTION,
  onCancel,
  onConfirm,
  isPending = false,
  confirmLabel = "Xóa kết quả",
  pendingLabel = "Đang xóa...",
}: DeleteRecordModalProps) {
  const titleId = useId();
  const descriptionId = useId();
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={descriptionId}
        className="w-full max-w-md rounded-3xl bg-white p-8 shadow-2xl animate-in fade-in zoom-in duration-200"
      >
        <div className="flex flex-col items-center text-center">
          <div className="mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-[#ffdad6] text-[#ba1a1a]">
            <Trash2 className="h-7 w-7" />
          </div>
          <h3 id={titleId} className="mb-2 text-2xl font-bold text-[#7a1711]">
            {title}
          </h3>
          <p id={descriptionId} className="mb-8 text-[#4e6360]">
            {description}
          </p>
          <div className="flex w-full flex-col gap-3">
            <button
              type="button"
              onClick={onConfirm}
              disabled={isPending}
              className="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-[#ba1a1a] py-4 font-bold text-white transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-70"
            >
              {isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
              {isPending ? pendingLabel : confirmLabel}
            </button>
            <button
              type="button"
              onClick={onCancel}
              disabled={isPending}
              className="w-full rounded-2xl border border-[#c5dfd9] bg-white py-4 font-bold text-[#4e6360] transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-70"
            >
              Hủy
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
