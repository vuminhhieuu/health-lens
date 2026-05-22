/**
 * Normalizes optional text for API payloads.
 * - Non-empty trimmed string → stored value
 * - Empty / whitespace → "" (explicit clear; backend treats as null)
 * - null → field omitted from partial updates when backend uses `!= null` checks
 */
export function normalizeOptionalTextField(value?: string | null): string | null {
  if (value == null) {
    return null;
  }
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : "";
}
