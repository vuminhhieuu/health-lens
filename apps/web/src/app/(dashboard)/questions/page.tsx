import Link from "next/link";
import { CircleHelp, ShieldAlert } from "lucide-react";

import { DashboardPageShell } from "@/components/layout/DashboardPageShell";

const faqs = [
  {
    question: "Nếu OCR không đọc được thì làm gì?",
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
  {
    question: "Tại sao tôi không đăng nhập hoặc tải file được?",
    answer:
      "Hãy kiểm tra kết nối mạng, phiên đăng nhập và loại file. Nếu tài khoản đang trong trạng thái chờ xóa hoặc quyền chia sẻ đã bị thu hồi, một số thao tác sẽ bị chặn.",
  },
];

export default function QuestionsPage() {
  return (
    <DashboardPageShell
      title="Thắc mắc thường gặp"
      subtitle="Câu trả lời nhanh cho những tình huống dễ gặp khi dùng HealthLens."
      breadcrumbs={[
        { label: "Trang chủ", href: "/home" },
        { label: "Thắc mắc" },
      ]}
      actions={
        <Link
          href="/help"
          className="inline-flex min-h-12 items-center rounded-full bg-[#00685f] px-5 text-sm font-bold text-white transition hover:bg-[#008378] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005049]"
        >
          Mở trang trợ giúp
        </Link>
      }
    >
      <section className="space-y-3">
        {faqs.map((faq) => (
          <details
            key={faq.question}
            className="group rounded-xl border border-[#bcc9c6]/30 bg-white p-5 shadow-sm"
          >
            <summary className="flex min-h-12 cursor-pointer list-none items-center gap-3 text-base font-bold text-[#121e1c] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#00685f]">
              <CircleHelp
                className="h-5 w-5 shrink-0 text-[#00685f]"
                aria-hidden="true"
              />
              <span className="flex-1">{faq.question}</span>
              <span
                aria-hidden="true"
                className="text-sm font-semibold text-[#6d7a77] group-open:hidden"
              >
                Xem
              </span>
              <span
                aria-hidden="true"
                className="hidden text-sm font-semibold text-[#6d7a77] group-open:inline"
              >
                Thu gọn
              </span>
            </summary>
            <p className="mt-3 pl-8 text-sm leading-6 text-[#4e6360]">{faq.answer}</p>
          </details>
        ))}
      </section>

      <section className="mt-6 rounded-xl border border-[#f2b8b5] bg-[#fff8f7] p-5">
        <div className="flex items-start gap-4">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-[#ffdad6] text-[#ba1a1a]">
            <ShieldAlert className="h-6 w-6" aria-hidden="true" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-[#121e1c]">
              Thông tin chỉ mang tính tham khảo
            </h2>
            <p className="mt-2 text-sm leading-6 text-[#4e6360]">
              HealthLens không thay thế tư vấn y tế chuyên môn. Khi có triệu chứng bất thường,
              kết quả đáng lo, hoặc cần quyết định điều trị, hãy trao đổi trực tiếp với bác sĩ.
            </p>
          </div>
        </div>
      </section>
    </DashboardPageShell>
  );
}
