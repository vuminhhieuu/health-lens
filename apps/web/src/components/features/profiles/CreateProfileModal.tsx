"use client";

import React from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { X, UserPlus, Calendar, Info, Loader2 } from "lucide-react";
import { CreateProfileInput, createProfileSchema } from "@healthlens/shared";

interface CreateProfileModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: CreateProfileInput) => void;
  isLoading?: boolean;
}

export function CreateProfileModal({
  isOpen,
  onClose,
  onSubmit,
  isLoading,
}: CreateProfileModalProps) {
  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<CreateProfileInput>({
    resolver: zodResolver(createProfileSchema),
    defaultValues: {
      displayName: "",
      birthDate: "",
      gender: "other",
      notes: "",
    },
  });

  const handleFormSubmit = (data: CreateProfileInput) => {
    const formattedData = {
      ...data,
      birthDate: data.birthDate || null,
      gender: data.gender || null,
      notes: data.notes || null,
    };
    onSubmit(formattedData);
    reset();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-in fade-in duration-200">
      <div 
        className="bg-white w-full max-w-lg rounded-[32px] overflow-hidden shadow-2xl shadow-black/20 flex flex-col max-h-[90vh] animate-in zoom-in-95 duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="relative p-8 pb-4">
          <button 
            onClick={onClose}
            className="absolute top-6 right-6 p-2 rounded-full hover:bg-[#e9f6f3] text-[#6d7a77] transition-colors"
          >
            <X size={20} />
          </button>
          
          <div className="flex items-center gap-4 mb-2">
            <div className="w-12 h-12 rounded-2xl bg-[#e9f6f3] flex items-center justify-center text-[#00685f]">
              <UserPlus size={24} />
            </div>
            <div>
              <h2 className="text-2xl font-black text-[#121e1c]">Tạo hồ sơ mới</h2>
              <p className="text-sm font-medium text-[#6d7a77]">Thêm hồ sơ sức khỏe cho người thân gia đình.</p>
            </div>
          </div>
        </div>

        {/* Form Content */}
        <div className="px-8 pb-8 overflow-y-auto">
          <form id="create-profile-form" onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6">
            
            <div className="space-y-2">
              <label className="text-sm font-bold text-[#6d7a77] ml-1">Họ tên / Tên hiển thị *</label>
              <input 
                {...register("displayName")}
                placeholder="Ví dụ: Bố, Mẹ, Anh Hai..."
                className="w-full h-12 px-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c] outline-none transition-all placeholder:text-[#bcc9c6]"
              />
              {errors.displayName && (
                <p className="text-xs font-bold text-[#ba1a1a] ml-1">{errors.displayName.message}</p>
              )}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <label className="text-sm font-bold text-[#6d7a77] ml-1">Ngày sinh</label>
                <div className="relative">
                  <input 
                    type="date"
                    {...register("birthDate")}
                    className="w-full h-12 px-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c] outline-none transition-all pr-12"
                  />
                  <Calendar className="absolute right-4 top-1/2 -translate-y-1/2 text-[#6d7a77] pointer-events-none w-5 h-5" />
                </div>
                {errors.birthDate && (
                  <p className="text-xs font-bold text-[#ba1a1a] ml-1">{errors.birthDate.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <label className="text-sm font-bold text-[#6d7a77] ml-1">Giới tính</label>
                <Controller
                  name="gender"
                  control={control}
                  render={({ field }) => (
                    <select 
                      {...field}
                      value={field.value ?? ""}
                      className="w-full h-12 px-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c] outline-none transition-all appearance-none cursor-pointer"
                    >
                      <option value="male">Nam</option>
                      <option value="female">Nữ</option>
                      <option value="other">Khác</option>
                    </select>
                  )}
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-bold text-[#6d7a77] ml-1">Ghi chú thêm</label>
              <textarea 
                {...register("notes")}
                placeholder="Mối quan hệ, tình trạng sức khỏe chung..."
                rows={3}
                className="w-full p-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c] outline-none transition-all resize-none placeholder:text-[#bcc9c6]"
              />
              {errors.notes && (
                <p className="text-xs font-bold text-[#ba1a1a] ml-1">{errors.notes.message}</p>
              )}
            </div>

            <div className="flex items-start gap-3 p-4 bg-[#e4f1ee] rounded-2xl text-[#005049]">
              <Info size={20} className="shrink-0 mt-0.5" />
              <p className="text-xs font-semibold leading-relaxed leading-snug">
                Thông tin hồ sơ này sẽ được dùng để cá nhân hóa phân tích kết quả y tế cho từng thành viên.
              </p>
            </div>
          </form>
        </div>

        {/* Footer */}
        <div className="p-8 pt-4 flex items-center justify-end gap-3 bg-[#f8faf9] border-t border-[#bcc9c6]/20">
          <button 
            type="button"
            onClick={onClose}
            className="px-6 py-3 rounded-xl font-bold text-[#3d4947] hover:bg-[#e9f6f3]/80 transition-colors"
          >
            Hủy
          </button>
          <button 
            form="create-profile-form"
            type="submit"
            disabled={isLoading}
            className="px-8 py-3 bg-gradient-to-r from-[#00685f] to-[#008378] text-white rounded-xl font-bold shadow-lg shadow-[#00685f]/20 active:scale-95 transition-all flex items-center gap-2 disabled:opacity-70 disabled:pointer-events-none"
          >
            {isLoading && <Loader2 size={18} className="animate-spin" />}
            {isLoading ? "Đang lưu..." : "Lưu hồ sơ"}
          </button>
        </div>
      </div>
    </div>
  );
}
