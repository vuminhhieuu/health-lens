"use client";

import React, { useState, useMemo } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Users, Search, Filter, Loader2, AlertCircle } from "lucide-react";
import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { ProfileCard, HealthStatus } from "@/components/features/profiles/ProfileCard";
import { CreateProfileModal } from "@/components/features/profiles/CreateProfileModal";
import { EditProfileModal } from "@/components/features/profiles/EditProfileModal";
import { CreateProfileInput, UpdateProfileInput } from "@healthlens/shared";

type Profile = {
  id: string;
  displayName: string;
  birthDate?: string;
  gender?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
  latestStatus?: HealthStatus; // Mocked for now until integrated with records
};

type UserProfile = {
  id: string;
  fullName: string;
  email: string;
  birthDate: string;
  gender: string;
};

export default function ProfilesPage() {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingProfileId, setEditingProfileId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");

  // 1. Fetch current user (Self)
  const { data: currentUser, isLoading: isUserLoading } = useQuery({
    queryKey: ["currentUser"],
    queryFn: async () => {
      const resp = await apiClient.get(API_ROUTES.USERS.ME);
      return resp.data.data as UserProfile;
    },
  });

  // 2. Fetch other profiles
  const { data: otherProfiles = [], isLoading: isProfilesLoading } = useQuery({
    queryKey: ["profiles"],
    queryFn: async () => {
      const resp = await apiClient.get(API_ROUTES.PROFILES.BASE);
      return resp.data.data as Profile[];
    },
  });

  // 3. Create profile mutation
  const createMutation = useMutation({
    mutationFn: async (data: CreateProfileInput) => {
      const resp = await apiClient.post(API_ROUTES.PROFILES.BASE, data);
      return resp.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["profiles"] });
      setIsModalOpen(false);
    },
    onError: (error: unknown) => {
      let message = "Đã xảy ra lỗi khi tạo hồ sơ.";
      
      // Safe access to axios error details (covers detail and message)
      if (error && typeof error === 'object' && 'response' in error) {
        const axiosError = error as { 
          response?: { 
            data?: { 
              detail?: string;
              message?: string;
            } 
          } 
        };
        const errorData = axiosError.response?.data;
        message = errorData?.detail || errorData?.message || message;
      }
      
      alert(message);
    },
  });

  const updateProfileMutation = useMutation({
    mutationFn: async ({ profileId, data }: { profileId: string; data: UpdateProfileInput }) => {
      const payload = {
        ...data,
        birthDate: data.birthDate || null,
        gender: data.gender || null,
        notes: data.notes || null,
      };
      const response = await apiClient.put(API_ROUTES.PROFILES.UPDATE(profileId), payload);
      return response.data.data as Profile;
    },
    onMutate: async ({ profileId, data }) => {
      await queryClient.cancelQueries({ queryKey: ["profiles"] });

      const previousProfiles = queryClient.getQueryData<Profile[]>(["profiles"]);
      queryClient.setQueryData<Profile[]>(["profiles"], (old = []) =>
        old.map((profile) => {
          if (profile.id !== profileId) {
            return profile;
          }

          return {
            ...profile,
            displayName: data.displayName.trim(),
            birthDate: data.birthDate || undefined,
            gender: data.gender || undefined,
            notes: data.notes?.trim() || undefined,
            updatedAt: new Date().toISOString(),
          };
        }),
      );

      return { previousProfiles };
    },
    onError: (error: unknown, _variables, context) => {
      if (context?.previousProfiles) {
        queryClient.setQueryData(["profiles"], context.previousProfiles);
      }

      let message = "Đã xảy ra lỗi khi cập nhật hồ sơ.";
      if (error && typeof error === "object" && "response" in error) {
        const axiosError = error as {
          response?: {
            data?: {
              detail?: string;
              title?: string;
              message?: string;
            };
          };
        };

        const errorData = axiosError.response?.data;
        message = errorData?.detail || errorData?.title || errorData?.message || message;
      }

      alert(message);
    },
    onSuccess: (updatedProfile) => {
      queryClient.setQueryData<Profile[]>(["profiles"], (old = []) =>
        old.map((profile) => (profile.id === updatedProfile.id ? updatedProfile : profile)),
      );
      queryClient.invalidateQueries({ queryKey: ["profiles"] });
      queryClient.invalidateQueries({ queryKey: ["profile", updatedProfile.id] });
      setIsEditModalOpen(false);
      setEditingProfileId(null);
    },
  });

  // Combine profiles for display
  const allProfiles = useMemo(() => {
    const combined: (Partial<Profile> & { 
      displayName: string; 
      relationship: string; 
      isSelf: boolean;
      id: string;
    })[] = [];
    
    // Add Self if available
    if (currentUser) {
      combined.push({
        id: "self",
        displayName: currentUser.fullName + " (Tôi)",
        relationship: "Chính chủ",
        birthDate: currentUser.birthDate,
        gender: currentUser.gender,
        latestStatus: "normal" as HealthStatus, // Mocked
        updatedAt: new Date().toISOString(), // Unified with others
        isSelf: true
      });
    }

    const normalizedSelfName = currentUser?.fullName.trim().toLowerCase();

    // Add others, filtering out default self-profile to avoid duplicate cards
    otherProfiles.forEach(p => {
      const normalizedProfileName = p.displayName.trim().toLowerCase();
      const isAlreadyAdded = !!normalizedSelfName && normalizedProfileName === normalizedSelfName;
      
      if (!isAlreadyAdded) {
        combined.push({
          ...p,
          relationship: "Người thân",
          isSelf: false
        });
      }
    });

    return combined.filter(p => 
      p.displayName.toLowerCase().includes(searchQuery.toLowerCase())
    );
  }, [currentUser, otherProfiles, searchQuery]);

  const editingProfile = useMemo(
    () => otherProfiles.find((profile) => profile.id === editingProfileId) ?? null,
    [otherProfiles, editingProfileId],
  );

  const isLimitReached = otherProfiles.length >= 10;

  const isLoading = isUserLoading || isProfilesLoading;

  if (isLoading) {
    return (
      <div className="grow flex items-center justify-center bg-[#effcf9] p-8">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="w-10 h-10 text-[#00685f] animate-spin" />
          <p className="font-bold text-[#6d7a77]">Đang tải danh sách hồ sơ...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="grow p-6 md:p-12 lg:p-16 max-w-7xl mx-auto bg-[#effcf9] min-h-screen text-[#121e1c]">
      
      {/* Header & Stats */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-12">
        <div>
          <nav className="flex text-sm text-[#6d7a77] mb-2 font-medium">
            <span>Dashboard</span>
            <span className="mx-2">/</span>
            <span className="text-[#121e1c]">Hồ sơ gia đình</span>
          </nav>
          <h1 className="text-4xl font-black tracking-tight text-[#121e1c] flex items-center gap-3">
            Hồ sơ sức khỏe
            <span className="text-sm font-bold px-3 py-1 bg-[#00685f]/10 text-[#00685f] rounded-full">
              {allProfiles.length}
            </span>
          </h1>
        </div>

        <button 
          onClick={() => setIsModalOpen(true)}
          disabled={isLimitReached}
          className="flex items-center gap-2 px-8 py-3 bg-linear-to-r from-[#00685f] to-[#008378] text-white rounded-2xl font-bold shadow-lg shadow-[#00685f]/20 active:scale-95 transition-all disabled:opacity-50 disabled:pointer-events-none"
        >
          <Plus size={20} />
          {isLimitReached ? "Đã đạt giới hạn" : "Tạo hồ sơ mới"}
        </button>
      </div>

      {/* Search & Filters */}
      <div className="flex flex-col sm:flex-row gap-4 mb-8">
        <div className="relative grow">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-[#6d7a77] w-5 h-5" />
          <input 
            type="text" 
            placeholder="Tìm kiếm hồ sơ..." 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full h-14 pl-12 pr-4 rounded-2xl bg-white/60 border-2 border-transparent focus:border-[#00685f]/20 focus:bg-white transition-all outline-none font-medium text-[#121e1c]"
          />
        </div>
        <button className="h-14 px-6 rounded-2xl bg-white/60 border-2 border-transparent hover:bg-white hover:border-[#bcc9c6]/20 transition-all flex items-center gap-2 font-bold text-[#3d4947]">
          <Filter size={18} />
          Sắp xếp
        </button>
      </div>

      {/* Main Grid */}
      {allProfiles.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 animate-in slide-in-from-bottom-4 duration-500">
          {allProfiles.map((profile) => (
            <ProfileCard 
              key={profile.id}
              name={profile.displayName}
              relationship={profile.relationship}
              notes={profile.notes}
              latestStatus={profile.latestStatus}
              lastUpdated={profile.updatedAt}
              onPress={
                profile.isSelf
                  ? undefined
                  : () => {
                    setEditingProfileId(profile.id);
                    setIsEditModalOpen(true);
                  }
              }
            />
          ))}
        </div>
      ) : (
        <div className="p-20 flex flex-col items-center justify-center bg-white/40 rounded-[40px] border-2 border-dashed border-[#bcc9c6]/30 text-center">
          <div className="w-24 h-24 rounded-full bg-[#e9f6f3] flex items-center justify-center text-[#00685f] mb-6">
            <Users size={48} />
          </div>
          <h3 className="text-2xl font-black text-[#121e1c] mb-2">Chưa tìm thấy hồ sơ nào</h3>
          <p className="text-[#6d7a77] font-medium max-w-xs mb-8">
            Bắt đầu quản lý sức khỏe bằng cách thêm hồ sơ cho các thành viên trong gia đình.
          </p>
          <button 
            onClick={() => setIsModalOpen(true)}
            className="flex items-center gap-2 px-8 py-3 border-2 border-[#00685f] text-[#00685f] rounded-2xl font-bold hover:bg-[#e9f6f3] transition-colors"
          >
            Tạo hồ sơ đầu tiên
          </button>
        </div>
      )}

      {/* Limit Warning */}
      {isLimitReached && (
        <div className="mt-12 p-6 rounded-4xl bg-[#fffbeb] border border-[#f59e0b]/20 flex items-start gap-4">
          <div className="w-10 h-10 rounded-xl bg-[#f59e0b]/10 flex items-center justify-center text-[#92400e]">
            <AlertCircle size={24} />
          </div>
          <div>
            <h4 className="font-black text-[#92400e]">Bạn đã đạt giới hạn 10 hồ sơ người thân</h4>
            <p className="text-sm font-medium text-[#92400e]/80">
              Vui lòng liên hệ hỗ trợ hoặc nâng cấp tài khoản để quản lý nhiều hồ sơ hơn.
            </p>
          </div>
        </div>
      )}

      {/* Creation Modal */}
      <CreateProfileModal 
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={(data) => createMutation.mutate(data)}
        isLoading={createMutation.isPending}
      />

      <EditProfileModal
        isOpen={isEditModalOpen}
        profile={editingProfile}
        onClose={() => {
          setIsEditModalOpen(false);
          setEditingProfileId(null);
        }}
        onSubmit={(profileId, data) => updateProfileMutation.mutate({ profileId, data })}
        isLoading={updateProfileMutation.isPending}
      />
    </div>
  );
}
