import { CircleHelp, ShieldAlert } from "lucide-react";

import { PublicContentLayout, PublicContentSection } from "@/components/marketing/PublicContentLayout";

const faqs = [
  {
    question: "Nếu hệ thống nhận dạng văn bản không đọc được thì làm gì?",
    answer:
      "Bạn có thể tải lại ảnh rõ hơn, kiểm tra ánh sáng và đảm bảo phiếu nằm trọn trong khung. Nếu vẫn lỗi, hãy nhập hoặc chỉnh sửa chỉ số thủ công theo phiếu gốc.",
  },
  {
    question: "Chỉ số bất thường có nghĩa là tôi đang bệnh nặng không?",
    answer:
      "Không thể kết luận chỉ từ một nhãn trạng thái. Chỉ số bất thường là tín hiệu để bạn chú ý, đối chiếu bối cảnh cá nhân và trao đổi với bác sĩ khi cần.",
  },
  {
    question: "HealthLens có thay thế bác sĩ không?",
    answer:
      "Không. HealthLens hỗ trợ diễn giải kết quả theo cách dễ hiểu hơn, nhưng không chẩn đoán, kê đơn hoặc thay thế tư vấn y tế chuyên môn.",
  },
  {
    question: "Người thân được xem những gì khi tôi chia sẻ?",
    answer:
      "Người được chia sẻ có thể xem những hồ sơ hoặc kết quả mà bạn cấp quyền. Hãy kiểm tra đúng email người nhận và phạm vi chia sẻ trước khi gửi lời mời.",
  },
  {
    question: "Tôi có thể thu hồi quyền chia sẻ không?",
    answer:
      "Có. Bạn có thể vào phần chia sẻ hồ sơ hoặc kết quả để thu hồi quyền truy cập. Sau khi thu hồi, người đó không còn xem được dữ liệu được chia sẻ.",
  },
  {
    question: "Tôi có thể xóa dữ liệu sức khỏe không?",
    answer:
      "Có. HealthLens hỗ trợ luồng yêu cầu xóa dữ liệu/tài khoản theo quy định bảo vệ dữ liệu cá nhân. Hãy đọc kỹ thông báo xác nhận trước khi gửi yêu cầu.",
  },
] as const;

export default function QuestionsPage() {
  return (
    <PublicContentLayout
      title="Thắc mắc"
      description="Câu trả lời nhanh cho các tình huống thường gặp khi sử dụng HealthLens."
      lastUpdated="20 tháng 5, 2026"
      toc={[
        { id: "cau-hoi", label: "Câu hỏi thường gặp" },
        { id: "luu-y", label: "Lưu ý y tế" },
      ]}
    >
      <PublicContentSection id="cau-hoi" title="1. Câu hỏi thường gặp">
        <div className="space-y-3">
          {faqs.map((faq) => (
            <details
              key={faq.question}
              className="group rounded-xl border border-[#bcc9c6]/35 bg-white p-4 shadow-sm"
            >
              <summary className="flex min-h-12 cursor-pointer list-none items-center gap-3 text-base font-bold text-[#121e1c]">
                <CircleHelp className="h-5 w-5 shrink-0 text-[#00685f]" aria-hidden />
                <span className="flex-1">{faq.question}</span>
              </summary>
              <p className="mt-3 border-t border-[#e1ebe8] pt-3 text-sm leading-6 text-[#4e6360]">
                {faq.answer}
              </p>
            </details>
          ))}
        </div>
      </PublicContentSection>

      <PublicContentSection id="luu-y" title="2. Lưu ý y tế">
        <div className="rounded-xl border border-[#f2b8b5] bg-[#fff8f7] p-5">
          <div className="flex items-start gap-4">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-[#ffdad6] text-[#ba1a1a]">
              <ShieldAlert className="h-6 w-6" aria-hidden />
            </div>
            <p className="text-sm leading-6 text-[#4e6360]">
              HealthLens không thay thế tư vấn y tế chuyên môn. Khi có triệu chứng bất thường hoặc kết quả
              đáng lo, hãy trao đổi trực tiếp với bác sĩ và cơ sở khám chữa bệnh.
            </p>
          </div>
        </div>
      </PublicContentSection>
    </PublicContentLayout>
  );
}
