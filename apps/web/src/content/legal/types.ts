import type { ReactNode } from "react";

export type LegalContentBlock =
  | { type: "paragraph"; text: string }
  | { type: "list"; items: string[] };

export type LegalSection = {
  id: string;
  title: string;
  blocks: LegalContentBlock[];
};

export type LegalPageContent = {
  title: string;
  description: string;
  lastUpdated: string;
  showDraftBanner?: boolean;
  sections: LegalSection[];
};

export type LegalSectionRendererProps = {
  section: LegalSection;
  children?: ReactNode;
};
