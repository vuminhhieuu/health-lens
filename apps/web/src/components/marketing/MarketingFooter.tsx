import Link from "next/link";
import { ExternalLink, Mail, Phone } from "lucide-react";

import { getMarketingSocialLinks, marketingContact } from "@/lib/marketing/contact";

const legalLinks = [
  { href: "/privacy", label: "Quy định bảo mật" },
  { href: "/terms", label: "Điều khoản sử dụng" },
] as const;

const supportLinks = [
  { href: "/help", label: "Trợ giúp" },
  { href: "/questions", label: "Thắc mắc" },
] as const;

const externalSocialClass =
  "inline-flex items-center gap-2 rounded-lg border border-transparent px-2 py-1.5 text-sm font-semibold text-[#00685f] transition hover:border-[#bcc9c6]/60 hover:bg-[#f6fbfa] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]";

export function MarketingFooter() {
  const year = new Date().getFullYear();
  const socialLinks = getMarketingSocialLinks();
  const mailtoHref = `mailto:${marketingContact.email}?subject=${encodeURIComponent("HealthLens - Liên hệ")}`;

  const linkClass =
    "text-sm font-semibold text-[#00685f] underline-offset-4 transition hover:text-[#005049] hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]";

  const columnTitleClass =
    "text-xs font-bold uppercase tracking-[0.14em] text-[#6d7a77]";

  return (
    <footer className="border-t border-[#bcc9c6]/40 bg-white/95 py-8">
      <div className="mx-auto max-w-7xl px-6 md:px-10 lg:px-12">
        <div className="grid grid-cols-1 gap-8 md:grid-cols-3 md:gap-10 lg:gap-12">
          <section aria-labelledby="footer-about-heading">
            <h2 id="footer-about-heading" className={columnTitleClass}>
              Về chúng tôi
            </h2>
            <p className="mt-3 text-sm leading-6 text-[#4e6360]">
              HealthLens giúp người dùng Việt Nam đọc và theo dõi kết quả sức khỏe dễ tiếp cận hơn —
              trong một không gian được thiết kế để tôn trọng quyền riêng tư của bạn.
            </p>
            <Link href="/" className={`mt-4 inline-block ${linkClass}`}>
              Về trang chủ HealthLens
            </Link>
          </section>

          <section aria-labelledby="footer-contact-heading">
            <h2 id="footer-contact-heading" className={columnTitleClass}>
              Liên hệ
            </h2>
            <ul className="mt-3 space-y-2.5 text-sm text-[#4e6360]">
              <li>
                <a
                  href={marketingContact.phoneHref}
                  className="inline-flex items-center gap-2 font-medium text-[#121e1c] transition hover:text-[#00685f] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
                >
                  <Phone className="h-4 w-4 shrink-0 text-[#00685f]" aria-hidden />
                  <span>{marketingContact.phoneDisplay}</span>
                </a>
              </li>
              <li>
                <a
                  href={mailtoHref}
                  className="inline-flex items-center gap-2 break-all font-medium text-[#121e1c] transition hover:text-[#00685f] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
                >
                  <Mail className="h-4 w-4 shrink-0 text-[#00685f]" aria-hidden />
                  {marketingContact.email}
                </a>
              </li>
            </ul>
            <div className="mt-4">
              <p className={columnTitleClass}>Mạng xã hội</p>
              {socialLinks.length > 0 ? (
                <ul className="mt-3 flex flex-col gap-1" aria-label="Liên kết mạng xã hội">
                  {socialLinks.map((item) => (
                    <li key={item.id}>
                      <a
                        href={item.href}
                        target="_blank"
                        rel="noopener noreferrer"
                        className={externalSocialClass}
                      >
                        {item.label}
                        <ExternalLink className="h-4 w-4 shrink-0 opacity-80" aria-hidden />
                        <span className="sr-only"> (mở tab mới)</span>
                      </a>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="mt-3 text-sm leading-6 text-[#6d7a77]">
                  {process.env.NODE_ENV === "development" ? (
                    <>
                      Các kênh chính thức sẽ hiển thị khi bạn đặt biến môi trường{" "}
                      <code className="rounded bg-[#e9f6f3] px-1 py-0.5 text-xs text-[#005049]">
                        NEXT_PUBLIC_MARKETING_SOCIAL_*
                      </code>{" "}
                      trong file env của ứng dụng web (xem <span className="whitespace-nowrap">.env.example</span>
                      ).
                    </>
                  ) : (
                    <>Theo dõi HealthLens trên các kênh chính thức — liên kết sẽ được cập nhật tại đây.</>
                  )}
                </p>
              )}
            </div>
          </section>

          <section aria-labelledby="footer-learn-heading">
            <h2 id="footer-learn-heading" className={columnTitleClass}>
              Tìm hiểu thêm
            </h2>
            <div className="mt-3 space-y-5">
              <div>
                <h3 className="text-xs font-semibold uppercase tracking-wide text-[#3d4947]">
                  Pháp lý &amp; quyền riêng tư
                </h3>
                <nav aria-label="Pháp lý và quyền riêng tư" className="mt-3 flex flex-col gap-2">
                  {legalLinks.map((item) => (
                    <Link key={item.href} href={item.href} className={linkClass}>
                      {item.label}
                    </Link>
                  ))}
                </nav>
              </div>
              <div>
                <h3 className="text-xs font-semibold uppercase tracking-wide text-[#3d4947]">
                  Hỗ trợ &amp; câu hỏi
                </h3>
                <nav aria-label="Hỗ trợ và câu hỏi" className="mt-3 flex flex-col gap-2">
                  {supportLinks.map((item) => (
                    <Link key={item.href} href={item.href} className={linkClass}>
                      {item.label}
                    </Link>
                  ))}
                </nav>
              </div>
            </div>
          </section>
        </div>

        <p className="mt-8 border-t border-[#e1ebe8] pt-5 text-xs leading-5 text-[#6d7a77]">
          © {year} HealthLens. Các tài liệu và liên kết trên có thể được cập nhật theo thời gian.
        </p>
        <p className="mt-3 max-w-3xl text-xs leading-5 text-[#6d7a77]">
          HealthLens hỗ trợ bạn theo dõi và hiểu kết quả sức khỏe; thông tin trên ứng dụng chỉ mang
          tính tham khảo và không thay thế chẩn đoán hoặc điều trị của bác sĩ và cơ sở y tế.
        </p>
      </div>
    </footer>
  );
}
