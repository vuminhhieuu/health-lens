import { TriangleAlert } from "lucide-react";

export function LegalDraftBanner() {
  return (
    <div
      role="status"
      className="mb-6 flex items-start gap-3 rounded-xl border border-[#924628]/30 bg-[#ffdbce] p-4 text-[#3d4947]"
    >
      <TriangleAlert className="mt-0.5 h-5 w-5 shrink-0 text-[#924628]" aria-hidden="true" />
      <p className="text-sm font-semibold leading-6">Bản nháp — đang cập nhật</p>
    </div>
  );
}
