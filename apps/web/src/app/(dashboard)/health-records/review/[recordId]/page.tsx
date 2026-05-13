"use client";

import { type ChangeEvent, type ReactNode, useEffect, useMemo, useState, useRef } from "react";
import Link from "next/link";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import {
  Loader2,
  AlertTriangle,
  CheckCircle,
  Edit2,
  Save,
  X,
  AlertCircle,
  FileText,
  Maximize2,
  AlertOctagon,
  Plus,
  ScanLine,
  PenLine,
  Trash2,
  FileDown,
  Share2,
  Building2,
  Info,
  Heart,
  Apple,
  Sparkles,
  ShieldAlert,
  ChevronDown,
  ChevronUp,
} from "lucide-react";
import { z } from "zod";

import { apiClient } from "@/lib/api/apiClient";
import { ALLOWED_FILE_TYPES, ApiPaths, UPLOAD_MAX_SIZE_BYTES } from "@healthlens/shared/constants";
import { HealthMetricCard } from "@/components/ui/HealthMetricCard";
import { OcrFailureScreen } from "@/components/features/upload/OcrFailureScreen";
import { DeleteRecordModal } from "@/components/features/health-records/DeleteRecordModal";
import { toThreeLineExplanation } from "@/lib/utils/explanationFormatter";

type MetricDto = {
  name: string;
  value: string;
  unit: string;
  rawName?: string;
  rawValue?: string;
  rawUnit?: string;
  normalizedName?: string;
  normalizedValue?: string;
  normalizedUnit?: string;
  confidence: number | null;
  confidenceLevel: "high" | "medium" | "low";
  source: string;
  displayNameVi?: string;
  status?: "normal" | "attention" | "abnormal" | "no_data";
  statusSource?: "document" | "system" | "none";
  interpretation?: "high" | "low" | "normal" | "critical" | "unknown";
  interpretationSource?: "document" | "computed" | "system";
  critical?: boolean;
  referenceRange?: {
    min: number;
    max: number;
    attentionMin: number;
    attentionMax: number;
    unit?: string;
  } | null;
  referenceRangeSource?: "document" | "system" | "none";
  rangeContext?: {
    gender?: "male" | "female" | null;
    ageRange?: string | null;
  } | null;
  explanation?: string;
};

type ConfirmMetricPayload = {
  name: string;
  value: string;
  unit: string;
  source: string;
  confidence: number | null;
  confidenceLevel: "high" | "medium" | "low";
};

type ReferenceMetricOption = {
  name: string;
  displayNameVi: string;
  unit: string;
};

type Profile = {
  id: string;
  displayName: string;
};

type ReviewRecordStatus = "processing" | "review_required" | "done" | "ocr_failed";

type ReviewRecordData = {
  profileId?: string;
  isOwner?: boolean;
  canEdit?: boolean;
  status: ReviewRecordStatus;
  fileUrl?: string;
  metrics?: MetricDto[];
  hasLowConfidenceMetrics?: boolean;
  ocrFailureReason?: string | null;
  examDate?: string | null;
  recordType?: string | null;
  hospitalName?: string | null;
  diagnosis?: string | null;
  analyzerModel?: string | null;
  testMethod?: string | null;
  labSite?: string | null;
};

type RecommendationsData = {
  recommendations: string[];
  disclaimer: string;
  allNormal: boolean;
};

type RecommendationCategory = "nutrition" | "lifestyle";

type RecommendationGroup = {
  category: RecommendationCategory;
  title: string;
  items: string[];
};

const metricSchema = z.object({
  name: z.string().min(1, "Tên chỉ số không được để trống"),
  value: z
    .string()
    .min(1, "Giá trị không được để trống")
    .regex(/^-?\d+(?:[.,]\d+)?$/, "Giá trị phải là số hợp lệ"),
  unit: z.string().min(1, "Đơn vị không được để trống"),
  source: z.enum(["ocr", "manual"]),
});

export default function ReviewRecordPage() {
  const params = useParams();
  const router = useRouter();
  const searchParams = useSearchParams();
  const recordId = params.recordId as string;
  const manualMode = searchParams.get("mode") === "manual";

  const [metrics, setMetrics] = useState<MetricDto[]>([]);
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [editForm, setEditForm] = useState<MetricDto | null>(null);
  const [examDate, setExamDate] = useState<string>("");
  const [recordType, setRecordType] = useState<string>("");
  const [hospitalName, setHospitalName] = useState<string>("");
  const [diagnosis, setDiagnosis] = useState<string>("");
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [showFullDoc, setShowFullDoc] = useState(false);
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [selectedMetricIndex, setSelectedMetricIndex] = useState<number | null>(null);
  const [isKeepingPartial, setIsKeepingPartial] = useState(false);
  const [isRetryUploading, setIsRetryUploading] = useState(false);
  const [retryUploadError, setRetryUploadError] = useState<string | null>(null);
  const [isDeletingRecord, setIsDeletingRecord] = useState(false);
  const [deleteRecordError, setDeleteRecordError] = useState<string | null>(null);
  const [showDeleteRecordModal, setShowDeleteRecordModal] = useState(false);
  const retryFileInputRef = useRef<HTMLInputElement | null>(null);

  const [showAddDialog, setShowAddDialog] = useState(false);
  const [addForm, setAddForm] = useState({ name: "", value: "", unit: "" });
  const [addError, setAddError] = useState<string | null>(null);

  const { data, refetch, isLoading, isError } = useQuery<ReviewRecordData>({
    queryKey: ["record-status", recordId],
    queryFn: async () => {
      const res = await apiClient.get(ApiPaths.HEALTH_RECORDS.GET(recordId));
      return res.data?.data;
    },
    refetchInterval: (query) => {
      if (query.state.data?.status === "processing") return 3000;
      return false;
    },
  });

  const { data: referenceMetrics = [] } = useQuery<ReferenceMetricOption[]>({
    queryKey: ["reference-metrics"],
    queryFn: async () => {
      const res = await apiClient.get(ApiPaths.REFERENCE_DATA.METRICS);
      return res.data ?? [];
    },
    enabled: showAddDialog,
    staleTime: 5 * 60 * 1000,
  });

  const { data: profiles = [] } = useQuery<Profile[]>({
    queryKey: ["profiles-for-record-owner-label"],
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.PROFILES.BASE);
      return (response.data?.data ?? []) as Profile[];
    },
    enabled: data?.status === "done" && !!data?.profileId,
    staleTime: 5 * 60 * 1000,
  });

  const { data: recommendationsData } = useQuery<RecommendationsData>({
    queryKey: ["record-recommendations", recordId, data?.status],
    queryFn: async () => {
      const res = await apiClient.get(ApiPaths.HEALTH_RECORDS.RECOMMENDATIONS(recordId));
      return res.data?.data;
    },
    enabled: Boolean(recordId && data?.status === "done"),
    staleTime: 5 * 60 * 1000,
  });
  const recommendationGroups = useMemo(
    () => groupRecommendations(recommendationsData?.recommendations ?? []),
    [recommendationsData?.recommendations]
  );

  const selectedMetric = selectedMetricIndex !== null ? metrics[selectedMetricIndex] : null;
  const selectedMetricStaticExplanation = selectedMetric?.explanation?.trim() || "";
  const { data: popupExplanationData, isLoading: isExplanationLoading } = useQuery({
    queryKey: ["metric-explanation", recordId, selectedMetric?.name, selectedMetric?.value, selectedMetric?.status],
    queryFn: async () => {
      if (!recordId || !selectedMetric) return { explanation: "", source: "fallback" };
      const res = await apiClient.get(ApiPaths.HEALTH_RECORDS.EXPLANATION(recordId, selectedMetric.name));
      const payload = res.data?.data;
      return {
        explanation: (payload?.explanation as string | undefined) ?? "",
        source: (payload?.source as string | undefined) ?? "fallback",
      };
    },
    enabled: selectedMetricIndex !== null && !selectedMetricStaticExplanation && Boolean(recordId),
    staleTime: 7 * 24 * 60 * 60 * 1000,
  });

  const initialized = useRef(false);
  const initialSnapshotRef = useRef<string>("");
  const skipUnloadWarningRef = useRef(false);

  const buildSnapshot = (payload: {
    metrics: MetricDto[];
    examDate: string;
    recordType: string;
    hospitalName: string;
    diagnosis: string;
  }) =>
    JSON.stringify({
      metrics: payload.metrics.map((metric) => ({
        name: metric.name,
        value: metric.value,
        unit: metric.unit,
        source: metric.source,
      })),
      examDate: payload.examDate,
      recordType: payload.recordType,
      hospitalName: payload.hospitalName,
      diagnosis: payload.diagnosis,
    });

  useEffect(() => {
    if (data && data.status !== "processing" && !initialized.current) {
      if (data.metrics) setMetrics(data.metrics);
      if (data.examDate) setExamDate(data.examDate);
      if (data.recordType) setRecordType(data.recordType);
      if (data.hospitalName) setHospitalName(data.hospitalName);
      if (data.diagnosis) setDiagnosis(data.diagnosis);
      initialSnapshotRef.current = buildSnapshot({
        metrics: data.metrics ?? [],
        examDate: data.examDate ?? "",
        recordType: data.recordType ?? "",
        hospitalName: data.hospitalName ?? "",
        diagnosis: data.diagnosis ?? "",
      });
      initialized.current = true;
      setEditMode(
        (data.status === "review_required" || (data.status === "ocr_failed" && manualMode)) &&
          (data.canEdit ?? data.isOwner ?? true)
      );
    }
  }, [data, manualMode]);

  const isDirty = useMemo(() => {
    if (!initialized.current) {
      return false;
    }

    return (
      buildSnapshot({
        metrics,
        examDate,
        recordType,
        hospitalName,
        diagnosis,
      }) !== initialSnapshotRef.current
    );
  }, [metrics, examDate, recordType, hospitalName, diagnosis]);

  useEffect(() => {
    if (!isDirty) {
      return;
    }

    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (skipUnloadWarningRef.current) {
        return;
      }
      e.preventDefault();
      e.returnValue = "";
    };
    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [isDirty]);

  const handleEditClick = (index: number) => {
    setEditingIndex(index);
    setEditForm({ ...metrics[index] });
  };

  const handleSaveEdit = () => {
    if (editingIndex === null || !editForm) return;

    const validation = metricSchema.safeParse({
      name: editForm.name,
      value: editForm.value,
      unit: editForm.unit,
      source: "manual",
    });

    if (!validation.success) {
      setSaveError(validation.error.issues[0]?.message ?? "Dữ liệu chỉ số không hợp lệ");
      return;
    }

    setSaveError(null);
    const updatedMetrics: MetricDto[] = [...metrics];
    updatedMetrics[editingIndex] = {
      ...editForm,
      source: "manual",
      confidenceLevel: "high" as const,
      confidence: 1.0,
    };
    setMetrics(updatedMetrics);
    setEditingIndex(null);
    setEditForm(null);
  };

  const handleCancelEdit = () => {
    setEditingIndex(null);
    setEditForm(null);
  };

  const handleDeleteMetric = (index: number) => {
    setMetrics((prev) => prev.filter((_, i) => i !== index));
    if (editingIndex === index) {
      setEditingIndex(null);
      setEditForm(null);
    }
  };

  const handleAddMetric = () => {
    const validation = metricSchema.safeParse({
      name: addForm.name,
      value: addForm.value,
      unit: addForm.unit,
      source: "manual",
    });

    if (!validation.success) {
      setAddError(validation.error.issues[0]?.message ?? "Dữ liệu không hợp lệ");
      return;
    }

    setAddError(null);
    const newMetric: MetricDto = {
      name: addForm.name,
      value: addForm.value,
      unit: addForm.unit,
      confidence: 1.0,
      confidenceLevel: "high",
      source: "manual",
    };
    setMetrics((prev) => [...prev, newMetric]);
    setShowAddDialog(false);
    setAddForm({ name: "", value: "", unit: "" });
  };

  const handleAddNameChange = (name: string) => {
    const ref = referenceMetrics.find((m) => m.name === name);
    setAddForm((prev) => ({
      ...prev,
      name,
      unit: ref?.unit ?? prev.unit,
    }));
  };

  const executeSave = async (keepPartial = false) => {
    try {
      setIsSaving(true);
      setSaveError(null);
      setShowConfirmModal(false);

      const resolvedKeepPartial = keepPartial || (data?.status === "ocr_failed" && manualMode);
      const finalMetrics: MetricDto[] = metrics;

      const payloadMetrics: ConfirmMetricPayload[] = finalMetrics.map((m) => ({
        name: m.name ?? "",
        value: m.value ?? "",
        unit: m.unit ?? "",
        source: m.source ?? "manual",
        confidence: typeof m.confidence === "number" ? m.confidence : null,
        confidenceLevel: m.confidenceLevel ?? "high",
      }));

      await apiClient.post(ApiPaths.HEALTH_RECORDS.CONFIRM_RECORD(recordId), {
        examDate: examDate || null,
        recordType: recordType || null,
        hospitalName: hospitalName || null,
        diagnosis: diagnosis || null,
        keepPartial: resolvedKeepPartial,
        metrics: payloadMetrics,
      });
      setEditMode(false);
      initialSnapshotRef.current = buildSnapshot({
        metrics: finalMetrics,
        examDate,
        recordType,
        hospitalName,
        diagnosis,
      });
      await refetch();
      alert("Lưu kết quả khám thành công!");
      return true;
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { detail?: string; title?: string }; status?: number } };
      const serverMsg = axiosErr?.response?.data?.detail ?? axiosErr?.response?.data?.title;
      const statusCode = axiosErr?.response?.status;
      if (serverMsg) {
        setSaveError(`Lỗi ${statusCode ?? ""}: ${serverMsg}`);
      } else {
        setSaveError("Đã có lỗi xảy ra khi lưu. Vui lòng thử lại.");
      }
      return false;
    } finally {
      setIsSaving(false);
    }
  };

  const handleKeepPartial = async () => {
    try {
      setIsKeepingPartial(true);
      const isSaved = await executeSave(true);
      if (isSaved) {
        router.push("/health-records");
      }
    } finally {
      setIsKeepingPartial(false);
    }
  };

  const validateRetryFile = (file: File) => {
    if (!ALLOWED_FILE_TYPES.includes(file.type as (typeof ALLOWED_FILE_TYPES)[number])) {
      return "Chỉ chấp nhận file PDF/JPG/PNG.";
    }
    if (file.size > UPLOAD_MAX_SIZE_BYTES) {
      return "File vượt quá giới hạn 20MB.";
    }
    return null;
  };

  const handleRetryUpload = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setRetryUploadError(null);
    const validationError = validateRetryFile(file);
    if (validationError) {
      setRetryUploadError(validationError);
      event.target.value = "";
      return;
    }

    if (!data?.profileId) {
      setRetryUploadError("Không xác định được hồ sơ người dùng để tải tệp mới.");
      event.target.value = "";
      return;
    }

    try {
      setIsRetryUploading(true);
      const fileType = file.type === "application/pdf" ? "pdf" : file.type;
      const uploadInfoResp = await apiClient.post(ApiPaths.HEALTH_RECORDS.UPLOAD_URL, {
        profileId: data.profileId,
        fileType,
        retryRecordId: recordId,
      });
      const uploadInfo = uploadInfoResp.data?.data as { uploadUrl: string; recordId: string };

      await axios.put(uploadInfo.uploadUrl, file, {
        headers: { "Content-Type": file.type },
      });

      await apiClient.post(ApiPaths.HEALTH_RECORDS.CONFIRM_UPLOAD(uploadInfo.recordId));
      initialized.current = false;
      await refetch();
      router.replace(`/health-records/review/${uploadInfo.recordId}`);
    } catch {
      setRetryUploadError("Tải tệp mới thất bại. Vui lòng thử lại.");
    } finally {
      setIsRetryUploading(false);
      event.target.value = "";
    }
  };

  const handleDeleteRecord = async () => {
    try {
      setIsDeletingRecord(true);
      setDeleteRecordError(null);
      setShowDeleteRecordModal(false);
      await apiClient.delete(ApiPaths.HEALTH_RECORDS.DELETE(recordId));
      const redirectPath = data?.profileId ? `/profiles/${data.profileId}/history` : "/health-records";
      router.push(redirectPath);
    } catch {
      setDeleteRecordError("Xóa kết quả thất bại. Vui lòng thử lại.");
    } finally {
      setIsDeletingRecord(false);
    }
  };

  const historyHref = data?.profileId ? `/profiles/${data.profileId}/history` : null;
  const deleteRecordModal = (
    <DeleteRecordModal
      open={showDeleteRecordModal}
      onCancel={() => setShowDeleteRecordModal(false)}
      onConfirm={() => void handleDeleteRecord()}
      isPending={isDeletingRecord}
    />
  );

  const renderReviewStateShell = (currentLabel: string, content: ReactNode) => (
    <main className="min-h-screen w-full bg-[#effcf9]">
      <div className="sticky top-16 z-30 border-b border-[#bcc9c6]/25 bg-[#effcf9]/95 px-6 py-3 backdrop-blur">
        <nav className="text-sm font-medium text-[#6d7a77]">
          <div className="flex flex-wrap items-center gap-1">
            <span className="inline-flex items-center gap-1">
              <Link href="/health-records" className="hover:text-[#00685f] hover:underline">
                Kết quả khám
              </Link>
              <span>/</span>
            </span>
            <span className="inline-flex items-center gap-1">
              {historyHref ? (
                <Link href={historyHref} className="text-[#6d7a77] hover:text-[#00685f] hover:underline">
                  Lịch sử khám bệnh
                </Link>
              ) : (
                <span className="text-[#6d7a77]">Lịch sử khám bệnh</span>
              )}
              <span>/</span>
            </span>
            <span className="text-[#3d4947]">{currentLabel}</span>
          </div>
        </nav>
      </div>
      <div className="mx-auto w-full max-w-[1360px] px-6 pb-10 pt-6">{content}</div>
    </main>
  );

  if (isLoading || data?.status === "processing") {
    return renderReviewStateShell(
      "Review kết quả khám",
      <div className="flex min-h-[50vh] w-full flex-col items-center justify-center gap-6">
        <Loader2 className="h-10 w-10 animate-spin text-[#00685f]" />
        <h2 className="text-xl font-bold text-[#121e1c]">Hệ thống đang xử lý OCR</h2>
        <p className="text-[#4e6360]">Quá trình này có thể mất một chút thời gian, vui lòng không đóng trang...</p>
      </div>
    );
  }

  if (isError) {
    return renderReviewStateShell(
      "Review kết quả khám",
      <div className="flex min-h-[50vh] w-full flex-col items-center justify-center gap-6">
        <AlertTriangle className="h-10 w-10 text-[#ba1a1a]" />
        <h2 className="text-xl font-bold text-[#ba1a1a]">Lỗi tải dữ liệu</h2>
        <button onClick={() => refetch()} className="rounded-xl bg-[#00685f] px-6 py-2 text-white">
          Thử lại
        </button>
      </div>
    );
  }

  if (data?.status === "ocr_failed" && !manualMode) {
    const hasPartialMetrics = (data.metrics?.length ?? 0) > 0 || Boolean(data.hasLowConfidenceMetrics);
    return renderReviewStateShell(
      "Review kết quả khám",
      <>
        <input
          ref={retryFileInputRef}
          type="file"
          className="hidden"
          accept="application/pdf,image/jpeg,image/png"
          onChange={handleRetryUpload}
        />
        <OcrFailureScreen
          hasPartialMetrics={hasPartialMetrics}
          ocrFailureReason={data.ocrFailureReason}
          isKeepingPartial={isKeepingPartial}
          isRetryUploading={isRetryUploading}
          retryUploadError={retryUploadError}
          onRetry={() => retryFileInputRef.current?.click()}
          onManualInput={() => router.push(`/health-records/review/${recordId}?mode=manual`)}
          onKeepPartial={handleKeepPartial}
        />
      </>
    );
  }

  if (!data) {
    return renderReviewStateShell(
      "Review kết quả khám",
      <div className="flex min-h-[50vh] w-full flex-col items-center justify-center gap-6 text-center">
        <AlertTriangle className="h-10 w-10 text-[#ba1a1a]" />
        <h2 className="text-xl font-bold text-[#ba1a1a]">Không tìm thấy dữ liệu hồ sơ</h2>
        <button
          onClick={() => router.push("/health-records")}
          className="rounded-xl bg-[#00685f] px-6 py-2 font-semibold text-white hover:brightness-110"
        >
          Quay lại danh sách
        </button>
      </div>
    );
  }

  const canConfirm = data?.status === "review_required" || (data?.status === "ocr_failed" && manualMode);
  const canToggleEditResults = data?.status === "done" || (data?.status === "ocr_failed" && manualMode);
  const isOwner = data.isOwner ?? true;
  const canEdit = data.canEdit ?? isOwner;
  const showMetricCards = !editMode;
  const showEditableTable = editMode;
  const showConfidenceColumn = canConfirm;
  const fileUrl = data.fileUrl ?? "";
  const isPdf = fileUrl.toLowerCase().includes(".pdf");
  const isDoneView = data.status === "done" && showMetricCards;
  const displayRecordType = recordType?.trim() || "Phiếu khám bệnh";
  const displayExamDate = examDate?.trim() || "Chưa có ngày khám";
  const displayHospitalName = hospitalName?.trim() || "Chưa cập nhật cơ sở y tế";
  const abnormalMetrics = metrics.filter((metric) => metric.status === "abnormal").length;
  const attentionMetrics = metrics.filter((metric) => metric.status === "attention").length;
  const overallSummary =
    abnormalMetrics > 0 ? "Cần theo dõi" : attentionMetrics > 0 ? "Cần chú ý" : "Bình thường";
  const profileDisplayName = data.profileId
    ? profiles.find((profile) => profile.id === data.profileId)?.displayName
    : undefined;
  const profileOwnerLabel = profileDisplayName ?? (isOwner ? "Tôi" : "Thành viên gia đình");

  if (isDoneView) {
    return (
      <main className="min-h-screen w-full bg-[#effcf9]">
        <div className="sticky top-16 z-30 border-b border-[#bcc9c6]/25 bg-[#effcf9]/95 px-6 py-3 backdrop-blur">
          <nav className="text-sm font-medium text-[#6d7a77]">
            <div className="flex flex-wrap items-center gap-1">
              <span className="inline-flex items-center gap-1">
                <Link href="/health-records" className="hover:text-[#00685f] hover:underline">
                  Kết quả khám
                </Link>
                <span>/</span>
              </span>
              <span className="inline-flex items-center gap-1">
                {data.profileId ? (
                  <Link href={`/profiles/${data.profileId}/history`} className="text-[#6d7a77] hover:text-[#00685f] hover:underline">
                    Lịch sử khám bệnh
                  </Link>
                ) : (
                  <span className="text-[#6d7a77]">Lịch sử khám bệnh</span>
                )}
                <span>/</span>
              </span>
              <span className="text-[#3d4947]">
                {displayRecordType} - {displayExamDate}
              </span>
            </div>
          </nav>
        </div>
        <div className="mx-auto w-full max-w-[1360px] px-6 pb-10 pt-6">
          <div className="space-y-6">

          <section className="flex flex-col gap-4 rounded-[28px] bg-white p-6 shadow-sm md:flex-row md:items-end md:justify-between">
            <div>
              <h1 className="text-3xl font-extrabold tracking-tight text-[#121e1c]">
                {displayRecordType} - {displayExamDate}
              </h1>
              <div className="mt-3 inline-flex items-center gap-2 rounded-full bg-[#deebe8] px-3 py-1.5 text-xs font-bold text-[#274d48]">
                <Building2 className="h-3.5 w-3.5" />
                {displayHospitalName}
              </div>
              <p className="mt-2 text-xs text-[#6d7a77]">
                Hồ sơ của: <span className="font-semibold text-[#3d4947]">{profileOwnerLabel}</span>
              </p>
            </div>
            <div className="flex items-center gap-1.5">
              {canEdit ? (
                <button
                  type="button"
                  title="Chỉnh sửa kết quả"
                  aria-label="Chỉnh sửa kết quả"
                  onClick={() => setEditMode(true)}
                  className="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-[#00685f] text-white transition hover:brightness-110"
                >
                  <Edit2 className="h-4 w-4" />
                </button>
              ) : null}
              <button
                type="button"
                title="Chia sẻ"
                aria-label="Chia sẻ"
                disabled
                className="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-[#e9f6f3] text-[#3d4947] disabled:cursor-not-allowed disabled:opacity-60"
              >
                <Share2 className="h-4 w-4" />
              </button>
              <button
                type="button"
                title="Tải PDF"
                aria-label="Tải PDF"
                disabled
                className="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-[#e9f6f3] text-[#3d4947] disabled:cursor-not-allowed disabled:opacity-60"
              >
                <FileDown className="h-4 w-4" />
              </button>
              {canEdit ? (
                <button
                  type="button"
                  title="Xóa kết quả"
                  aria-label="Xóa kết quả"
                  onClick={() => setShowDeleteRecordModal(true)}
                  disabled={isDeletingRecord}
                  className="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-[#ffdad6] text-[#ba1a1a] disabled:cursor-not-allowed disabled:opacity-70"
                >
                  {isDeletingRecord ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
                </button>
              ) : null}
            </div>
          </section>
          {deleteRecordError ? (
            <div className="rounded-xl bg-[#ffdad6] px-4 py-3 text-sm text-[#ba1a1a]">{deleteRecordError}</div>
          ) : null}

          {/* ── Zone 1: Status Summary (Gradient Card) ── */}
          <section>
            <article className="relative overflow-hidden rounded-[28px] bg-gradient-to-br from-[#00685f] to-[#008378] p-7 text-white shadow-md">
              <div className="relative z-10">
                <span className="inline-flex items-center gap-2 rounded-full bg-white/20 px-3 py-1 text-sm font-bold">
                  <CheckCircle className="h-4 w-4" />
                  {overallSummary}
                </span>
                <h2 className="mt-4 text-2xl font-bold">Tổng quan kết quả xét nghiệm</h2>
                <p className="mt-2 max-w-2xl text-white/90">
                  {overallSummary === "Bình thường"
                    ? "Các chỉ số chính đang trong ngưỡng an toàn. Tiếp tục duy trì lối sống lành mạnh."
                    : "Một số chỉ số cần theo dõi thêm. Bạn nên xem kỹ phần giải thích và khuyến nghị bên dưới."}
                </p>
              </div>
              <div className="pointer-events-none absolute -right-10 -bottom-12 h-44 w-44 rounded-full bg-white/10 blur-2xl" />
            </article>
          </section>

          {/* ── Zone 2: AI Recommendations (White Cards) ── */}
          {recommendationsData && recommendationGroups.length > 0 && (
            <section className="space-y-4">
              <div className="flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-[#00685f]" />
                <h3 className="text-sm font-bold uppercase tracking-wider text-[#3d4947]">Khuyến nghị từ AI</h3>
              </div>

              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                {recommendationGroups.map((group) => {
                  const GroupIcon = recommendationIcon(group.category);
                  return (
                    <article key={group.category} className="rounded-2xl border border-[#bcc9c6]/20 bg-white p-5 shadow-sm">
                      <div className="mb-3 flex items-center gap-2.5">
                        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-[#e9f6f3]">
                          <GroupIcon className="h-4 w-4 text-[#00685f]" />
                        </div>
                        <h4 className="text-sm font-bold text-[#121e1c]">{group.title}</h4>
                      </div>
                      <ul className="space-y-2">
                        {group.items.map((item, idx) => (
                          <li key={idx} className="flex items-start gap-2 text-sm leading-relaxed text-[#3d4947]">
                            <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-[#00685f]/40" />
                            {item}
                          </li>
                        ))}
                      </ul>
                    </article>
                  );
                })}
              </div>

              <div className="flex items-start gap-3 rounded-xl border border-[#e8c86e]/40 bg-[#fffbeb] px-4 py-3.5">
                <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-[#fef3c7]">
                  <ShieldAlert className="h-4 w-4 text-[#92700e]" />
                </div>
                <div>
                  <p className="text-xs font-bold text-[#92700e]">Lưu ý quan trọng</p>
                  <p className="mt-0.5 text-xs leading-relaxed text-[#78650d]">
                    {recommendationsData.disclaimer ?? "Thông tin trên được tạo bởi AI, chỉ mang tính tham khảo và không thay thế tư vấn của bác sĩ chuyên khoa."}
                  </p>
                </div>
              </div>
            </section>
          )}

          <section className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-xl font-bold text-[#121e1c]">Chỉ số chi tiết</h3>
              <span className="rounded-full bg-[#deebe8] px-3 py-1 text-xs font-bold uppercase text-[#00685f]">
                Tổng {metrics.length}
              </span>
            </div>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {metrics.map((metric, idx) => {
                const metricValue = metric.value?.trim() || "--";
                const metricUnit = metric.unit?.trim() || "";
                const metricRangeText = compactRangeText(metric);
                const metricPercent = compactMetricPercent(metric);
                const isNormal = (metric.status ?? "no_data") === "normal";
                return (
                  <article
                    key={`${metric.name}-${idx}`}
                    role="button"
                    tabIndex={0}
                    className="group cursor-pointer rounded-3xl bg-white p-5 shadow-sm transition-all duration-200 hover:shadow-md hover:ring-2 hover:ring-[#00685f]/15"
                    onClick={() => setSelectedMetricIndex(idx)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") {
                        e.preventDefault();
                        setSelectedMetricIndex(idx);
                      }
                    }}
                  >
                    <div className="mb-5 flex items-start justify-between gap-2">
                      <span className="line-clamp-2 text-xs font-extrabold tracking-wide text-[#3d4947] uppercase">
                        {metric.displayNameVi || metric.name}
                      </span>
                      <CheckCircle className={`h-4 w-4 shrink-0 ${isNormal ? "text-[#00685f]" : "text-[#6d7a77]"}`} />
                    </div>
                    <div className="flex items-end gap-1.5">
                      <span className="text-[44px] leading-none font-black tracking-tight text-[#121e1c]">{metricValue}</span>
                      <span className="pb-1 text-2xs font-semibold text-[#3d4947]">{metricUnit}</span>
                    </div>
                    <div className="mt-4 h-2 w-full rounded-full bg-[#deebe8]">
                      <div
                        className="h-full rounded-full bg-[#008378] transition-all"
                        style={{ width: `${metricPercent}%` }}
                      />
                    </div>
                    <div className="mt-3 flex items-center justify-between text-xs font-extrabold uppercase tracking-wide">
                      <span className="text-[#4e6360]">Ngưỡng: {metricRangeText}</span>
                      <span className={isNormal ? "text-[#00685f]" : "text-[#773215]"}>{recordStatusLabel(metric.status)}</span>
                    </div>
                  </article>
                );
              })}
            </div>
          </section>

          {/* Metric Detail Popup */}
          {selectedMetricIndex !== null && metrics[selectedMetricIndex] && (() => {
            const m = metrics[selectedMetricIndex];
            const mValue = m.value?.trim() || "--";
            const mUnit = m.unit?.trim() || "";
            const mPercent = compactMetricPercent(m);
            const mIsNormal = (m.status ?? "no_data") === "normal";
            const mDisplayRef = m.referenceRange
              ? `${m.referenceRange.min} - ${m.referenceRange.max} ${m.referenceRange.unit ?? m.unit}`
              : "Không có dữ liệu tham chiếu";
            const mRangeCtx = m.rangeContext;
            const mCtxNote = mRangeCtx && (mRangeCtx.gender || mRangeCtx.ageRange)
              ? `Ngưỡng áp dụng cho: ${[mRangeCtx.gender === "female" ? "Nữ" : mRangeCtx.gender === "male" ? "Nam" : null, mRangeCtx.ageRange ? `${mRangeCtx.ageRange} tuổi` : null].filter(Boolean).join(", ")}`
              : null;
            return (
              <div
                className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4"
                onClick={() => setSelectedMetricIndex(null)}
              >
                <div
                  role="dialog"
                  aria-modal="true"
                  aria-labelledby="metric-detail-title"
                  className="w-full max-w-md rounded-3xl bg-white shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-200"
                  onClick={(e) => e.stopPropagation()}
                >
                  {/* Header */}
                  <div className="relative overflow-hidden bg-gradient-to-br from-[#00685f] to-[#008378] px-6 pt-6 pb-5 text-white">
                    <div className="relative z-10">
                      <div className="flex items-start justify-between gap-3">
                        <h3 id="metric-detail-title" className="text-lg font-bold leading-snug">
                          {m.displayNameVi || m.name}
                        </h3>
                        <button
                          type="button"
                          aria-label="Đóng chi tiết chỉ số"
                          onClick={() => setSelectedMetricIndex(null)}
                          className="-mr-1 -mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-white/20 transition hover:bg-white/30"
                        >
                          <X className="h-4 w-4" />
                        </button>
                      </div>
                      <div className="mt-4 flex items-end gap-2">
                        <span className="text-4xl font-black tracking-tight">{mValue}</span>
                        <span className="pb-0.5 text-sm font-semibold text-white/80">{mUnit}</span>
                      </div>
                      <div className="mt-3 h-1.5 w-full rounded-full bg-white/20">
                        <div className="h-full rounded-full bg-white/80 transition-all" style={{ width: `${mPercent}%` }} />
                      </div>
                      <div className="mt-2 flex items-center justify-between text-xs font-bold">
                        <span className="text-white/70">Ngưỡng: {compactRangeText(m)}</span>
                        <span className={`rounded-full px-2 py-0.5 text-xs font-bold ${
                          mIsNormal ? "bg-white/20 text-white" : "bg-[#ffdad6] text-[#ba1a1a]"
                        }`}>
                          {recordStatusLabel(m.status)}
                        </span>
                      </div>
                    </div>
                    <div className="pointer-events-none absolute -right-8 -bottom-10 h-36 w-36 rounded-full bg-white/10 blur-2xl" />
                  </div>

                  {/* Detail Body */}
                  <div className="px-6 py-5 space-y-3">
                    <div className="rounded-xl bg-[#f7fbfa] p-4 text-sm leading-relaxed text-[#35514c] space-y-2.5">
                      <p>
                        <span className="font-semibold">Ngưỡng tham chiếu: </span>
                        {mDisplayRef}
                      </p>
                      {m.referenceRangeSource && m.referenceRangeSource !== "none" ? (
                        <p>
                          <span className="font-semibold">Nguồn ngưỡng: </span>
                          {m.referenceRangeSource === "document" ? "Theo phiếu xét nghiệm" : "Theo hệ thống tham chiếu"}
                        </p>
                      ) : null}
                      {m.referenceRangeSource === "system" ? (
                        <p>
                          <span className="font-semibold">Ngữ cảnh ngưỡng: </span>
                          {mCtxNote ?? "Ngưỡng tham chiếu chung"}
                        </p>
                      ) : null}
                      {m.critical ? (
                        <p className="rounded-lg bg-[#fff2f2] px-3 py-2 text-[#ba1a1a]">
                          Chỉ số có dấu hiệu vượt ngưỡng nguy cấp, nên liên hệ bác sĩ để được tư vấn sớm.
                      </p>
                      ) : null}
                      {(() => {
                        const staticExp = m.explanation?.trim() || "";
                        const explanationText = staticExp
                          ? toThreeLineExplanation(staticExp)
                          : !isExplanationLoading
                            ? toThreeLineExplanation(popupExplanationData?.explanation)
                            : "";
                        const showSkeleton = !staticExp && isExplanationLoading;
                        return (
                          <>
                            {showSkeleton ? (
                              <div className="space-y-2 pt-1">
                                <div className="h-3 w-full animate-pulse rounded bg-[#d4e7e3]" />
                                <div className="h-3 w-4/5 animate-pulse rounded bg-[#d4e7e3]" />
                                <div className="h-3 w-3/5 animate-pulse rounded bg-[#d4e7e3]" />
                              </div>
                            ) : null}
                            {explanationText ? (
                              <p className="whitespace-pre-line">
                                <span className="font-semibold">Giải thích: </span>
                                {explanationText}
                              </p>
                            ) : null}
                          </>
                        );
                      })()}
                    </div>

                    {/* Navigation arrows */}
                    <div className="flex items-center justify-between pt-1">
                      <button
                        type="button"
                        disabled={selectedMetricIndex <= 0}
                        onClick={() => setSelectedMetricIndex((prev) => (prev !== null && prev > 0 ? prev - 1 : prev))}
                        className="inline-flex items-center gap-1 rounded-xl px-3 py-2 text-xs font-semibold text-[#00685f] transition hover:bg-[#e9f6f3] disabled:opacity-40 disabled:cursor-not-allowed"
                      >
                        <ChevronUp className="h-3.5 w-3.5 -rotate-90" />
                        Trước
                      </button>
                      <span className="text-xs text-[#6d7a77]">
                        {selectedMetricIndex + 1} / {metrics.length}
                      </span>
                      <button
                        type="button"
                        disabled={selectedMetricIndex >= metrics.length - 1}
                        onClick={() => setSelectedMetricIndex((prev) => (prev !== null && prev < metrics.length - 1 ? prev + 1 : prev))}
                        className="inline-flex items-center gap-1 rounded-xl px-3 py-2 text-xs font-semibold text-[#00685f] transition hover:bg-[#e9f6f3] disabled:opacity-40 disabled:cursor-not-allowed"
                      >
                        Sau
                        <ChevronDown className="h-3.5 w-3.5 -rotate-90" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            );
          })()}

          {!canEdit ? (
            <p className="rounded-xl border border-[#d7e5e1] bg-white px-4 py-3 text-sm text-[#4e6360]">
              Bạn đang xem hồ sơ ở chế độ chia sẻ. Chỉnh sửa và xóa dữ liệu đã bị vô hiệu hóa.
            </p>
          ) : null}
          </div>
        </div>
        {deleteRecordModal}
      </main>
    );
  }

  return renderReviewStateShell(
    "Review kết quả khám",
    <div className="w-full py-2">
      <header className="mb-8">
        <h1 className="text-3xl font-bold text-[#121e1c]">
          {isDoneView ? "Chi tiết kết quả đã xác nhận" : "Kiểm tra kết quả trích xuất"}
        </h1>
        <p className="mt-2 text-sm text-[#4e6360]">
          {isDoneView
            ? "Bạn có thể xem ngưỡng tham chiếu cho từng chỉ số hoặc mở chế độ chỉnh sửa khi cần cập nhật."
            : "Vui lòng so sánh với hồ sơ gốc và điều chỉnh các chỉ số nếu cần thiết trước khi lưu."}
        </p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Left Column: Original Document */}
        <div className="lg:col-span-5 xl:col-span-4 lg:sticky lg:top-10">
          <div className="rounded-2xl border border-[#b7d8d1] bg-white overflow-hidden shadow-md flex flex-col h-[calc(100vh-180px)]">
            <div className="bg-[#effcf9] px-4 py-3 border-b border-[#b7d8d1] flex justify-between items-center">
              <div className="flex items-center gap-2 font-semibold text-[#005049]">
                <FileText className="h-4 w-4" />
                Hồ sơ gốc
              </div>
              <button 
                onClick={() => setShowFullDoc(true)}
                className="p-1.5 hover:bg-[#c5dfd9] rounded-lg transition"
                title="Xem toàn màn hình"
              >
                <Maximize2 className="h-4 w-4 text-[#00685f]" />
              </button>
            </div>
            <div className="flex-1 bg-gray-100 overflow-auto p-4 flex items-start justify-center">
              {isPdf ? (
                <iframe 
                  src={fileUrl} 
                  className="w-full h-full rounded-lg"
                  title="PDF Viewer"
                />
              ) : (
                /* eslint-disable-next-line @next/next/no-img-element */
                <img 
                  src={fileUrl} 
                  alt="Original Document" 
                  className="max-w-none w-full h-auto shadow-sm rounded-lg cursor-zoom-in"
                  onClick={() => setShowFullDoc(true)}
                />
              )}
            </div>
            <div className="px-4 py-3 text-xs text-[#4e6360] bg-white border-t border-[#b7d8d1] flex items-center gap-2">
              <AlertOctagon className="h-3 w-3" />
              <span>Dùng con lăn chuột để cuộn dọc xem hết hồ sơ</span>
            </div>
          </div>
        </div>

        {/* Right Column: Verification Form */}
        <div className="lg:col-span-7 xl:col-span-8 space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="rounded-2xl border border-[#b7d8d1] bg-white p-6 shadow-sm">
              <label className="block text-sm font-medium text-[#3d4947]">Ngày khám</label>
              <input
                type="date"
                className="mt-2 w-full rounded-xl border border-[#c5dfd9] px-3 py-2 outline-none focus:border-[#008378]"
                value={examDate}
                onChange={(e) => setExamDate(e.target.value)}
                disabled={!canEdit}
              />
            </div>
            <div className="rounded-2xl border border-[#b7d8d1] bg-white p-6 shadow-sm">
              <label className="block text-sm font-medium text-[#3d4947]">Loại phiếu (VD: Xét nghiệm máu...)</label>
              <input
                type="text"
                placeholder="Loại phiếu khám..."
                className="mt-2 w-full rounded-xl border border-[#c5dfd9] px-3 py-2 outline-none focus:border-[#008378]"
                value={recordType}
                onChange={(e) => setRecordType(e.target.value)}
                disabled={!canEdit}
              />
            </div>
          </div>

          <div className="rounded-2xl border border-[#b7d8d1] bg-white p-6 shadow-sm">
            <label className="block text-sm font-medium text-[#3d4947]">Tên bệnh viện / Phòng khám</label>
            <input
              type="text"
              placeholder="Nhập tên bệnh viện..."
              className="mt-2 w-full rounded-xl border border-[#c5dfd9] px-3 py-2 outline-none focus:border-[#008378]"
              value={hospitalName}
              onChange={(e) => setHospitalName(e.target.value)}
              disabled={!canEdit}
            />
          </div>

          <div className="rounded-2xl border border-[#b7d8d1] bg-white p-6 shadow-sm">
            <label className="block text-sm font-medium text-[#3d4947]">Chẩn đoán / Kết luận của bác sĩ</label>
            <textarea
              rows={3}
              placeholder="Nhập chẩn đoán hoặc kết luận chung..."
              className="mt-2 w-full rounded-xl border border-[#c5dfd9] px-3 py-2 outline-none focus:border-[#008378] resize-none"
              value={diagnosis}
              onChange={(e) => setDiagnosis(e.target.value)}
              disabled={!canEdit}
            />
          </div>

          {showMetricCards && (
            <div className="rounded-2xl border border-[#b7d8d1] bg-white shadow-sm overflow-hidden">
              <div className="border-b border-[#c5dfd9] bg-[#effcf9] px-6 py-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-[#005049]">Danh sách chỉ số và ngưỡng tham chiếu</h3>
                  {canToggleEditResults && canEdit ? (
                    <button
                      type="button"
                      onClick={() => setEditMode(true)}
                      className="rounded-lg border border-[#00685f] px-3 py-1.5 text-xs font-semibold text-[#00685f] hover:bg-[#effcf9]"
                    >
                      Chỉnh sửa kết quả
                    </button>
                  ) : null}
                </div>
              </div>
              <div className="space-y-3 p-4">
                {metrics.map((metric, idx) => (
                  <HealthMetricCard
                    key={`${metric.name}-${idx}`}
                    recordId={recordId}
                    metricName={metric.name}
                    displayNameVi={metric.displayNameVi}
                    value={metric.value}
                    unit={metric.unit}
                    referenceRange={metric.referenceRange}
                    rangeContext={metric.rangeContext}
                    referenceRangeSource={metric.referenceRangeSource}
                    status={metric.status ?? "no_data"}
                    critical={metric.critical}
                    explanation={metric.explanation}
                  />
                ))}
              </div>
            </div>
          )}

          {showMetricCards && data.status === "done" && recommendationsData ? (
            <div className="rounded-2xl border border-[#b7d8d1] bg-white shadow-sm overflow-hidden">
              <div className="border-b border-[#c5dfd9] bg-[#effcf9] px-6 py-4">
                <h3 className="text-sm font-semibold text-[#005049]">Khuyến nghị theo nhóm</h3>
              </div>
              <div className="space-y-3 p-6">
                {recommendationGroups.map((group) => {
                  const GroupIcon = recommendationIcon(group.category);
                  return (
                    <div key={group.category} className="rounded-xl border border-[#d7e5e1] bg-[#f9fcfb] p-4">
                      <div className="mb-2 flex items-center gap-2 text-[#005049]">
                        <GroupIcon className="h-4 w-4 shrink-0" />
                        <h4 className="text-sm font-semibold">{group.title}</h4>
                      </div>
                      <div className="space-y-1">
                        {group.items.length ? (
                          group.items.map((item, idx) => (
                            <p key={`${group.category}-${idx}`} className="text-sm text-[#1d3b36]">
                              - {item}
                            </p>
                          ))
                        ) : (
                          <p className="text-sm text-[#6d7a77]">Chưa có khuyến nghị cho nhóm này.</p>
                        )}
                      </div>
                    </div>
                  );
                })}
                <div className="mt-4 rounded-lg bg-[#f5f7f7] px-4 py-3 text-xs text-[#6d7a77] whitespace-pre-line">
                  {recommendationsData.disclaimer}
                </div>
              </div>
            </div>
          ) : null}

          {showEditableTable && canEdit && (
            <div className="rounded-2xl border border-[#b7d8d1] bg-white shadow-sm overflow-hidden">
              <div className="flex items-center justify-between border-b border-[#c5dfd9] bg-[#effcf9] px-6 py-4">
                <h3 className="text-sm font-semibold text-[#005049]">Chỉnh sửa danh sách chỉ số</h3>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      setAddForm({ name: "", value: "", unit: "" });
                      setAddError(null);
                      setShowAddDialog(true);
                    }}
                    className="flex items-center gap-1.5 rounded-lg bg-[#00685f] px-3 py-1.5 text-xs font-semibold text-white hover:brightness-110"
                  >
                    <Plus className="h-3.5 w-3.5" />
                    Thêm chỉ số
                  </button>
                  {canToggleEditResults ? (
                    <button
                      type="button"
                      onClick={() => setEditMode(false)}
                      className="rounded-lg border border-[#c5dfd9] px-3 py-1.5 text-xs font-semibold text-[#4e6360] hover:bg-white"
                    >
                      Quay lại thẻ chỉ số
                    </button>
                  ) : null}
                </div>
              </div>
              <div className="overflow-x-auto">
                {data.hasLowConfidenceMetrics ? (
                  <div className="mx-6 mt-4 rounded-xl border border-[#e6b144] bg-[#fff4dd] px-4 py-3 text-sm font-medium text-[#825500]">
                    Có chỉ số OCR độ tin cậy thấp. Vui lòng kiểm tra lại trước khi lưu.
                  </div>
                ) : null}
                <table className="w-full text-left text-sm text-[#4e6360]">
                  <thead className="bg-[#effcf9] text-[#005049]">
                    <tr>
                      <th className="px-6 py-4 font-semibold">Chỉ số</th>
                      <th className="px-6 py-4 font-semibold">Giá trị</th>
                      <th className="px-6 py-4 font-semibold">Đơn vị</th>
                      <th className="px-6 py-4 font-semibold">Nguồn</th>
                      {showConfidenceColumn ? <th className="px-6 py-4 font-semibold">Độ tin cậy</th> : null}
                      <th className="px-6 py-4 font-semibold text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#c5dfd9]">
                    {metrics.map((metric, idx) => {
                      const isEditing = editingIndex === idx;

                      return (
                        <tr
                          key={idx}
                          className={`hover:bg-gray-50 transition ${
                            metric.confidenceLevel !== "high" ? "bg-[#fff8e8]" : ""
                          }`}
                        >
                          <td className="px-6 py-4">
                            {isEditing ? (
                              <input
                                className="w-full rounded border border-[#00685f] px-2 py-1"
                                value={editForm?.name || ""}
                                onChange={(e) => setEditForm({ ...editForm!, name: e.target.value })}
                                autoFocus
                              />
                            ) : (
                              <span className="font-medium text-[#005049]">{metric.name}</span>
                            )}
                          </td>
                          <td className="px-6 py-4">
                            {isEditing ? (
                              <input
                                className="w-full rounded border border-[#00685f] px-2 py-1"
                                value={editForm?.value || ""}
                                onChange={(e) => setEditForm({ ...editForm!, value: e.target.value })}
                              />
                            ) : (
                              <span className={metric.value ? "" : "text-gray-400 italic"}>
                                {metric.value || "Trống"}
                              </span>
                            )}
                          </td>
                          <td className="px-6 py-4">
                            {isEditing ? (
                              <input
                                className="w-full rounded border border-[#00685f] px-2 py-1"
                                value={editForm?.unit || ""}
                                onChange={(e) => setEditForm({ ...editForm!, unit: e.target.value })}
                              />
                            ) : (
                              metric.unit || "-"
                            )}
                          </td>
                          <td className="px-6 py-4">
                            {!isEditing && <SourceBadge source={metric.source} />}
                          </td>
                          {showConfidenceColumn ? (
                            <td className="px-6 py-4">
                              {!isEditing && (
                                <span
                                  className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ${
                                    metric.confidenceLevel === "high"
                                      ? "bg-[#ccfbf1] text-[#0f766e]"
                                      : metric.confidenceLevel === "medium"
                                      ? "bg-[#ffddb3] text-[#825500]"
                                      : "bg-[#ffdad6] text-[#ba1a1a]"
                                  }`}
                                >
                                  {metric.confidenceLevel === "high" && <CheckCircle className="h-3 w-3" />}
                                  {metric.confidenceLevel === "medium" && <AlertTriangle className="h-3 w-3" />}
                                  {metric.confidenceLevel === "low" && <AlertCircle className="h-3 w-3" />}
                                  {metric.confidenceLevel === "high"
                                    ? "Cao"
                                    : metric.confidenceLevel === "medium"
                                    ? "Trung bình"
                                    : "Vui lòng kiểm tra"}
                                </span>
                              )}
                            </td>
                          ) : null}
                          <td className="px-6 py-4 text-right">
                            {isEditing ? (
                              <div className="flex justify-end gap-2">
                                <button onClick={handleSaveEdit} className="text-[#00685f] hover:brightness-110">
                                  <Save className="h-5 w-5" />
                                </button>
                                <button onClick={handleCancelEdit} className="text-[#ba1a1a] hover:brightness-110">
                                  <X className="h-5 w-5" />
                                </button>
                              </div>
                            ) : (
                              <div className="flex justify-end gap-2">
                                <button
                                  onClick={() => handleEditClick(idx)}
                                  className="text-[#4e6360] hover:text-[#005049]"
                                  title="Chỉnh sửa"
                                >
                                  <Edit2 className="h-5 w-5" />
                                </button>
                                <button
                                  onClick={() => handleDeleteMetric(idx)}
                                  className="text-[#4e6360] hover:text-[#ba1a1a]"
                                  title="Xóa chỉ số"
                                >
                                  <Trash2 className="h-5 w-5" />
                                </button>
                              </div>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  {metrics.length === 0 && (
                    <tr>
                      <td colSpan={showConfidenceColumn ? 6 : 5} className="px-6 py-8 text-center text-[#4e6360]">
                        Không tìm thấy chỉ số nào từ kết quả OCR.
                      </td>
                    </tr>
                  )}
                </tbody>
                </table>
              </div>
            </div>
          )}

          {saveError && (
            <div className="rounded-xl bg-[#ffdad6] p-4 text-sm text-[#ba1a1a] flex items-center gap-2">
              <AlertCircle className="h-5 w-5" />
              {saveError}
            </div>
          )}

          {canEdit ? (
            <div className="flex justify-end pt-4 pb-12">
              <button
                onClick={() => (canConfirm ? setShowConfirmModal(true) : executeSave(false))}
                disabled={isSaving || (!canConfirm && !isDirty)}
                className="inline-flex items-center gap-2 rounded-xl bg-[#00685f] px-10 py-4 font-bold text-white shadow-lg transition hover:brightness-110 hover:translate-y-[-2px] disabled:opacity-70 disabled:transform-none"
              >
                {isSaving ? <Loader2 className="h-5 w-5 animate-spin" /> : <Save className="h-5 w-5" />}
                {canConfirm ? "XÁC NHẬN VÀ LƯU HỒ SƠ" : "LƯU CHỈNH SỬA"}
              </button>
            </div>
          ) : (
            <p className="rounded-xl border border-[#d7e5e1] bg-white px-4 py-3 text-sm text-[#4e6360]">
              Bạn đang xem hồ sơ ở chế độ chia sẻ. Chỉnh sửa và xóa dữ liệu đã bị vô hiệu hóa.
            </p>
          )}
          {!canConfirm && !isDirty && !showEditableTable && canEdit && (
            <p className="text-right text-sm text-[#6d7a77]">
              Hồ sơ đã xác nhận. Chỉnh sửa thông tin hành chính ở trên hoặc bấm &quot;Chỉnh sửa kết quả&quot; để cập nhật các chỉ số.
            </p>
          )}
        </div>
      </div>

      {/* Confirmation Modal */}
      {showConfirmModal && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl overflow-hidden p-8 animate-in fade-in zoom-in duration-200">
            <div className="flex flex-col items-center text-center">
              <div className="h-16 w-16 bg-[#effcf9] text-[#00685f] rounded-full flex items-center justify-center mb-6">
                <CheckCircle className="h-8 w-8" />
              </div>
              <h3 className="text-2xl font-bold text-[#005049] mb-2">Hoàn tất kiểm tra?</h3>
              <p className="text-[#4e6360] mb-8">
                Bạn đã đối chiếu các thông tin với hồ sơ gốc chưa? Kết quả sau khi lưu sẽ được cập nhật vào hồ sơ sức khỏe.
              </p>

              <div className="flex flex-col w-full gap-3">
                <button
                  onClick={() => executeSave(false)}
                  className="w-full bg-[#00685f] text-white py-4 rounded-2xl font-bold hover:brightness-110 transition flex items-center justify-center gap-2 shadow-lg shadow-[#00685f]/20"
                >
                  <Save className="h-5 w-5" />
                  Xác nhận và lưu kết quả
                </button>
                <button
                  onClick={() => setShowConfirmModal(false)}
                  className="w-full bg-white border border-[#c5dfd9] text-[#4e6360] py-4 rounded-2xl font-bold hover:bg-gray-50 transition mt-2"
                >
                  Hủy, kiểm tra thêm
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {deleteRecordModal}

      {/* Add Metric Dialog */}
      {showAddDialog && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl overflow-hidden p-8 animate-in fade-in zoom-in duration-200">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-xl font-bold text-[#005049]">Thêm chỉ số</h3>
              <button
                onClick={() => setShowAddDialog(false)}
                className="p-2 hover:bg-gray-100 rounded-full transition"
              >
                <X className="h-5 w-5 text-[#4e6360]" />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-[#3d4947] mb-1.5">
                  Tên chỉ số <span className="text-[#ba1a1a]">*</span>
                </label>
                {referenceMetrics.length > 0 ? (
                  <select
                    className="w-full rounded-xl border border-[#c5dfd9] px-3 py-2.5 text-sm outline-none focus:border-[#008378] bg-white"
                    value={addForm.name}
                    onChange={(e) => handleAddNameChange(e.target.value)}
                  >
                    <option value="">-- Chọn chỉ số --</option>
                    {referenceMetrics.map((m) => (
                      <option key={m.name} value={m.name}>
                        {m.displayNameVi} ({m.name})
                      </option>
                    ))}
                  </select>
                ) : (
                  <input
                    type="text"
                    placeholder="Nhập tên chỉ số..."
                    className="w-full rounded-xl border border-[#c5dfd9] px-3 py-2.5 text-sm outline-none focus:border-[#008378]"
                    value={addForm.name}
                    onChange={(e) => setAddForm((prev) => ({ ...prev, name: e.target.value }))}
                  />
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-[#3d4947] mb-1.5">
                  Giá trị <span className="text-[#ba1a1a]">*</span>
                </label>
                <input
                  type="text"
                  inputMode="decimal"
                  placeholder="VD: 5.4"
                  className="w-full rounded-xl border border-[#c5dfd9] px-3 py-2.5 text-sm outline-none focus:border-[#008378]"
                  value={addForm.value}
                  onChange={(e) => {
                    setAddForm((prev) => ({ ...prev, value: e.target.value }));
                    setAddError(null);
                  }}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-[#3d4947] mb-1.5">
                  Đơn vị <span className="text-[#ba1a1a]">*</span>
                </label>
                <input
                  type="text"
                  placeholder="VD: mmol/L"
                  className="w-full rounded-xl border border-[#c5dfd9] px-3 py-2.5 text-sm outline-none focus:border-[#008378]"
                  value={addForm.unit}
                  onChange={(e) => setAddForm((prev) => ({ ...prev, unit: e.target.value }))}
                />
              </div>

              {addError && (
                <div className="rounded-xl bg-[#ffdad6] px-4 py-3 text-sm text-[#ba1a1a] flex items-center gap-2">
                  <AlertCircle className="h-4 w-4 flex-shrink-0" />
                  {addError}
                </div>
              )}
            </div>

            <div className="mt-6 flex gap-3">
              <button
                onClick={() => setShowAddDialog(false)}
                className="flex-1 rounded-2xl border border-[#c5dfd9] py-3 font-semibold text-[#4e6360] hover:bg-gray-50 transition"
              >
                Hủy
              </button>
              <button
                onClick={handleAddMetric}
                className="flex-1 rounded-2xl bg-[#00685f] py-3 font-semibold text-white hover:brightness-110 transition"
              >
                Thêm chỉ số
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Fullscreen Document Modal */}
      {showFullDoc && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/90 p-4">
          <div className="relative w-full max-w-[95vw] h-[95vh] bg-white rounded-2xl overflow-hidden flex flex-col">
            <div className="flex justify-between items-center px-6 py-4 border-b">
              <h3 className="font-bold text-lg text-[#005049]">Hồ sơ gốc (Chi tiết)</h3>
              <button
                onClick={() => setShowFullDoc(false)}
                className="p-2 hover:bg-gray-100 rounded-full transition"
              >
                <X className="h-6 w-6" />
              </button>
            </div>
            <div className="flex-1 overflow-auto bg-gray-200 p-8 flex justify-center items-start">
              {isPdf ? (
                <iframe src={fileUrl} className="w-full h-full" title="Full PDF" />
              ) : (
                /* eslint-disable-next-line @next/next/no-img-element */
                <img src={fileUrl} alt="Full Document" className="max-w-none w-auto shadow-2xl rounded-lg" />
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function SourceBadge({ source }: { source: string }) {
  const isManual = source === "manual";
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ${
        isManual
          ? "bg-[#e8f4ff] text-[#0055aa]"
          : "bg-[#f0fdf4] text-[#166534]"
      }`}
    >
      {isManual ? <PenLine className="h-3 w-3" /> : <ScanLine className="h-3 w-3" />}
      {isManual ? "Nhập tay" : "OCR"}
    </span>
  );
}

function compactRangeText(metric: MetricDto): string {
  const min = metric.referenceRange?.min;
  const max = metric.referenceRange?.max;
  if (typeof min === "number" && typeof max === "number") {
    return `${min}-${max}`;
  }
  if (typeof max === "number") {
    return `<${max}`;
  }
  return "N/A";
}

function compactMetricPercent(metric: MetricDto): number {
  const raw = metric.value?.replace(",", ".").trim() ?? "";
  const numericValue = Number(raw);
  const min = metric.referenceRange?.min;
  const max = metric.referenceRange?.max;
  if (!Number.isFinite(numericValue) || typeof min !== "number" || typeof max !== "number" || max <= min) {
    return 60;
  }
  const ratio = ((numericValue - min) / (max - min)) * 100;
  return Math.max(8, Math.min(100, Math.round(ratio)));
}

function recordStatusLabel(status?: MetricDto["status"]): string {
  if (status === "abnormal") return "Bất thường";
  if (status === "attention") return "Cần chú ý";
  if (status === "normal") return "Bình thường";
  return "Không rõ";
}



function recommendationCategory(item: string): RecommendationCategory {
  const content = item.toLowerCase();
  if (content.startsWith("chế độ sinh hoạt:") || content.startsWith("sinh hoạt:")) {
    return "lifestyle";
  }
  if (content.startsWith("chế độ dinh dưỡng:") || content.startsWith("dinh dưỡng:")) {
    return "nutrition";
  }
  if (/(ăn|dinh dưỡng|khẩu phần|chất béo|đường|muối|rau|trái cây|thực phẩm)/i.test(content)) {
    return "nutrition";
  }
  return "lifestyle";
}

function recommendationTitle(category: RecommendationCategory): string {
  if (category === "nutrition") return "Chế độ dinh dưỡng";
  return "Chế độ sinh hoạt";
}

function recommendationIcon(category: RecommendationCategory) {
  if (category === "nutrition") return Apple;
  return Heart;
}

function groupRecommendations(items: string[]): RecommendationGroup[] {
  const groups = new Map<RecommendationCategory, string[]>();
  items.forEach((item) => {
    const category = recommendationCategory(item);
    const current = groups.get(category) ?? [];
    current.push(item);
    groups.set(category, current);
  });

  const categoryOrder: RecommendationCategory[] = ["nutrition", "lifestyle"];
  return categoryOrder
    .map((category) => ({
      category,
      title: recommendationTitle(category),
      items: groups.get(category) ?? [],
    }))
    .filter((group) => group.items.length > 0);
}
