"use client";

import React, { useEffect } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { X, FileEdit, Info, Loader2 } from "lucide-react";
import { UpdateProfileInput, updateProfileSchema } from "@healthlens/shared";
import type { ProfileGender } from "@healthlens/shared";

import { InlineFieldError } from "@/components/ui/StateComponents";

function normalizeOptionalTextField(value?: string | null): string | null {
  if (value == null) {
    return null;
  }
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

type EditableProfile = {
  id: string;
  displayName: string;
  birthDate?: string;
  gender?: string | null;
  notes?: string;
  chronicConditions?: string;
  currentMedications?: string;
  allergies?: string;
};

const isGender = (value: unknown): value is ProfileGender =>
  value === "male" || value === "female" || value === "other";

const normalizeGender = (
  value?: string | null,
): UpdateProfileInput["gender"] => {
  if (isGender(value)) {
    return value;
  }

  return "other";
};

interface EditProfileModalProps {
  isOpen: boolean;
  profile: EditableProfile | null;
  onClose: () => void;
  onSubmit: (profileId: string, data: UpdateProfileInput) => void;
  isLoading?: boolean;
  isReadOnly?: boolean;
}

export function EditProfileModal({
  isOpen,
  profile,
  onClose,
  onSubmit,
  isLoading,
  isReadOnly = false,
}: EditProfileModalProps) {
  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<UpdateProfileInput>({
    resolver: zodResolver(updateProfileSchema),
    defaultValues: {
      displayName: "",
      birthDate: "",
      gender: "other",
      notes: "",
      chronicConditions: "",
      currentMedications: "",
      allergies: "",
    },
  });

  useEffect(() => {
    if (!profile) {
      return;
    }

    reset({
      displayName: profile.displayName || "",
      birthDate: profile.birthDate || "",
      gender: normalizeGender(profile.gender),
      notes: profile.notes || "",
      chronicConditions: profile.chronicConditions || "",
      currentMedications: profile.currentMedications || "",
      allergies: profile.allergies || "",
    });
  }, [profile, reset]);

  const handleFormSubmit = (data: UpdateProfileInput) => {
    if (!profile) {
      return;
    }

    const formattedData: UpdateProfileInput = {
      ...data,
      birthDate: data.birthDate || null,
      gender: data.gender || null,
      notes: normalizeOptionalTextField(data.notes),
      chronicConditions: normalizeOptionalTextField(data.chronicConditions),
      currentMedications: normalizeOptionalTextField(data.currentMedications),
      allergies: normalizeOptionalTextField(data.allergies),
    };

    onSubmit(profile.id, formattedData);
  };

  if (!isOpen || !profile) return null;

  return (
    <div className="fixed inset-0 z-100 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className="bg-white w-full max-w-lg rounded-4xl overflow-hidden shadow-2xl shadow-black/20 flex flex-col max-h-[90vh] animate-in zoom-in-95 duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="relative p-8 pb-4">
          <button
            onClick={onClose}
            className="absolute top-6 right-6 p-2 rounded-full hover:bg-[#e9f6f3] text-[#6d7a77] transition-colors"
          >
            <X size={20} />
          </button>

          <div className="flex items-center gap-4 mb-2">
            <div className="w-12 h-12 rounded-2xl bg-[#e9f6f3] flex items-center justify-center text-[#00685f]">
              <FileEdit size={24} />
            </div>
            <div>
              <h2 className="text-2xl font-black text-[#121e1c]">
                {isReadOnly ? "Thông tin hồ sơ" : "Chỉnh sửa hồ sơ"}
              </h2>
              <p className="text-sm font-medium text-[#6d7a77]">
                {isReadOnly 
                  ? "Thông tin chi tiết về hồ sơ sức khỏe này." 
                  : "Cập nhật tên hiển thị và ghi chú để dễ phân biệt hồ sơ."}
              </p>
            </div>
          </div>
        </div>

        <div className="px-8 pb-8 overflow-y-auto">
          <form
            id="edit-profile-form"
            onSubmit={handleSubmit(handleFormSubmit)}
            className="space-y-6"
          >
            <div className="space-y-2">
              <label htmlFor="edit-profile-display-name" className="text-sm font-bold text-[#6d7a77] ml-1">
                Tên hiển thị *
              </label>
              <input
                id="edit-profile-display-name"
                {...register("displayName")}
                readOnly={isReadOnly}
                aria-invalid={Boolean(errors.displayName)}
                aria-describedby={errors.displayName ? "edit-profile-display-name-error" : undefined}
                placeholder="Ví dụ: Mẹ, Bố..."
                className={`w-full h-12 px-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c] outline-none transition-all placeholder:text-[#bcc9c6] ${isReadOnly ? "cursor-default" : ""}`}
              />
              <InlineFieldError
                id="edit-profile-display-name-error"
                message={errors.displayName?.message}
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <label htmlFor="edit-profile-birth-date" className="text-sm font-bold text-[#6d7a77] ml-1">
                  Ngày sinh
                </label>
                <input
                  id="edit-profile-birth-date"
                  type="date"
                  {...register("birthDate")}
                  readOnly={isReadOnly}
                  aria-invalid={Boolean(errors.birthDate)}
                  aria-describedby={errors.birthDate ? "edit-profile-birth-date-error" : undefined}
                  className={`w-full h-12 px-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c] outline-none transition-all ${isReadOnly ? "cursor-default" : ""}`}
                />
                <InlineFieldError id="edit-profile-birth-date-error" message={errors.birthDate?.message} />
              </div>

              <div className="space-y-2">
                <label htmlFor="edit-profile-gender" className="text-sm font-bold text-[#6d7a77] ml-1">
                  Giới tính
                </label>
                <Controller
                  name="gender"
                  control={control}
                  render={({ field }) => (
                    <select
                      id="edit-profile-gender"
                      {...field}
                      disabled={isReadOnly}
                      value={field.value ?? ""}
                      aria-invalid={Boolean(errors.gender)}
                      aria-describedby={errors.gender ? "edit-profile-gender-error" : undefined}
                      className={`w-full h-12 px-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c] outline-none transition-all appearance-none ${isReadOnly ? "cursor-default" : "cursor-pointer"}`}
                    >
                      <option value="male">Nam</option>
                      <option value="female">Nữ</option>
                      <option value="other">Khác</option>
                    </select>
                  )}
                />
                <InlineFieldError id="edit-profile-gender-error" message={errors.gender?.message} />
              </div>
            </div>

            <div className="space-y-2">
              <label htmlFor="edit-profile-chronic" className="text-sm font-bold text-[#6d7a77] ml-1">
                Bệnh nền / tình trạng lâu dài
              </label>
              <textarea
                id="edit-profile-chronic"
                {...register("chronicConditions")}
                readOnly={isReadOnly}
                aria-invalid={Boolean(errors.chronicConditions)}
                aria-describedby={errors.chronicConditions ? "edit-profile-chronic-error" : undefined}
                placeholder="Ví dụ: Tiểu đường type 2..."
                rows={2}
                className={`w-full p-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c] outline-none transition-all resize-none placeholder:text-[#bcc9c6] ${isReadOnly ? "cursor-default" : ""}`}
              />
              <InlineFieldError id="edit-profile-chronic-error" message={errors.chronicConditions?.message} />
            </div>

            <div className="space-y-2">
              <label htmlFor="edit-profile-medications" className="text-sm font-bold text-[#6d7a77] ml-1">
                Thuốc đang dùng
              </label>
              <textarea
                id="edit-profile-medications"
                {...register("currentMedications")}
                readOnly={isReadOnly}
                aria-invalid={Boolean(errors.currentMedications)}
                aria-describedby={errors.currentMedications ? "edit-profile-medications-error" : undefined}
                placeholder="Ví dụ: Metformin 500mg..."
                rows={2}
                className={`w-full p-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c] outline-none transition-all resize-none placeholder:text-[#bcc9c6] ${isReadOnly ? "cursor-default" : ""}`}
              />
              <InlineFieldError id="edit-profile-medications-error" message={errors.currentMedications?.message} />
            </div>

            <div className="space-y-2">
              <label htmlFor="edit-profile-allergies" className="text-sm font-bold text-[#6d7a77] ml-1">
                Dị ứng đã biết
              </label>
              <textarea
                id="edit-profile-allergies"
                {...register("allergies")}
                readOnly={isReadOnly}
                aria-invalid={Boolean(errors.allergies)}
                aria-describedby={errors.allergies ? "edit-profile-allergies-error" : undefined}
                placeholder="Ví dụ: Penicillin..."
                rows={2}
                className={`w-full p-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c] outline-none transition-all resize-none placeholder:text-[#bcc9c6] ${isReadOnly ? "cursor-default" : ""}`}
              />
              <InlineFieldError id="edit-profile-allergies-error" message={errors.allergies?.message} />
            </div>

            <div className="space-y-2">
              <label htmlFor="edit-profile-notes" className="text-sm font-bold text-[#6d7a77] ml-1">
                Ghi chú thêm
              </label>
              <textarea
                id="edit-profile-notes"
                {...register("notes")}
                readOnly={isReadOnly}
                aria-invalid={Boolean(errors.notes)}
                aria-describedby={errors.notes ? "edit-profile-notes-error" : undefined}
                placeholder="Ví dụ: Tiểu đường type 2, cần theo dõi huyết áp..."
                rows={4}
                className={`w-full p-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c] outline-none transition-all resize-none placeholder:text-[#bcc9c6] ${isReadOnly ? "cursor-default" : ""}`}
              />
              <InlineFieldError id="edit-profile-notes-error" message={errors.notes?.message} />
            </div>

            <div className="flex items-start gap-3 p-4 bg-[#e4f1ee] rounded-2xl text-[#005049]">
              <Info size={20} className="shrink-0 mt-0.5" />
              <p className="text-xs font-semibold leading-relaxed">
                Ghi chú giúp bạn nhận diện nhanh hồ sơ khi có nhiều thành viên
                trong gia đình.
              </p>
            </div>
          </form>
        </div>

        <div className="p-8 pt-4 flex items-center justify-end gap-3 bg-[#f8faf9] border-t border-[#bcc9c6]/20">
          <button
            type="button"
            onClick={onClose}
            className="px-6 py-3 rounded-xl font-bold text-[#3d4947] hover:bg-[#e9f6f3]/80 transition-colors"
          >
            {isReadOnly ? "Đóng" : "Hủy"}
          </button>
          {!isReadOnly && (
            <button
              form="edit-profile-form"
              type="submit"
              disabled={isLoading}
              className="px-8 py-3 bg-linear-to-r from-[#00685f] to-[#008378] text-white rounded-xl font-bold shadow-lg shadow-[#00685f]/20 active:scale-95 transition-all flex items-center gap-2 disabled:opacity-70 disabled:pointer-events-none"
            >
              {isLoading && <Loader2 size={18} className="animate-spin" />}
              {isLoading ? "Đang lưu..." : "Lưu thay đổi"}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
