"use client";

import { Bell, Mail } from "lucide-react";
import { useEffect, useState } from "react";

import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { NotificationInboxList } from "@/components/features/notifications/NotificationInboxList";
import {
  useNotificationEmailPreferences,
  type NotificationEmailPreferences,
} from "@/hooks/useNotificationEmailPreferences";
import { useNotificationInbox } from "@/hooks/useNotificationInbox";

import { SettingsAccountNav } from "../_components/SettingsAccountNav";
import { SettingsPageCard, SettingsPageIntro } from "../_components/SettingsPageCard";
import { settingsCardClassName } from "../_components/settingsStyles";
import { EmailPreferenceToggle } from "./_components/EmailPreferenceToggle";

const DEFAULT_EMAIL_PREFS: NotificationEmailPreferences = {
  shareInvite: true,
  shareAccepted: true,
  followUpReminder: true,
  security: true,
};

export default function NotificationSettingsPage() {
  const {
    items,
    unreadCount,
    isLoading: inboxLoading,
    isError: inboxError,
    refetch: refetchInbox,
    markAsRead,
    markAllAsRead,
    isMarkingAllRead,
  } = useNotificationInbox();

  const {
    preferences,
    isLoading: prefsLoading,
    isError: prefsError,
    refetch: refetchPrefs,
    savePreferences,
    isSaving,
  } = useNotificationEmailPreferences();

  const [draftPrefs, setDraftPrefs] = useState<NotificationEmailPreferences>(DEFAULT_EMAIL_PREFS);
  const [isDirty, setIsDirty] = useState(false);

  useEffect(() => {
    if (preferences) {
      setDraftPrefs(preferences);
      setIsDirty(false);
    }
  }, [preferences]);

  const updateDraft = (patch: Partial<NotificationEmailPreferences>) => {
    setDraftPrefs((current) => ({ ...current, ...patch }));
    setIsDirty(true);
  };

  const handleSaveEmailPrefs = () => {
    savePreferences(draftPrefs);
  };

  return (
    <DashboardPageShell
      title="Thông báo"
      subtitle="Quản lý thông báo trong app và email bạn muốn nhận."
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
            ) : (
              <div className="mt-6 space-y-4">
                <EmailPreferenceToggle
                  id="shareInvite"
                  label="Lời mời chia sẻ"
                  description="Email khi ai đó mời bạn xem hồ sơ hoặc kết quả khám."
                  checked={draftPrefs.shareInvite}
                  onChange={(checked) => updateDraft({ shareInvite: checked })}
                />
                <EmailPreferenceToggle
                  id="shareAccepted"
                  label="Chấp nhận lời mời"
                  description="Email khi người được mời đã chấp nhận chia sẻ hồ sơ của bạn."
                  checked={draftPrefs.shareAccepted}
                  onChange={(checked) => updateDraft({ shareAccepted: checked })}
                />
                <EmailPreferenceToggle
                  id="followUpReminder"
                  label="Nhắc tái khám"
                  description="Email nhắc vào ngày đã lưu (không gửi ngay khi tạo nhắc cho ngày tương lai). Cần bật để nhận email."
                  checked={draftPrefs.followUpReminder}
                  onChange={(checked) => updateDraft({ followUpReminder: checked })}
                />
                <EmailPreferenceToggle
                  id="security"
                  label="Bảo mật tài khoản"
                  description="Xác minh email, đặt lại mật khẩu và thông báo liên quan đến tài khoản."
                  checked={true}
                  disabled
                  onChange={() => undefined}
                />

                <div className="flex justify-end border-t border-[#bcc9c6]/20 pt-4">
                  <button
                    type="button"
                    disabled={!isDirty || isSaving}
                    onClick={handleSaveEmailPrefs}
                    className="rounded-xl bg-gradient-to-r from-[#00685f] to-[#008378] px-6 py-3 text-sm font-bold text-white shadow-lg shadow-[#00685f]/20 transition disabled:opacity-60"
                  >
                    {isSaving ? "Đang lưu..." : "Lưu tùy chọn email"}
                  </button>
                </div>
              </div>
            )}
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
