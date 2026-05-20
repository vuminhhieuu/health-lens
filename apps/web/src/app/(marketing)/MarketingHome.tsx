import type { ReactNode } from "react";
import { Activity, FileText, ShieldCheck, Upload, UserRound, Sparkles } from "lucide-react";
import Link from "next/link";

import { LandingDashboardPreview } from "@/components/marketing/LandingDashboardPreview";
import { LandingGuestCtas } from "@/components/marketing/LandingGuestCtas";
import { marketingBand, marketingShell } from "@/lib/marketing/layout";
import { MEDICAL_RECOMMENDATIONS_DISCLAIMER } from "@/lib/utils/medicalDisclaimer";

const howItWorksSteps = [
  {
    step: "01",
    icon: UserRound,
    title: "Tạo tài khoản và hồ sơ",
    description:
      "Đăng ký bằng email, xác minh tài khoản nếu được yêu cầu, rồi tạo hồ sơ cho bản thân hoặc người thân trong gia đình.",
  },
  {
    step: "02",
    icon: Upload,
    title: "Tải kết quả khám",
    description:
      "Tải phiếu xét nghiệm dạng tệp hoặc ảnh rõ nét. HealthLens trích xuất và chuẩn hóa dữ liệu để bạn đối chiếu với phiếu gốc.",
  },
  {
    step: "03",
    icon: Sparkles,
    title: "Theo dõi và trao đổi",
    description:
      "Xem chỉ số với ngữ cảnh tiếng Việt dễ hiểu, chuẩn bị câu hỏi trước khi gặp bác sĩ — trong không gian bạn kiểm soát quyền riêng tư.",
  },
] as const;

const featureHighlights = [
  {
    icon: FileText,
    title: "Tải và số hóa kết quả",
    description:
      "Lưu trữ kết quả khám tập trung, giảm việc tìm lại từng phiếu giấy hoặc ảnh rời rạc trên điện thoại.",
  },
  {
    icon: Activity,
    title: "Giải thích chỉ số dễ hiểu",
    description:
      "Trình bày chỉ số sức khỏe bằng ngôn ngữ tiếng Việt rõ ràng, giúp bạn nắm bối cảnh trước khi trao đổi với bác sĩ.",
  },
  {
    icon: ShieldCheck,
    title: "Quản lý hồ sơ và quyền riêng tư",
    description:
      "Theo dõi theo từng hồ sơ, kiểm soát đồng thuận và quyền chia sẻ dữ liệu sức khỏe theo thiết kế minh bạch.",
  },
] as const;

const sectionTitle = "text-2xl font-black tracking-tight text-[#121e1c] md:text-3xl";
const sectionLead = "mt-4 max-w-3xl text-base leading-7 text-[#3d4947] md:text-lg";

function MarketingSection({
  id,
  bandClassName,
  children,
}: {
  id?: string;
  bandClassName: string;
  children: ReactNode;
}) {
  return (
    <section id={id} className={bandClassName}>
      <div className={marketingShell}>{children}</div>
    </section>
  );
}

export function MarketingHome() {
  return (
    <main id="main-content" className="flex-1 text-[#121e1c]">
      <MarketingSection bandClassName={marketingBand.mint}>
        <div className="grid grid-cols-1 items-center gap-10 py-12 md:grid-cols-[1.05fr_0.95fr] md:py-16 lg:py-20">
          <div className="max-w-2xl">
            <p className="text-sm font-medium text-[#4e6360]">Theo dõi kết quả khám, dễ hiểu hơn</p>
            <h1 className="mt-3 text-4xl font-black leading-tight tracking-tight text-[#121e1c] md:text-5xl">
              Một nơi an toàn để lưu trữ, theo dõi và hiểu kết quả khám sức khỏe của bạn.
            </h1>
            <p className="mt-5 max-w-2xl text-lg leading-8 text-[#3d4947]">
              HealthLens hỗ trợ bạn đọc kết quả xét nghiệm bằng ngôn ngữ dễ hiểu, tổ chức dữ liệu theo từng hồ sơ
              cá nhân hoặc gia đình, và chuẩn bị thông tin cần thiết trước khi trao đổi với bác sĩ.
            </p>
            <LandingGuestCtas className="mt-8" />
          </div>
          <LandingDashboardPreview />
        </div>
      </MarketingSection>

      <MarketingSection id="cach-hoat-dong" bandClassName={`${marketingBand.white} py-16 md:py-20`}>
        <h2 className={sectionTitle}>Cách HealthLens hoạt động</h2>
        <p className={sectionLead}>
          Ba bước đơn giản để bạn bắt đầu theo dõi kết quả khám mà không cần thay đổi quy trình chăm sóc y tế hiện tại.
        </p>
        <ol className="mt-10 grid gap-6 md:grid-cols-3">
          {howItWorksSteps.map((item) => (
            <li
              key={item.step}
              className="rounded-xl border border-[#bcc9c6]/40 bg-[#f6fbfa] p-6 shadow-[0_8px_28px_rgba(18,30,28,0.06)]"
            >
              <div className="flex items-start justify-between gap-3">
                <span className="text-xs font-semibold text-[#00685f]">Bước {item.step}</span>
                <item.icon className="h-6 w-6 shrink-0 text-[#00685f]" aria-hidden="true" />
              </div>
              <h3 className="mt-4 text-lg font-bold text-[#121e1c]">{item.title}</h3>
              <p className="mt-3 text-sm leading-6 text-[#4e6360]">{item.description}</p>
            </li>
          ))}
        </ol>
      </MarketingSection>

      <MarketingSection id="tin-cay" bandClassName={`${marketingBand.mintDeep} py-16 md:py-20`}>
        <h2 className={sectionTitle}>Tin cậy và tuân thủ</h2>
        <p className={sectionLead}>
          HealthLens được thiết kế với trọng tâm minh bạch về dữ liệu sức khỏe và ranh giới thông tin y tế.
        </p>
        <div className="mt-8 grid gap-6 lg:grid-cols-2">
          <article className="rounded-xl border border-[#bcc9c6]/40 bg-white p-6 shadow-sm">
            <h3 className="text-lg font-bold text-[#121e1c]">Bảo vệ dữ liệu cá nhân (NĐ 13/2023/NĐ-CP)</h3>
            <p className="mt-3 text-sm leading-7 text-[#4e6360]">
              Chúng tôi mô tả rõ loại dữ liệu được thu thập, mục đích xử lý và quyền của bạn theo quy định hiện hành
              về bảo vệ dữ liệu cá nhân tại Việt Nam. Khi sử dụng ứng dụng, bạn được hướng dẫn đồng thuận trước khi
              xử lý dữ liệu sức khỏe nhạy cảm.
            </p>
            <Link
              href="/privacy"
              className="mt-4 inline-block text-sm font-semibold text-[#00685f] underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
            >
              Đọc quy định bảo mật
            </Link>
          </article>
          <article className="rounded-xl border border-[#bcc9c6]/40 bg-white p-6 shadow-sm">
            <h3 className="text-lg font-bold text-[#121e1c]">Lưu ý y tế</h3>
            <p className="mt-3 text-sm leading-7 text-[#4e6360]">{MEDICAL_RECOMMENDATIONS_DISCLAIMER}</p>
            <p className="mt-3 text-sm leading-7 text-[#4e6360]">
              Kết quả trích xuất từ tệp tải lên có thể cần đối chiếu với phiếu gốc; HealthLens không thay thế chẩn
              đoán hay điều trị của bác sĩ và cơ sở y tế.
            </p>
            <Link
              href="/terms"
              className="mt-4 inline-block text-sm font-semibold text-[#00685f] underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
            >
              Xem điều khoản sử dụng
            </Link>
          </article>
        </div>
      </MarketingSection>

      <MarketingSection id="tinh-nang" bandClassName={`${marketingBand.white} py-16 md:py-20`}>
        <h2 className={sectionTitle}>Điểm nổi bật</h2>
        <p className={sectionLead}>
          Các tính năng cốt lõi giúp bạn chủ động hơn khi đọc lại kết quả khám — không hứa hẹn chẩn đoán tự động.
        </p>
        <ul className="mt-10 grid gap-6 md:grid-cols-3">
          {featureHighlights.map((item) => (
            <li
              key={item.title}
              className="rounded-xl border border-[#bcc9c6]/40 bg-[#f6fbfa] p-6 shadow-[0_8px_28px_rgba(18,30,28,0.06)]"
            >
              <item.icon className="h-6 w-6 text-[#00685f]" aria-hidden="true" />
              <h3 className="mt-4 text-lg font-bold text-[#121e1c]">{item.title}</h3>
              <p className="mt-3 text-sm leading-6 text-[#4e6360]">{item.description}</p>
            </li>
          ))}
        </ul>
      </MarketingSection>
    </main>
  );
}
