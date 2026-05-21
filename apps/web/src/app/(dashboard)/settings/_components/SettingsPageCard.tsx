import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

type SettingsPageCardProps = {
  children: ReactNode;
  className?: string;
};

/** Shared surface for settings sub-pages (privacy, security, coming-soon, etc.). */
export function SettingsPageCard({ children, className = "" }: SettingsPageCardProps) {
  return (
    <section
      className={`rounded-xl border border-[#bcc9c6]/30 bg-white p-6 shadow-sm sm:p-8 ${className}`.trim()}
    >
      {children}
    </section>
  );
}

type SettingsPageIntroProps = {
  icon: LucideIcon;
  eyebrow?: string;
  title: string;
  description: string;
};

export function SettingsPageIntro({ icon: Icon, eyebrow, title, description }: SettingsPageIntroProps) {
  return (
    <div className="flex flex-col gap-5 sm:flex-row sm:items-start">
      <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-xl bg-[#e9f6f3] text-[#00685f]">
        <Icon className="h-7 w-7" aria-hidden="true" />
      </div>
      <div className="min-w-0">
        {eyebrow ? (
          <p className="text-sm font-bold uppercase tracking-wider text-[#00685f]">{eyebrow}</p>
        ) : null}
        <h2 className={`${eyebrow ? "mt-2" : ""} text-2xl font-bold text-[#121e1c]`}>{title}</h2>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-[#4e6360]">{description}</p>
      </div>
    </div>
  );
}
