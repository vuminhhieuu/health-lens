"use client";

import { useState } from "react";

import type { NotificationEmailPreferences } from "@/hooks/useNotificationEmailPreferences";

import { EmailPreferenceToggle } from "./EmailPreferenceToggle";

type EmailPreferencesFormProps = {
  preferences: NotificationEmailPreferences;
  onSave: (prefs: NotificationEmailPreferences) => void;
  isSaving: boolean;
};

export function EmailPreferencesForm({
  preferences,
  onSave,
  isSaving,
}: EmailPreferencesFormProps) {
  const [draftPrefs, setDraftPrefs] = useState(preferences);
  const [isDirty, setIsDirty] = useState(false);
  const [syncedPreferences, setSyncedPreferences] = useState(preferences);

  if (preferences !== syncedPreferences) {
    setSyncedPreferences(preferences);
    setDraftPrefs(preferences);
    setIsDirty(false);
  }

  const updateDraft = (patch: Partial<NotificationEmailPreferences>) => {
    setDraftPrefs((current) => ({ ...current, ...patch }));
    setIsDirty(true);
  };

  const handleSave = () => {
    onSave(draftPrefs);
    setIsDirty(false);
  };

  return (
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
          onClick={handleSave}
          className="rounded-xl bg-gradient-to-r from-[#00685f] to-[#008378] px-6 py-3 text-sm font-bold text-white shadow-lg shadow-[#00685f]/20 transition disabled:opacity-60"
        >
          {isSaving ? "Đang lưu..." : "Lưu tùy chọn email"}
        </button>
      </div>
    </div>
  );
}
