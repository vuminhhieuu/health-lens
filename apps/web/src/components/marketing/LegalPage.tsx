import { PublicContentLayout, PublicContentSection } from "@/components/marketing/PublicContentLayout";
import { LegalDraftBanner } from "@/components/marketing/LegalDraftBanner";
import type { LegalPageContent } from "@/content/legal/types";

type LegalPageProps = {
  content: LegalPageContent;
};

export function LegalPage({ content }: LegalPageProps) {
  const toc = content.sections.map((section) => ({
    id: section.id,
    label: section.title.replace(/^\d+\.\s*/, ""),
  }));

  return (
    <PublicContentLayout
      title={content.title}
      description={content.description}
      lastUpdated={content.lastUpdated}
      toc={toc}
    >
      {content.showDraftBanner ? <LegalDraftBanner /> : null}
      {content.sections.map((section) => (
        <PublicContentSection key={section.id} id={section.id} title={section.title}>
          {section.blocks.map((block, index) => {
            if (block.type === "paragraph") {
              return <p key={`${section.id}-p-${index}`}>{block.text}</p>;
            }
            return (
              <ul key={`${section.id}-ul-${index}`} className="list-disc space-y-2 pl-5">
                {block.items.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            );
          })}
        </PublicContentSection>
      ))}
    </PublicContentLayout>
  );
}
