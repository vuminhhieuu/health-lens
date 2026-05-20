import { Activity, CalendarDays, ChevronRight, FileText, Stethoscope } from "lucide-react";

const previewRecords = [
  {
    title: "Phiếu xét nghiệm máu",
    dateLabel: "Ngày thực hiện",
    status: "Bình thường",
    statusClass: "bg-[#e6f6f2] text-[#00685f]",
  },
  {
    title: "Siêu âm tổng quát",
    dateLabel: "Ngày thực hiện",
    status: "Cần theo dõi",
    statusClass: "bg-[#fff9eb] text-[#92700e]",
  },
] as const;

const previewStats = [
  { icon: Activity, label: "Hồ sơ theo dõi", detail: "Ví dụ sau đăng ký" },
  { icon: FileText, label: "Tổng kết quả", detail: "Theo từng hồ sơ" },
] as const;

export function LandingDashboardPreview() {
  return (
    <figure className="overflow-hidden rounded-2xl border border-[#bcc9c6]/40 bg-[#f6fbfa] shadow-[0_24px_80px_rgba(18,30,28,0.1)]">
      <div className="border-b border-[#e1ebe8] bg-white px-5 py-4">
        <p className="text-lg font-bold text-[#121e1c]">Chào mừng quay lại!</p>
        <p className="mt-1 text-sm text-[#6d7a77]">Tổng quan tình trạng sức khỏe (minh họa)</p>
      </div>

      <div className="space-y-4 p-4 md:p-5">
        <div className="grid grid-cols-2 gap-3">
          {previewStats.map((stat) => (
            <article
              key={stat.label}
              className="rounded-xl border-l-4 border-[#00685f] bg-white p-4 shadow-sm"
            >
              <div className="mb-3 flex items-start justify-between gap-2">
                <div className="rounded-lg bg-[#e9f6f3] p-2 text-[#00685f]">
                  <stat.icon className="h-5 w-5" aria-hidden />
                </div>
                <span className="rounded-full bg-[#e6f6f2] px-2 py-0.5 text-[10px] font-bold text-[#00685f]">
                  Mẫu
                </span>
              </div>
              <p className="text-xs font-bold text-[#6d7a77]">{stat.label}</p>
              <p className="mt-2 text-2xl font-black tracking-tight text-[#bcc9c6]">—</p>
              <p className="mt-1 text-xs font-semibold text-[#6d7a77]">{stat.detail}</p>
            </article>
          ))}
        </div>

        <div>
          <div className="mb-3 flex items-center justify-between gap-2">
            <h3 className="text-base font-bold text-[#121e1c]">Kết quả gần đây</h3>
            <span className="inline-flex items-center gap-0.5 text-xs font-bold text-[#00685f]">
              Xem tất cả
              <ChevronRight className="h-3.5 w-3.5" aria-hidden />
            </span>
          </div>
          <div className="space-y-3">
            {previewRecords.map((record) => (
              <article
                key={record.title}
                className="flex items-center gap-3 rounded-2xl bg-white p-4 shadow-sm"
              >
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-[#e9f6f3] text-[#00685f]">
                  <Stethoscope className="h-6 w-6" aria-hidden />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-bold text-[#121e1c]">{record.title}</p>
                  <p className="mt-1 flex items-center gap-1.5 text-xs text-[#6d7a77]">
                    <CalendarDays className="h-3.5 w-3.5 shrink-0" aria-hidden />
                    {record.dateLabel}: —
                  </p>
                </div>
                <span
                  className={`shrink-0 rounded-full px-2.5 py-1 text-[10px] font-bold ${record.statusClass}`}
                >
                  {record.status}
                </span>
              </article>
            ))}
          </div>
        </div>
      </div>

      <figcaption className="border-t border-[#e1ebe8] bg-white px-5 py-3 text-center text-xs text-[#6d7a77]">
        Giao diện minh họa theo bố cục ứng dụng — không phải dữ liệu bệnh nhân thật
      </figcaption>
    </figure>
  );
}
