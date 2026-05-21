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
import { API_ROUTES } from "@/lib/api/routes";
import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { SettingsDirectContactCard } from "../_components/SettingsDirectContactCard";
import { notify } from "@/lib/notify";
import SafeImage from "@/components/ui/SafeImage";
import {
  updateUserProfileSchema,
  UpdateUserProfileInput,
} from "@healthlens/shared";

type UserProfile = {
  id: string;
  email: string;
  fullName: string;
  birthDate: string;
  gender: string;
  emailVerified: boolean;
  avatarUrl?: string | null;
};

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
  } = useForm<UpdateUserProfileInput>({
    resolver: zodResolver(updateUserProfileSchema),
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
    mutationFn: async (data: UpdateUserProfileInput) => {
      const payload = {
        ...data,
        birthDate: data.birthDate ? data.birthDate : null,
        gender: data.gender ? data.gender : null,
      };
      const profileResponse = await apiClient.put(API_ROUTES.USERS.ME, payload);

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

      return profileResponse.data;
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
      // user-facing feedback below; error handling/telemetry can be added later if needed
      setAvatarError("Không thể lưu ảnh đại diện. Vui lòng thử lại.");
      notify.error("Đã xảy ra lỗi khi cập nhật.");
    },
  });

  const onSubmit = (data: UpdateUserProfileInput) => {
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

  if (isLoading)
    return (
      <div className="p-8 text-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[#00685f] mx-auto"></div>
      </div>
    );
  if (isError)
    return (
      <div className="p-8 text-center text-[#ba1a1a]">
        Không thể tải thông tin hồ sơ.
      </div>
    );

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
                <label className="text-sm font-bold text-[#6d7a77]">
                  Họ tên *
                </label>
                <input
                  type="text"
                  {...register("fullName")}
                  className="h-12 px-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium w-full text-[#121e1c]"
                  placeholder="Nhập họ và tên"
                />
                {errors.fullName && (
                  <p className="text-sm text-[#ba1a1a]">
                    {errors.fullName.message}
                  </p>
                )}
              </div>

              <div className="flex flex-col gap-2">
                <label className="text-sm font-bold text-[#6d7a77]">
                  Email (Read-only)
                </label>
                <input
                  type="email"
                  value={userProfile?.email || ""}
                  readOnly
                  className="h-12 px-4 rounded-xl bg-[#d8e5e2]/40 border-none text-[#6d7a77] font-medium cursor-not-allowed w-full"
                />
              </div>

              <div className="flex flex-col gap-2">
                <label className="text-sm font-bold text-[#6d7a77]">
                  Ngày sinh
                </label>
                <div>
                  <input
                    id="profile-settings-birth-date"
                    type="date"
                    {...register("birthDate")}
                    className="h-12 w-full px-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c]"
                  />
                </div>
                {errors.birthDate && (
                  <p className="text-sm text-[#ba1a1a]">
                    {errors.birthDate.message}
                  </p>
                )}
              </div>

              <div className="flex flex-col gap-2">
                <label className="text-sm font-bold text-[#6d7a77]">
                  Giới tính
                </label>
                <Controller
                  name="gender"
                  control={control}
                  render={({ field }) => (
                    <div className="flex gap-6 h-12 items-center">
                      <label className="flex items-center gap-3 cursor-pointer group">
                        <input
                          type="radio"
                          value="male"
                          checked={field.value === "male"}
                          onChange={() => field.onChange("male")}
                          className="w-5 h-5 text-[#00685f] border-[#bcc9c6] bg-[#e9f6f3] focus:ring-[#00685f]"
                        />
                        <span className="text-[#121e1c] font-medium group-hover:text-[#00685f] transition-colors">
                          Nam
                        </span>
                      </label>
                      <label className="flex items-center gap-3 cursor-pointer group">
                        <input
                          type="radio"
                          value="female"
                          checked={field.value === "female"}
                          onChange={() => field.onChange("female")}
                          className="w-5 h-5 text-[#00685f] border-[#bcc9c6] bg-[#e9f6f3] focus:ring-[#00685f]"
                        />
                        <span className="text-[#121e1c] font-medium group-hover:text-[#00685f] transition-colors">
                          Nữ
                        </span>
                      </label>
                      <label className="flex items-center gap-3 cursor-pointer group">
                        <input
                          type="radio"
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
                {errors.gender && (
                  <p className="text-sm text-[#ba1a1a]">
                    {errors.gender.message}
                  </p>
                )}
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
              Bệnh nền, thuốc và dị ứng được quản lý theo từng hồ sơ gia đình — không
              nhập tại trang cài đặt tài khoản.
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
              <div
                className="flex items-center justify-between py-2"
                aria-describedby="profile-2fa-coming-soon-hint"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-[#e9f6f3]">
                    <Shield className="w-4 h-4" />
                  </div>
                  <span className="font-medium text-[#3d4947]">
                    Xác thực hai yếu tố (2FA)
                  </span>
                </div>
                <Link
                  href="/settings"
                  className="text-xs font-bold uppercase tracking-wide text-[#6d7a77] bg-[#e9f6f3] px-3 py-1 rounded-full hover:bg-[#c2ebe3] hover:text-[#00685f] transition-colors"
                  title="Xác thực hai yếu tố sẽ có trong bản cập nhật tiếp theo"
                >
                  Sắp có
                </Link>
                <span id="profile-2fa-coming-soon-hint" className="sr-only">
                  Tính năng đang được chuẩn bị. Bạn sẽ bật xác thực hai yếu tố tại
                  trang cài đặt khi ra mắt.
                </span>
              </div>
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
