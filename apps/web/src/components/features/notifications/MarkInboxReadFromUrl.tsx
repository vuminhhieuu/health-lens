"use client";

import { useMarkInboxReadFromSearchParams } from "@/hooks/useMarkInboxReadFromSearchParams";

/** Client helper for pages that handle `?inboxRead=` from notification navigation. */
export function MarkInboxReadFromUrl() {
  useMarkInboxReadFromSearchParams();
  return null;
}
