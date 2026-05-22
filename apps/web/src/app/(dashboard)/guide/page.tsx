import Link from "next/link";
import {
  FileDown,
  FileText,
  HeartPulse,
  LockKeyhole,
  Share2,
  Upload,
  Users,
} from "lucide-react";

import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { breadcrumbFromHome } from "@/lib/layout/dashboardBreadcrumbTrails";

const helpSections = [
  {
    title: "Tải kết quả khám",
    icon: Upload,
    body:
      "Vào Kết quả khám, chọn hồ sơ cần lưu kết quả, rồi tải lên file PDF hoặc ảnh rõ nét. Nếu hệ thống cần bạn kiểm tra lại chỉ số, hãy đối chiếu với phiếu gốc trước khi lưu.",
    href: "/health-records",
    action: "Tải kết quả mới",
  },
  {
    title: "Xem kết quả và giải thích chỉ số",
    icon: HeartPulse,
    body:
      "Mỗi kết quả có trạng thái dễ đọc như bình thường, cần chú ý hoặc bất thường. Mở chi tiết để xem giá trị, ngưỡng tham chiếu và phần giải thích bằng tiếng Việt đơn giản.",
    href: "/health-records",
    action: "Xem kết quả khám",
  },
  {
    title: "Quản lý hồ sơ",
    icon: Users,
    body:
      "Bạn có thể cập nhật hồ sơ cá nhân và quản lý hồ sơ gia đình. Hãy đặt tên hồ sơ rõ ràng để tránh nhầm lẫn khi tải kết quả cho nhiều người.",
    href: "/settings/profile",
    action: "Cập nhật hồ sơ cá nhân",
  },
  {
    title: "Chia sẻ cho người thân",
    icon: Share2,
    body:
      "Từ trang Home hoặc Hồ sơ gia đình, bạn có thể mời người thân xem hồ sơ sức khỏe. Chỉ chia sẻ với người bạn tin tưởng và thu hồi quyền khi không còn cần nữa.",
    href: "/profiles",
    action: "Xem hồ sơ gia đình",
  },
  {
    title: "Tải PDF kết quả",
    icon: FileDown,
    body:
      "Trong trang chi tiết kết quả, dùng nút Tải PDF để lưu bản tổng hợp HealthLens. File này giúp bạn in, lưu trữ hoặc gửi cho bác sĩ/người thân khi cần.",
    href: "/health-records",
    action: "Mở danh sách kết quả",
  },
  {
    title: "Quyền riêng tư dữ liệu",
    icon: LockKeyhole,
    body:
      "Dữ liệu sức khỏe là thông tin nhạy cảm. HealthLens yêu cầu đồng ý xử lý dữ liệu, cho phép quản lý quyền chia sẻ, và hỗ trợ yêu cầu xóa dữ liệu tài khoản.",
    href: "/settings/delete-account",
    action: "Quản lý xóa dữ liệu",
  },
];

export default function GuidePage() {
  return (
    <DashboardPageShell
      title="Trang hướng dẫn"
      subtitle="Các hướng dẫn ngắn để bạn dùng HealthLens tự tin hơn."
      breadcrumbs={breadcrumbFromHome("Hướng dẫn")}
    >
      <section className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {helpSections.map((section) => {
          const Icon = section.icon;

          return (
            <article
              key={section.title}
              className="rounded-xl border border-[#bcc9c6]/30 bg-white p-5 shadow-sm"
            >
              <div className="flex items-start gap-4">
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-[#e9f6f3] text-[#00685f]">
                  <Icon className="h-6 w-6" aria-hidden="true" />
                </div>
                <div className="min-w-0">
                  <h2 className="text-lg font-bold text-[#121e1c]">{section.title}</h2>
                  <p className="mt-2 text-sm leading-6 text-[#4e6360]">{section.body}</p>
                  <Link
                    href={section.href}
                    className="mt-4 inline-flex min-h-12 items-center rounded-full bg-[#00685f] px-4 text-sm font-bold text-white transition hover:bg-[#008378] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005049]"
                  >
                    {section.action}
                  </Link>
                </div>
              </div>
            </article>
          );
        })}
      </section>

      <section className="mt-6 rounded-xl border border-[#f2b8b5] bg-[#fff8f7] p-5">
        <div className="flex items-start gap-4">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-[#ffdad6] text-[#ba1a1a]">
            <FileText className="h-6 w-6" aria-hidden="true" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-[#121e1c]">Lưu ý y tế quan trọng</h2>
            <p className="mt-2 text-sm leading-6 text-[#4e6360]">
              HealthLens giúp bạn đọc và theo dõi kết quả dễ hơn, nhưng thông tin chỉ mang tính
              tham khảo và không thay thế tư vấn y tế chuyên môn. Nếu chỉ số bất thường hoặc bạn
              thấy không khỏe, hãy liên hệ bác sĩ hoặc cơ sở y tế.
            </p>
          </div>
        </div>
      </section>
    </DashboardPageShell>
  );
}
