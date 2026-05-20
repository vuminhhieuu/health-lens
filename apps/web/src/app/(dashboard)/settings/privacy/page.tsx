import { Shield } from "lucide-react";

import { SettingsComingSoonPage } from "../_components/SettingsComingSoonPage";

export default function PrivacySettingsPage() {
  return (
    <SettingsComingSoonPage
      title="Quyền riêng tư"
      description="Quản lý lựa chọn đồng ý xử lý dữ liệu và quyền riêng tư."
      icon={Shield}
    />
  );
}
