import Link from "next/link";
import {
  Bell,
  ChevronRight,
  Info,
  KeyRound,
  Shield,
  Trash2,
  UserRound,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";

import { DashboardPageShell } from "@/components/layout/DashboardPageShell";

type SettingsSection = {
  title: string;
  description: string;
  href: string;
  icon: LucideIcon;
  danger?: boolean;
};

const settingsSections: SettingsSection[] = [
  {
    title: "Hồ sơ",
    description: "Cập nhật họ tên, ngày sinh, giới tính và ảnh đại diện.",
    href: "/settings/profile",
    icon: UserRound,
  },
  {
    title: "Bảo mật",
    description: "Đổi mật khẩu và quản lý các thiết lập bảo vệ tài khoản.",
    href: "/settings/change-password",
    icon: KeyRound,
  },
  {
    title: "Quyền riêng tư",
    description: "Xem lựa chọn đồng ý xử lý dữ liệu và các liên kết pháp lý.",
    href: "/settings/privacy",
    icon: Shield,
  },
  {
    title: "Thông báo",
    description: "Thiết lập cách HealthLens nhắc bạn về kết quả và cập nhật.",
    href: "/settings/notifications",
    icon: Bell,
  },
  {
    title: "Giới thiệu",
    description: "Thông tin phiên bản, hỗ trợ và kênh liên hệ của HealthLens.",
    href: "/settings/about",
    icon: Info,
  },
  {
    title: "Xóa tài khoản",
    description: "Gửi yêu cầu xóa tài khoản và dữ liệu liên quan.",
    href: "/settings/delete-account",
    icon: Trash2,
    danger: true,
  },
];

export default function SettingsPage() {
  return (
    <DashboardPageShell
      title="Cài đặt"
      subtitle="Quản lý hồ sơ, bảo mật, quyền riêng tư và các lựa chọn tài khoản của bạn."
    >
      <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {settingsSections.map((section) => {
          const Icon = section.icon;
          const tone = section.danger
            ? {
                card: "border-[#f2b8b5] bg-[#fff8f7] hover:border-[#ba1a1a]/45",
                icon: "bg-[#ffdad6] text-[#ba1a1a]",
                arrow: "text-[#ba1a1a]",
              }
            : {
                card: "border-[#bcc9c6]/30 bg-white hover:border-[#00685f]/35",
                icon: "bg-[#e9f6f3] text-[#00685f]",
                arrow: "text-[#00685f]",
              };

          return (
            <Link
              key={section.href}
              href={section.href}
              className={`group flex min-h-44 w-full flex-col justify-between rounded-xl border p-5 shadow-sm transition ${tone.card} focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]`}
            >
              <div className="flex items-start gap-4">
                <span className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-xl ${tone.icon}`}>
                  <Icon className="h-6 w-6" aria-hidden="true" />
                </span>
                <div className="min-w-0">
                  <h2 className="text-lg font-bold text-[#121e1c]">{section.title}</h2>
                  <p className="mt-2 text-sm leading-6 text-[#4e6360]">{section.description}</p>
                </div>
              </div>

              <span className={`mt-5 inline-flex items-center gap-2 text-sm font-bold ${tone.arrow}`}>
                Mở mục này
                <ChevronRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" aria-hidden="true" />
              </span>
            </Link>
          );
        })}
      </section>
    </DashboardPageShell>
  );
}
