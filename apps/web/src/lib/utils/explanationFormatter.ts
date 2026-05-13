/**
 * Shared utility for formatting metric explanations into a consistent 3-line format.
 * Used by both ReviewRecordPage popup and HealthMetricCard component.
 */

export const FALLBACK_EXPLANATION = [
  "Chỉ số này là gì: Đây là một chỉ số xét nghiệm phản ánh tình trạng sức khỏe hiện tại.",
  "Chỉ số này liên quan đến: Cân bằng chuyển hóa, miễn dịch hoặc chức năng cơ quan tùy loại xét nghiệm.",
  "Ảnh hưởng thường gặp nếu chỉ số lệch ngưỡng: Bạn nên theo dõi thêm và trao đổi với bác sĩ để được tư vấn phù hợp.",
].join("\n");

export function toThreeLineExplanation(raw: string | undefined): string {
  if (!raw || !raw.trim()) {
    return FALLBACK_EXPLANATION;
  }
  const lines = raw
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean);
  if (lines.length >= 3) {
    return lines.slice(0, 3).join("\n");
  }
  return FALLBACK_EXPLANATION;
}
