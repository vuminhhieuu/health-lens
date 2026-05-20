import Link from "next/link";
import { ExternalLink, Mail, Phone } from "lucide-react";

import { getMarketingSocialLinks, marketingContact } from "@/lib/marketing/contact";
import { marketingBand, marketingShell } from "@/lib/marketing/layout";

const legalLinks = [
  { href: "/privacy", label: "Quy định bảo mật" },
  { href: "/terms", label: "Điều khoản sử dụng" },
] as const;

const supportLinks = [
  { href: "/support", label: "Hỗ trợ" },
  { href: "/help", label: "Trợ giúp" },
  { href: "/questions", label: "Thắc mắc" },
] as const;

const footerLinkClass =
  "text-sm font-medium text-[#d8ebe8] transition hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white";

const footerHeadingClass = "text-sm font-bold text-white";

export function MarketingFooter() {
  const year = new Date().getFullYear();
  const socialLinks = getMarketingSocialLinks();
  const mailtoHref = `mailto:${marketingContact.email}?subject=${encodeURIComponent("HealthLens - Liên hệ")}`;

  return (
    <footer className={marketingBand.teal}>
      <div className={`${marketingShell} py-12 md:py-14`}>
        <div className="flex flex-col gap-10 border-b border-white/15 pb-10 lg:flex-row lg:items-start lg:justify-between lg:gap-16">
          <div className="max-w-md shrink-0">
            <Link
              href="/"
              className="text-2xl font-bold tracking-tight text-white transition hover:text-[#c8f7ef] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
            >
              HealthLens
            </Link>
            <p className="mt-3 text-sm leading-7 text-[#d8ebe8]">
              Giúp bạn đọc và theo dõi kết quả sức khỏe bằng tiếng Việt dễ hiểu — trong không gian tôn trọng quyền
              riêng tư của bạn.
            </p>
          </div>

          <div className="grid flex-1 grid-cols-1 gap-8 sm:grid-cols-2 lg:max-w-2xl lg:grid-cols-3 lg:gap-10">
            <section aria-labelledby="footer-learn-heading">
              <h2 id="footer-learn-heading" className={footerHeadingClass}>
                Tìm hiểu thêm
              </h2>
              <nav aria-label="Liên kết tìm hiểu thêm" className="mt-4 flex flex-col gap-2.5">
                {supportLinks.map((item) => (
                  <Link key={item.href} href={item.href} className={footerLinkClass}>
                    {item.label}
                  </Link>
                ))}
              </nav>
            </section>

            <section aria-labelledby="footer-legal-heading">
              <h2 id="footer-legal-heading" className={footerHeadingClass}>
                Pháp lý
              </h2>
              <nav aria-label="Pháp lý và quyền riêng tư" className="mt-4 flex flex-col gap-2.5">
                {legalLinks.map((item) => (
                  <Link key={item.href} href={item.href} className={footerLinkClass}>
                    {item.label}
                  </Link>
                ))}
              </nav>
            </section>

            <section aria-labelledby="footer-contact-heading" className="sm:col-span-2 lg:col-span-1">
              <h2 id="footer-contact-heading" className={footerHeadingClass}>
                Liên hệ
              </h2>
              <ul className="mt-4 space-y-3">
                <li>
                  <a
                    href={marketingContact.phoneHref}
                    className="inline-flex items-center gap-2 text-sm font-medium text-white transition hover:text-[#c8f7ef] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
                  >
                    <Phone className="h-4 w-4 shrink-0 text-[#89f5e7]" aria-hidden />
                    {marketingContact.phoneDisplay}
                  </a>
                </li>
                <li>
                  <a
                    href={mailtoHref}
                    className="inline-flex items-center gap-2 break-all text-sm font-medium text-white transition hover:text-[#c8f7ef] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
                  >
                    <Mail className="h-4 w-4 shrink-0 text-[#89f5e7]" aria-hidden />
                    {marketingContact.email}
                  </a>
                </li>
              </ul>
              {socialLinks.length > 0 ? (
                <ul className="mt-4 flex flex-col gap-1" aria-label="Liên kết mạng xã hội">
                  {socialLinks.map((item) => (
                    <li key={item.id}>
                      <a
                        href={item.href}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-2 rounded-lg py-1 text-sm font-medium text-[#c8f7ef] transition hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
                      >
                        {item.label}
                        <ExternalLink className="h-3.5 w-3.5 opacity-80" aria-hidden />
                        <span className="sr-only"> (mở tab mới)</span>
                      </a>
                    </li>
                  ))}
                </ul>
              ) : null}
            </section>
          </div>
        </div>

        <div className="pt-8">
          <p className="text-xs leading-5 text-[#b8dbd6]">
            © {year} HealthLens. Các tài liệu và liên kết trên có thể được cập nhật theo thời gian.
          </p>
          <p className="mt-3 max-w-3xl text-xs leading-5 text-[#b8dbd6]">
            Thông tin trên ứng dụng chỉ mang tính tham khảo và không thay thế chẩn đoán hoặc điều trị của bác sĩ và cơ
            sở y tế.
          </p>
        </div>
      </div>
    </footer>
  );
}
