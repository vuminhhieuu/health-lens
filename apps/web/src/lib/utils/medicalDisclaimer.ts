export const MEDICAL_RECOMMENDATIONS_DISCLAIMER =
  "Thông tin do HealthLens cung cấp chỉ mang tính tham khảo, không thay thế tư vấn, chẩn đoán hoặc điều trị từ bác sĩ.";

export function recommendationDisclaimerText(disclaimer: string | null | undefined): string {
  const normalized = disclaimer?.trim();
  return normalized || MEDICAL_RECOMMENDATIONS_DISCLAIMER;
}
