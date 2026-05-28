"use client";

import { useState } from "react";
import { Bell, Mail } from "lucide-react";

import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { breadcrumbFromSettings } from "@/lib/layout/dashboardBreadcrumbTrails";
import { NotificationInboxList } from "@/components/features/notifications/NotificationInboxList";
import { useNotificationEmailPreferences } from "@/hooks/useNotificationEmailPreferences";
import { useNotificationInbox } from "@/hooks/useNotificationInbox";

import { SettingsAccountSidebar } from "../_components/SettingsAccountSidebar";
import { SettingsDirectContactCard } from "../_components/SettingsDirectContactCard";
import { SettingsPageIntro } from "../_components/SettingsPageCard";
import { settingsCardClassName } from "../_components/settingsStyles";
import { EmailPreferencesForm } from "./_components/EmailPreferencesForm";

export default function NotificationSettingsPage() {
  const [inboxPage, setInboxPage] = useState(0);
  const [inboxLimit, setInboxLimit] = useState(5);
  const {
    items,
    unreadCount,
    pagination,
    isLoading: inboxLoading,
    isError: inboxError,
    refetch: refetchInbox,
    markAsRead,
    markAllAsRead,
    isMarkingAllRead,
  } = useNotificationInbox({ mode: "paged", page: inboxPage, limit: inboxLimit });

  const {
    preferences,
    isLoading: prefsLoading,
    isError: prefsError,
    refetch: refetchPrefs,
    savePreferences,
    isSaving,
  } = useNotificationEmailPreferences();

  return (
    <DashboardPageShell
      title="Thông báo"
      subtitle="Quản lý thông báo trong app và email bạn muốn nhận."
      breadcrumbs={breadcrumbFromSettings("Thông báo")}
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
                  : "Lời mời chia sẻ, nhắc lịch tái khám và cập nhật khác hiển thị tại đây và trên biểu tượng chuông ở thanh đầu trang."
              }
            />

            <div className="mt-6">
              <NotificationInboxList
                items={items}
                unreadCount={unreadCount}
                isLoading={inboxLoading}
                isError={inboxError}
                onRetry={() => {
                  void refetchInbox();
                }}
                onMarkRead={markAsRead}
                onMarkAllRead={markAllAsRead}
                isMarkingAllRead={isMarkingAllRead}
                variant="full"
                pagination={pagination}
                onPageChange={setInboxPage}
                onPageSizeChange={(limit) => {
                  setInboxLimit(limit);
                  setInboxPage(0);
                }}
                isPaginationDisabled={isMarkingAllRead}
              />
            </div>

            <p className="mt-4 text-xs text-[#6d7a77]">
              Danh sách tự làm mới khi bạn quay lại tab hoặc mỗi 30 giây.
            </p>
          </section>

          <section className={settingsCardClassName}>
            <SettingsPageIntro
              icon={Mail}
              title="Thông báo qua email"
              description="Chọn loại email bạn muốn nhận. Email bảo mật (xác minh, đổi mật khẩu, xóa tài khoản) luôn được gửi."
            />

            {prefsLoading ? (
              <p className="mt-6 text-sm text-[#6d7a77]">Đang tải tùy chọn email...</p>
            ) : prefsError ? (
              <div className="mt-6 space-y-3">
                <p className="text-sm text-[#ba1a1a]">Không tải được tùy chọn email.</p>
                <button
                  type="button"
                  onClick={() => {
                    void refetchPrefs();
                  }}
                  className="rounded-xl bg-[#00685f] px-4 py-2 text-sm font-bold text-white"
                >
                  Thử lại
                </button>
              </div>
            ) : preferences ? (
              <EmailPreferencesForm
                preferences={preferences}
                onSave={savePreferences}
                isSaving={isSaving}
              />
            ) : null}
          </section>

          <p className="text-sm leading-6 text-[#4e6360]">
            Chấp nhận hoặc từ chối lời mời trực tiếp trên trang{" "}
            <span className="font-bold text-[#00685f]">Hồ sơ gia đình</span> và{" "}
            <span className="font-bold text-[#00685f]">Kết quả khám</span> sau khi mở từ
            thông báo.
          </p>
        </div>

        <aside className="space-y-8">
          <SettingsAccountSidebar active="notifications" />
          <SettingsDirectContactCard
            title="Cần hỗ trợ?"
            description="Liên hệ đội ngũ nếu bạn có thắc mắc về thông báo trong app hoặc email."
          />
        </aside>
      </div>
    </DashboardPageShell>
  );
}
