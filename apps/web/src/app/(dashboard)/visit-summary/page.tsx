"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  AlertTriangle,
  CalendarDays,
  CheckSquare,
  ClipboardList,
  FileText,
  Printer,
  Stethoscope,
  Upload,
  Users,
} from "lucide-react";

import { ApiPaths } from "@healthlens/shared/constants";

import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { breadcrumbFromHome } from "@/lib/layout/dashboardBreadcrumbTrails";
import { ErrorState, LoadingState } from "@/components/ui";
import { apiClient } from "@/lib/api/apiClient";
import { mapProfilesResponse } from "@/lib/profileMappings";

type Profile = {
  id: string;
  displayName: string;
  isDefault: boolean;
  currentMedications?: string;
  allergies?: string;
};

type HealthRecord = {
  id: string;
  status?: string | null;
  testType?: string | null;
  examDate?: string | null;
  overallStatus?: "normal" | "attention" | "abnormal" | string | null;
  abnormalCount?: number | null;
};

const doctorQuestions = [
  "Kết quả này có điểm nào tôi nên theo dõi thêm trong sinh hoạt hằng ngày không?",
  "Tôi nên mang thêm thông tin hoặc kết quả cũ nào để bác sĩ đối chiếu không?",
  "Các chỉ số cần chú ý nên được kiểm tra lại sau bao lâu?",
  "Có yếu tố cá nhân nào như tuổi, thuốc đang dùng hoặc bệnh nền có thể ảnh hưởng kết quả không?",
  "Tôi nên làm gì để chuẩn bị tốt hơn cho lần tái khám hoặc xét nghiệm tiếp theo?",
];

const patientPrepFields = [
  { label: "Mục tiêu buổi khám" },
  { label: "Triệu chứng hoặc điều làm tôi lo nhất" },
  { label: "Thuốc/thực phẩm chức năng đang dùng", profileField: "currentMedications" as const },
  { label: "Dị ứng hoặc phản ứng thuốc đã biết", profileField: "allergies" as const },
];

function profileFieldHint(
  profile: Profile | undefined,
  field: (typeof patientPrepFields)[number],
): string | null {
  if (!profile || !("profileField" in field) || !field.profileField) {
    return null;
  }
  const value = profile[field.profileField];
  return value?.trim() ? value.trim() : null;
}

export default function VisitSummaryPage() {
  return (
    <Suspense fallback={<VisitSummaryFallback />}>
      <VisitSummaryPageContent />
    </Suspense>
  );
}

function VisitSummaryFallback() {
  return (
    <DashboardPageShell
      title="Tóm tắt đi khám"
      subtitle="Chuẩn bị thông tin chính để trao đổi với bác sĩ khi bạn đi khám trực tiếp."
      breadcrumbs={breadcrumbFromHome("Tóm tắt đi khám")}
    >
      <LoadingState
        title="Đang tải tóm tắt đi khám"
        className="min-h-64 rounded-xl border-[#bcc9c6]/30 bg-white shadow-sm"
      />
    </DashboardPageShell>
  );
}

function VisitSummaryPageContent() {
  const searchParams = useSearchParams();
  const [manualProfileId, setManualProfileId] = useState("");

  const {
    data: profiles = [],
    isLoading: isProfilesLoading,
    isError: isProfilesError,
  } = useQuery({
    queryKey: ["visit-summary-profiles"],
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.PROFILES.BASE);
      return mapProfilesResponse(response.data?.data);
    },
  });

  const profileFromQuery = searchParams.get("profileId")?.trim() ?? "";
  const selectedProfileId = useMemo(() => {
    const manualProfile = profiles.find((profile) => profile.id === manualProfileId);
    if (manualProfile) return manualProfile.id;

    const queryProfile = profiles.find((profile) => profile.id === profileFromQuery);
    if (queryProfile) return queryProfile.id;

    return profiles.find((profile) => profile.isDefault)?.id ?? profiles[0]?.id ?? "";
  }, [manualProfileId, profileFromQuery, profiles]);

  const selectedProfile = useMemo(
    () => profiles.find((profile) => profile.id === selectedProfileId),
    [profiles, selectedProfileId],
  );

  const {
    data: records = [],
    isLoading: isRecordsLoading,
    isError: isRecordsError,
  } = useQuery({
    queryKey: ["visit-summary-records", selectedProfileId],
    enabled: Boolean(selectedProfileId),
    queryFn: async () => {
      const response = await apiClient.get(
        ApiPaths.PROFILES.HEALTH_RECORDS(selectedProfileId),
        {
          params: { page: 0, limit: 1 },
        },
      );
      return (response.data?.data ?? []) as HealthRecord[];
    },
  });

  const latestRecord = records[0];
  const uploadHref = selectedProfileId
    ? `/health-records?profileId=${selectedProfileId}&openUpload=1`
    : "/health-records?openUpload=1";

  function handlePrint() {
    window.print();
  }

  return (
    <DashboardPageShell
      title="Tóm tắt đi khám"
      subtitle="Chuẩn bị thông tin chính để trao đổi với bác sĩ khi bạn đi khám trực tiếp."
      breadcrumbs={breadcrumbFromHome("Tóm tắt đi khám")}
      actions={
        <button
          type="button"
          onClick={handlePrint}
          className="inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-full bg-[#00685f] px-5 text-sm font-bold text-white transition hover:bg-[#008378] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005049] lg:w-auto"
        >
          <Printer className="h-4 w-4" aria-hidden="true" />
          In / Lưu PDF
        </button>
      }
    >
      <PrintStyles />
      {isProfilesLoading ? (
        <LoadingState
          title="Đang tải hồ sơ sức khỏe"
          className="min-h-64 rounded-xl border-[#bcc9c6]/30 bg-white shadow-sm"
        />
      ) : null}

      {!isProfilesLoading && isProfilesError ? (
        <ErrorPanel message="Không thể tải danh sách hồ sơ. Vui lòng thử lại sau." />
      ) : null}

      {!isProfilesLoading && !isProfilesError && profiles.length === 0 ? (
        <section className="rounded-xl border border-[#bcc9c6]/30 bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
            <IconBox>
              <Users className="h-6 w-6" aria-hidden="true" />
            </IconBox>
            <div>
              <h2 className="text-lg font-bold text-[#121e1c]">
                Bạn chưa có hồ sơ sức khỏe
              </h2>
              <p className="mt-2 text-sm leading-6 text-[#4e6360]">
                Tạo hồ sơ trước để HealthLens có thể tổng hợp thông tin theo từng người.
              </p>
              <Link
                href="/profiles"
                className="mt-4 inline-flex min-h-12 items-center rounded-full bg-[#00685f] px-5 text-sm font-bold text-white transition hover:bg-[#008378] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005049]"
              >
                Quản lý hồ sơ
              </Link>
            </div>
          </div>
        </section>
      ) : null}

      {!isProfilesLoading && !isProfilesError && profiles.length > 0 ? (
        <div className="grid grid-cols-1 gap-6 xl:grid-cols-12">
          <section className="space-y-6 xl:col-span-8" aria-label="Bản tóm tắt">
            <section className="visit-summary-screen-only rounded-xl border border-[#bcc9c6]/30 bg-white p-5 shadow-sm">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
                <IconBox>
                  <Printer className="h-6 w-6" aria-hidden="true" />
                </IconBox>
                <div>
                  <h2 className="text-lg font-bold text-[#121e1c]">
                    Bản PDF dành cho buổi khám
                  </h2>
                  <p className="mt-2 text-sm leading-6 text-[#4e6360]">
                    Khi bấm In / Lưu PDF, HealthLens sẽ tạo bố cục gọn để bác sĩ đọc nhanh:
                    thông tin hồ sơ, kết quả gần nhất, câu hỏi nên trao đổi và các ô trống để
                    bạn tự ghi thuốc, dị ứng hoặc điều cần hỏi.
                  </p>
                </div>
              </div>
            </section>

            <div className="visit-summary-screen-only rounded-xl border border-[#bcc9c6]/30 bg-white p-5 shadow-sm">
              <label
                htmlFor="visit-summary-profile"
                className="text-sm font-bold text-[#121e1c]"
              >
                Hồ sơ
              </label>
              <select
                id="visit-summary-profile"
                value={selectedProfileId}
                onChange={(event) => setManualProfileId(event.target.value)}
                className="mt-2 h-12 w-full rounded-xl border border-[#b7e8e0] bg-[#f0faf8] px-4 text-sm font-semibold text-[#3d4947] outline-none transition focus:border-[#008378] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
              >
                {profiles.map((profile) => (
                  <option key={profile.id} value={profile.id}>
                    {profile.displayName}
                  </option>
                ))}
              </select>
            </div>

            <article className="visit-summary-screen-only rounded-xl border border-[#bcc9c6]/30 bg-white p-6 shadow-sm">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <p className="text-sm font-semibold text-[#00685f]">
                    Bản tóm tắt cho {selectedProfile?.displayName ?? "hồ sơ đã chọn"}
                  </p>
                  <h2 className="mt-2 text-2xl font-bold text-[#121e1c]">
                    Kết quả gần nhất
                  </h2>
                </div>
                <IconBox>
                  <ClipboardList className="h-6 w-6" aria-hidden="true" />
                </IconBox>
              </div>

              {isRecordsLoading ? (
                <LoadingState
                  title="Đang tải kết quả khám gần nhất"
                  className="mt-6 min-h-36 rounded-xl border-none bg-[#f6fbfa] shadow-none"
                />
              ) : null}

              {!isRecordsLoading && isRecordsError ? (
                <div className="mt-6">
                  <ErrorPanel message="Không thể tải kết quả khám gần nhất. Vui lòng thử lại sau." />
                </div>
              ) : null}

              {!isRecordsLoading && !isRecordsError && !latestRecord ? (
                <div className="mt-6 rounded-xl border border-dashed border-[#bcc9c6] bg-[#f6fbfa] p-5">
                  <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
                    <IconBox>
                      <Upload className="h-6 w-6" aria-hidden="true" />
                    </IconBox>
                    <div>
                      <h3 className="text-lg font-bold text-[#121e1c]">
                        Chưa có kết quả nào để tóm tắt cho hồ sơ này.
                      </h3>
                      <p className="mt-2 text-sm leading-6 text-[#4e6360]">
                        Tải phiếu khám hoặc xét nghiệm trước để HealthLens tạo bản tóm tắt từ dữ liệu thật.
                      </p>
                      <Link
                        href={uploadHref}
                        className="mt-4 inline-flex min-h-12 items-center rounded-full bg-[#00685f] px-5 text-sm font-bold text-white transition hover:bg-[#008378] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005049]"
                      >
                        Tải kết quả khám
                      </Link>
                    </div>
                  </div>
                </div>
              ) : null}

              {!isRecordsLoading && !isRecordsError && latestRecord ? (
                <dl className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <SummaryItem
                    icon={<FileText className="h-5 w-5" aria-hidden="true" />}
                    label="Loại kết quả"
                    value={latestRecord.testType || "Phiếu khám bệnh"}
                  />
                  <SummaryItem
                    icon={<CalendarDays className="h-5 w-5" aria-hidden="true" />}
                    label="Ngày thực hiện"
                    value={formatExamDate(latestRecord.examDate)}
                  />
                  <SummaryItem
                    icon={<Stethoscope className="h-5 w-5" aria-hidden="true" />}
                    label="Trạng thái tổng quan"
                    value={recordStatusLabel(resolveRecordStatus(latestRecord))}
                    valueClassName={recordStatusClass(resolveRecordStatus(latestRecord))}
                  />
                  <SummaryItem
                    icon={<AlertTriangle className="h-5 w-5" aria-hidden="true" />}
                    label="Chỉ số bất thường"
                    value={`${latestRecord.abnormalCount ?? 0} chỉ số`}
                  />
                </dl>
              ) : null}
            </article>

            <section className="visit-summary-print-page rounded-xl border border-[#bcc9c6]/30 bg-white p-6 shadow-sm">
              <div className="flex flex-col gap-2 border-b border-[#d8e4e1] pb-4">
                <p className="text-xs font-bold uppercase text-[#00685f]">
                  HealthLens
                </p>
                <h2 className="text-2xl font-bold text-[#121e1c]">
                  Tóm tắt chuẩn bị đi khám
                </h2>
                <p className="text-sm text-[#4e6360]">
                  Hồ sơ: {selectedProfile?.displayName ?? "hồ sơ đã chọn"}
                </p>
              </div>

              <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2">
                {patientPrepFields.map((field) => {
                  const hint = profileFieldHint(selectedProfile, field);
                  return (
                  <div key={field.label} className="rounded-xl border border-[#d8e4e1] p-4">
                    <h3 className="text-sm font-bold text-[#121e1c]">{field.label}</h3>
                    <div
                      className="mt-4 h-16 rounded-lg border border-dashed border-[#9fb3ae]"
                      aria-hidden="true"
                    />
                    {hint ? (
                      <p className="mt-2 text-xs text-[#6d7a77]">
                        Gợi ý từ hồ sơ (tham khảo): {hint}
                      </p>
                    ) : (
                      <p className="mt-2 text-xs text-[#6d7a77]">
                        Người dùng tự điền trước hoặc trong buổi khám.
                      </p>
                    )}
                  </div>
                  );
                })}
              </div>

              <div className="mt-5 rounded-xl border border-[#d8e4e1] p-4">
                <h3 className="text-sm font-bold text-[#121e1c]">
                  Kết quả gần nhất để đối chiếu
                </h3>
                {latestRecord ? (
                  <dl className="mt-3 grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
                    <PrintSummaryRow
                      label="Loại kết quả"
                      value={latestRecord.testType || "Phiếu khám bệnh"}
                    />
                    <PrintSummaryRow
                      label="Ngày thực hiện"
                      value={formatExamDate(latestRecord.examDate)}
                    />
                    <PrintSummaryRow
                      label="Trạng thái tổng quan"
                      value={recordStatusLabel(resolveRecordStatus(latestRecord))}
                    />
                    <PrintSummaryRow
                      label="Chỉ số bất thường"
                      value={`${latestRecord.abnormalCount ?? 0} chỉ số`}
                    />
                  </dl>
                ) : (
                  <p className="mt-3 text-sm text-[#4e6360]">
                    Chưa có kết quả nào để tóm tắt cho hồ sơ này.
                  </p>
                )}
              </div>

              <div className="mt-5 rounded-xl border border-[#d8e4e1] p-4">
                <h3 className="text-sm font-bold text-[#121e1c]">
                  Câu hỏi nên trao đổi với bác sĩ
                </h3>
                <ul className="mt-3 space-y-2 text-sm text-[#3d4947]">
                  {doctorQuestions.map((question) => (
                    <li key={question} className="flex gap-2">
                      <CheckSquare className="mt-0.5 h-4 w-4 shrink-0 text-[#00685f]" aria-hidden="true" />
                      <span>{question}</span>
                    </li>
                  ))}
                </ul>
              </div>

              <div className="mt-5 rounded-xl border border-[#f2b8b5] bg-[#fff8f7] p-4">
                <h3 className="text-sm font-bold text-[#121e1c]">
                  Lưu ý y tế quan trọng
                </h3>
                <p className="mt-2 text-sm leading-6 text-[#4e6360]">
                  HealthLens giúp bạn chuẩn bị thông tin tham khảo. Nội dung này không
                  thay thế tư vấn, chẩn đoán hoặc điều trị từ bác sĩ. Vui lòng đối chiếu
                  với phiếu gốc và trao đổi trực tiếp với nhân viên y tế.
                </p>
              </div>
            </section>
          </section>

          <aside className="space-y-6 xl:col-span-4">
            <section className="rounded-xl border border-[#bcc9c6]/30 bg-white p-6 shadow-sm">
              <h2 className="text-xl font-bold text-[#121e1c]">
                Câu hỏi nên trao đổi với bác sĩ
              </h2>
              <ul className="mt-4 space-y-3">
                {doctorQuestions.map((question) => (
                  <li key={question} className="rounded-xl bg-[#f0faf8] p-4 text-sm leading-6 text-[#3d4947]">
                    {question}
                  </li>
                ))}
              </ul>
            </section>

            <section className="rounded-xl border border-[#f2b8b5] bg-[#fff8f7] p-5">
              <div className="flex items-start gap-4">
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-[#ffdad6] text-[#ba1a1a]">
                  <AlertTriangle className="h-6 w-6" aria-hidden="true" />
                </div>
                <div>
                  <h2 className="text-lg font-bold text-[#121e1c]">
                    Lưu ý y tế quan trọng
                  </h2>
                  <p className="mt-2 text-sm leading-6 text-[#4e6360]">
                    HealthLens giúp bạn chuẩn bị thông tin tham khảo. Nội dung này
                    không thay thế tư vấn, chẩn đoán hoặc điều trị từ bác sĩ.
                  </p>
                </div>
              </div>
            </section>
          </aside>
        </div>
      ) : null}
    </DashboardPageShell>
  );
}

function IconBox({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-[#e9f6f3] text-[#00685f]">
      {children}
    </div>
  );
}

function ErrorPanel({ message }: { message: string }) {
  return (
    <ErrorState
      title="Không thể tải dữ liệu"
      description={message}
      className="min-h-24 rounded-xl border-[#f2b8b5] bg-[#fff8f7] shadow-sm"
    />
  );
}

function PrintSummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="font-semibold text-[#6d7a77]">{label}</dt>
      <dd className="mt-1 font-bold text-[#121e1c]">{value}</dd>
    </div>
  );
}

function SummaryItem({
  icon,
  label,
  value,
  valueClassName,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  valueClassName?: string;
}) {
  return (
    <div className="rounded-xl border border-[#bcc9c6]/30 bg-[#f6fbfa] p-4">
      <dt className="flex items-center gap-2 text-xs font-bold uppercase text-[#6d7a77]">
        <span className="text-[#00685f]">{icon}</span>
        {label}
      </dt>
      <dd className={`mt-3 text-lg font-bold text-[#121e1c] ${valueClassName ?? ""}`}>
        {value}
      </dd>
    </div>
  );
}

function PrintStyles() {
  return (
    <style>{`
      .visit-summary-print-page {
        display: none;
      }

      @media print {
        @page {
          size: A4;
          margin: 14mm;
        }

        body {
          background: #ffffff !important;
          color: #121e1c !important;
        }

        nav,
        aside,
        .visit-summary-screen-only {
          display: none !important;
        }

        main,
        main > div {
          background: #ffffff !important;
          padding: 0 !important;
          max-width: none !important;
        }

        main header {
          display: none !important;
        }

        .visit-summary-print-page {
          display: block !important;
          border: 0 !important;
          box-shadow: none !important;
          padding: 0 !important;
        }

        .visit-summary-print-page * {
          break-inside: avoid;
          box-shadow: none !important;
        }
      }
    `}</style>
  );
}

function formatExamDate(value?: string | null) {
  if (!value) return "Chưa có ngày khám";
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return "Chưa có ngày khám";

  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
}

function recordStatusClass(status: string) {
  if (status === "abnormal") return "text-[#ba1a1a]";
  if (status === "attention") return "text-[#773215]";
  if (status === "normal") return "text-[#00685f]";
  if (status === "error") return "text-[#be123c]";
  return "text-[#64748b]";
}

function recordStatusLabel(status: string) {
  if (status === "abnormal") return "Bất thường";
  if (status === "attention") return "Cần chú ý";
  if (status === "normal") return "Bình thường";
  if (status === "error") return "Lỗi";
  return "Chưa xác thực";
}

function resolveRecordStatus(record: HealthRecord): string {
  const recordStatus = record.status?.toLowerCase();
  if (recordStatus === "done") {
    if (record.overallStatus === "abnormal" || record.overallStatus === "attention") {
      return record.overallStatus;
    }
    return "normal";
  }

  if (
    recordStatus === "review_required" ||
    recordStatus === "processing" ||
    recordStatus === "pending"
  ) {
    return "unverified";
  }

  if (
    recordStatus === "ocr_failed" ||
    recordStatus === "failed" ||
    recordStatus === "error"
  ) {
    return "error";
  }

  return record.overallStatus === "abnormal" || record.overallStatus === "attention"
    ? record.overallStatus
    : "unverified";
}
