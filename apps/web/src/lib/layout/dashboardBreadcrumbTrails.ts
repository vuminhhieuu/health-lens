import type { DashboardBreadcrumbItem } from "@/components/layout/DashboardBreadcrumbBar";

/** Wide content shell used by health-record review (wider than default max-w-7xl). */
export const reviewPageContentShell = "mx-auto w-full max-w-[1360px] px-6";

/** Trang chủ › current */
export function breadcrumbFromHome(
  currentLabel: string,
): DashboardBreadcrumbItem[] {
  return [
    { label: "Trang chủ", href: "/home" },
    { label: currentLabel },
  ];
}

/** Cài đặt › current (trang con settings — không có Trang chủ) */
export function breadcrumbFromSettings(
  currentLabel: string,
): DashboardBreadcrumbItem[] {
  return [
    { label: "Cài đặt", href: "/settings" },
    { label: currentLabel },
  ];
}

/** Kết quả khám › [Lịch sử khám bệnh] › current */
export function breadcrumbForHealthRecordReview(
  currentLabel: string,
  options: { historyHref?: string | null } = {},
): DashboardBreadcrumbItem[] {
  const items: DashboardBreadcrumbItem[] = [
    { label: "Kết quả khám", href: "/health-records" },
  ];

  if (options.historyHref) {
    items.push({ label: "Lịch sử khám bệnh", href: options.historyHref });
  } else {
    items.push({ label: "Lịch sử khám bệnh" });
  }

  items.push({ label: currentLabel });
  return items;
}

/** Kết quả khám › profile name */
export function breadcrumbForProfileHistory(
  profileName: string,
): DashboardBreadcrumbItem[] {
  return [
    { label: "Kết quả khám", href: "/health-records" },
    { label: profileName },
  ];
}
