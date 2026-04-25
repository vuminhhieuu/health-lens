"use client";

import { type ChangeEvent, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import axios from "axios";
import { ApiPaths, ALLOWED_FILE_TYPES, UPLOAD_MAX_SIZE_BYTES } from "@healthlens/shared/constants";
import { Upload, Loader2, AlertCircle } from "lucide-react";

import { apiClient } from "@/lib/api/apiClient";

type UploadStatus = "idle" | "uploading" | "done" | "error";

interface UploadButtonProps {
  profileId: string;
}

export function UploadButton({ profileId }: UploadButtonProps) {
  const router = useRouter();
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [status, setStatus] = useState<UploadStatus>("idle");
  const [error, setError] = useState<string | null>(null);
  const [uploadProgress, setUploadProgress] = useState(0);

  const validateFile = (file: File) => {
    if (!ALLOWED_FILE_TYPES.includes(file.type as (typeof ALLOWED_FILE_TYPES)[number])) {
      return "Chỉ chấp nhận file PDF/JPG/PNG.";
    }
    if (file.size > UPLOAD_MAX_SIZE_BYTES) {
      return "File vượt quá giới hạn 20MB.";
    }
    return null;
  };

  const handleSelect = () => fileInputRef.current?.click();



  const handleFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setError(null);
    const validationError = validateFile(file);
    if (validationError) {
      setStatus("error");
      setError(validationError);
      return;
    }

    try {
      setStatus("uploading");
      setUploadProgress(0);

      const fileType = file.type === "application/pdf" ? "pdf" : file.type;
      const uploadInfoResp = await apiClient.post(ApiPaths.HEALTH_RECORDS.UPLOAD_URL, { profileId, fileType });
      const uploadInfo = uploadInfoResp.data?.data as { uploadUrl: string; recordId: string };

      await axios.put(uploadInfo.uploadUrl, file, {
        headers: { "Content-Type": file.type },
        onUploadProgress: (progressEvent) => {
          if (!progressEvent.total) return;
          setUploadProgress(Math.round((progressEvent.loaded / progressEvent.total) * 100));
        },
      });

      await apiClient.post(ApiPaths.HEALTH_RECORDS.CONFIRM_UPLOAD(uploadInfo.recordId));
      setStatus("done");
      // Redirect to review page
      router.push(`/health-records/review/${uploadInfo.recordId}`);
    } catch {
      setStatus("error");
      setError("Upload thất bại. Vui lòng thử lại.");
    } finally {
      event.target.value = "";
    }
  };

  return (
    <div className="space-y-4">
      <input
        ref={fileInputRef}
        type="file"
        className="hidden"
        accept="application/pdf,image/jpeg,image/png"
        onChange={handleFileChange}
      />

      <button
        onClick={handleSelect}
        disabled={status === "uploading"}
        className="inline-flex w-full sm:w-auto items-center justify-center gap-2 rounded-xl bg-[#00685f] px-6 py-3 font-semibold text-white transition hover:brightness-110 disabled:opacity-70 shadow-sm"
      >
        {status === "uploading" ? <Loader2 className="h-5 w-5 animate-spin" /> : <Upload className="h-5 w-5" />}
        Tải lên PDF/JPG/PNG
      </button>

      {status === "uploading" && (
        <div className="space-y-2">
          <p className="text-sm font-medium text-[#3d4947]">Đang tải lên: {uploadProgress}%</p>
          <div className="h-2 overflow-hidden rounded-full bg-[#d6ebe7]">
            <div className="h-full bg-[#008378] transition-all" style={{ width: `${uploadProgress}%` }} />
          </div>
        </div>
      )}

      {status === "done" && <p className="text-sm font-medium text-[#0f766e]">Upload thành công, hệ thống đang xử lý kết quả.</p>}

      {status === "error" && (
        <p className="inline-flex items-center gap-2 text-sm font-medium text-[#ba1a1a]">
          <AlertCircle className="h-4 w-4" />
          {error}
        </p>
      )}
    </div>
  );
}
