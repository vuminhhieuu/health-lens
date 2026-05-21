"use client";

export type ReferenceRange = {
  min: number;
  max: number;
  attentionMin: number;
  attentionMax: number;
  unit?: string;
};

export type RangeContext = {
  gender?: string | null;
  ageRange?: string | null;
};

type ReferenceRangeIndicatorProps = {
  referenceRange?: ReferenceRange | null;
  unit: string;
  referenceRangeSource?: "document" | "system" | "none";
  rangeContext?: RangeContext | null;
  critical?: boolean;
  explanation?: string;
  isExplanationLoading?: boolean;
  className?: string;
  skeletonLines?: number;
};

export function ReferenceRangeIndicator({
  referenceRange,
  unit,
  referenceRangeSource,
  rangeContext,
  critical,
  explanation,
  isExplanationLoading = false,
  className = "space-y-2.5 rounded-xl bg-[#f7fbfa] p-4 text-sm leading-relaxed text-[#35514c]",
  skeletonLines = 3,
}: ReferenceRangeIndicatorProps) {
  const displayReference = referenceRange
    ? `${referenceRange.min} - ${referenceRange.max} ${referenceRange.unit ?? unit}`
    : "Không có dữ liệu tham chiếu";
  const contextNote = formatRangeContextNote(rangeContext);
  const staticExplanation = explanation?.trim() || "";
  const showSkeleton = isExplanationLoading && !staticExplanation;

  return (
    <div className={className} aria-live={showSkeleton ? "polite" : undefined}>
      <p>
        <span className="font-semibold">Ngưỡng tham chiếu: </span>
        {displayReference}
      </p>
      {referenceRangeSource && referenceRangeSource !== "none" ? (
        <p>
          <span className="font-semibold">Nguồn ngưỡng: </span>
          {referenceRangeSource === "document" ? "Theo phiếu xét nghiệm" : "Theo hệ thống tham chiếu"}
        </p>
      ) : null}
      {referenceRangeSource === "system" ? (
        <p>
          <span className="font-semibold">Ngữ cảnh ngưỡng: </span>
          {contextNote ?? "Ngưỡng tham chiếu chung"}
        </p>
      ) : null}
      {critical ? (
        <p className="rounded-lg bg-[#fff2f2] px-3 py-2 text-[#ba1a1a]">
          Chỉ số có dấu hiệu vượt ngưỡng nguy cấp, nên liên hệ bác sĩ để được tư vấn sớm.
        </p>
      ) : null}
      {showSkeleton ? (
        <div className="space-y-2 pt-1" data-testid="reference-range-skeleton">
          {Array.from({ length: skeletonLines }).map((_, index) => (
            <div
              key={index}
              className={`h-3 animate-pulse rounded bg-[#d4e7e3] ${
                index === 0 ? "w-full" : index === 1 ? "w-4/5" : "w-3/5"
              }`}
            />
          ))}
        </div>
      ) : null}
      {staticExplanation ? (
        <p className="whitespace-pre-line">
          <span className="font-semibold">Giải thích: </span>
          {staticExplanation}
        </p>
      ) : null}
    </div>
  );
}

export function formatRangeContextNote(rangeContext?: RangeContext | null): string | null {
  if (!rangeContext || (!rangeContext.gender && !rangeContext.ageRange)) {
    return null;
  }

  const genderLabel =
    rangeContext.gender === "female"
      ? "Nữ"
      : rangeContext.gender === "male"
        ? "Nam"
        : null;
  const ageLabel = rangeContext.ageRange ? `${rangeContext.ageRange} tuổi` : null;
  const parts = [genderLabel, ageLabel].filter(Boolean);
  return parts.length > 0 ? `Ngưỡng áp dụng cho: ${parts.join(", ")}` : null;
}
