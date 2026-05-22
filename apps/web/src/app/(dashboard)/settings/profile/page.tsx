"use client";

import React, { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Pencil,
  Key,
  Shield,
  Trash2,
  User,
  Users,
  X,
} from "lucide-react";

import { apiClient } from "@/lib/api/apiClient";
import { normalizeOptionalTextField } from "@/lib/forms/normalizeOptionalTextField";
import { API_ROUTES } from "@/lib/api/routes";
import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { SettingsDirectContactCard } from "../_components/SettingsDirectContactCard";
import { notify } from "@/lib/notify";
import SafeImage from "@/components/ui/SafeImage";
import { ErrorState, InlineFieldError, LoadingState } from "@/components/ui/StateComponents";
import {
  updateUserProfileSchema,
  updateHealthContextSchema,
  UpdateUserProfileInput,
  UpdateHealthContextInput,
} from "@healthlens/shared";

type UserProfile = {
  id: string;
  email: string;
  fullName: string;
  birthDate: string;
  gender: string;
  emailVerified: boolean;
  avatarUrl?: string | null;
  personalDescription?: string | null;
  personalNotes?: string | null;
  chronicConditions?: string | null;
  currentMedications?: string | null;
  allergies?: string | null;
};

const profileSettingsSchema = updateUserProfileSchema.merge(updateHealthContextSchema);
type ProfileSettingsInput = UpdateUserProfileInput & UpdateHealthContextInput;

const MAX_AVATAR_BYTES = 2 * 1024 * 1024;
const ALLOWED_AVATAR_TYPES = ["image/jpeg", "image/png", "image/webp"];

function getInitials(name?: string | null) {
  const parts = (name ?? "").trim().split(/\s+/).filter(Boolean);

  if (parts.length === 0) {
    return "";
  }

  return parts
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}

export default function ProfileSettingsPage() {
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [pendingAvatarFile, setPendingAvatarFile] = useState<File | null>(null);
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState<string | null>(null);
  const [isAvatarRemovalPending, setIsAvatarRemovalPending] = useState(false);
  const [avatarError, setAvatarError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isValid, isDirty },
  } = useForm<ProfileSettingsInput>({
    resolver: zodResolver(profileSettingsSchema),
    mode: "onBlur",
  });

  const {
    data: userProfile,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["currentUser"],
    queryFn: async () => {
      const response = await apiClient.get(API_ROUTES.USERS.ME);
      return response.data.data as UserProfile;
    },
    refetchInterval: isDirty ? false : 30000,
    refetchOnWindowFocus: true,
  });

  useEffect(() => {
    if (userProfile) {
      reset({
        fullName: userProfile.fullName || "",
        birthDate: userProfile.birthDate || "",
        gender: userProfile.gender || "",
        personalDescription: userProfile.personalDescription || "",
        personalNotes: userProfile.personalNotes || "",
        chronicConditions: userProfile.chronicConditions || "",
        currentMedications: userProfile.currentMedications || "",
        allergies: userProfile.allergies || "",
      });
    }
  }, [userProfile, reset]);

  useEffect(() => {
    return () => {
      if (avatarPreviewUrl) {
        URL.revokeObjectURL(avatarPreviewUrl);
      }
    };
  }, [avatarPreviewUrl]);

  const saveMutation = useMutation({
    mutationFn: async (data: ProfileSettingsInput) => {
      const userPayload = {
        fullName: data.fullName,
        birthDate: data.birthDate ? data.birthDate : null,
        gender: data.gender ? data.gender : null,
        personalDescription: normalizeOptionalTextField(data.personalDescription),
        personalNotes: normalizeOptionalTextField(data.personalNotes),
      };
      const healthPayload = {
        chronicConditions: normalizeOptionalTextField(data.chronicConditions),
        currentMedications: normalizeOptionalTextField(data.currentMedications),
        allergies: normalizeOptionalTextField(data.allergies),
      };
      await apiClient.put(API_ROUTES.USERS.ME, userPayload);
      let latestUserPayload;
      try {
        const healthResponse = await apiClient.put(
          API_ROUTES.USERS.ME_HEALTH_CONTEXT,
          healthPayload,
        );
        latestUserPayload = healthResponse.data;
      } catch (healthError) {
        await queryClient.invalidateQueries({ queryKey: ["currentUser"] });
        throw Object.assign(
          new Error(
            "Đã lưu thông tin cơ bản nhưng không thể lưu thông tin sức khỏe. Vui lòng thử lại.",
          ),
          { cause: healthError },
        );
      }

      if (pendingAvatarFile) {
        const formData = new FormData();
        formData.append("file", pendingAvatarFile);
        const avatarResponse = await apiClient.put(
          API_ROUTES.USERS.ME_AVATAR,
          formData,
        );
        return avatarResponse.data;
      }

      if (isAvatarRemovalPending) {
        const avatarResponse = await apiClient.delete(
          API_ROUTES.USERS.ME_AVATAR,
        );
        return avatarResponse.data;
      }

      return latestUserPayload;
    },
    onSuccess: (payload) => {
      setAvatarError(null);
      setPendingAvatarFile(null);
      setIsAvatarRemovalPending(false);
      setAvatarPreviewUrl(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
      if (payload?.data) {
        queryClient.setQueryData(["currentUser"], payload.data);
      }
      notify.success("Cập nhật thông tin thành công!");
    },
    onError: (error) => {
      console.error("Failed to update profile", error);
      const message =
        error instanceof Error && error.message.includes("thông tin sức khỏe")
          ? error.message
          : "Đã xảy ra lỗi khi cập nhật.";
      notify.error(message);
    },
  });

  const onSubmit = (data: ProfileSettingsInput) => {
    saveMutation.mutate(data);
  };

  const validateAvatarFile = (file: File) => {
    if (!ALLOWED_AVATAR_TYPES.includes(file.type)) {
      return "Chỉ hỗ trợ ảnh JPG, PNG hoặc WebP.";
    }
    if (file.size === 0) {
      return "Ảnh đại diện không được để trống.";
    }
    if (file.size > MAX_AVATAR_BYTES) {
      return "Ảnh đại diện tối đa 2MB.";
    }
    return null;
  };

  const handleAvatarChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }

    const validationError = validateAvatarFile(file);
    if (validationError) {
      setAvatarPreviewUrl(null);
      setAvatarError(validationError);
      notify.error(validationError);
      event.target.value = "";
      return;
    }

    const previewUrl = URL.createObjectURL(file);
    setAvatarPreviewUrl((previous) => {
      if (previous) {
        URL.revokeObjectURL(previous);
      }
      return previewUrl;
    });
    setAvatarError(null);
    setPendingAvatarFile(file);
    setIsAvatarRemovalPending(false);
    event.target.value = "";
  };

  const handleAvatarRemove = () => {
    if (pendingAvatarFile || avatarPreviewUrl) {
      setPendingAvatarFile(null);
      setAvatarPreviewUrl((previous) => {
        if (previous) {
          URL.revokeObjectURL(previous);
        }
        return null;
      });
      setIsAvatarRemovalPending(false);
      setAvatarError(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
      return;
    }

    setIsAvatarRemovalPending(true);
    setAvatarError(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const handleCancel = () => {
    reset();
    setPendingAvatarFile(null);
    setIsAvatarRemovalPending(false);
    setAvatarError(null);
    setAvatarPreviewUrl((previous) => {
      if (previous) {
        URL.revokeObjectURL(previous);
      }
      return null;
    });
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const avatarSrc = isAvatarRemovalPending
    ? null
    : (avatarPreviewUrl ?? userProfile?.avatarUrl ?? null);
  const initials = getInitials(userProfile?.fullName);
  const hasPendingAvatarChange =
    Boolean(pendingAvatarFile) || isAvatarRemovalPending;
  const isSaving = saveMutation.isPending;

  if (isLoading) {
    return (
      <LoadingState
        title="Đang tải thông tin hồ sơ"
        className="min-h-64 border-none bg-transparent shadow-none"
      />
    );
  }
  if (isError) {
    return (
      <ErrorState
        title="Không thể tải thông tin hồ sơ"
        description="Vui lòng thử tải lại trang sau ít phút."
        className="min-h-64 border-none bg-transparent shadow-none"
      />
    );
  }

  return (
    <DashboardPageShell
      title="Hồ sơ của tôi"
      subtitle="Quản lý thông tin cá nhân và cài đặt tài khoản tại một giao diện thống nhất."
    >
      {/* Profile Bento Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        {/* Left Column: Primary Information */}
        <div className="lg:col-span-2 space-y-8">
          {/* Profile Information Card */}
          <section className="bg-white rounded-3xl p-8 shadow-[0_8px_32px_rgba(18,30,28,0.04)] border border-[#bcc9c6]/20 relative overflow-hidden">
            <div className="absolute top-0 right-0 w-32 h-32 bg-[#00685f]/5 rounded-bl-full -mr-16 -mt-16"></div>

            <div className="flex flex-col md:flex-row md:items-center gap-8 mb-10">
              <div className="relative group">
                <div className="w-24 h-24 rounded-2xl overflow-hidden ring-4 ring-[#e9f6f3] shadow-md bg-[#d8e5e2] flex items-center justify-center text-2xl font-black text-[#00685f]">
                  {avatarSrc ? (
                    <SafeImage
                      raw
                      src={avatarSrc}
                      alt="Ảnh đại diện"
                      className="w-full h-full object-cover"
                    />
                  ) : initials ? (
                    <span aria-hidden="true">{initials}</span>
                  ) : (
                    <User className="h-10 w-10" aria-hidden="true" />
                  )}
                </div>
                <button
                  type="button"
                  aria-label="Chọn ảnh đại diện"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={isSaving}
                  className="absolute -bottom-2 -right-2 bg-[#00685f] text-white p-2 rounded-lg shadow-lg active:scale-90 transition-transform disabled:opacity-60"
                >
                  <Pencil className="w-4 h-4" />
                </button>
                {avatarPreviewUrl ||
                  (userProfile?.avatarUrl && !isAvatarRemovalPending) ? (
                  <button
                    type="button"
                    aria-label="Gỡ ảnh đại diện"
                    onClick={handleAvatarRemove}
                    disabled={isSaving}
                    className="absolute -top-2 -right-2 bg-white text-[#ba1a1a] p-2 rounded-lg shadow-lg ring-1 ring-[#ba1a1a]/20 active:scale-90 transition-transform disabled:opacity-60"
                  >
                    <X className="w-4 h-4" />
                  </button>
                ) : null}
              </div>
              <div className="flex-grow">
                <h3 className="text-xl font-bold text-[#121e1c] mb-1">
                  Ảnh đại diện
                </h3>
                <p className="text-sm text-[#6d7a77] mb-4">
                  Cập nhật ảnh để bác sĩ dễ dàng nhận diện bạn hơn.
                </p>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept={ALLOWED_AVATAR_TYPES.join(",")}
                  onChange={handleAvatarChange}
                  className="sr-only"
                  aria-label="Tải ảnh đại diện"
                />
                {pendingAvatarFile ? (
                  <p className="text-sm font-bold text-[#00685f]" role="status">
                    Ảnh sẽ được cập nhật khi lưu.
                  </p>
                ) : null}
                {isAvatarRemovalPending ? (
                  <p className="text-sm font-bold text-[#00685f]" role="status">
                    Ảnh sẽ được gỡ khi lưu.
                  </p>
                ) : null}
                {avatarError ? (
                  <p
                    className="mt-3 text-sm font-medium text-[#ba1a1a]"
                    role="alert"
                    aria-live="assertive"
                  >
                    {avatarError}
                  </p>
                ) : null}
              </div>
            </div>

            <form
              className="grid grid-cols-1 md:grid-cols-2 gap-6"
              onSubmit={handleSubmit(onSubmit)}
            >
              <div className="flex flex-col gap-2">
                <label htmlFor="profile-settings-full-name" className="text-sm font-bold text-[#6d7a77]">
                  Họ tên *
                </label>
                <input
                  id="profile-settings-full-name"
                  type="text"
                  {...register("fullName")}
                  aria-invalid={Boolean(errors.fullName)}
                  aria-describedby={errors.fullName ? "profile-settings-full-name-error" : undefined}
                  className="h-12 px-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium w-full text-[#121e1c]"
                  placeholder="Nhập họ và tên"
                />
                <InlineFieldError
                  id="profile-settings-full-name-error"
                  message={errors.fullName?.message}
                />
              </div>

              <div className="flex flex-col gap-2">
                <label htmlFor="profile-settings-email" className="text-sm font-bold text-[#6d7a77]">
                  Email (Read-only)
                </label>
                <input
                  id="profile-settings-email"
                  type="email"
                  value={userProfile?.email || ""}
                  readOnly
                  className="h-12 px-4 rounded-xl bg-[#d8e5e2]/40 border-none text-[#6d7a77] font-medium cursor-not-allowed w-full"
                />
              </div>

              <div className="flex flex-col gap-2">
                <label htmlFor="profile-settings-birth-date" className="text-sm font-bold text-[#6d7a77]">
                  Ngày sinh
                </label>
                <input
                  id="profile-settings-birth-date"
                  type="date"
                  {...register("birthDate")}
                  aria-invalid={Boolean(errors.birthDate)}
                  aria-describedby={errors.birthDate ? "profile-settings-birth-date-error" : undefined}
                  className="h-12 w-full px-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c]"
                />
                <InlineFieldError
                  id="profile-settings-birth-date-error"
                  message={errors.birthDate?.message}
                />
              </div>

              <div className="flex flex-col gap-2">
                <fieldset>
                  <legend className="text-sm font-bold text-[#6d7a77]">Giới tính</legend>
                  <Controller
                    name="gender"
                    control={control}
                    render={({ field }) => (
                      <div className="flex gap-6 h-12 items-center" role="radiogroup" aria-label="Giới tính">
                        <label htmlFor="profile-settings-gender-male" className="flex items-center gap-3 cursor-pointer group">
                          <input
                            id="profile-settings-gender-male"
                            type="radio"
                            name="profile-settings-gender"
                            value="male"
                            checked={field.value === "male"}
                            onChange={() => field.onChange("male")}
                            className="w-5 h-5 text-[#00685f] border-[#bcc9c6] bg-[#e9f6f3] focus:ring-[#00685f]"
                          />
                          <span className="text-[#121e1c] font-medium group-hover:text-[#00685f] transition-colors">
                            Nam
                          </span>
                        </label>
                        <label htmlFor="profile-settings-gender-female" className="flex items-center gap-3 cursor-pointer group">
                          <input
                            id="profile-settings-gender-female"
                            type="radio"
                            name="profile-settings-gender"
                            value="female"
                            checked={field.value === "female"}
                            onChange={() => field.onChange("female")}
                            className="w-5 h-5 text-[#00685f] border-[#bcc9c6] bg-[#e9f6f3] focus:ring-[#00685f]"
                          />
                          <span className="text-[#121e1c] font-medium group-hover:text-[#00685f] transition-colors">
                            Nữ
                          </span>
                        </label>
                        <label htmlFor="profile-settings-gender-other" className="flex items-center gap-3 cursor-pointer group">
                          <input
                            id="profile-settings-gender-other"
                            type="radio"
                            name="profile-settings-gender"
                            value="other"
                            checked={field.value === "other"}
                            onChange={() => field.onChange("other")}
                            className="w-5 h-5 text-[#00685f] border-[#bcc9c6] bg-[#e9f6f3] focus:ring-[#00685f]"
                          />
                          <span className="text-[#121e1c] font-medium group-hover:text-[#00685f] transition-colors">
                            Khác
                          </span>
                        </label>
                      </div>
                    )}
                  />
                </fieldset>
                <InlineFieldError id="profile-settings-gender-error" message={errors.gender?.message} />
              </div>

              <div className="md:col-span-2 space-y-6 pt-6 border-t border-[#bcc9c6]/20">
                <div>
                  <h3 className="text-lg font-bold text-[#121e1c] mb-1">
                    Mô tả & ghi chú cá nhân
                  </h3>
                  <p className="text-sm text-[#6d7a77] mb-4">
                    Ghi chú tài khoản.
                  </p>
                  <div className="grid grid-cols-1 gap-6">
                    <div className="flex flex-col gap-2">
                      <label htmlFor="profile-settings-personal-description" className="text-sm font-bold text-[#6d7a77]">
                        Mô tả ngắn về bạn
                      </label>
                      <textarea
                        id="profile-settings-personal-description"
                        {...register("personalDescription")}
                        rows={3}
                        aria-invalid={Boolean(errors.personalDescription)}
                        aria-describedby={errors.personalDescription ? "profile-settings-personal-description-error" : undefined}
                        className="w-full p-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c] resize-none"
                        placeholder="Ví dụ: Tôi thường đi khám định kỳ 6 tháng/lần..."
                      />
                      <InlineFieldError
                        id="profile-settings-personal-description-error"
                        message={errors.personalDescription?.message}
                      />
                    </div>
                    <div className="flex flex-col gap-2">
                      <label htmlFor="profile-settings-personal-notes" className="text-sm font-bold text-[#6d7a77]">
                        Ghi chú cá nhân (không thay thế hồ sơ y tế)
                      </label>
                      <textarea
                        id="profile-settings-personal-notes"
                        {...register("personalNotes")}
                        rows={3}
                        aria-invalid={Boolean(errors.personalNotes)}
                        aria-describedby={errors.personalNotes ? "profile-settings-personal-notes-error" : undefined}
                        className="w-full p-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c] resize-none"
                        placeholder="Ghi chú riêng về bạn"
                      />
                      <InlineFieldError
                        id="profile-settings-personal-notes-error"
                        message={errors.personalNotes?.message}
                      />
                    </div>
                  </div>
                </div>

                <div>
                  <h3 className="text-lg font-bold text-[#121e1c] mb-1">
                    Thông tin sức khỏe của tôi
                  </h3>
                  <p className="text-sm text-[#6d7a77] mb-4">
                    Bệnh nền, thuốc và dị ứng.
                  </p>
                  <div className="grid grid-cols-1 gap-6">
                    <div className="flex flex-col gap-2">
                      <label htmlFor="profile-settings-chronic" className="text-sm font-bold text-[#6d7a77]">
                        Bệnh nền / tình trạng lâu dài
                      </label>
                      <textarea
                        id="profile-settings-chronic"
                        {...register("chronicConditions")}
                        rows={2}
                        aria-invalid={Boolean(errors.chronicConditions)}
                        aria-describedby={errors.chronicConditions ? "profile-settings-chronic-error" : undefined}
                        className="w-full p-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c] resize-none"
                      />
                      <InlineFieldError id="profile-settings-chronic-error" message={errors.chronicConditions?.message} />
                    </div>
                    <div className="flex flex-col gap-2">
                      <label htmlFor="profile-settings-medications" className="text-sm font-bold text-[#6d7a77]">
                        Thuốc đang dùng
                      </label>
                      <textarea
                        id="profile-settings-medications"
                        {...register("currentMedications")}
                        rows={2}
                        aria-invalid={Boolean(errors.currentMedications)}
                        aria-describedby={errors.currentMedications ? "profile-settings-medications-error" : undefined}
                        className="w-full p-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c] resize-none"
                      />
                      <InlineFieldError id="profile-settings-medications-error" message={errors.currentMedications?.message} />
                    </div>
                    <div className="flex flex-col gap-2">
                      <label htmlFor="profile-settings-allergies" className="text-sm font-bold text-[#6d7a77]">
                        Dị ứng đã biết
                      </label>
                      <textarea
                        id="profile-settings-allergies"
                        {...register("allergies")}
                        rows={2}
                        aria-invalid={Boolean(errors.allergies)}
                        aria-describedby={errors.allergies ? "profile-settings-allergies-error" : undefined}
                        className="w-full p-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c] resize-none"
                      />
                      <InlineFieldError id="profile-settings-allergies-error" message={errors.allergies?.message} />
                    </div>
                  </div>
                  <p className="mt-4 text-xs text-[#6d7a77]">
                    Thông tin tham khảo — trao đổi với bác sĩ trước khi quyết định điều trị.
                  </p>
                </div>
              </div>

              <div className="md:col-span-2 flex justify-end gap-4 mt-4 pt-6 border-t border-[#bcc9c6]/20">
                <button
                  type="button"
                  onClick={handleCancel}
                  className="px-8 py-3 rounded-xl font-bold text-[#3d4947] hover:bg-[#e9f6f3] transition-colors"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isSaving || (!isValid && !hasPendingAvatarChange)}
                  className="px-8 py-3 bg-gradient-to-r from-[#00685f] to-[#008378] text-white rounded-xl font-bold shadow-lg shadow-[#00685f]/20 active:scale-95 transition-all disabled:opacity-60"
                >
                  {isSaving ? "Đang lưu..." : "Lưu thay đổi"}
                </button>
              </div>
            </form>
          </section>

          <section className="bg-white rounded-3xl p-8 shadow-[0_8px_32px_rgba(18,30,28,0.04)] border border-[#bcc9c6]/20">
            <div className="flex items-center gap-3 mb-4">
              <Users className="text-[#00685f] w-6 h-6" />
              <h2 className="text-xl font-bold text-[#121e1c]">
                Hồ sơ gia đình & lịch sử khám
              </h2>
            </div>
            <p className="text-sm text-[#6d7a77] mb-6 leading-relaxed">
              Bệnh nền, thuốc và dị ứng của người thân được quản lý theo từng hồ sơ gia đình.
            </p>
            <Link
              href="/profiles"
              className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-[#e9f6f3] text-[#00685f] font-bold hover:bg-[#c2ebe3] transition-colors"
            >
              Mở hồ sơ gia đình
            </Link>
          </section>
        </div>

        {/* Right Column: Settings & Support */}
        <div className="space-y-8">
          {/* Account Settings Card */}
          <section className="bg-white rounded-3xl p-8 shadow-[0_8px_32px_rgba(18,30,28,0.04)] border border-[#bcc9c6]/20">
            <div className="flex items-center gap-3 mb-8">
              <Shield className="text-[#00685f] w-6 h-6" />
              <h2 className="text-xl font-bold text-[#121e1c]">
                Cài đặt tài khoản
              </h2>
            </div>

            <div className="space-y-6">
              <Link href="/settings/change-password" className="flex items-center justify-between group py-2">
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-[#e9f6f3] group-hover:bg-[#008378] group-hover:text-white transition-colors">
                    <Key className="w-4 h-4" />
                  </div>
                  <span className="font-medium text-[#3d4947] group-hover:text-[#00685f] transition-colors">
                    Đổi mật khẩu
                  </span>
                </div>
              </Link>
              <Link href="/settings/security" className="flex items-center justify-between group py-2">
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-[#e9f6f3] group-hover:bg-[#008378] group-hover:text-white transition-colors">
                    <Shield className="w-4 h-4" />
                  </div>
                  <span className="font-medium text-[#3d4947] group-hover:text-[#00685f] transition-colors">
                    Xác thực hai yếu tố (2FA)
                  </span>
                </div>
              </Link>
              <div className="pt-6 border-t border-[#bcc9c6]/20">
                <Link
                  href="/settings/delete-account"
                  className="w-full py-3 rounded-xl border-2 border-[#ba1a1a]/20 text-[#ba1a1a] font-bold hover:bg-[#ba1a1a]/5 transition-colors flex items-center justify-center gap-2 block"
                >
                  <Trash2 className="w-4 h-4" /> Xóa tài khoản
                </Link>
              </div>
            </div>
          </section>

          <SettingsDirectContactCard
            title="Cần hỗ trợ?"
            description="Nếu bạn gặp khó khăn khi cập nhật thông tin, liên hệ đội ngũ hỗ trợ."
          />
        </div>
      </div>
    </DashboardPageShell>
  );
}
