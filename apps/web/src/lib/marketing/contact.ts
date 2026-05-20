const SAFE_EXTERNAL_PROTOCOLS = new Set(["https:", "http:"]);

function optionalPublicUrl(envKey: string): string | undefined {
  const raw = process.env[envKey];
  const trimmed = raw?.trim();
  if (!trimmed) {
    return undefined;
  }

  try {
    const url = new URL(trimmed);
    if (!SAFE_EXTERNAL_PROTOCOLS.has(url.protocol)) {
      return undefined;
    }
    return url.href;
  } catch {
    return undefined;
  }
}

/** Thông tin liên hệ hiển thị trên footer marketing (đồng bộ với các màn có sẵn trong app). */
export const marketingContact = {
  phoneDisplay: "1900 1234",
  phoneHref: "tel:19001234",
  email: "support@healthlens.vn",
} as const;

export type MarketingSocialId = "facebook" | "linkedin" | "instagram" | "youtube";

export type MarketingSocialLink = {
  id: MarketingSocialId;
  label: string;
  href: string;
};

/** Điền URL trong `.env` — chỉ hiển thị icon khi có giá trị. */
export function getMarketingSocialLinks(): MarketingSocialLink[] {
  const candidates: Array<{ id: MarketingSocialId; label: string; envKey: string }> = [
    { id: "facebook", label: "Facebook", envKey: "NEXT_PUBLIC_MARKETING_SOCIAL_FACEBOOK" },
    { id: "linkedin", label: "LinkedIn", envKey: "NEXT_PUBLIC_MARKETING_SOCIAL_LINKEDIN" },
    { id: "instagram", label: "Instagram", envKey: "NEXT_PUBLIC_MARKETING_SOCIAL_INSTAGRAM" },
    { id: "youtube", label: "YouTube", envKey: "NEXT_PUBLIC_MARKETING_SOCIAL_YOUTUBE" },
  ];

  return candidates.flatMap((item) => {
    const href = optionalPublicUrl(item.envKey);
    return href ? [{ id: item.id, label: item.label, href }] : [];
  });
}
