"use client";

import { useEffect, useRef } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

import { useNotificationInbox } from "@/hooks/useNotificationInbox";

/** Marks a single inbox item read when landing with `?inboxRead=<itemId>`. */
export function useMarkInboxReadFromSearchParams() {
  const searchParams = useSearchParams();
  const pathname = usePathname();
  const router = useRouter();
  const { markAsRead } = useNotificationInbox();
  const processedRef = useRef<string | null>(null);

  const inboxReadId = searchParams.get("inboxRead");

  useEffect(() => {
    if (!inboxReadId || processedRef.current === inboxReadId) {
      return;
    }

    processedRef.current = inboxReadId;
    markAsRead(inboxReadId);

    const params = new URLSearchParams(searchParams.toString());
    params.delete("inboxRead");
    const query = params.toString();
    router.replace(query ? `${pathname}?${query}` : pathname);
  }, [inboxReadId, markAsRead, pathname, router, searchParams]);
}
