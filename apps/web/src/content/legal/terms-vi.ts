import type { LegalPageContent } from "./types";

/** Điều khoản sử dụng — căn chỉnh với ConsentModal (tư vấn y tế tham khảo, OCR). */
export const termsViContent: LegalPageContent = {
  title: "Điều khoản sử dụng",
  description:
    "Điều khoản này quy định phạm vi sử dụng dịch vụ HealthLens, trách nhiệm của người dùng và giới hạn trách nhiệm của nền tảng.",
  lastUpdated: "20 tháng 5, 2026",
  showDraftBanner: true,
  sections: [
    {
      id: "mo-ta",
      title: "1. Mô tả dịch vụ",
      blocks: [
        {
          type: "paragraph",
          text: "HealthLens hỗ trợ lưu trữ, xem lại và diễn giải thông tin từ kết quả khám theo định hướng dễ hiểu. Dịch vụ không thay thế chẩn đoán, điều trị hay tư vấn y khoa chuyên môn.",
        },
      ],
    },
    {
      id: "tai-khoan",
      title: "2. Tài khoản và bảo mật",
      blocks: [
        {
          type: "paragraph",
          text: "Bạn có trách nhiệm bảo mật thông tin đăng nhập và mọi hoạt động dưới tài khoản của mình. Khi phát hiện truy cập trái phép, vui lòng đổi mật khẩu và liên hệ hỗ trợ càng sớm càng tốt.",
        },
      ],
    },
    {
      id: "noi-dung",
      title: "3. Nội dung người dùng cung cấp",
      blocks: [
        {
          type: "paragraph",
          text: "Bạn cam kết có quyền hợp pháp với dữ liệu tải lên, và không sử dụng nền tảng để phát tán nội dung vi phạm pháp luật hoặc xâm phạm quyền, lợi ích hợp pháp của bên thứ ba.",
        },
      ],
    },
    {
      id: "ocr",
      title: "4. Giới hạn OCR và dữ liệu trích xuất",
      blocks: [
        {
          type: "paragraph",
          text: "Kết quả trích xuất tự động từ ảnh/PDF có thể không chính xác 100%. Bạn chịu trách nhiệm rà soát chỉ số trước khi dựa vào chúng cho quyết định sức khỏe. HealthLens không đảm bảo OCR hoàn hảo với mọi định dạng tài liệu.",
        },
      ],
    },
    {
      id: "tu-van",
      title: "5. Tuyên bố miễn trừ y khoa",
      blocks: [
        {
          type: "paragraph",
          text: "Khuyến nghị và giải thích từ HealthLens chỉ mang tính tham khảo, không thay thế tư vấn từ bác sĩ hoặc chuyên gia có chứng chỉ hành nghề. Luôn tham khảo ý kiến chuyên môn trước khi thay đổi điều trị hoặc chế độ sinh hoạt.",
        },
      ],
    },
    {
      id: "gioi-han",
      title: "6. Giới hạn trách nhiệm",
      blocks: [
        {
          type: "paragraph",
          text: "HealthLens cung cấp thông tin theo phạm vi tính năng hiện có. Trong giới hạn pháp luật cho phép, chúng tôi không chịu trách nhiệm cho quyết định y khoa được đưa ra chỉ dựa trên dữ liệu hiển thị trong ứng dụng mà không có tư vấn chuyên môn.",
        },
      ],
    },
    {
      id: "du-lieu",
      title: "7. Dữ liệu cá nhân và đồng thuận",
      blocks: [
        {
          type: "paragraph",
          text: "Việc thu thập và xử lý dữ liệu sức khỏe tuân theo Quy định bảo mật và Nghị định 13/2023/NĐ-CP. Bạn cần đồng thuận rõ ràng trước khi sử dụng các tính năng liên quan đến tải lên và kết quả khám.",
        },
      ],
    },
    {
      id: "cap-nhat",
      title: "8. Cập nhật điều khoản",
      blocks: [
        {
          type: "paragraph",
          text: "Chúng tôi có thể cập nhật điều khoản để phản ánh thay đổi sản phẩm hoặc yêu cầu pháp lý. Việc tiếp tục sử dụng sau thời điểm công bố phiên bản mới được hiểu là bạn chấp nhận nội dung cập nhật, trừ khi pháp luật yêu cầu đồng thuận lại.",
        },
      ],
    },
  ],
};
