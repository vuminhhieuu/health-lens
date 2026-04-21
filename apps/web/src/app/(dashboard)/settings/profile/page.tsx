"use client";

import React, { useEffect, useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Box, Button, Callout } from "@radix-ui/themes";
import { 
  Pencil, Calendar, Cross, Key, Shield, Trash2, CheckCircle2, InfoIcon 
} from "lucide-react";

import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { updateUserProfileSchema, UpdateUserProfileInput } from "@healthlens/shared";

type UserProfile = {
  id: string;
  email: string;
  fullName: string;
  birthDate: string;
  gender: string;
  emailVerified: boolean;
};

export default function ProfileSettingsPage() {
  const queryClient = useQueryClient();
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const { data: userProfile, isLoading, isError } = useQuery({
    queryKey: ["currentUser"],
    queryFn: async () => {
      const response = await apiClient.get(API_ROUTES.USERS.ME);
      return response.data.data as UserProfile;
    },
  });

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isValid, isSubmitting },
  } = useForm<UpdateUserProfileInput>({
    resolver: zodResolver(updateUserProfileSchema),
    mode: "onBlur",
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

  const updateMutation = useMutation({
    mutationFn: async (data: UpdateUserProfileInput) => {
      const payload = {
        ...data,
        birthDate: data.birthDate ? data.birthDate : null,
      };
      const response = await apiClient.put(API_ROUTES.USERS.ME, payload);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["currentUser"] });
      setSuccessMessage("Cập nhật thông tin thành công!");
      setTimeout(() => setSuccessMessage(null), 3000);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    onError: (error: unknown) => {
      console.error(error);
      alert("Đã xảy ra lỗi khi cập nhật.");
    },
  });

  const onSubmit = (data: UpdateUserProfileInput) => {
    updateMutation.mutate(data);
  };

  if (isLoading) return <div className="p-8 text-center"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[#00685f] mx-auto"></div></div>;
  if (isError) return <div className="p-8 text-center text-[#ba1a1a]">Không thể tải thông tin hồ sơ.</div>;

  return (
    <div className="flex-grow p-6 md:p-12 lg:p-16 max-w-7xl mx-auto bg-[#effcf9] min-h-screen text-[#121e1c]">
      
      {/* Breadcrumbs & Header */}
      <div className="mb-10">
        <nav className="flex text-sm text-[#6d7a77] mb-2">
          <span className="hover:text-[#00685f] cursor-pointer transition-colors">Dashboard</span>
          <span className="mx-2">/</span>
          <span className="text-[#121e1c] font-medium">Hồ sơ của tôi</span>
        </nav>
        <h1 className="text-4xl font-extrabold tracking-tight text-[#121e1c]">Hồ sơ của tôi</h1>
      </div>

      {successMessage && (
        <Callout.Root color="green" mb="6" className="bg-[#e4f1ee] border border-[#00685f]/20 shadow-sm rounded-xl py-3 px-4 flex items-center gap-3">
          <Callout.Icon>
            <CheckCircle2 size={20} className="text-[#00685f]" />
          </Callout.Icon>
          <Callout.Text className="text-[#005049] font-semibold">{successMessage}</Callout.Text>
        </Callout.Root>
      )}

      {/* Profile Bento Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        
        {/* Left Column: Primary Information */}
        <div className="lg:col-span-2 space-y-8">
          
          {/* Profile Information Card */}
          <section className="bg-white rounded-3xl p-8 shadow-[0_8px_32px_rgba(18,30,28,0.04)] border border-[#bcc9c6]/20 relative overflow-hidden">
            <div className="absolute top-0 right-0 w-32 h-32 bg-[#00685f]/5 rounded-bl-full -mr-16 -mt-16"></div>
            
            <div className="flex flex-col md:flex-row md:items-center gap-8 mb-10">
              <div className="relative group">
                <div className="w-24 h-24 rounded-2xl overflow-hidden ring-4 ring-[#e9f6f3] shadow-md bg-[#d8e5e2]">
                  <img alt="Avatar" className="w-full h-full object-cover" src="https://ui-avatars.com/api/?name=H+L&background=00685f&color=fff&size=256" />
                </div>
                <button className="absolute -bottom-2 -right-2 bg-[#00685f] text-white p-2 rounded-lg shadow-lg active:scale-90 transition-transform">
                  <Pencil className="w-4 h-4" />
                </button>
              </div>
              <div className="flex-grow">
                <h3 className="text-xl font-bold text-[#121e1c] mb-1">Ảnh đại diện</h3>
                <p className="text-sm text-[#6d7a77] mb-4">Cập nhật ảnh để bác sĩ dễ dàng nhận diện bạn hơn.</p>
                <button className="px-5 py-2 border-2 border-[#6bd8cb] text-[#00685f] font-bold rounded-xl text-sm hover:bg-[#e9f6f3] transition-colors">
                  Thay đổi ảnh
                </button>
              </div>
            </div>

            <form className="grid grid-cols-1 md:grid-cols-2 gap-6" onSubmit={handleSubmit(onSubmit)}>
              
              <div className="flex flex-col gap-2">
                <label className="text-sm font-bold text-[#6d7a77]">Họ tên *</label>
                <input 
                  type="text" 
                  {...register("fullName")}
                  className="h-12 px-4 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium w-full text-[#121e1c]" 
                  placeholder="Nhập họ và tên"
                />
                {errors.fullName && <p className="text-sm text-[#ba1a1a]">{errors.fullName.message}</p>}
              </div>

              <div className="flex flex-col gap-2">
                <label className="text-sm font-bold text-[#6d7a77]">Email (Read-only)</label>
                <input 
                  type="email" 
                  value={userProfile?.email || ""}
                  readOnly 
                  className="h-12 px-4 rounded-xl bg-[#d8e5e2]/40 border-none text-[#6d7a77] font-medium cursor-not-allowed w-full" 
                />
              </div>

              <div className="flex flex-col gap-2">
                <label className="text-sm font-bold text-[#6d7a77]">Ngày sinh</label>
                <div className="relative">
                  <input 
                    type="date" 
                    {...register("birthDate")}
                    className="h-12 w-full px-4 pr-10 rounded-xl bg-[#e9f6f3] border-none focus:ring-2 focus:ring-[#00685f]/20 font-medium text-[#121e1c]" 
                  />
                  <Calendar className="absolute right-3 top-1/2 -translate-y-1/2 text-[#6d7a77] pointer-events-none w-5 h-5" />
                </div>
                {errors.birthDate && <p className="text-sm text-[#ba1a1a]">{errors.birthDate.message}</p>}
              </div>

              <div className="flex flex-col gap-2">
                <label className="text-sm font-bold text-[#6d7a77]">Giới tính</label>
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
                        <span className="text-[#121e1c] font-medium group-hover:text-[#00685f] transition-colors">Nam</span>
                      </label>
                      <label className="flex items-center gap-3 cursor-pointer group">
                        <input 
                          type="radio" 
                          value="female"
                          checked={field.value === "female"}
                          onChange={() => field.onChange("female")}
                          className="w-5 h-5 text-[#00685f] border-[#bcc9c6] bg-[#e9f6f3] focus:ring-[#00685f]" 
                        />
                        <span className="text-[#121e1c] font-medium group-hover:text-[#00685f] transition-colors">Nữ</span>
                      </label>
                      <label className="flex items-center gap-3 cursor-pointer group">
                        <input 
                          type="radio" 
                          value="other"
                          checked={field.value === "other"}
                          onChange={() => field.onChange("other")}
                          className="w-5 h-5 text-[#00685f] border-[#bcc9c6] bg-[#e9f6f3] focus:ring-[#00685f]" 
                        />
                        <span className="text-[#121e1c] font-medium group-hover:text-[#00685f] transition-colors">Khác</span>
                      </label>
                    </div>
                  )}
                />
                {errors.gender && <p className="text-sm text-[#ba1a1a]">{errors.gender.message}</p>}
              </div>

              <div className="md:col-span-2 flex justify-end gap-4 mt-4 pt-6 border-t border-[#bcc9c6]/20">
                <button 
                  type="button" 
                  onClick={() => reset()}
                  className="px-8 py-3 rounded-xl font-bold text-[#3d4947] hover:bg-[#e9f6f3] transition-colors"
                >
                  Hủy
                </button>
                <button 
                  type="submit" 
                  disabled={isSubmitting || !isValid}
                  className="px-8 py-3 bg-gradient-to-r from-[#00685f] to-[#008378] text-white rounded-xl font-bold shadow-lg shadow-[#00685f]/20 active:scale-95 transition-all disabled:opacity-60"
                >
                  {isSubmitting ? "Đang lưu..." : "Lưu thay đổi"}
                </button>
              </div>
            </form>
          </section>

          {/* Health Information Card (Stub based on UI) */}
          <section className="bg-white rounded-3xl p-8 shadow-[0_8px_32px_rgba(18,30,28,0.04)] border border-[#bcc9c6]/20">
            <div className="flex items-center gap-3 mb-8">
              <Cross className="text-[#00685f] w-6 h-6" />
              <h2 className="text-2xl font-bold text-[#121e1c]">Thông tin sức khỏe</h2>
            </div>
            
            <div className="space-y-8 opacity-60 pointer-events-none">
              <div>
                <label className="block text-sm font-bold text-[#6d7a77] mb-4">Bệnh mãn tính (Chưa khả dụng)</label>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                  {["Tiểu đường", "Huyết áp cao", "Cholesterol cao", "Bệnh tim", "Hen suyễn"].map(disease => (
                    <label key={disease} className="flex items-center gap-3 p-3 rounded-xl bg-[#e9f6f3] border border-transparent">
                      <input type="checkbox" className="rounded text-[#00685f]" disabled />
                      <span className="text-sm font-medium text-[#121e1c]">{disease}</span>
                    </label>
                  ))}
                </div>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="flex flex-col gap-2">
                  <label className="text-sm font-bold text-[#6d7a77]">Thuốc hiện tại</label>
                  <textarea className="w-full p-4 rounded-2xl bg-[#e9f6f3] border-none text-sm resize-none" placeholder="Nhập tên thuốc..." rows={3} disabled></textarea>
                </div>
                <div className="flex flex-col gap-2">
                  <label className="text-sm font-bold text-[#6d7a77]">Dị ứng</label>
                  <textarea className="w-full p-4 rounded-2xl bg-[#e9f6f3] border-none text-sm resize-none" placeholder="Ví dụ: Hải sản..." rows={3} disabled></textarea>
                </div>
              </div>
            </div>
          </section>
        </div>

        {/* Right Column: Settings & Support */}
        <div className="space-y-8">
          
          {/* Account Settings Card */}
          <section className="bg-white rounded-3xl p-8 shadow-[0_8px_32px_rgba(18,30,28,0.04)] border border-[#bcc9c6]/20">
            <div className="flex items-center gap-3 mb-8">
              <Shield className="text-[#00685f] w-6 h-6" />
              <h2 className="text-xl font-bold text-[#121e1c]">Cài đặt tài khoản</h2>
            </div>
            
            <div className="space-y-6">
              <a href="#" className="flex items-center justify-between group py-2">
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-[#e9f6f3] group-hover:bg-[#008378] group-hover:text-white transition-colors">
                    <Key className="w-4 h-4" />
                  </div>
                  <span className="font-medium text-[#3d4947] group-hover:text-[#00685f] transition-colors">Đổi mật khẩu</span>
                </div>
              </a>
              <div className="flex items-center justify-between py-2 opacity-60">
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-[#e9f6f3]">
                    <Shield className="w-4 h-4" />
                  </div>
                  <span className="font-medium text-[#3d4947]">Xác thực hai yếu tố (2FA)</span>
                </div>
              </div>
              <div className="pt-6 border-t border-[#bcc9c6]/20">
                <button type="button" className="w-full py-3 rounded-xl border-2 border-[#ba1a1a]/20 text-[#ba1a1a] font-bold hover:bg-[#ba1a1a]/5 transition-colors flex items-center justify-center gap-2">
                  <Trash2 className="w-4 h-4" /> Xóa tài khoản
                </button>
              </div>
            </div>
          </section>

          {/* Profile Completion Card */}
          <section className="bg-gradient-to-br from-[#c2ebe3] to-[#a6cfc8] text-[#456b66] rounded-3xl p-8 shadow-md border-none relative overflow-hidden">
            <div className="relative z-10">
              <div className="w-12 h-12 bg-white/40 backdrop-blur-md rounded-2xl flex items-center justify-center mb-6">
                <CheckCircle2 className="text-[#00685f] w-6 h-6" />
              </div>
              <h3 className="text-xl font-extrabold mb-2">Độ hoàn thiện hồ sơ</h3>
              <div className="flex items-end gap-2 mb-4">
                <span className="text-4xl font-black text-[#00201d]">85%</span>
                <span className="text-sm font-bold mb-1 opacity-70">Rất tốt!</span>
              </div>
              <div className="w-full h-2 bg-white/50 rounded-full mb-6 overflow-hidden">
                <div className="h-full bg-[#00685f] w-[85%] rounded-full shadow-sm"></div>
              </div>
              <p className="text-sm leading-relaxed mb-6 text-[#274d48]">Thêm thông tin về bảo hiểm y tế để hoàn thiện 100% hồ sơ của bạn.</p>
              <button disabled className="w-full bg-[#456b66] text-white py-3 rounded-xl font-bold shadow-lg shadow-black/10 active:scale-95 transition-transform opacity-70">
                Hoàn thiện ngay
              </button>
            </div>
            <div className="absolute -bottom-10 -right-10 w-40 h-40 bg-[#00685f]/10 rounded-full blur-3xl"></div>
          </section>

          {/* Support Section */}
          <div className="p-6 rounded-3xl bg-[#e4f1ee] border border-[#bcc9c6]/20 text-center">
            <p className="text-xs text-[#6d7a77] font-bold uppercase tracking-widest mb-4">Cần hỗ trợ?</p>
            <p className="text-sm text-[#3d4947] mb-6 leading-relaxed">Nếu bạn gặp khó khăn khi cập nhật thông tin, liên hệ đội ngũ hỗ trợ.</p>
            <div className="flex flex-col gap-2">
              <a href="tel:19001234" className="text-[#00685f] font-bold hover:underline">1900 1234</a>
              <a href="mailto:support@healthlens.vn" className="text-[#00685f] font-bold hover:underline">support@healthlens.vn</a>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}
