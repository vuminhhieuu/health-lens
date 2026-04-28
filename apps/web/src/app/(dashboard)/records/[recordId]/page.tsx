"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import confetti from "canvas-confetti";
import { CheckCircle2, AlertTriangle, AlertOctagon, ChevronDown, ChevronUp } from "lucide-react";

import { apiClient } from "@/lib/api/apiClient";
import { useUploadStore } from "@/stores/uploadStore";
import { ApiPaths } from "@healthlens/shared/constants";

type MetricDto = {
  name: string;
  displayNameVi?: string;
  value: string;
  unit: string;
  status?: "normal" | "attention" | "abnormal" | "no_data";
};

type RecordDetailData = {
  id: string;
  status?: "processing" | "review_required" | "ocr_failed" | "done";
  profileId?: string;
  profileName?: string | null;
  examDate?: string | null;
  recordType?: string | null;
  overallStatus?: "normal" | "attention" | "abnormal";
  summary?: string;
  keyMetrics?: MetricDto[];
  allMetrics?: MetricDto[];
};

type RecommendationsData = {
  recommendations: string[];
  disclaimer: string;
  allNormal: boolean;
};

export default function RecordDetailPage() {
  const params = useParams();
  const router = useRouter();
  const recordId = params.recordId as string;
  const [showAllResults, setShowAllResults] = useState(false);
  const justUploaded = useUploadStore((state) => state.justUploaded);
  const clearJustUploaded = useUploadStore((state) => state.clearJustUploaded);

  const { data, isLoading, isError } = useQuery<RecordDetailData>({
    queryKey: ["record-detail", recordId],
    queryFn: async () => {
      const res = await apiClient.get(ApiPaths.HEALTH_RECORDS.GET(recordId));
      return res.data?.data;
    },
    enabled: Boolean(recordId),
  });

  const { data: recommendationsData } = useQuery<RecommendationsData>({
    queryKey: ["record-recommendations", recordId],
    queryFn: async () => {
      const res = await apiClient.get(ApiPaths.HEALTH_RECORDS.RECOMMENDATIONS(recordId));
      return res.data?.data;
    },
    enabled: Boolean(recordId),
  });

  useEffect(() => {
    if (!justUploaded) return;
    confetti({ particleCount: 80, spread: 60, origin: { y: 0.6 } });
    const timer = setTimeout(() => clearJustUploaded(), 2400);
    return () => clearTimeout(timer);
  }, [justUploaded, clearJustUploaded]);

  useEffect(() => {
    if (!recordId || !data?.status) return;
    if (data.status !== "done") {
      router.replace(`/health-records/review/${recordId}`);
    }
  }, [data?.status, recordId, router]);

  const allMetrics = useMemo(() => data?.allMetrics ?? data?.keyMetrics ?? [], [data]);
  const keyMetrics = useMemo(() => data?.keyMetrics ?? [], [data]);
  const overallStatus = data?.overallStatus ?? "normal";
  const summaryText =
    data?.summary ??
    (overallStatus === "abnormal"
      ? "Có chỉ số cần chú ý. Hãy tham khảo ý kiến bác sĩ sớm."
      : overallStatus === "attention"
      ? "Có chỉ số ở ngưỡng cần theo dõi thêm."
      : "Tất cả chỉ số trong giới hạn bình thường. Tiếp tục duy trì!");

  if (isLoading) {
    return <div className="mx-auto max-w-5xl p-8 text-[#4e6360]">Đang tải kết quả...</div>;
  }
  if (isError || !data) {
    return <div className="mx-auto max-w-5xl p-8 text-[#ba1a1a]">Không thể tải chi tiết kết quả.</div>;
  }

  return (
    <div className="mx-auto min-h-screen w-full max-w-5xl space-y-5 bg-[#effcf9] px-6 py-8">
      {justUploaded ? (
        <div className="fixed right-6 top-6 z-50 rounded-xl bg-[#00685f] px-4 py-2 text-sm font-semibold text-white shadow-lg">
          Đã lưu!
        </div>
      ) : null}

      <section className="rounded-2xl border border-[#b7d8d1] bg-white p-5">
        <h1 className="text-2xl font-bold text-[#005049]">{data.recordType || "Kết quả xét nghiệm"}</h1>
        <p className="mt-1 text-sm text-[#4e6360]">
          Ngày khám: {data.examDate || "Không rõ"} • Hồ sơ: {data.profileName || "Không rõ"}
        </p>
        <div className="mt-4">
          <button
            type="button"
            onClick={() => router.push(`/health-records/review/${recordId}`)}
            className="rounded-lg border border-[#8eb7af] px-3 py-2 text-sm font-medium text-[#005049] hover:bg-[#effcf9]"
          >
            Chỉnh sửa kết quả
          </button>
        </div>
      </section>

      <section className="rounded-2xl border border-[#b7d8d1] bg-white p-5">
        <div className="flex items-center gap-2">
          {overallStatus === "abnormal" ? (
            <AlertOctagon className="h-5 w-5 text-[#ba1a1a]" />
          ) : overallStatus === "attention" ? (
            <AlertTriangle className="h-5 w-5 text-[#b26a00]" />
          ) : (
            <CheckCircle2 className="h-5 w-5 text-[#0f766e]" />
          )}
          <span className="font-semibold text-[#005049]">
            {overallStatus === "abnormal" ? "Bất thường" : overallStatus === "attention" ? "Cần theo dõi" : "Bình thường"}
          </span>
        </div>
        <p className="mt-2 text-sm text-[#4e6360]">{summaryText}</p>
      </section>

      <section className="rounded-2xl border border-[#b7d8d1] bg-white p-5">
        <h2 className="text-base font-semibold text-[#005049]">Chỉ số ưu tiên</h2>
        <div className="mt-3 space-y-2">
          {keyMetrics.length === 0 ? (
            <p className="text-sm text-[#4e6360]">Không có chỉ số bất thường nổi bật.</p>
          ) : (
            keyMetrics.map((metric, idx) => (
              <div key={`${metric.name}-${idx}`} className="rounded-xl border border-[#e4ecea] px-3 py-2">
                <p className="text-sm font-medium text-[#005049]">{metric.displayNameVi || metric.name}</p>
                <p className="text-sm text-[#4e6360]">{metric.value} {metric.unit}</p>
              </div>
            ))
          )}
        </div>
      </section>

      <section className="rounded-2xl border border-[#b7d8d1] bg-white p-5">
        <button
          type="button"
          onClick={() => setShowAllResults((prev) => !prev)}
          className="flex w-full items-center justify-between text-left"
        >
          <span className="text-base font-semibold text-[#005049]">
            {showAllResults ? "Thu gọn" : "Xem tất cả"} ({allMetrics.length})
          </span>
          {showAllResults ? <ChevronUp className="h-4 w-4 text-[#4e6360]" /> : <ChevronDown className="h-4 w-4 text-[#4e6360]" />}
        </button>
        {showAllResults ? (
          <div className="mt-3 space-y-2">
            {allMetrics.map((metric, idx) => (
              <div key={`${metric.name}-${idx}`} className="rounded-xl border border-[#e4ecea] px-3 py-2">
                <p className="text-sm font-medium text-[#005049]">{metric.displayNameVi || metric.name}</p>
                <p className="text-sm text-[#4e6360]">{metric.value} {metric.unit}</p>
              </div>
            ))}
          </div>
        ) : null}
      </section>

      {recommendationsData ? (
        <section className="rounded-2xl border border-[#b7d8d1] bg-white p-5">
          <h2 className="text-base font-semibold text-[#005049]">Khuyến nghị</h2>
          <div className="mt-3 space-y-2">
            {recommendationsData.recommendations.map((item, idx) => (
              <p key={`${idx}-${item}`} className="text-sm text-[#1d3b36]">💚 {item}</p>
            ))}
          </div>
          <p className="mt-3 whitespace-pre-line rounded-lg bg-[#f5f7f7] px-3 py-2 text-xs text-[#6d7a77]">
            {recommendationsData.disclaimer}
          </p>
        </section>
      ) : null}
    </div>
  );
}
