"use client";

import { type ChangeEvent, type ReactNode, useEffect, useMemo, useState, useRef } from "react";
import Link from "next/link";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
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
  Heart,
  Apple,
  Sparkles,
  ShieldAlert,
  Shield,
  ChevronDown,
  ChevronUp,
  Mail,
  UserPlus,
} from "lucide-react";
import { z } from "zod";

import { apiClient } from "@/lib/api/apiClient";
import { notify } from "@/lib/notify";
import { ALLOWED_FILE_TYPES, ApiPaths, UPLOAD_MAX_SIZE_BYTES } from "@healthlens/shared/constants";
import { HealthMetricCard } from "@/components/ui/HealthMetricCard";
import { ErrorState, InlineFieldError, LoadingState } from "@/components/ui";
import { OcrFailureScreen } from "@/components/features/upload/OcrFailureScreen";
import { DeleteRecordModal } from "@/components/features/health-records/DeleteRecordModal";
import { recommendationDisclaimerText } from "@/lib/utils/medicalDisclaimer";
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
  profileDisplayName?: string;
  isOwner?: boolean;
  canEdit?: boolean;
  shareScope?: "owner" | "profile" | "record";
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

type HealthRecordSharedMember = {
  id: string;
  viewerId?: string;
  email: string;
  status: "pending" | "accepted" | "expired" | "revoked" | string;
  shareScope?: "record" | "profile" | string;
  accessLevel?: "view" | "edit" | string;
  expiresAt?: string | null;
  createdAt?: string | null;
  acceptedAt?: string | null;
};

type RecommendationCategory = "nutrition" | "lifestyle";
type AddMetricField = "name" | "value" | "unit";

type RecommendationGroup = {
  category: RecommendationCategory;
  title: string;
  items: string[];
};

const metricSchema = z.object({
  name: z.string().trim().min(1, "Tên chỉ số không được để trống"),
  value: z
    .string()
    .trim()
    .min(1, "Giá trị không được để trống"),
  unit: z.string().trim().min(1, "Đơn vị không được để trống"),
  source: z.enum(["ocr", "manual"]),
});

function metricLabel(metric: Pick<MetricDto, "name"> | null | undefined, index: number) {
  const name = metric?.name?.trim();
  return name ? "“" + name + "”" : "#" + (index + 1);
}

function validateMetricForSave(metric: MetricDto, index: number): string | null {
  const validation = metricSchema.safeParse({
    name: metric.name,
    value: metric.value,
    unit: metric.unit,
    source: "manual",
  });
  if (validation.success) return null;
  const message = validation.error.issues[0]?.message ?? "Dữ liệu chỉ số không hợp lệ";
  return "Chỉ số " + metricLabel(metric, index) + ": " + message.charAt(0).toLowerCase() + message.slice(1);
}

function extractFilename(contentDisposition: unknown): string | null {
  if (typeof contentDisposition !== "string") return null;
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1].trim().replace(/^"|"$/g, ""));
  }
  const asciiMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
  return asciiMatch?.[1]?.trim() ?? null;
}

function slugifyFilenamePart(value: string | null | undefined): string {
  if (!value) return "kham";
  const slug = value
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
  return slug || "kham";
}

function getResponse(error: unknown) {
  if (!error || typeof error !== "object") {
    return undefined;
  }
  return (error as { response?: { headers?: Record<string, unknown>; status?: number } }).response;
}

function getResponseCorrelationId(error: unknown) {
  const headers = getResponse(error)?.headers;
  const rawId =
    headers?.["x-correlation-id"] ??
    headers?.["x-request-id"] ??
    headers?.["X-Correlation-Id"] ??
    headers?.["X-Request-Id"];
  return typeof rawId === "string" ? rawId : undefined;
}

function isPdfFileUrl(fileUrl: string) {
  try {
    return new URL(fileUrl).pathname.toLowerCase().endsWith(".pdf");
  } catch {
    return fileUrl.split(/[?#]/, 1)[0]?.toLowerCase().endsWith(".pdf") ?? false;
  }
}

function logReviewActionError(action: string, error: unknown) {
  const response = getResponse(error);
  console.error("Health record review action failed", {
    action,
    status: response?.status,
    correlationId: getResponseCorrelationId(error),
    errorType: error instanceof Error ? error.name : typeof error,
  });
}

export default function ReviewRecordPage() {
  const params = useParams();
  const router = useRouter();
  const queryClient = useQueryClient();
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
  const [documentPreviewFailed, setDocumentPreviewFailed] = useState(false);
  const [selectedMetricIndex, setSelectedMetricIndex] = useState<number | null>(null);
  const [isKeepingPartial, setIsKeepingPartial] = useState(false);
  const [isRetryUploading, setIsRetryUploading] = useState(false);
  const [retryUploadError, setRetryUploadError] = useState<string | null>(null);
  const [isDeletingRecord, setIsDeletingRecord] = useState(false);
  const [deleteRecordError, setDeleteRecordError] = useState<string | null>(null);
  const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);
  const [pdfDownloadError, setPdfDownloadError] = useState<string | null>(null);
  const [showDeleteRecordModal, setShowDeleteRecordModal] = useState(false);
  const [showRecordShareModal, setShowRecordShareModal] = useState(false);
  const [recordShareEmail, setRecordShareEmail] = useState("");
  const [recordShareAccessLevel, setRecordShareAccessLevel] = useState<"view" | "edit">("view");
  const [recordShareError, setRecordShareError] = useState<string | null>(null);
  const [pendingRecordShareRevoke, setPendingRecordShareRevoke] = useState<HealthRecordSharedMember | null>(null);
  const retryFileInputRef = useRef<HTMLInputElement | null>(null);

  const [showAddDialog, setShowAddDialog] = useState(false);
  const [addForm, setAddForm] = useState({ name: "", value: "", unit: "" });
  const [addError, setAddError] = useState<string | null>(null);
  const [addErrorField, setAddErrorField] = useState<AddMetricField | null>(null);

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

  const { data: recordSharedMembers = [], isFetching: isRecordSharedMembersLoading } = useQuery<
    HealthRecordSharedMember[]
  >({
    queryKey: ["health-record-shared-members", recordId],
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.HEALTH_RECORDS.INVITATIONS(recordId));
      return ((response.data?.data ?? []) as HealthRecordSharedMember[]).filter(
        (member) => member.status !== "revoked" && member.status !== "expired"
      );
    },
    enabled: showRecordShareModal,
  });

  const inviteRecordMutation = useMutation({
    mutationFn: async (payload: { email: string; accessLevel: "view" | "edit" }) => {
      const response = await apiClient.post(ApiPaths.HEALTH_RECORDS.INVITATIONS(recordId), {
        email: payload.email,
        accessLevel: payload.accessLevel,
      });
      return response.data?.data as HealthRecordSharedMember;
    },
    onSuccess: async () => {
      setRecordShareEmail("");
      setRecordShareError(null);
      await queryClient.invalidateQueries({ queryKey: ["health-record-shared-members", recordId] });
      notify.success("Đã gửi lời mời chia sẻ kết quả khám.");
    },
    onError: () => {
      const message = "Gửi lời mời thất bại. Vui lòng kiểm tra email và thử lại.";
      setRecordShareError(message);
      notify.error(message);
    },
  });

  const revokeRecordShareMutation = useMutation({
    mutationFn: async (member: HealthRecordSharedMember) => {
      const viewerId = member.viewerId ?? member.id;
      if (member.shareScope === "profile") {
        if (!data?.profileId) {
          throw new Error("Missing profile id");
        }
        await apiClient.delete(ApiPaths.PROFILES.REVOKE_SHARE(data.profileId, viewerId));
        return member;
      }
      await apiClient.delete(ApiPaths.HEALTH_RECORDS.REVOKE_SHARE(recordId, viewerId));
      return member;
    },
    onSuccess: async (member) => {
      setPendingRecordShareRevoke(null);
      setRecordShareError(null);
      queryClient.setQueryData(
        ["health-record-shared-members", recordId],
        (previous: HealthRecordSharedMember[] | undefined) =>
          (previous ?? []).filter((item) => item.id !== member.id && item.email.toLowerCase() !== member.email.toLowerCase())
      );
      await queryClient.invalidateQueries({ queryKey: ["health-record-shared-members", recordId] });
      notify.success("Đã thu hồi quyền truy cập kết quả khám.");
    },
    onError: () => {
      const message = "Thu hồi quyền thất bại. Vui lòng thử lại.";
      setRecordShareError(message);
      notify.error(message);
    },
  });

  const updateRecordShareAccessMutation = useMutation({
    mutationFn: async (payload: { member: HealthRecordSharedMember; accessLevel: "view" | "edit" }) => {
      if (payload.member.shareScope === "profile") {
        if (!data?.profileId) {
          throw new Error("Missing profile id");
        }
        await apiClient.post(ApiPaths.PROFILES.INVITATIONS(data.profileId), {
          email: payload.member.email,
          accessLevel: payload.accessLevel,
        });
        return;
      }
      await apiClient.post(ApiPaths.HEALTH_RECORDS.INVITATIONS(recordId), {
        email: payload.member.email,
        accessLevel: payload.accessLevel,
      });
    },
    onSuccess: async (_, variables) => {
      setRecordShareError(null);
      queryClient.setQueryData(
        ["health-record-shared-members", recordId],
        (previous: HealthRecordSharedMember[] | undefined) =>
          (previous ?? []).map((item) =>
            item.id === variables.member.id || item.email.toLowerCase() === variables.member.email.toLowerCase()
              ? { ...item, accessLevel: variables.accessLevel }
              : item
          )
      );
      await queryClient.invalidateQueries({ queryKey: ["health-record-shared-members", recordId] });
      notify.success(`Đã cập nhật quyền ${variables.accessLevel === "edit" ? "chỉnh sửa" : "chỉ xem"}.`);
    },
    onError: () => {
      const message = "Cập nhật quyền thất bại. Vui lòng thử lại.";
      setRecordShareError(message);
      notify.error(message);
    },
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
  const notifiedOcrFailureRef = useRef<string | null>(null);

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

  useEffect(() => {
    setDocumentPreviewFailed(false);
    setShowFullDoc(false);
  }, [data?.fileUrl]);

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

  useEffect(() => {
    if (data?.status !== "ocr_failed" || manualMode) {
      return;
    }

    if (notifiedOcrFailureRef.current === recordId) {
      return;
    }

    notifiedOcrFailureRef.current = recordId;
    notify.error(data.ocrFailureReason?.trim() || "Không thể nhận diện dữ liệu từ tệp đã tải lên.");
  }, [data?.ocrFailureReason, data?.status, manualMode, recordId]);

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

    const cleanedMetric = validation.data;
    setSaveError(null);
    const updatedMetrics: MetricDto[] = [...metrics];
    updatedMetrics[editingIndex] = {
      ...editForm,
      name: cleanedMetric.name,
      value: cleanedMetric.value,
      unit: cleanedMetric.unit,
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
      const field = validation.error.issues[0]?.path[0];
      setAddErrorField(field === "name" || field === "value" || field === "unit" ? field : null);
      setAddError(validation.error.issues[0]?.message ?? "Dữ liệu không hợp lệ");
      return;
    }

    const cleanedMetric = validation.data;
    setAddError(null);
    setAddErrorField(null);
    const newMetric: MetricDto = {
      name: cleanedMetric.name,
      value: cleanedMetric.value,
      unit: cleanedMetric.unit,
      confidence: 1.0,
      confidenceLevel: "high",
      source: "manual",
    };
    setMetrics((prev) => [...prev, newMetric]);
    setShowAddDialog(false);
    setAddForm({ name: "", value: "", unit: "" });
    setAddErrorField(null);
  };

  const handleAddNameChange = (name: string) => {
    const ref = referenceMetrics.find((m) => m.name === name);
    setAddForm((prev) => ({
      ...prev,
      name,
      unit: ref?.unit ?? prev.unit,
    }));
    setAddError(null);
    setAddErrorField(null);
  };

  const executeSave = async (keepPartial = false) => {
    try {
      setIsSaving(true);
      setSaveError(null);
      setShowConfirmModal(false);

      const resolvedKeepPartial = keepPartial || (data?.status === "ocr_failed" && manualMode);
      const finalMetrics: MetricDto[] = metrics;
      const metricValidationError = finalMetrics.map(validateMetricForSave).find(Boolean);
      if (metricValidationError) {
        setSaveError(metricValidationError);
        return false;
      }

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
      notify.success("Lưu kết quả khám thành công!");
      return true;
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { detail?: string; title?: string }; status?: number } };
      const serverMsg = axiosErr?.response?.data?.detail ?? axiosErr?.response?.data?.title;
      const statusCode = axiosErr?.response?.status;
      if (serverMsg) {
        const message = statusCode
          ? `Không thể lưu kết quả khám. Mã lỗi ${statusCode}: ${serverMsg}`
          : `Không thể lưu kết quả khám. ${serverMsg}`;
        setSaveError(message);
        notify.error(message);
      } else {
        const message = "Đã có lỗi xảy ra khi lưu. Vui lòng thử lại.";
        setSaveError(message);
        notify.error(message);
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
      notify.error(validationError);
      event.target.value = "";
      return;
    }

    if (!data?.profileId) {
      const message = "Không xác định được hồ sơ người dùng để tải tệp mới.";
      setRetryUploadError(message);
      notify.error(message);
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
      notify.success("Đã tải tệp mới. Hệ thống đang xử lý lại OCR.");
      initialized.current = false;
      await refetch();
      router.replace(`/health-records/review/${uploadInfo.recordId}`);
    } catch (error) {
      logReviewActionError("retry-upload", error);
      const message = "Tải tệp mới thất bại. Vui lòng thử lại.";
      setRetryUploadError(message);
      notify.error(message);
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
      notify.success("Đã xóa kết quả khám thành công.");
      const redirectPath = data?.profileId ? `/profiles/${data.profileId}/history` : "/health-records";
      router.push(redirectPath);
    } catch (error) {
      logReviewActionError("delete-record", error);
      const message = "Xóa kết quả thất bại. Vui lòng thử lại.";
      setDeleteRecordError(message);
      notify.error(message);
    } finally {
      setIsDeletingRecord(false);
    }
  };

  const handleDownloadPdf = async () => {
    if (!recordId || isDownloadingPdf) return;
    try {
      setIsDownloadingPdf(true);
      setPdfDownloadError(null);
      const response = await apiClient.get(ApiPaths.HEALTH_RECORDS.DOWNLOAD_PDF(recordId), {
        responseType: "blob",
        headers: { Accept: "application/pdf" },
      });
      const blob = new Blob([response.data], { type: "application/pdf" });
      const objectUrl = URL.createObjectURL(blob);
      const filename =
        extractFilename(response.headers["content-disposition"]) ??
        `healthlens-ket-qua-${slugifyFilenamePart(data?.recordType)}.pdf`;
      const link = document.createElement("a");
      link.href = objectUrl;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(objectUrl);
    } catch (error) {
      logReviewActionError("download-pdf", error);
      const message = "Tải PDF thất bại. Vui lòng thử lại.";
      setPdfDownloadError(message);
      notify.error(message);
    } finally {
      setIsDownloadingPdf(false);
    }
  };

  const handleInviteRecordShare = () => {
    const trimmedEmail = recordShareEmail.trim().toLowerCase();
    if (!trimmedEmail) {
      const message = "Vui lòng nhập địa chỉ email.";
      setRecordShareError(message);
      notify.error(message);
      return;
    }
    setRecordShareError(null);
    inviteRecordMutation.mutate({
      email: trimmedEmail,
      accessLevel: recordShareAccessLevel,
    });
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
  const renderOriginalDocumentPreview = (
    fileUrlValue: string | null | undefined,
    options: { compact?: boolean; fit?: "contain" | "width"; unavailableReason?: string } = {}
  ) => {
    const resolvedFileUrl = fileUrlValue?.trim() ?? "";
    const resolvedIsPdf = isPdfFileUrl(resolvedFileUrl);
    const isCompact = options.compact ?? false;
    const canRenderPreview = Boolean(resolvedFileUrl) && !documentPreviewFailed;
    const imageClassName =
      options.fit === "contain"
        ? "max-h-[22rem] w-auto max-w-full rounded-lg shadow-sm"
        : "max-w-none h-auto w-full rounded-lg shadow-sm";

    return (
      <div
        className={`overflow-hidden rounded-2xl border border-[#b7d8d1] bg-white shadow-sm ${
          isCompact ? "" : "flex h-[calc(100vh-180px)] flex-col"
        }`}
      >
        <div className="flex items-center justify-between border-b border-[#b7d8d1] bg-[#effcf9] px-4 py-3">
          <div className="flex items-center gap-2 font-semibold text-[#005049]">
            <FileText className="h-4 w-4" />
            Hồ sơ gốc
          </div>
          {canRenderPreview ? (
            <button
              type="button"
              onClick={() => setShowFullDoc(true)}
              className="rounded-lg p-1.5 transition hover:bg-[#c5dfd9]"
              title="Xem toàn màn hình"
              aria-label="Xem hồ sơ gốc toàn màn hình"
            >
              <Maximize2 className="h-4 w-4 text-[#00685f]" />
            </button>
          ) : null}
        </div>
        <div
          className={`flex items-start justify-center overflow-auto bg-gray-100 p-4 ${
            isCompact ? "min-h-72 lg:min-h-80" : "flex-1"
          }`}
        >
          {canRenderPreview ? (
            resolvedIsPdf ? (
              <iframe
                src={resolvedFileUrl}
                className="h-full min-h-72 w-full rounded-lg lg:min-h-80"
                title="Hồ sơ gốc PDF"
                sandbox="allow-scripts allow-same-origin allow-downloads"
                referrerPolicy="no-referrer"
                onError={() => setDocumentPreviewFailed(true)}
              />
            ) : (
              /* eslint-disable-next-line @next/next/no-img-element */
              <img
                src={resolvedFileUrl}
                alt="Hồ sơ gốc"
                className={`${imageClassName} cursor-zoom-in`}
                onClick={() => setShowFullDoc(true)}
                onError={() => setDocumentPreviewFailed(true)}
              />
            )
          ) : (
            <div className="flex min-h-56 w-full flex-col items-center justify-center rounded-xl border border-dashed border-[#b7d8d1] bg-white px-6 py-10 text-center">
              <FileText className="h-8 w-8 text-[#8aa09c]" />
              <p className="mt-3 text-sm font-semibold text-[#274d48]">Không thể mở hồ sơ gốc</p>
              <p className="mt-1 max-w-sm text-xs text-[#6d7a77]">
                {options.unavailableReason ?? "Tệp gốc không còn khả dụng hoặc bạn chưa có quyền truy cập tệp này."}
              </p>
            </div>
          )}
        </div>
        <div className="flex items-center gap-2 border-t border-[#b7d8d1] bg-white px-4 py-3 text-xs text-[#4e6360]">
          <AlertOctagon className="h-3 w-3" />
          <span>
            {canRenderPreview
              ? "Dùng con lăn chuột để cuộn dọc xem hết hồ sơ"
              : "Bạn vẫn có thể xem hoặc chỉnh sửa dữ liệu đã trích xuất nếu có quyền."}
          </span>
        </div>
      </div>
    );
  };
  const renderFullscreenDocumentModal = (fileUrlValue: string | null | undefined) => {
    const resolvedFileUrl = fileUrlValue?.trim() ?? "";
    if (!showFullDoc || !resolvedFileUrl || documentPreviewFailed) return null;
    const resolvedIsPdf = isPdfFileUrl(resolvedFileUrl);

    return (
      <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 px-4 py-6 backdrop-blur-sm">
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="source-document-dialog-title"
          className="relative flex max-h-[86vh] w-full max-w-5xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl"
        >
          <div className="flex items-center justify-between border-b border-[#d7e7e3] px-5 py-4">
            <h3 id="source-document-dialog-title" className="text-base font-bold text-[#005049]">
              Hồ sơ gốc
            </h3>
            <button
              onClick={() => setShowFullDoc(false)}
              className="rounded-full p-2 text-[#0f1f1c] transition hover:bg-[#eaf5f2]"
              aria-label="Đóng hồ sơ gốc"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
          <div className="flex min-h-[52vh] flex-1 items-center justify-center overflow-auto bg-[#eef1f3] p-4 sm:p-6">
            {resolvedIsPdf ? (
              <iframe
                src={resolvedFileUrl}
                className="h-[62vh] w-full rounded-lg bg-white shadow-sm"
                title="Hồ sơ gốc PDF"
                sandbox="allow-scripts allow-same-origin allow-downloads"
                referrerPolicy="no-referrer"
                onError={() => setDocumentPreviewFailed(true)}
              />
            ) : (
              /* eslint-disable-next-line @next/next/no-img-element */
              <img
                src={resolvedFileUrl}
                alt="Hồ sơ gốc chi tiết"
                className="max-h-[62vh] max-w-full rounded-lg bg-white object-contain shadow-lg"
                onError={() => setDocumentPreviewFailed(true)}
              />
            )}
          </div>
        </div>
      </div>
    );
  };
  const recordShareModal = (
    <RecordShareModal
      open={showRecordShareModal}
      email={recordShareEmail}
      accessLevel={recordShareAccessLevel}
      members={recordSharedMembers}
      error={recordShareError}
      isLoadingMembers={isRecordSharedMembersLoading}
      isInviting={inviteRecordMutation.isPending}
      isRevoking={revokeRecordShareMutation.isPending}
      isUpdatingAccess={updateRecordShareAccessMutation.isPending}
      pendingRevokeMember={pendingRecordShareRevoke}
      onEmailChange={(nextEmail) => {
        setRecordShareEmail(nextEmail);
        setRecordShareError(null);
      }}
      onAccessLevelChange={(nextAccessLevel) => {
        setRecordShareAccessLevel(nextAccessLevel);
      }}
      onInvite={handleInviteRecordShare}
      onClose={() => {
        if (inviteRecordMutation.isPending || revokeRecordShareMutation.isPending || updateRecordShareAccessMutation.isPending) return;
        setShowRecordShareModal(false);
        setRecordShareEmail("");
        setRecordShareAccessLevel("view");
        setRecordShareError(null);
        setPendingRecordShareRevoke(null);
      }}
      onUpdateMemberAccess={(member, nextAccessLevel) => {
        const currentAccess = member.accessLevel === "edit" ? "edit" : "view";
        if (currentAccess === nextAccessLevel) return;
        updateRecordShareAccessMutation.mutate({
          member,
          accessLevel: nextAccessLevel,
        });
      }}
      onAskRevoke={setPendingRecordShareRevoke}
      onCancelRevoke={() => setPendingRecordShareRevoke(null)}
      onConfirmRevoke={() => {
        if (pendingRecordShareRevoke) {
          revokeRecordShareMutation.mutate(pendingRecordShareRevoke);
        }
      }}
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
      {renderFullscreenDocumentModal(data?.fileUrl)}
    </main>
  );

  if (isLoading || data?.status === "processing") {
    return renderReviewStateShell(
      "Review kết quả khám",
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        {data ? (
          <div className="order-2 lg:order-1 lg:col-span-5 xl:col-span-4">
            {renderOriginalDocumentPreview(data.fileUrl, {
              compact: true,
              unavailableReason: "Tệp gốc chưa sẵn sàng hoặc quyền truy cập tệp chưa được cấp.",
            })}
          </div>
        ) : null}
        <div className={data ? "order-1 lg:order-2 lg:col-span-7 xl:col-span-8" : "lg:col-span-12"}>
          <LoadingState
            title="Hệ thống đang xử lý OCR"
            description="Quá trình này có thể mất một chút thời gian, vui lòng không đóng trang."
          />
        </div>
      </div>
    );
  }

  if (isError) {
    return renderReviewStateShell(
      "Review kết quả khám",
      <ErrorState
        title="Lỗi tải dữ liệu"
        description="Không thể tải kết quả khám. Vui lòng thử lại."
        actionLabel="Thử lại"
        onAction={() => void refetch()}
      />
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
          originalDocumentPreview={renderOriginalDocumentPreview(data.fileUrl, {
            compact: true,
            unavailableReason: "Tệp gốc không còn khả dụng để đối chiếu sau lỗi OCR.",
          })}
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
      <ErrorState
        title="Không tìm thấy dữ liệu hồ sơ"
        description="Kết quả này không còn khả dụng hoặc bạn không có quyền xem."
        actionLabel="Quay lại danh sách"
        onAction={() => router.push("/health-records")}
      />
    );
  }

  const canConfirm = data?.status === "review_required" || (data?.status === "ocr_failed" && manualMode);
  const canToggleEditResults = data?.status === "done" || (data?.status === "ocr_failed" && manualMode);
  const isOwner = data.isOwner ?? true;
  const canEdit = data.canEdit ?? isOwner;
  const canShareRecord = isOwner && data.status === "done";
  const canDeleteRecord = isOwner;
  const showMetricCards = !editMode;
  const showEditableTable = editMode;
  const showConfidenceColumn = canConfirm;
  const fileUrl = data.fileUrl?.trim() ?? "";
  const isDoneView = data.status === "done" && showMetricCards;
  const displayRecordType = recordType?.trim() || "Phiếu khám bệnh";
  const displayExamDate = examDate?.trim() || "Chưa có ngày khám";
  const displayHospitalName = hospitalName?.trim() || "Chưa cập nhật cơ sở y tế";
  const abnormalMetrics = metrics.filter((metric) => metric.status === "abnormal").length;
  const attentionMetrics = metrics.filter((metric) => metric.status === "attention").length;
  const overallSummary =
    abnormalMetrics > 0 ? "Cần theo dõi" : attentionMetrics > 0 ? "Cần chú ý" : "Bình thường";
  const profileDisplayName =
    data.profileDisplayName ??
    (data.profileId ? profiles.find((profile) => profile.id === data.profileId)?.displayName : undefined);
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
                  Kết quả khám của: <span className="font-semibold text-[#3d4947]">{profileOwnerLabel}</span>
                </p>
              </div>
              <div className="flex items-center gap-1.5">
                {fileUrl && !documentPreviewFailed ? (
                  <button
                    type="button"
                    title="Mở hồ sơ gốc"
                    aria-label="Mở hồ sơ gốc"
                    onClick={() => setShowFullDoc(true)}
                    className="inline-flex h-9 items-center justify-center gap-2 rounded-xl bg-[#e9f6f3] px-3 text-sm font-bold text-[#00685f] transition hover:brightness-95"
                  >
                    <FileText className="h-4 w-4" />
                    <span className="hidden sm:inline">Hồ sơ gốc</span>
                  </button>
                ) : (
                  <span className="rounded-xl border border-[#d7e5e1] bg-[#f7fbfa] px-3 py-2 text-xs font-semibold text-[#6d7a77]">
                    Không mở được hồ sơ gốc
                  </span>
                )}
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
                {canShareRecord ? (
                  <button
                    type="button"
                    title="Chia sẻ kết quả"
                    aria-label="Chia sẻ kết quả"
                    onClick={() => setShowRecordShareModal(true)}
                    className="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-[#e9f6f3] text-[#00685f] transition hover:brightness-95"
                  >
                    <Share2 className="h-4 w-4" />
                  </button>
                ) : null}
                <button
                  type="button"
                  title="Tải PDF"
                  aria-label="Tải PDF"
                  onClick={() => void handleDownloadPdf()}
                  disabled={isDownloadingPdf}
                  className="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-[#e9f6f3] text-[#3d4947] transition hover:brightness-95 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {isDownloadingPdf ? <Loader2 className="h-4 w-4 animate-spin" /> : <FileDown className="h-4 w-4" />}
                </button>
                {canDeleteRecord ? (
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
            {pdfDownloadError ? (
              <div className="rounded-xl bg-[#ffdad6] px-4 py-3 text-sm text-[#ba1a1a]">{pdfDownloadError}</div>
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
                      {recommendationDisclaimerText(recommendationsData.disclaimer)}
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
                      onKeyDown={(event) => {
                        if (event.key === "Enter" || event.key === " ") {
                          event.preventDefault();
                          setSelectedMetricIndex(idx);
                        }
                      }}
                    >
                      <div className="mb-5 flex items-start justify-between gap-2">
                        <span className="line-clamp-2 text-xs font-extrabold uppercase text-[#3d4947]">
                          {metric.displayNameVi || metric.name}
                        </span>
                        <CheckCircle className={`h-4 w-4 shrink-0 ${isNormal ? "text-[#00685f]" : "text-[#6d7a77]"}`} />
                      </div>
                      <div className="flex items-end gap-1.5">
                        <span className="text-[44px] leading-none font-black text-[#121e1c]">{metricValue}</span>
                        <span className="pb-1 text-2xs font-semibold text-[#3d4947]">{metricUnit}</span>
                      </div>
                      <div className="mt-4 h-2 w-full rounded-full bg-[#deebe8]">
                        <div
                          className="h-full rounded-full bg-[#008378] transition-all"
                          style={{ width: `${metricPercent}%` }}
                        />
                      </div>
                      <div className="mt-3 flex items-center justify-between gap-3 text-xs font-extrabold uppercase">
                        <span className="truncate text-[#4e6360]">Ngưỡng: {metricRangeText}</span>
                        <span className={isNormal ? "text-[#00685f]" : "text-[#773215]"}>{recordStatusLabel(metric.status)}</span>
                      </div>
                    </article>
                  );
                })}
              </div>
            </section>

            {selectedMetricIndex !== null && metrics[selectedMetricIndex] ? (
              <MetricDetailPopup
                metric={metrics[selectedMetricIndex]}
                metricIndex={selectedMetricIndex}
                metricsLength={metrics.length}
                isExplanationLoading={isExplanationLoading}
                popupExplanation={popupExplanationData?.explanation}
                onClose={() => setSelectedMetricIndex(null)}
                onSelectMetric={setSelectedMetricIndex}
              />
            ) : null}

            {!canEdit ? (
              <p className="rounded-xl border border-[#d7e5e1] bg-white px-4 py-3 text-sm text-[#4e6360]">
                Bạn đang xem hồ sơ ở chế độ chia sẻ. Chỉnh sửa và xóa dữ liệu đã bị vô hiệu hóa.
              </p>
            ) : null}
          </div>
        </div>
        {deleteRecordModal}
        {recordShareModal}
        {renderFullscreenDocumentModal(fileUrl)}
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
          {renderOriginalDocumentPreview(fileUrl)}
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
                  {recommendationDisclaimerText(recommendationsData.disclaimer)}
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
                      setAddErrorField(null);
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
                          className={`hover:bg-gray-50 transition ${metric.confidenceLevel !== "high" ? "bg-[#fff8e8]" : ""
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
                                  className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ${metric.confidenceLevel === "high"
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
      {recordShareModal}

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
                    aria-invalid={addErrorField === "name"}
                    aria-describedby={addErrorField === "name" ? "add-metric-error" : undefined}
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
                    onChange={(e) => {
                      setAddForm((prev) => ({ ...prev, name: e.target.value }));
                      setAddError(null);
                      setAddErrorField(null);
                    }}
                    aria-invalid={addErrorField === "name"}
                    aria-describedby={addErrorField === "name" ? "add-metric-error" : undefined}
                  />
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-[#3d4947] mb-1.5">
                  Giá trị <span className="text-[#ba1a1a]">*</span>
                </label>
                <input
                  type="text"
                  placeholder="VD: 5.4 hoặc Âm tính"
                  className="w-full rounded-xl border border-[#c5dfd9] px-3 py-2.5 text-sm outline-none focus:border-[#008378]"
                  value={addForm.value}
                  aria-invalid={addErrorField === "value"}
                  aria-describedby={addErrorField === "value" ? "add-metric-error" : undefined}
                  onChange={(e) => {
                    setAddForm((prev) => ({ ...prev, value: e.target.value }));
                    setAddError(null);
                    setAddErrorField(null);
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
                  aria-invalid={addErrorField === "unit"}
                  aria-describedby={addErrorField === "unit" ? "add-metric-error" : undefined}
                  onChange={(e) => {
                    setAddForm((prev) => ({ ...prev, unit: e.target.value }));
                    setAddError(null);
                    setAddErrorField(null);
                  }}
                />
              </div>

              {addError && (
                <InlineFieldError id="add-metric-error" message={addError} />
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

      {renderFullscreenDocumentModal(fileUrl)}
    </div>
  );
}

function RecordShareModal({
  open,
  email,
  accessLevel,
  members,
  error,
  isLoadingMembers,
  isInviting,
  isRevoking,
  isUpdatingAccess,
  pendingRevokeMember,
  onEmailChange,
  onAccessLevelChange,
  onInvite,
  onClose,
  onUpdateMemberAccess,
  onAskRevoke,
  onCancelRevoke,
  onConfirmRevoke,
}: {
  open: boolean;
  email: string;
  accessLevel: "view" | "edit";
  members: HealthRecordSharedMember[];
  error: string | null;
  isLoadingMembers: boolean;
  isInviting: boolean;
  isRevoking: boolean;
  isUpdatingAccess: boolean;
  pendingRevokeMember: HealthRecordSharedMember | null;
  onEmailChange: (email: string) => void;
  onAccessLevelChange: (accessLevel: "view" | "edit") => void;
  onInvite: () => void;
  onClose: () => void;
  onUpdateMemberAccess: (member: HealthRecordSharedMember, accessLevel: "view" | "edit") => void;
  onAskRevoke: (member: HealthRecordSharedMember) => void;
  onCancelRevoke: () => void;
  onConfirmRevoke: () => void;
}) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/45 p-4 backdrop-blur-sm">
      <div className="flex max-h-[90vh] w-full max-w-2xl flex-col overflow-hidden rounded-3xl bg-white shadow-2xl">
        <div className="relative p-7 pb-4">
          <button
            type="button"
            onClick={onClose}
            className="absolute right-5 top-5 rounded-full p-2 text-[#6d7a77] transition hover:bg-[#e9f6f3]"
            aria-label="Đóng"
          >
            <X className="h-5 w-5" />
          </button>
          <div className="flex items-start gap-4 pr-12">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-[#e9f6f3] text-[#00685f]">
              <UserPlus className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-2xl font-black leading-tight text-[#121e1c]">Chia sẻ kết quả khám</h2>
              <p className="mt-1 text-sm font-medium text-[#6d7a77]">
                Người nhận chỉ được cấp quyền trên kết quả này, không áp dụng cho toàn bộ lịch sử hồ sơ.
              </p>
            </div>
          </div>
        </div>

        <div className="space-y-5 overflow-y-auto px-7 pb-6">
          <div>
            <label className="mb-2 block text-sm font-bold text-[#121e1c]" htmlFor="record-share-email">
              Địa chỉ email
            </label>
            <div className="relative">
              <Mail className="absolute left-4 top-1/2 h-4.5 w-4.5 -translate-y-1/2 text-[#9ba9a6]" />
              <input
                id="record-share-email"
                type="email"
                autoComplete="email"
                placeholder="ví dụ: email@vidu.com"
                value={email}
                onChange={(event) => onEmailChange(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    event.preventDefault();
                    onInvite();
                  }
                }}
                className="h-12 w-full rounded-2xl border border-[#c5dfd9] pl-12 pr-4 text-sm text-[#3d4947] outline-none focus:border-[#008378]"
              />
            </div>
          </div>

          <div>
            <label className="mb-2 block text-sm font-bold text-[#121e1c]" htmlFor="record-share-access-level">
              Quyền truy cập <span className="text-[#ba1a1a]">*</span>
            </label>
            <div className="relative">
              <Shield className="pointer-events-none absolute left-5 top-1/2 h-5 w-5 -translate-y-1/2 text-[#00685f]" />
              <select
                id="record-share-access-level"
                value={accessLevel}
                onChange={(event) => onAccessLevelChange(event.target.value === "edit" ? "edit" : "view")}
                className="h-12 w-full appearance-none rounded-[28px] border border-[#9ad9cf] bg-[#deebe8] pl-14 pr-12 text-base font-semibold text-[#2d3a38] outline-none focus:border-[#008378]"
              >
                <option value="view">Chỉ xem</option>
                <option value="edit">Có thể chỉnh sửa</option>
              </select>
              <ChevronDown className="pointer-events-none absolute right-5 top-1/2 h-4 w-4 -translate-y-1/2 text-[#00685f]" />
            </div>
          </div>

          {error ? <p className="text-sm font-medium text-[#ba1a1a]">{error}</p> : null}

          <section className="rounded-2xl border border-[#d6ece7] bg-[#f7fcfa] p-4">
            <div className="mb-3 flex items-center justify-between">
              <p className="text-sm font-bold text-[#121e1c]">Người đã được chia sẻ</p>
              {isLoadingMembers || isRevoking || isUpdatingAccess ? (
                <span className="inline-flex items-center gap-1 text-xs font-semibold text-[#6d7a77]">
                  <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  {isRevoking ? "Đang thu hồi..." : isUpdatingAccess ? "Đang cập nhật quyền..." : "Đang tải"}
                </span>
              ) : null}
            </div>
            {members.length === 0 ? (
              <p className="text-sm text-[#6d7a77]">Chưa có ai được cấp quyền xem kết quả này.</p>
            ) : (
              <ul className="max-h-52 space-y-2 overflow-y-auto pr-1">
                {members.map((member) => (
                  (() => {
                    return (
                      <li
                        key={member.id}
                        className="flex items-center justify-between gap-3 rounded-xl border border-[#e2efeb] bg-white px-3 py-2.5"
                      >
                        <div className="min-w-0">
                          <p className="truncate text-sm font-semibold text-[#23312f]">{member.email}</p>
                          <p className="text-xs text-[#6d7a77]">{recordShareStatusLabel(member.status)}</p>
                        </div>
                        <div className="ml-auto flex shrink-0 items-center gap-2">
                          <div className="relative">
                            <select
                              disabled={isUpdatingAccess || isRevoking || member.status === "revoked" || member.status === "expired"}
                              value={member.accessLevel === "edit" ? "edit" : "view"}
                              onChange={(event) => {
                                const nextAction = event.target.value as "view" | "edit" | "revoke";
                                if (nextAction === "revoke") {
                                  onAskRevoke(member);
                                  return;
                                }
                                onUpdateMemberAccess(member, nextAction);
                              }}
                              className="h-8 appearance-none rounded-full border border-[#b7e8e0] bg-[#d7e5e2] px-3 pr-7 text-left text-xs font-bold text-[#00685f] outline-none disabled:cursor-not-allowed disabled:opacity-70"
                            >
                              <option value="view">Chỉ xem</option>
                              <option value="edit">Có thể chỉnh sửa</option>
                              {member.status === "accepted" ? <option value="revoke">Thu hồi quyền truy cập</option> : null}
                            </select>
                            <span className="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 text-[10px] text-[#00685f]">
                              ▾
                            </span>
                          </div>
                        </div>
                      </li>
                    );
                  })()
                ))}
              </ul>
            )}
          </section>
        </div>

        <div className="mt-auto flex items-center justify-end border-t border-[#e8eeec] px-7 py-5">
          <button
            type="button"
            onClick={onInvite}
            disabled={isInviting}
            className="inline-flex items-center gap-2 rounded-2xl bg-[#008378] px-6 py-3 text-sm font-bold text-white shadow-md transition hover:brightness-110 disabled:opacity-70"
          >
            {isInviting ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            Gửi lời mời
          </button>
        </div>
      </div>

      {pendingRevokeMember ? (
        <div className="fixed inset-0 z-[120] flex items-center justify-center bg-black/45 p-4">
          <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl">
            <h3 className="text-lg font-black text-[#121e1c]">Xác nhận thu hồi quyền</h3>
            <p className="mt-2 text-sm text-[#3d4947]">
              Thu hồi quyền xem kết quả của <span className="font-bold">{pendingRevokeMember.email}</span>?
            </p>
            <div className="mt-5 flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={onCancelRevoke}
                disabled={isRevoking}
                className="rounded-xl border border-[#d7e5e1] px-4 py-2 text-sm font-bold text-[#4e6360] hover:bg-[#f7fbfa] disabled:opacity-60"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={onConfirmRevoke}
                disabled={isRevoking}
                className="inline-flex items-center gap-2 rounded-xl bg-[#ba1a1a] px-4 py-2 text-sm font-bold text-white hover:brightness-110 disabled:opacity-70"
              >
                {isRevoking ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
                Thu hồi
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function recordShareStatusLabel(status: string): string {
  if (status === "accepted") return "Đã chấp nhận";
  if (status === "pending") return "Đang chờ";
  if (status === "expired") return "Đã hết hạn";
  if (status === "revoked") return "Đã thu hồi";
  return status;
}

function SourceBadge({ source }: { source: string }) {
  const isManual = source === "manual";
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ${isManual
          ? "bg-[#e8f4ff] text-[#0055aa]"
          : "bg-[#f0fdf4] text-[#166534]"
        }`}
    >
      {isManual ? <PenLine className="h-3 w-3" /> : <ScanLine className="h-3 w-3" />}
      {isManual ? "Nhập tay" : "OCR"}
    </span>
  );
}

function MetricDetailPopup({
  metric,
  metricIndex,
  metricsLength,
  isExplanationLoading,
  popupExplanation,
  onClose,
  onSelectMetric,
}: {
  metric: MetricDto;
  metricIndex: number;
  metricsLength: number;
  isExplanationLoading: boolean;
  popupExplanation?: string;
  onClose: () => void;
  onSelectMetric: (index: number) => void;
}) {
  const metricValue = metric.value?.trim() || "--";
  const metricUnit = metric.unit?.trim() || "";
  const metricPercent = compactMetricPercent(metric);
  const isNormal = (metric.status ?? "no_data") === "normal";
  const displayReference = metric.referenceRange
    ? `${metric.referenceRange.min} - ${metric.referenceRange.max} ${metric.referenceRange.unit ?? metric.unit}`
    : "Không có dữ liệu tham chiếu";
  const rangeContext = metric.rangeContext;
  const contextNote =
    rangeContext && (rangeContext.gender || rangeContext.ageRange)
      ? `Ngưỡng áp dụng cho: ${[
          rangeContext.gender === "female" ? "Nữ" : rangeContext.gender === "male" ? "Nam" : null,
          rangeContext.ageRange ? `${rangeContext.ageRange} tuổi` : null,
        ]
          .filter(Boolean)
          .join(", ")}`
      : null;
  const staticExplanation = metric.explanation?.trim() || "";
  const explanationText = staticExplanation
    ? toThreeLineExplanation(staticExplanation)
    : !isExplanationLoading
      ? toThreeLineExplanation(popupExplanation)
      : "";

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="metric-detail-title"
        className="w-full max-w-md overflow-hidden rounded-3xl bg-white shadow-2xl"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="relative overflow-hidden bg-gradient-to-br from-[#00685f] to-[#008378] px-6 pb-5 pt-6 text-white">
          <div className="relative z-10">
            <div className="flex items-start justify-between gap-3">
              <h3 id="metric-detail-title" className="text-lg font-bold leading-snug">
                {metric.displayNameVi || metric.name}
              </h3>
              <button
                type="button"
                aria-label="Đóng chi tiết chỉ số"
                onClick={onClose}
                className="-mr-1 -mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-white/20 transition hover:bg-white/30"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="mt-4 flex items-end gap-2">
              <span className="text-4xl font-black">{metricValue}</span>
              <span className="pb-0.5 text-sm font-semibold text-white/80">{metricUnit}</span>
            </div>
            <div className="mt-3 h-1.5 w-full rounded-full bg-white/20">
              <div className="h-full rounded-full bg-white/80 transition-all" style={{ width: `${metricPercent}%` }} />
            </div>
            <div className="mt-2 flex items-center justify-between gap-3 text-xs font-bold">
              <span className="truncate text-white/70">Ngưỡng: {compactRangeText(metric)}</span>
              <span className={`rounded-full px-2 py-0.5 ${isNormal ? "bg-white/20 text-white" : "bg-[#ffdad6] text-[#ba1a1a]"}`}>
                {recordStatusLabel(metric.status)}
              </span>
            </div>
          </div>
        </div>

        <div className="space-y-3 px-6 py-5">
          <div className="space-y-2.5 rounded-xl bg-[#f7fbfa] p-4 text-sm leading-relaxed text-[#35514c]">
            <p>
              <span className="font-semibold">Ngưỡng tham chiếu: </span>
              {displayReference}
            </p>
            {metric.referenceRangeSource && metric.referenceRangeSource !== "none" ? (
              <p>
                <span className="font-semibold">Nguồn ngưỡng: </span>
                {metric.referenceRangeSource === "document" ? "Theo phiếu xét nghiệm" : "Theo hệ thống tham chiếu"}
              </p>
            ) : null}
            {metric.referenceRangeSource === "system" ? (
              <p>
                <span className="font-semibold">Ngữ cảnh ngưỡng: </span>
                {contextNote ?? "Ngưỡng tham chiếu chung"}
              </p>
            ) : null}
            {metric.critical ? (
              <p className="rounded-lg bg-[#fff2f2] px-3 py-2 text-[#ba1a1a]">
                Chỉ số có dấu hiệu vượt ngưỡng nguy cấp, nên liên hệ bác sĩ để được tư vấn sớm.
              </p>
            ) : null}
            {isExplanationLoading && !staticExplanation ? (
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
          </div>

          <div className="flex items-center justify-between pt-1">
            <button
              type="button"
              disabled={metricIndex <= 0}
              onClick={() => onSelectMetric(metricIndex - 1)}
              className="inline-flex items-center gap-1 rounded-xl px-3 py-2 text-xs font-semibold text-[#00685f] transition hover:bg-[#e9f6f3] disabled:cursor-not-allowed disabled:opacity-40"
            >
              <ChevronUp className="h-3.5 w-3.5 -rotate-90" />
              Trước
            </button>
            <span className="text-xs text-[#6d7a77]">
              {metricIndex + 1} / {metricsLength}
            </span>
            <button
              type="button"
              disabled={metricIndex >= metricsLength - 1}
              onClick={() => onSelectMetric(metricIndex + 1)}
              className="inline-flex items-center gap-1 rounded-xl px-3 py-2 text-xs font-semibold text-[#00685f] transition hover:bg-[#e9f6f3] disabled:cursor-not-allowed disabled:opacity-40"
            >
              Sau
              <ChevronDown className="h-3.5 w-3.5 -rotate-90" />
            </button>
          </div>
        </div>
      </div>
    </div>
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
