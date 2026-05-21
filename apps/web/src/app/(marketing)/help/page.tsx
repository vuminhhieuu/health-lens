import Link from "next/link";
import { ArrowRight, BookOpen, Shield } from "lucide-react";

import { PublicContentLayout, PublicContentSection } from "@/components/marketing/PublicContentLayout";

export default function PublicHelpPage() {
  return (
    <PublicContentLayout
      title="Trợ giúp"
      description="Các hướng dẫn nhanh giúp bạn bắt đầu với HealthLens, xử lý sự cố thường gặp và tìm đúng kênh hỗ trợ khi cần."
      lastUpdated="20 tháng 5, 2026"
      toc={[
        { id: "bat-dau", label: "Bắt đầu nhanh" },
        { id: "tai-nguyen", label: "Tài nguyên liên quan" },
        { id: "luu-y", label: "Lưu ý y tế" },
      ]}
    >
      <PublicContentSection id="bat-dau" title="1. Bắt đầu nhanh">
        <ol className="mt-4 list-decimal space-y-3 pl-5">
          <li>Tạo tài khoản bằng email và hoàn tất xác minh nếu được yêu cầu.</li>
          <li>Đọc và chấp nhận các điều khoản và quy định về dữ liệu sức khỏe trong ứng dụng.</li>
          <li>Tải lên kết quả khám dưới dạng PDF hoặc ảnh rõ nét, đối chiếu với phiếu gốc khi hệ thống nhắc.</li>
        </ol>
        <Link
          href="/register"
          className="mt-6 inline-flex items-center gap-2 rounded-lg bg-[#00685f] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#005049] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
        >
          Tạo tài khoản miễn phí
          <ArrowRight className="h-4 w-4" aria-hidden />
        </Link>
      </PublicContentSection>

      <PublicContentSection id="tai-nguyen" title="2. Tài nguyên liên quan">
        <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-xl border border-[#bcc9c6]/30 bg-white p-5">
          <Shield className="h-6 w-6 text-[#00685f]" aria-hidden />
          <h3 className="mt-3 font-bold text-[#121e1c]">Quyền riêng tư</h3>
          <p className="mt-2 text-sm leading-6 text-[#4e6360]">
            Xem cách chúng tôi mô tả dữ liệu được xử lý trong quy định bảo mật.
          </p>
          <Link
            href="/privacy"
            className="mt-3 inline-block text-sm font-semibold text-[#00685f] underline-offset-2 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
          >
            Đọc quy định bảo mật
          </Link>
        </div>
        <div className="rounded-xl border border-[#bcc9c6]/30 bg-white p-5">
          <BookOpen className="h-6 w-6 text-[#00685f]" aria-hidden />
          <h3 className="mt-3 font-bold text-[#121e1c]">Câu hỏi thường gặp</h3>
          <p className="mt-2 text-sm leading-6 text-[#4e6360]">
            Các câu hỏi chung về tài khoản và dịch vụ được tóm tắt tại trang thắc mắc.
          </p>
          <Link
            href="/questions"
            className="mt-3 inline-block text-sm font-semibold text-[#00685f] underline-offset-2 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
          >
            Xem thắc mắc
          </Link>
        </div>
        </div>
      </PublicContentSection>

      <PublicContentSection id="luu-y" title="3. Lưu ý y tế">
        <p>
          HealthLens giúp bạn theo dõi và đọc lại kết quả dễ hơn, nhưng không thay thế tư vấn y tế
          chuyên môn. Khi có triệu chứng bất thường hoặc cần quyết định điều trị, hãy liên hệ bác sĩ
          hoặc cơ sở y tế.
        </p>
      </PublicContentSection>
    </PublicContentLayout>
  );
}
