/** Thông báo sau khi tạo / import chỉ số tham chiếu — dùng chung trang danh sách và trang import */
export const NOTICE_NEW_METRIC_SINGLE_ADMIN = "Đã tạo chỉ số mới thành công.";

export function formatNoticeNewMetricMultiAdmin(requestCount: number): string {
  const n = Math.max(0, Math.floor(requestCount));
  return `Đã gửi ${n} yêu cầu tạo chỉ số mới để phê duyệt.`;
}
