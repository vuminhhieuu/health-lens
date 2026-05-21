import type { LegalPageContent } from "./types";

/** Nội dung bảo mật — căn chỉnh chủ đề với ConsentModal (NĐ 13/2023, không hứa hẹn mâu thuẫn). */
export const privacyViContent: LegalPageContent = {
  title: "Quy định bảo mật",
  description:
    "Chính sách này giải thích cách HealthLens thu thập, sử dụng, lưu trữ và bảo vệ dữ liệu cá nhân cũng như dữ liệu sức khỏe của bạn khi sử dụng dịch vụ.",
  lastUpdated: "20 tháng 5, 2026",
  showDraftBanner: true,
  sections: [
    {
      id: "pham-vi",
      title: "1. Phạm vi áp dụng",
      blocks: [
        {
          type: "paragraph",
          text: "Quy định này áp dụng cho trang công khai (đăng ký, đăng nhập, trợ giúp) và khu vực ứng dụng có đăng nhập của HealthLens, bao gồm tải lên kết quả khám, xem dữ liệu, quản lý hồ sơ và đồng thuận xử lý dữ liệu.",
        },
      ],
    },
    {
      id: "du-lieu",
      title: "2. Loại dữ liệu được xử lý",
      blocks: [
        {
          type: "paragraph",
          text: "Chúng tôi xử lý các nhóm dữ liệu sau, phù hợp với mô tả trong màn hình đồng thuận khi bạn sử dụng tính năng tải lên/kết quả:",
        },
        {
          type: "list",
          items: [
            "Thông tin tài khoản: email, họ tên, ngày sinh, ảnh đại diện và thiết lập hồ sơ.",
            "Dữ liệu sức khỏe do bạn cung cấp: tệp kết quả khám (PDF/ảnh), chỉ số nhập tay, ghi chú và nội dung liên quan.",
            "Dữ liệu kỹ thuật cần thiết: nhật ký đăng nhập, mã phiên, địa chỉ IP rút gọn và thông tin thiết bị phục vụ bảo mật và vận hành.",
            "Nhật ký đồng thuận: phiên bản chính sách bạn chấp nhận và thời điểm ghi nhận.",
          ],
        },
      ],
    },
    {
      id: "muc-dich",
      title: "3. Mục đích xử lý",
      blocks: [
        {
          type: "paragraph",
          text: "Dữ liệu được dùng để cung cấp tính năng cốt lõi, không cho mục đích quảng cáo hồ sơ sức khỏe của bạn cho bên thứ ba nếu không có sự đồng ý của bạn:",
        },
        {
          type: "list",
          items: [
            "Hiển thị, lưu trữ và giải thích thông tin từ kết quả khám theo cách dễ hiểu.",
            "Theo dõi chỉ số, nhắc tái khám và cá nhân hóa trải nghiệm trong phạm vi bạn chọn.",
            "Bảo mật tài khoản, phòng chống lạm dụng và cải thiện độ ổn định dịch vụ.",
            "Đáp ứng yêu cầu pháp lý và hỗ trợ khi bạn thực hiện quyền theo Nghị định 13/2023/NĐ-CP.",
          ],
        },
      ],
    },
    {
      id: "luu-tru",
      title: "4. Thời hạn lưu trữ",
      blocks: [
        {
          type: "paragraph",
          text: "Chúng tôi lưu dữ liệu trong thời gian cần thiết để cung cấp dịch vụ hoặc theo nghĩa vụ pháp luật:",
        },
        {
          type: "list",
          items: [
            "Dữ liệu tài khoản và hồ sơ sức khỏe: cho đến khi bạn xóa tài khoản hoặc yêu cầu xóa theo quy trình trong ứng dụng (có thời gian chờ xử lý theo quy định).",
            "Nhật ký đồng thuận và hoạt động quan trọng: theo yêu cầu lưu trữ tối thiểu của pháp luật hiện hành.",
            "Token đặt lại mật khẩu và phiên đăng nhập: thời hạn ngắn (ví dụ vài giờ đến vài ngày) rồi thu hồi hoặc xóa.",
          ],
        },
      ],
    },
    {
      id: "bao-mat",
      title: "5. Bảo mật dữ liệu",
      blocks: [
        {
          type: "paragraph",
          text: "Dữ liệu được mã hóa khi lưu trữ (AES-256) và chỉ sử dụng cho mục đích cung cấp dịch vụ. HealthLens cam kết không chia sẻ dữ liệu sức khỏe của bạn với bên thứ ba nếu không có sự đồng ý của bạn, trừ trường hợp pháp luật yêu cầu.",
        },
      ],
    },
    {
      id: "quyen",
      title: "6. Quyền của bạn (Nghị định 13/2023/NĐ-CP)",
      blocks: [
        {
          type: "paragraph",
          text: "Bạn có các quyền sau trong phạm vi nền tảng và quy định pháp luật cho phép:",
        },
        {
          type: "list",
          items: [
            "Quyền được thông báo về hoạt động xử lý dữ liệu cá nhân.",
            "Quyền đồng ý hoặc rút lại đồng thuận đối với dữ liệu sức khỏe.",
            "Quyền truy cập, chỉnh sửa, yêu cầu xóa hoặc hạn chế xử lý dữ liệu.",
            "Quyền khiếu nại, tố cáo hoặc khởi kiện theo quy định pháp luật.",
          ],
        },
        {
          type: "paragraph",
          text: "Bạn có thể thực hiện một số quyền trực tiếp trong ứng dụng (cập nhật hồ sơ, rút đồng thuận, yêu cầu xóa tài khoản). Các yêu cầu khác vui lòng liên hệ kênh hỗ trợ chính thức.",
        },
      ],
    },
    {
      id: "ocr",
      title: "7. Giới hạn độ chính xác OCR",
      blocks: [
        {
          type: "paragraph",
          text: "Khi bạn tải ảnh hoặc PDF kết quả khám, hệ thống có thể dùng nhận dạng ký tự (OCR) để trích xuất chỉ số. OCR có thể sai với ảnh mờ, bảng phức tạp hoặc phông chữ đặc biệt. Bạn nên đối chiếu với bản gốc và chỉnh sửa trước khi lưu.",
        },
      ],
    },
    {
      id: "lien-he",
      title: "8. Liên hệ",
      blocks: [
        {
          type: "paragraph",
          text: "Mọi yêu cầu về quyền riêng tư và bảo vệ dữ liệu, vui lòng liên hệ qua email hỗ trợ chính thức của HealthLens. Chúng tôi phản hồi trong thời hạn hợp lý theo quy định hiện hành.",
        },
      ],
    },
  ],
};
