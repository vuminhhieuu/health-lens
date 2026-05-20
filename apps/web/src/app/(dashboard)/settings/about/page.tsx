import { Info } from "lucide-react";

import { SettingsComingSoonPage } from "../_components/SettingsComingSoonPage";

export default function AboutSettingsPage() {
  return (
    <SettingsComingSoonPage
      title="Giới thiệu"
      description="Thông tin phiên bản, hỗ trợ và liên hệ của HealthLens."
      icon={Info}
    />
  );
}
