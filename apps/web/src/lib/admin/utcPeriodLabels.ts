import { format, getISOWeek } from "date-fns";

/**
 * Backend activity buckets use {@code yyyy-MM-dd} as UTC calendar dates
 * (weekly {@code periodStart} = Monday, aligned with Postgres
 * {@code DATE_TRUNC('week', created_at AT TIME ZONE 'UTC')}).
 */

export function parseUtcPeriodStart(isoDate: string): Date {
  const date = new Date(`${isoDate}T00:00:00Z`);
  if (Number.isNaN(date.getTime())) {
    throw new Error(`Invalid UTC period start: ${isoDate}`);
  }
  return date;
}

/**
 * Feeds date-fns (local-calendar APIs) the UTC y-m-d without timezone drift.
 */
function utcCalendarAsLocalDate(isoDate: string): Date {
  const utc = parseUtcPeriodStart(isoDate);
  return new Date(utc.getUTCFullYear(), utc.getUTCMonth(), utc.getUTCDate());
}

/** ISO week number (Monday-based), matching backend weekly buckets. */
export function utcIsoWeekNumber(isoDate: string): number {
  return getISOWeek(utcCalendarAsLocalDate(isoDate));
}

/** X-axis label for weekly buckets, e.g. {@code T10}. */
export function formatUtcIsoWeekAxisLabel(periodStart: string): string {
  try {
    return `T${utcIsoWeekNumber(periodStart)}`;
  } catch {
    return periodStart;
  }
}

/** Tooltip / title for a UTC week bucket (Monday {@code periodStart}). */
export function formatUtcWeekBucketTooltip(periodStart: string): string {
  try {
    return `Tuần bắt đầu ${format(utcCalendarAsLocalDate(periodStart), "dd/MM/yyyy")} (UTC)`;
  } catch {
    return periodStart;
  }
}

/** Day bucket label in UTC calendar. */
export function formatUtcDayAxisLabel(periodStart: string): string {
  try {
    return format(utcCalendarAsLocalDate(periodStart), "dd/MM");
  } catch {
    return periodStart;
  }
}

/** Day bucket tooltip in UTC calendar. */
export function formatUtcDayBucketTooltip(periodStart: string): string {
  try {
    return format(utcCalendarAsLocalDate(periodStart), "dd/MM/yyyy");
  } catch {
    return periodStart;
  }
}

export function formatActivityBucketLabel(
  periodStart: string,
  granularity: "day" | "week",
): string {
  return granularity === "week"
    ? formatUtcIsoWeekAxisLabel(periodStart)
    : formatUtcDayAxisLabel(periodStart);
}

export function formatActivityBucketTooltip(
  periodStart: string,
  granularity: "day" | "week",
): string {
  return granularity === "week"
    ? formatUtcWeekBucketTooltip(periodStart)
    : formatUtcDayBucketTooltip(periodStart);
}
