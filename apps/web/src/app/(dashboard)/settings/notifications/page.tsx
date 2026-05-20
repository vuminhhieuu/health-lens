import { Bell } from "lucide-react";

import { SettingsComingSoonPage } from "../_components/SettingsComingSoonPage";

export default function NotificationSettingsPage() {
  return (
    <SettingsComingSoonPage
      title="Thông báo"
      description="Thiết lập cách HealthLens gửi nhắc nhở và cập nhật."
      icon={Bell}
    />
  );
}
