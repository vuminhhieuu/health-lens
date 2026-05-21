import Link from "next/link";
import { Fragment } from "react";

import { AUTH_PUBLIC_FOOTER_LINKS } from "@/lib/authPublicLinks";

export function AuthPageFooter() {
  return (
    <div className="mt-6 flex flex-wrap items-center justify-center gap-4 text-xs text-[#6d7a77]">
      {AUTH_PUBLIC_FOOTER_LINKS.map((link, index) => (
        <Fragment key={link.href}>
          {index > 0 ? <span className="text-[#d8e5e2]">|</span> : null}
          <Link href={link.href} className="transition hover:text-[#00685f]">
            {link.label}
          </Link>
        </Fragment>
      ))}
    </div>
  );
}
