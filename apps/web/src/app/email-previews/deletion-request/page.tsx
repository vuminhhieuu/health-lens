"use client";

import Link from "next/link";

/** Preview dev/QA — căn chỉnh copy với Stitch (Email xác nhận yêu cầu xóa, project 2069125245324220624). */
export default function DeletionRequestEmailPreviewPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-[#deebe8] p-4 text-[#121e1c] sm:p-8">
      <header className="fixed top-0 z-50 flex w-full items-center justify-between bg-[#effcf9]/80 px-6 py-5 shadow-[0_2px_10px_rgba(0,80,73,0.05)] backdrop-blur-lg">
        <div className="text-2xl font-bold leading-relaxed tracking-tight text-teal-900">
          HealthLens <span className="ml-2 text-lg font-medium text-teal-700/60">Email Client</span>
        </div>
        <div className="flex gap-4">
          <span className="material-symbols-outlined cursor-pointer rounded-full p-2 text-teal-700 transition-colors hover:bg-teal-50/50">
            help_outline
          </span>
          <span className="material-symbols-outlined cursor-pointer rounded-full p-2 text-teal-700 transition-colors hover:bg-teal-50/50">
            settings
          </span>
        </div>
      </header>

      <main className="mx-auto mb-16 mt-24 flex w-full max-w-3xl flex-col overflow-hidden rounded-xl bg-white shadow-[0_8px_32px_rgba(18,30,28,0.06)]">
        <div className="flex items-center gap-4 bg-[#e9f6f3] px-6 py-4">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#00685f] text-lg font-bold text-white">
            H
          </div>
          <div className="min-w-0 flex-1">
            <h2 className="text-base font-semibold text-[#121e1c]">HealthLens — Đội ngũ quyền riêng tư</h2>
            <p className="truncate text-sm text-[#3d4947]">privacy@healthlens.vn</p>
          </div>
          <div className="whitespace-nowrap text-xs text-[#3d4947]">Hôm nay, 14:30</div>
        </div>

        <div className="border-b border-[#d8e5e2] bg-[#effcf9] px-8 py-6">
          <p className="text-xs font-semibold uppercase tracking-wide text-[#6d7a77]">
            Subject
          </p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-[#121e1c]">
            Xác nhận yêu cầu xóa dữ liệu
          </h1>
        </div>

        <div className="space-y-8 bg-white px-8 py-8 text-base leading-relaxed text-[#121e1c]">
          <p>
            Xin chào <span className="font-semibold text-[#00685f]">Arthur</span>,
          </p>
          <p>
            Chúng tôi đã ghi nhận yêu cầu xóa dữ liệu cá nhân và hồ sơ liên quan theo quy định bảo vệ
            dữ liệu (tham chiếu NĐ 13/2023/NĐ-CP). Yêu cầu đang ở trạng thái chờ xử lý; trong thời
            gian ân hạn bạn có thể hủy qua liên kết dưới đây.
          </p>

          <div className="relative overflow-hidden rounded-xl bg-[#e9f6f3] p-6">
            <div className="absolute left-0 top-0 h-full w-1 bg-[#924628]" />
            <h3 className="mb-3 flex items-center gap-2 font-semibold text-[#121e1c]">
              <span className="material-symbols-outlined text-[#924628]">delete_sweep</span>
              Dữ liệu dự kiến bị xóa
            </h3>
            <ul className="list-disc space-y-2 pl-5 text-sm text-[#3d4947]">
              <li>Thông tin định danh cá nhân (PII)</li>
              <li>Hồ sơ sức khỏe, chẩn đoán và kết quả xét nghiệm</li>
              <li>Tất cả tài liệu PDF và hình ảnh đã tải lên</li>
              <li>Nhật ký chấp thuận và lịch sử kiểm toán</li>
            </ul>
          </div>

          <div className="flex gap-4 rounded-xl bg-[#ffdad6]/30 p-5">
            <span className="material-symbols-outlined mt-1 text-[#ba1a1a]">timer</span>
            <div>
              <h4 className="mb-1 font-semibold text-[#121e1c]">Thời hạn xử lý: 72 giờ</h4>
              <p className="text-sm text-[#3d4947]">
                Bạn có <strong>72 giờ</strong> (ví dụ đến 14:30 ngày 28/10/2023) để hủy yêu cầu.
                Sau thời điểm này dữ liệu được xóa vĩnh viễn và không khôi phục được.
              </p>
            </div>
          </div>

          <p>
            Nếu đây là nhầm lẫn hoặc bạn muốn giữ lại dữ liệu, hãy nhấn nút bên dưới để hủy yêu cầu
            xóa.
          </p>

          <div className="flex justify-center pb-2 pt-4">
            <button
              type="button"
              className="flex h-12 items-center gap-2 rounded-full bg-gradient-to-br from-[#00685f] to-[#008378] px-8 text-base font-semibold text-white shadow-[0_4px_14px_rgba(0,104,95,0.2)] transition-all duration-300 hover:shadow-[0_6px_20px_rgba(0,104,95,0.3)] active:scale-95"
            >
              <span className="material-symbols-outlined">undo</span>
              Hủy yêu cầu xóa
            </button>
          </div>

          <p className="text-sm text-[#3d4947]">
            Hoặc mở liên kết an toàn:{" "}
            <a className="text-[#00685f] hover:underline" href="#">
              https://healthlens.vn/cancel-deletion?token=…
            </a>
          </p>

          <div className="mt-8 border-t border-[#d8e5e2] pt-8">
            <p className="text-xs text-[#3d4947]/80">
              Trân trọng,
              <br />
              <strong>Đội ngũ Bảo mật &amp; Quyền riêng tư HealthLens</strong>
            </p>
          </div>

          <div className="mt-6 rounded-lg bg-[#e4f1ee] px-6 py-4">
            <p className="text-xs leading-tight text-[#6d7a77]">
              <strong>Thông báo pháp lý:</strong> Email tự động. Quy trình tuân thủ bảo vệ dữ liệu
              tại Việt Nam. Thắc mắc:{" "}
              <a className="font-semibold text-[#00685f] hover:underline" href="mailto:privacy@healthlens.vn">
                privacy@healthlens.vn
              </a>
              .
            </p>
          </div>

          <div className="pt-2">
            <Link
              href="/settings/delete-account"
              className="text-sm font-semibold text-[#00685f] hover:underline"
            >
              Quay lại luồng xóa tài khoản
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}
