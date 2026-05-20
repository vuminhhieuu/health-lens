import { PublicContentLayout, PublicContentSection } from "@/components/marketing/PublicContentLayout";

export default function PrivacyPage() {
  return (
    <PublicContentLayout
      title="Quy định bảo mật"
      description="Chính sách này giải thích cách HealthLens thu thập, sử dụng, lưu trữ và bảo vệ dữ liệu cá nhân cũng như dữ liệu sức khỏe của bạn khi sử dụng dịch vụ."
      lastUpdated="20 tháng 5, 2026"
      toc={[
        { id: "pham-vi", label: "Phạm vi áp dụng" },
        { id: "du-lieu", label: "Dữ liệu được xử lý" },
        { id: "muc-dich", label: "Mục đích xử lý" },
        { id: "quyen", label: "Quyền của bạn" },
        { id: "lien-he", label: "Liên hệ" },
      ]}
    >
      <PublicContentSection id="pham-vi" title="1. Phạm vi áp dụng">
        <p>
          Quy định này áp dụng cho các trang công khai và khu vực ứng dụng có đăng nhập của HealthLens,
          bao gồm hoạt động đăng ký, xác thực, tải lên kết quả khám, xem dữ liệu và quản lý hồ sơ.
        </p>
      </PublicContentSection>

      <PublicContentSection id="du-lieu" title="2. Dữ liệu được xử lý">
        <ul className="list-disc space-y-2 pl-5">
          <li>Thông tin tài khoản như email, họ tên, thông tin hồ sơ người dùng.</li>
          <li>Dữ liệu sức khỏe do bạn cung cấp: tệp kết quả khám, ảnh chụp, chỉ số nhập tay và nội dung liên quan.</li>
          <li>Dữ liệu kỹ thuật cần thiết để bảo mật và vận hành ổn định dịch vụ.</li>
        </ul>
      </PublicContentSection>

      <PublicContentSection id="muc-dich" title="3. Mục đích xử lý">
        <p>
          Dữ liệu được dùng để cung cấp tính năng cốt lõi của sản phẩm, cải thiện trải nghiệm sử dụng,
          hỗ trợ chăm sóc khách hàng và đáp ứng các nghĩa vụ pháp lý áp dụng theo quy định hiện hành.
        </p>
      </PublicContentSection>

      <PublicContentSection id="quyen" title="4. Quyền của bạn">
        <p>
          Bạn có thể yêu cầu truy cập, chỉnh sửa, rút lại đồng thuận hoặc xóa dữ liệu trong phạm vi
          mà nền tảng và quy định pháp luật cho phép. Chúng tôi ưu tiên thiết kế cơ chế minh bạch để
          bạn chủ động kiểm soát thông tin của mình.
        </p>
      </PublicContentSection>

      <PublicContentSection id="lien-he" title="5. Liên hệ">
        <p>
          Với mọi yêu cầu liên quan đến quyền riêng tư, vui lòng liên hệ qua email hỗ trợ chính thức
          của HealthLens để được hướng dẫn theo đúng quy trình.
        </p>
      </PublicContentSection>
    </PublicContentLayout>
  );
}
