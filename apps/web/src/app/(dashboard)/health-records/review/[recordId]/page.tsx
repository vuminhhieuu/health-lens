"use client";

import { useEffect, useMemo, useState, useRef } from "react";
import { useParams, useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Loader2, AlertTriangle, CheckCircle, Edit2, Save, X, AlertCircle, FileText, Maximize2, AlertOctagon } from "lucide-react";

import { apiClient } from "@/lib/api/apiClient";
import { ApiPaths } from "@healthlens/shared/constants";

type MetricDto = {
  name: string;
  value: string;
  unit: string;
  confidence: number;
  confidenceLevel: "high" | "medium" | "low";
  source: string;
};

type ReviewRecordStatus = "processing" | "review_required" | "done" | "ocr_failed";

type ReviewRecordData = {
  status: ReviewRecordStatus;
  fileUrl?: string;
  metrics?: MetricDto[];
  examDate?: string | null;
  recordType?: string | null;
  hospitalName?: string | null;
  diagnosis?: string | null;
};

export default function ReviewRecordPage() {
  const params = useParams();
  const router = useRouter();
  const recordId = params.recordId as string;

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

  const { data, refetch, isLoading, isError } = useQuery<ReviewRecordData>({
    queryKey: ["record-status", recordId],
    queryFn: async () => {
      const res = await apiClient.get(ApiPaths.HEALTH_RECORDS.STATUS(recordId));
      return res.data?.data;
    },
    refetchInterval: (query) => {
      if (query.state.data?.status === "processing") return 3000;
      return false;
    },
  });

  const initialized = useRef(false);
  const initialSnapshotRef = useRef<string>("");

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
    }
  }, [data]);

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
    if (editingIndex !== null && editForm) {
      const updatedMetrics: MetricDto[] = [...metrics];
      updatedMetrics[editingIndex] = {
        ...editForm,
        confidenceLevel: "high" as const,
        confidence: 1.0,
      };
      setMetrics(updatedMetrics);
      setEditingIndex(null);
      setEditForm(null);
    }
  };

  const handleCancelEdit = () => {
    setEditingIndex(null);
    setEditForm(null);
  };

  const executeSave = async (autoApproveAll = false) => {
    try {
      setIsSaving(true);
      setSaveError(null);
      setShowConfirmModal(false);

      const finalMetrics: MetricDto[] = autoApproveAll
        ? metrics.map(
            (m): MetricDto => ({
              ...m,
              confidenceLevel: "high" as const,
              confidence: 1.0,
            }),
          )
        : metrics;

      await apiClient.post(ApiPaths.HEALTH_RECORDS.CONFIRM_RECORD(recordId), {
        examDate: examDate || null,
        recordType: recordType || null,
        hospitalName: hospitalName || null,
        diagnosis: diagnosis || null,
        metrics: finalMetrics,
      });
      initialSnapshotRef.current = buildSnapshot({
        metrics: finalMetrics,
        examDate,
        recordType,
        hospitalName,
        diagnosis,
      });
      alert("Lưu kết quả khám thành công!");
      router.push("/health-records");
    } catch {
      setSaveError("Đã có lỗi xảy ra khi lưu. Vui lòng thử lại.");
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading || data?.status === "processing") {
    return (
      <div className="mx-auto flex min-h-screen w-full max-w-7xl flex-col items-center justify-center gap-6 bg-[#effcf9] px-6 py-10">
        <Loader2 className="h-10 w-10 animate-spin text-[#00685f]" />
        <h2 className="text-xl font-bold text-[#005049]">Hệ thống đang xử lý OCR</h2>
        <p className="text-[#4e6360]">Quá trình này có thể mất một chút thời gian, vui lòng không đóng trang...</p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="mx-auto flex min-h-screen w-full max-w-7xl flex-col items-center justify-center gap-6 bg-[#effcf9] px-6 py-10">
        <AlertTriangle className="h-10 w-10 text-[#ba1a1a]" />
        <h2 className="text-xl font-bold text-[#ba1a1a]">Lỗi tải dữ liệu</h2>
        <button onClick={() => refetch()} className="rounded-xl bg-[#00685f] px-6 py-2 text-white">
          Thử lại
        </button>
      </div>
    );
  }

  if (data?.status === "ocr_failed") {
    return (
      <div className="mx-auto flex min-h-screen w-full max-w-7xl flex-col items-center justify-center gap-6 bg-[#effcf9] px-6 py-10 text-center">
        <AlertTriangle className="h-10 w-10 text-[#ba1a1a]" />
        <h2 className="text-xl font-bold text-[#ba1a1a]">Không thể xử lý OCR cho hồ sơ này</h2>
        <p className="max-w-xl text-[#4e6360]">
          Hệ thống không trích xuất được dữ liệu từ tệp đã tải lên. Vui lòng quay lại danh sách hồ sơ và tải lại tệp rõ nét hơn.
        </p>
        <div className="flex gap-3">
          <button
            onClick={() => refetch()}
            className="rounded-xl border border-[#00685f] px-6 py-2 font-semibold text-[#00685f] hover:bg-[#effcf9]"
          >
            Thử lại
          </button>
          <button
            onClick={() => router.push("/health-records")}
            className="rounded-xl bg-[#00685f] px-6 py-2 font-semibold text-white hover:brightness-110"
          >
            Quay lại danh sách
          </button>
        </div>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="mx-auto flex min-h-screen w-full max-w-7xl flex-col items-center justify-center gap-6 bg-[#effcf9] px-6 py-10 text-center">
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

  const canConfirm = data?.status === "review_required";
  const fileUrl = data.fileUrl ?? "";
  const isPdf = fileUrl.toLowerCase().includes(".pdf");

  return (
    <div className="mx-auto min-h-screen w-full max-w-[1600px] bg-[#effcf9] px-6 py-10">
      <header className="mb-8">
        <h1 className="text-3xl font-bold text-[#005049]">Kiểm tra kết quả trích xuất</h1>
        <p className="mt-2 text-sm text-[#4e6360]">
          Vui lòng so sánh với hồ sơ gốc và điều chỉnh các chỉ số nếu cần thiết trước khi lưu.
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
              />
            </div>
            <div className="rounded-2xl border border-[#b7d8d1] bg-white p-6 shadow-sm">
              <label className="block text-sm font-medium text-[#3d4947]">Loại phiếu (Vd: Xét nghiệm máu...)</label>
              <input
                type="text"
                placeholder="Loại phiếu khám..."
                className="mt-2 w-full rounded-xl border border-[#c5dfd9] px-3 py-2 outline-none focus:border-[#008378]"
                value={recordType}
                onChange={(e) => setRecordType(e.target.value)}
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
            />
          </div>

          <div className="rounded-2xl border border-[#b7d8d1] bg-white shadow-sm overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-[#4e6360]">
                <thead className="bg-[#effcf9] text-[#005049]">
                  <tr>
                    <th className="px-6 py-4 font-semibold">Chỉ số</th>
                    <th className="px-6 py-4 font-semibold">Giá trị</th>
                    <th className="px-6 py-4 font-semibold">Đơn vị</th>
                    <th className="px-6 py-4 font-semibold">Độ tin cậy</th>
                    <th className="px-6 py-4 font-semibold text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#c5dfd9]">
                  {metrics.map((metric, idx) => {
                    const isEditing = editingIndex === idx;

                    return (
                      <tr key={idx} className={`hover:bg-gray-50 transition ${metric.confidenceLevel === 'low' ? 'bg-red-50/30' : ''}`}>
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
                              {metric.confidenceLevel === "high" ? "Cao" : metric.confidenceLevel === "medium" ? "TB" : "Thấp"}
                            </span>
                          )}
                        </td>
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
                            <button onClick={() => handleEditClick(idx)} className="text-[#4e6360] hover:text-[#005049]">
                              <Edit2 className="h-5 w-5" />
                            </button>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                  {metrics.length === 0 && (
                    <tr>
                      <td colSpan={5} className="px-6 py-8 text-center text-[#4e6360]">
                        Không tìm thấy chỉ số nào từ kết quả OCR.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {saveError && (
            <div className="rounded-xl bg-[#ffdad6] p-4 text-sm text-[#ba1a1a] flex items-center gap-2">
              <AlertCircle className="h-5 w-5" />
              {saveError}
            </div>
          )}

          <div className="flex justify-end pt-4 pb-12">
            <button
              onClick={() => setShowConfirmModal(true)}
              disabled={isSaving || !canConfirm}
              className="inline-flex items-center gap-2 rounded-xl bg-[#00685f] px-10 py-4 font-bold text-white shadow-lg transition hover:brightness-110 hover:translate-y-[-2px] disabled:opacity-70 disabled:transform-none"
            >
              {isSaving ? <Loader2 className="h-5 w-5 animate-spin" /> : <Save className="h-5 w-5" />}
              XÁC NHẬN VÀ LƯU HỒ SƠ
            </button>
          </div>
          {!canConfirm && (
            <p className="text-right text-sm text-[#6d7a77]">
              Hồ sơ hiện không ở trạng thái có thể xác nhận.
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
                  Xác nhận và Lưu kết quả
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
