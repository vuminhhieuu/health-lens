import { PublicContentLayout, PublicContentSection } from "@/components/marketing/PublicContentLayout";

export default function TermsPage() {
  return (
    <PublicContentLayout
      title="Điều khoản sử dụng"
      description="Điều khoản này quy định phạm vi sử dụng dịch vụ HealthLens, trách nhiệm của người dùng và giới hạn trách nhiệm của nền tảng."
      lastUpdated="20 tháng 5, 2026"
      toc={[
        { id: "mo-ta", label: "Mô tả dịch vụ" },
        { id: "tai-khoan", label: "Tài khoản và bảo mật" },
        { id: "noi-dung", label: "Nội dung người dùng cung cấp" },
        { id: "gioi-han", label: "Giới hạn trách nhiệm" },
        { id: "cap-nhat", label: "Cập nhật điều khoản" },
      ]}
    >
      <PublicContentSection id="mo-ta" title="1. Mô tả dịch vụ">
        <p>
          HealthLens hỗ trợ lưu trữ, xem lại và diễn giải thông tin từ kết quả khám theo định hướng
          dễ hiểu. Dịch vụ không thay thế chẩn đoán hay điều trị chuyên môn.
        </p>
      </PublicContentSection>

      <PublicContentSection id="tai-khoan" title="2. Tài khoản và bảo mật">
        <p>
          Bạn có trách nhiệm bảo mật thông tin đăng nhập và quản lý hoạt động dưới tài khoản của mình.
          Khi phát hiện truy cập trái phép, vui lòng liên hệ hỗ trợ càng sớm càng tốt.
        </p>
      </PublicContentSection>

      <PublicContentSection id="noi-dung" title="3. Nội dung người dùng cung cấp">
        <p>
          Bạn cam kết có quyền hợp pháp với dữ liệu tải lên, và không sử dụng nền tảng để phát tán nội
          dung vi phạm pháp luật hoặc xâm phạm quyền, lợi ích hợp pháp của bên thứ ba.
        </p>
      </PublicContentSection>

      <PublicContentSection id="gioi-han" title="4. Giới hạn trách nhiệm">
        <p>
          HealthLens cung cấp thông tin theo phạm vi tính năng hiện có. Trong giới hạn pháp luật cho
          phép, chúng tôi không chịu trách nhiệm cho các quyết định y khoa được đưa ra chỉ dựa trên dữ
          liệu hiển thị trong ứng dụng mà không có tư vấn chuyên môn.
        </p>
      </PublicContentSection>

      <PublicContentSection id="cap-nhat" title="5. Cập nhật điều khoản">
        <p>
          Chúng tôi có thể cập nhật điều khoản để phản ánh thay đổi sản phẩm hoặc yêu cầu pháp lý.
          Việc tiếp tục sử dụng sau thời điểm công bố phiên bản mới được hiểu là bạn chấp nhận nội dung cập nhật.
        </p>
      </PublicContentSection>
    </PublicContentLayout>
  );
}
