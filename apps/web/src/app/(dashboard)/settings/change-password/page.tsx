import { KeyRound } from "lucide-react";

import { SettingsComingSoonPage } from "../_components/SettingsComingSoonPage";

export default function ChangePasswordPage() {
  return (
    <SettingsComingSoonPage
      title="Bảo mật"
      description="Đổi mật khẩu và các thiết lập bảo vệ tài khoản."
      icon={KeyRound}
    />
  );
}
