import { Activity, FileText, ShieldCheck } from "lucide-react";

const highlights = [
  {
    icon: FileText,
    title: "Tải và số hóa kết quả",
    description:
      "Tải phiếu xét nghiệm dạng tệp hoặc ảnh để HealthLens trích xuất dữ liệu và chuẩn hóa thành bản theo dõi dễ đọc.",
  },
  {
    icon: Activity,
    title: "Giải thích chỉ số dễ hiểu",
    description:
      "Hiển thị các chỉ số sức khỏe với ngữ cảnh tiếng Việt rõ ràng, giúp bạn chuẩn bị tốt hơn trước khi trao đổi với bác sĩ.",
  },
  {
    icon: ShieldCheck,
    title: "Quản lý hồ sơ và quyền riêng tư",
    description:
      "Theo dõi theo từng hồ sơ cá nhân/gia đình, đồng thời kiểm soát đồng thuận và quyền chia sẻ dữ liệu sức khỏe.",
  },
];

export function MarketingHome() {
  return (
    <main id="main-content" className="flex-1 text-[#121e1c]">
      <section className="mx-auto grid min-h-[calc(100vh-4rem)] w-full max-w-[88rem] grid-cols-1 items-center gap-10 px-5 py-14 md:grid-cols-[1.02fr_0.98fr] md:px-8 lg:px-10 xl:px-12">
        <div className="max-w-2xl">
          <h1 className="text-4xl font-black leading-tight tracking-normal text-[#121e1c] md:text-5xl">
            Một nơi an toàn để lưu trữ, theo dõi và hiểu kết quả khám sức khỏe của bạn.
          </h1>
          <p className="mt-5 max-w-2xl text-lg leading-8 text-[#3d4947]">
            HealthLens hỗ trợ bạn đọc kết quả xét nghiệm bằng ngôn ngữ dễ hiểu, tổ chức dữ liệu theo từng hồ sơ cá nhân
            hoặc gia đình, và chuẩn bị thông tin cần thiết trước khi trao đổi với bác sĩ.
          </p>
          <p className="mt-4 max-w-2xl text-base leading-7 text-[#4e6360]">
            Ứng dụng không thay thế chẩn đoán y khoa; HealthLens tập trung vào việc giúp bạn theo dõi dữ liệu sức khỏe
            rõ ràng hơn, chủ động hơn và riêng tư hơn trong suốt quá trình sử dụng.
          </p>
        </div>

        <div className="relative min-h-[420px] overflow-hidden rounded-lg border border-[#bcc9c6]/60 bg-white shadow-[0_24px_80px_rgba(18,30,28,0.12)]">
          <div className="border-b border-[#e1ebe8] bg-[#effcf9] px-5 py-4">
            <div className="h-3 w-28 rounded-full bg-[#00685f]" />
          </div>
          <div className="space-y-5 p-5">
            <div className="grid grid-cols-3 gap-3">
              <div className="h-24 rounded-lg bg-[#e9f6f3] p-4">
                <div className="h-3 w-12 rounded-full bg-[#00685f]" />
                <div className="mt-8 h-5 w-20 rounded-full bg-[#89f5e7]" />
              </div>
              <div className="h-24 rounded-lg bg-[#fff9eb] p-4">
                <div className="h-3 w-12 rounded-full bg-[#92700e]" />
                <div className="mt-8 h-5 w-20 rounded-full bg-[#f3e3be]" />
              </div>
              <div className="h-24 rounded-lg bg-[#f6f4ff] p-4">
                <div className="h-3 w-12 rounded-full bg-[#5f55a3]" />
                <div className="mt-8 h-5 w-20 rounded-full bg-[#d8d3ff]" />
              </div>
            </div>
            <div className="rounded-lg border border-[#e1ebe8] p-4">
              <div className="mb-4 flex items-center justify-between">
                <div className="h-4 w-36 rounded-full bg-[#3d4947]" />
                <div className="h-8 w-20 rounded-lg bg-[#00685f]" />
              </div>
              <div className="space-y-3">
                <div className="h-3 w-full rounded-full bg-[#e1ebe8]" />
                <div className="h-3 w-10/12 rounded-full bg-[#e1ebe8]" />
                <div className="h-3 w-7/12 rounded-full bg-[#e1ebe8]" />
              </div>
            </div>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              {highlights.map((item) => (
                <div key={item.title} className="rounded-lg border border-[#e1ebe8] p-4">
                  <item.icon className="h-5 w-5 text-[#00685f]" aria-hidden="true" />
                  <p className="mt-3 text-sm font-bold text-[#121e1c]">{item.title}</p>
                  <p className="mt-2 text-xs leading-5 text-[#3d4947]">{item.description}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
