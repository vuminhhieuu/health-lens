"use client";

import { Bell, Mail } from "lucide-react";

import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { NotificationInboxList } from "@/components/features/notifications/NotificationInboxList";
import { useNotificationInbox } from "@/hooks/useNotificationInbox";

import { SettingsAccountNav } from "../_components/SettingsAccountNav";
import { SettingsPageCard, SettingsPageIntro } from "../_components/SettingsPageCard";
import { settingsCardClassName } from "../_components/settingsStyles";

export default function NotificationSettingsPage() {
  const {
    items,
    unreadCount,
    isLoading,
    isError,
    refetch,
    markAsRead,
    markAllAsRead,
    isMarkingAllRead,
  } = useNotificationInbox();

  return (
    <DashboardPageShell
      title="Thông báo"
      subtitle="Lời mời và cập nhật trong app hiển thị ở chuông và tại đây; tùy chọn email sẽ có trong bản cập nhật tiếp theo."
      breadcrumbs={[
        { label: "Trang chủ", href: "/home" },
        { label: "Cài đặt", href: "/settings" },
        { label: "Thông báo" },
      ]}
    >
      <div className="grid grid-cols-1 items-start gap-8 lg:grid-cols-3">
        <div className="space-y-8 lg:col-span-2">
          <section className={settingsCardClassName}>
            <SettingsPageIntro
              icon={Bell}
              title="Trong ứng dụng"
              description={
                unreadCount > 0
                  ? `Bạn có ${unreadCount} lời mời chưa xử lý. Bấm một mục để mở trang Hồ sơ hoặc Kết quả khám và chấp nhận lời mời.`
                  : "Lời mời chia sẻ hồ sơ và kết quả khám hiển thị tại đây và trên biểu tượng chuông ở thanh đầu trang."
              }
            />

            <div className="mt-6">
              <NotificationInboxList
                items={items}
                unreadCount={unreadCount}
                isLoading={isLoading}
                isError={isError}
                onRetry={() => {
                  void refetch();
                }}
                onMarkRead={markAsRead}
                onMarkAllRead={markAllAsRead}
                isMarkingAllRead={isMarkingAllRead}
                variant="full"
              />
            </div>

            <p className="mt-4 text-xs text-[#6d7a77]">
              Danh sách tự làm mới khi bạn quay lại tab hoặc mỗi 30 giây.
            </p>
          </section>

          <section className={settingsCardClassName}>
            <SettingsPageIntro
              icon={Mail}
              eyebrow="Sắp có"
              title="Thông báo qua email"
              description="Bạn sẽ chọn nhận email cho lời mời chia sẻ, nhắc tái khám và các thông báo khác. Email bảo mật (đổi mật khẩu, xác minh) vẫn được gửi."
            />
          </section>

          <p className="text-sm leading-6 text-[#4e6360]">
            Chấp nhận hoặc từ chối lời mời trực tiếp trên trang{" "}
            <span className="font-bold text-[#00685f]">Hồ sơ gia đình</span> và{" "}
            <span className="font-bold text-[#00685f]">Kết quả khám</span> sau khi mở từ
            thông báo.
          </p>
        </div>

        <aside className="space-y-6">
          <SettingsPageCard>
            <SettingsPageIntro
              icon={Bell}
              title="Lối tắt"
              description="Quay lại các mục cài đặt tài khoản khác."
            />
            <div className="mt-6">
              <SettingsAccountNav active="notifications" />
            </div>
          </SettingsPageCard>
        </aside>
      </div>
    </DashboardPageShell>
  );
}
