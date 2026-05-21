import { CircleHelp } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";

import { AUTH_PUBLIC_HELP_HREF } from "@/lib/authPublicLinks";
import { authenticatedHeaderInnerClassName } from "@/lib/layout/shell";

type AuthPageHeaderProps = {
  actions?: ReactNode;
};

export function AuthPageHeader({ actions }: AuthPageHeaderProps) {
  return (
    <header className="fixed top-0 z-50 w-full border-b border-[#d8e5e2] bg-[#effcf9]/80 px-6 backdrop-blur-md">
      <div className={`h-16 ${authenticatedHeaderInnerClassName}`}>
        <Link href="/" className="text-2xl font-bold tracking-tight text-[#005049]">
          HealthLens
        </Link>
        <div className="flex items-center gap-1">
          <Link
            href={AUTH_PUBLIC_HELP_HREF}
            aria-label="Trợ giúp"
            className="rounded-full p-2 text-[#3f6560] transition hover:bg-[#d8e5e2]"
          >
            <CircleHelp className="h-5 w-5" />
          </Link>
          {actions}
        </div>
      </div>
    </header>
  );
}
