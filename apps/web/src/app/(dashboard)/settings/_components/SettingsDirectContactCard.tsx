import { Mail, Phone } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";

import { PUBLIC_SUPPORT_HREF, supportContact } from "@/lib/supportContact";

import { settingsCardClassName } from "./settingsStyles";

type SettingsDirectContactCardProps = {
  title?: string;
  /** Plain text or custom nodes. Omit to use the default copy with help-page link. */
  description?: ReactNode;
  /** Optional mailto subject (e.g. app version for support tickets). */
  mailtoSubject?: string;
};

function DefaultDescription() {
  return (
    <>
      Gọi hoặc email đội ngũ hỗ trợ. Bạn cũng có thể mở{" "}
      <Link href={PUBLIC_SUPPORT_HREF} className="font-bold text-[#00685f] hover:underline">
        trang trợ giúp
      </Link>{" "}
      để xem hướng dẫn chi tiết.
    </>
  );
}

export function SettingsDirectContactCard({
  title = "Liên hệ trực tiếp",
  description,
  mailtoSubject,
}: SettingsDirectContactCardProps) {
  const mailtoHref = mailtoSubject
    ? `mailto:${supportContact.email}?subject=${encodeURIComponent(mailtoSubject)}`
    : `mailto:${supportContact.email}`;

  return (
    <section className={settingsCardClassName}>
      <div className="mb-6 flex items-center gap-3">
        <Mail className="h-6 w-6 shrink-0 text-[#00685f]" aria-hidden="true" />
        <h3 className="text-xl font-bold text-[#121e1c]">{title}</h3>
      </div>
      <p className="text-sm leading-relaxed text-[#3d4947]">
        {description ?? <DefaultDescription />}
      </p>
      <div className="mt-6 space-y-3">
        <a
          href={supportContact.phoneHref}
          className="flex min-h-12 items-center justify-center gap-2 rounded-full bg-[#e9f6f3] px-4 text-sm font-bold text-[#00685f] transition hover:bg-[#d8ebe6] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
        >
          <Phone className="h-4 w-4" aria-hidden="true" />
          {supportContact.phoneDisplay}
        </a>
        <a
          href={mailtoHref}
          className="flex min-h-12 items-center justify-center gap-2 rounded-full bg-[#00685f] px-4 text-sm font-bold text-white shadow-lg shadow-[#00685f]/20 transition hover:bg-[#008378] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
        >
          <Mail className="h-4 w-4" aria-hidden="true" />
          {supportContact.email}
        </a>
      </div>
    </section>
  );
}
