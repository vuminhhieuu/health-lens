import type { ReactNode } from "react";
import Link from "next/link";

type TocItem = {
  id: string;
  label: string;
};

type PublicContentLayoutProps = {
  title: string;
  description: string;
  lastUpdated: string;
  toc: TocItem[];
  children: ReactNode;
};

export function PublicContentLayout({
  title,
  description,
  lastUpdated,
  toc,
  children,
}: PublicContentLayoutProps) {
  return (
    <main id="main-content" className="mx-auto w-full max-w-[96rem] flex-1 px-4 py-10 md:px-6 lg:px-8 xl:px-10">
      <section className="rounded-2xl border border-[#bcc9c6]/40 bg-white p-6 shadow-[0_8px_28px_rgba(18,30,28,0.08)] md:p-8">
        <h1 className="text-3xl font-black tracking-tight text-[#121e1c] md:text-4xl">{title}</h1>
        <p className="mt-4 text-base leading-7 text-[#3d4947]">{description}</p>
        <p className="mt-4 text-sm font-medium text-[#6d7a77]">Cập nhật lần cuối: {lastUpdated}</p>
      </section>

      <section className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-[16rem_minmax(0,1fr)]">
        <aside className="rounded-xl border border-[#bcc9c6]/35 bg-white p-5 shadow-sm lg:sticky lg:top-24 lg:self-start">
          <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#6d7a77]">Mục lục</p>
          <nav aria-label="Mục lục nội dung" className="mt-4 flex flex-col gap-2">
            {toc.map((item) => (
              <Link
                key={item.id}
                href={`#${item.id}`}
                className="rounded-md px-2 py-1.5 text-sm font-semibold text-[#00685f] transition hover:bg-[#e9f6f3] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
              >
                {item.label}
              </Link>
            ))}
          </nav>
        </aside>

        <div className="space-y-4">{children}</div>
      </section>
    </main>
  );
}

type PublicContentSectionProps = {
  id: string;
  title: string;
  children: ReactNode;
};

export function PublicContentSection({ id, title, children }: PublicContentSectionProps) {
  return (
    <section id={id} className="scroll-mt-24 rounded-xl border border-[#bcc9c6]/35 bg-white p-5 shadow-sm md:p-6">
      <h2 className="text-xl font-bold text-[#121e1c]">{title}</h2>
      <div className="mt-3 space-y-3 text-sm leading-7 text-[#4e6360] md:text-base">{children}</div>
    </section>
  );
}
