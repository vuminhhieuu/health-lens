"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Activity,
  Bell,
  CalendarDays,
  ChevronRight,
  Eye,
  FileText,
  Share2,
  ShieldPlus,
  Stethoscope,
  Upload,
  UserPlus,
  X,
} from "lucide-react";

import { ApiPaths } from "@healthlens/shared/constants";

import { apiClient } from "@/lib/api/apiClient";
import { notify } from "@/lib/notify";
import { InviteMemberModal } from "@/components/features/profiles/InviteMemberModal";
import { DashboardPageShell } from "@/components/layout/DashboardPageShell";

type Profile = {
  id: string;
  displayName: string;
  isDefault: boolean;
};

type HealthRecord = {
  id: string;
  status?: string | null;
  testType?: string | null;
  examDate?: string | null;
  overallStatus?: "normal" | "attention" | "abnormal" | string;
  abnormalCount?: number;
  createdAt?: string | null;
};

type ProfileInvitation = {
  id: string;
  viewerId?: string | null;
  email: string;
  status: string;
  accessLevel: "view" | "edit" | string;
};

type HealthRecordHistoryResponse = {
  records: HealthRecord[];
  totalItems: number;
};

function extractApiDetail(error: unknown, fallback: string): string {
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
    const data = axiosError.response?.data;
    return data?.detail ?? data?.title ?? data?.message ?? fallback;
  }
  return fallback;
}

export default function DashboardHomePage() {
  const queryClient = useQueryClient();
  const [profilePickerOpen, setProfilePickerOpen] = useState(false);
  const [inviteModalOpen, setInviteModalOpen] = useState(false);
  const [invitingProfileId, setInvitingProfileId] = useState<string | null>(
    null,
  );
  const [selectedProfileId, setSelectedProfileId] = useState<string>("");
  const [, setPendingAccessUpdates] = useState<Record<string, boolean>>({});
  useEffect(() => {
    void apiClient.post(ApiPaths.PROFILES.ENSURE_DEFAULT);
  }, []);

  const {
    data: profiles = [],
    isLoading: isProfilesLoading,
    isError: isProfilesError,
    refetch: refetchProfiles,
  } = useQuery({
    queryKey: ["home-profiles"],
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.PROFILES.BASE);
      return (response.data?.data ?? []) as Profile[];
    },
    refetchInterval: 30000,
    refetchOnWindowFocus: true,
  });

  const primaryProfileId = profiles.find((p) => p.isDefault)?.id ?? profiles[0]?.id;
  const primaryProfileName =
    profiles.find((p) => p.id === primaryProfileId)?.displayName ?? "hồ sơ hiện tại";

  const {
    data: recentRecordResponse = { records: [], totalItems: 0 },
    isLoading: isRecordsLoading,
    isError: isRecordsError,
    refetch: refetchRecentRecords,
  } = useQuery<HealthRecordHistoryResponse>({
    queryKey: ["home-recent-records", primaryProfileId],
    enabled: Boolean(primaryProfileId),
    queryFn: async () => {
      const response = await apiClient.get(
        ApiPaths.PROFILES.HEALTH_RECORDS(primaryProfileId as string),
        {
          params: { page: 0, limit: 3 },
        },
      );
      const records = (response.data?.data ?? []) as HealthRecord[];
      const rawTotalItems =
        response.data?.pagination?.totalItems ??
        response.data?.pagination?.total ??
        records.length;
      const totalItems = Number(rawTotalItems);
      return { records, totalItems };
    },
  });

  const recentRecords = recentRecordResponse.records;
  const latestRecord = recentRecords[0];
  const hasRecords = recentRecordResponse.totalItems > 0 || recentRecords.length > 0;
  const isDashboardLoading = isProfilesLoading || (Boolean(primaryProfileId) && isRecordsLoading);
  const hasDashboardError = isProfilesError || isRecordsError;
  const latestRecordHref = latestRecord
    ? `/health-records/review/${latestRecord.id}`
    : historyHrefForProfile(primaryProfileId);
  const profileStatValue = isProfilesError
    ? "—"
    : isDashboardLoading
      ? "..."
      : profiles.length.toString();
  const recordCountStatValue = isRecordsError
    ? "—"
    : isDashboardLoading
      ? "..."
      : recentRecordResponse.totalItems.toString();
  const latestRecordStatValue = isRecordsError
    ? "—"
    : isDashboardLoading
      ? "..."
      : formatShortDate(latestRecord?.examDate ?? latestRecord?.createdAt);
  const latestRecordStatStatus = isRecordsError
    ? "Không tải được"
    : latestRecord
      ? recordStatusLabel(resolveHomeRecordStatus(latestRecord))
      : "Chưa có dữ liệu";
  const latestRecordStatDetail = isRecordsError
    ? "Vui lòng thử lại"
    : latestRecord?.testType || "Lần khám gần nhất";
  const abnormalCountStatValue = isRecordsError
    ? "—"
    : isDashboardLoading
      ? "..."
      : formatAbnormalCount(latestRecord);
  const abnormalCountStatStatus = isRecordsError
    ? "Không tải được"
    : latestRecord
      ? "Từ kết quả mới nhất"
      : "Chưa có dữ liệu";
  const abnormalCountStatDetail = isRecordsError
    ? "Vui lòng thử lại"
    : "Trong kết quả mới nhất";

  const historyHref = useMemo(() => {
    if (!primaryProfileId) return "/health-records";
    return `/profiles/${primaryProfileId}/history`;
  }, [primaryProfileId]);

  const updateAccessMutation = useMutation({
    mutationFn: async (payload: { profileId: string; email: string; accessLevel: "view" | "edit" }) => {
      await apiClient.post(ApiPaths.PROFILES.INVITATIONS(payload.profileId), {
        email: payload.email,
        accessLevel: payload.accessLevel,
      });
    },
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({ queryKey: ["profile-shared-members", variables.profileId] });
      notify.success(`Đã cập nhật quyền ${variables.accessLevel === "edit" ? "chỉnh sửa" : "chỉ xem"} cho ${variables.email}.`);
    },
    onError: (error: unknown) => {
      notify.error(extractApiDetail(error, "Không thể cập nhật quyền truy cập."));
    },
  });

  const inviteMutation = useMutation({
    mutationFn: async (payload: { profileId: string; email?: string; accessLevel?: "view" | "edit" }) => {
      let invitedCount = 0;
      if (payload.email && payload.accessLevel) {
        await apiClient.post(ApiPaths.PROFILES.INVITATIONS(payload.profileId), {
          email: payload.email,
          accessLevel: payload.accessLevel,
        });
        invitedCount += 1;
      }
      return {
        invitedCount,
      };
    },
    onSuccess: (result, variables) => {
      void queryClient.invalidateQueries({ queryKey: ["profile-shared-members", variables.profileId] });
      setInviteModalOpen(false);
      setInvitingProfileId(null);
      if (result.invitedCount > 0) {
        notify.success("Đã gửi lời mời chia sẻ thành công.");
      }
    },
    onError: (error: unknown) => {
      notify.error(extractApiDetail(error, "Không thể gửi lời mời chia sẻ."));
    },
  });

  const revokeShareMutation = useMutation({
    mutationFn: async (payload: { targetId: string; email: string }) => {
      if (!invitingProfileId) {
        throw new Error("Thiếu profile để thu hồi quyền.");
      }
      await apiClient.delete(ApiPaths.PROFILES.REVOKE_SHARE(invitingProfileId, payload.targetId));
      return payload.email;
    },
    onSuccess: (email) => {
      queryClient.setQueryData(
        ["profile-shared-members", invitingProfileId],
        (previous: Array<{ id: string; viewerId?: string; email: string; accessLevel: string }> | undefined) =>
          (previous ?? []).filter((member) => member.email.toLowerCase() !== email.toLowerCase())
      );
      void queryClient.invalidateQueries({ queryKey: ["profile-shared-members", invitingProfileId] });
      setPendingAccessUpdates((prev) => {
        const next = { ...prev };
        delete next[email.toLowerCase()];
        return next;
      });
      notify.success("Đã thu hồi quyền truy cập thành công.");
    },
    onError: (error: unknown) => {
      notify.error(extractApiDetail(error, "Không thể thu hồi quyền truy cập."));
    },
  });

  const { data: sharedMembers = [], isFetching: isSharedMembersLoading } = useQuery({
    queryKey: ["profile-shared-members", invitingProfileId],
    enabled: inviteModalOpen && Boolean(invitingProfileId),
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.PROFILES.INVITATIONS(invitingProfileId as string));
      const invitations = (response.data?.data ?? []) as ProfileInvitation[];
      return invitations
        .filter((invitation) => invitation.status === "accepted")
        .map((invitation) => ({
          id: invitation.id,
          viewerId: invitation.viewerId ?? undefined,
          email: invitation.email,
          accessLevel: invitation.accessLevel,
        }));
    },
  });

  const displaySharedMembers = sharedMembers;

  return (
    <DashboardPageShell
      title="Chào mừng quay lại!"
      subtitle="Đây là tổng quan tình trạng sức khỏe của bạn hôm nay."
      actions={
        <div className="text-left lg:text-right">
          <p className="text-base font-bold text-[#00685f]">
            Theo dõi sức khỏe mỗi ngày
          </p>
        </div>
      }
    >
      <section className="mb-12 grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
        <StatCard
          icon={<Activity className="h-6 w-6" />}
          value={profileStatValue}
          label="Hồ sơ theo dõi"
          detail="Từ tài khoản của bạn"
          status={isProfilesError ? "Không tải được" : "Từ tài khoản"}
        />
        <StatCard
          icon={<FileText className="h-6 w-6" />}
          value={recordCountStatValue}
          label="Tổng kết quả"
          detail={`Của ${primaryProfileName}`}
          status={isRecordsError ? "Không tải được" : "Hồ sơ hiện tại"}
        />
        <StatCard
          icon={<CalendarDays className="h-6 w-6" />}
          value={latestRecordStatValue}
          label="Kết quả mới nhất"
          detail={latestRecordStatDetail}
          status={latestRecordStatStatus}
        />
        <StatCard
          icon={<ShieldPlus className="h-6 w-6" />}
          value={abnormalCountStatValue}
          label="Chỉ số cần chú ý"
          detail={abnormalCountStatDetail}
          status={abnormalCountStatStatus}
        />
      </section>

      {hasDashboardError ? (
        <section className="mb-8 rounded-2xl border border-[#fecdd3] bg-[#fff1f2] p-5 text-sm text-[#9f1239]">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="font-bold">Không thể tải dữ liệu dashboard.</p>
              <p className="mt-1 text-[#be123c]">
                Vui lòng thử lại để xem dữ liệu sức khỏe mới nhất.
              </p>
            </div>
            <button
              type="button"
              onClick={() => {
                void refetchProfiles();
                if (primaryProfileId) {
                  void refetchRecentRecords();
                }
              }}
              className="inline-flex min-h-11 items-center justify-center rounded-full bg-[#be123c] px-5 text-sm font-bold text-white transition hover:bg-[#9f1239]"
            >
              Thử lại
            </button>
          </div>
        </section>
      ) : null}

      <section className="grid grid-cols-1 gap-8 lg:grid-cols-12">
        <div className="lg:col-span-7">
          <div className="mb-5 flex items-center justify-between">
            <h2 className="text-2xl font-bold text-[#121e1c]">
              Kết quả gần đây
            </h2>
            <Link
              href={historyHref}
              className="inline-flex items-center gap-1 text-sm font-bold text-[#00685f] hover:underline"
            >
              Xem tất cả
              <ChevronRight className="h-4 w-4" />
            </Link>
          </div>

          <div className="space-y-4">
            {isDashboardLoading ? (
              <div className="rounded-2xl border border-[#bcc9c6]/30 bg-white p-5 text-sm text-[#6d7a77] shadow-sm">
                Đang tải dữ liệu sức khỏe mới nhất...
              </div>
            ) : hasDashboardError ? (
              <div className="rounded-2xl border border-[#fecdd3] bg-white p-5 text-sm text-[#9f1239] shadow-sm">
                Chưa thể hiển thị kết quả gần đây do lỗi tải dữ liệu.
              </div>
            ) : recentRecords.length === 0 ? (
              <div className="rounded-2xl border border-[#bcc9c6]/30 bg-white p-6 text-sm text-[#6d7a77] shadow-sm">
                <p className="text-lg font-bold text-[#121e1c]">
                  Chưa có kết quả sức khỏe cho {primaryProfileName}.
                </p>
                <p className="mt-2">
                  Tải phiếu xét nghiệm hoặc kết quả khám đầu tiên để dashboard hiển thị dữ liệu thật.
                </p>
                <Link
                  href={uploadHrefForProfile(primaryProfileId)}
                  className="mt-5 inline-flex min-h-11 items-center rounded-full bg-[#00685f] px-5 text-sm font-bold text-white transition hover:bg-[#008378]"
                >
                  Tải kết quả lên
                </Link>
              </div>
            ) : (
              recentRecords.map((record) => (
                <article
                  key={record.id}
                  className="group flex items-center gap-4 rounded-2xl bg-white p-5 shadow-sm transition-colors hover:bg-[#e9f6f3]"
                >
                  <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-[#e9f6f3] text-[#00685f] transition-colors group-hover:bg-[#00685f] group-hover:text-white">
                    <Stethoscope className="h-7 w-7" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-lg font-bold text-[#121e1c]">
                      {record.testType || "Phiếu khám bệnh"}
                    </p>
                    <p className="mt-1 flex items-center gap-2 text-sm text-[#6d7a77]">
                      <CalendarDays className="h-4 w-4" />
                      Ngày thực hiện: {record.examDate || "Chưa có ngày khám"}
                    </p>
                    <p className="mt-1 text-sm text-[#6d7a77]">
                      Số chỉ số bất thường: {record.abnormalCount ?? 0}
                    </p>
                  </div>
                  <div className="flex flex-col items-end gap-2">
                    <span
                      className={`rounded-full px-3 py-1 text-xs font-bold ${recordStatusClass(resolveHomeRecordStatus(record))}`}
                    >
                      {recordStatusLabel(resolveHomeRecordStatus(record))}
                    </span>
                    <Link
                      href={`/health-records/review/${record.id}`}
                      className="inline-flex items-center gap-1 text-sm font-semibold text-[#00685f] hover:underline"
                    >
                      Xem chi tiết
                      <ChevronRight className="h-4 w-4" />
                    </Link>
                  </div>
                </article>
              ))
            )}
          </div>
        </div>

        <div className="lg:col-span-5">
          <h2 className="mb-5 text-2xl font-bold text-[#121e1c]">
            Thao tác nhanh
          </h2>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
            <ActionTile
              icon={<Upload className="h-6 w-6" />}
              label="Tải kết quả"
              href={uploadHrefForProfile(primaryProfileId)}
            />
            <ActionTile
              icon={<Bell className="h-6 w-6" />}
              label="Nhắc lịch tái khám"
              href="/follow-up-reminders"
            />
            <ActionTile
              icon={<Eye className="h-6 w-6" />}
              label="Xem kết quả"
              href={historyHref}
            />
            <ActionTile
              icon={<Share2 className="h-6 w-6" />}
              label="Chia sẻ"
              disabled={profiles.length === 0}
              onClick={() => {
                setSelectedProfileId(primaryProfileId ?? profiles[0]?.id ?? "");
                setProfilePickerOpen(true);
              }}
            />
            <ActionTile
              icon={<FileText className="h-6 w-6" />}
              label="Tóm tắt đi khám"
              href="/visit-summary"
            />
            <ActionTile
              icon={<ShieldPlus className="h-6 w-6" />}
              label="Hướng dẫn"
              href="/guide"
            />
          </div>

          <div className="relative mt-8 overflow-hidden rounded-3xl bg-linear-to-br from-[#00685f] to-[#008378] p-7 text-white shadow-xl">
            <h3 className="text-xl font-bold">Chăm sóc sức khỏe chủ động</h3>
            <p className="mt-2 max-w-sm text-sm text-[#d8fffa]">
              {hasRecords
                ? "Mở kết quả gần nhất để xem chi tiết chỉ số, trạng thái xử lý và khuyến nghị đã được tạo từ dữ liệu của bạn."
                : "Khi bạn tải kết quả khám đầu tiên, dashboard sẽ dùng dữ liệu thật để tạo tổng quan sức khỏe."}
            </p>
            <Link
              href={hasRecords ? latestRecordHref : uploadHrefForProfile(primaryProfileId)}
              className="mt-5 inline-flex min-h-12 items-center rounded-full bg-white px-5 text-sm font-bold text-[#00685f] transition hover:bg-[#e9f6f3] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
            >
              {hasRecords ? "Xem kết quả mới nhất" : "Tải kết quả lên"}
            </Link>
            <div className="pointer-events-none absolute -bottom-10 -right-8 opacity-20">
              <ShieldPlus className="h-36 w-36" />
            </div>
          </div>
        </div>
      </section>
      <InviteMemberModal
        isOpen={inviteModalOpen}
        onClose={() => {
          setInviteModalOpen(false);
          setInvitingProfileId(null);
        }}
        isLoading={inviteMutation.isPending || revokeShareMutation.isPending}
        sharedMembers={displaySharedMembers}
        isSharedMembersLoading={isSharedMembersLoading}
        isRevokingMember={revokeShareMutation.isPending}
        hasPendingAccessChanges={false}
        onRevokeMember={(member) => revokeShareMutation.mutate({ targetId: member.viewerId, email: member.email })}
        onChangeAccessLevel={(member) =>
          invitingProfileId &&
          updateAccessMutation.mutate({
            profileId: invitingProfileId,
            email: member.email,
            accessLevel: member.accessLevel,
          })
        }
        isUpdatingAccessLevel={updateAccessMutation.isPending}
        title="Chia sẻ quyền xem kết quả khám"
        description="Nhập email người nhận."
        onSubmit={(payload) =>
          invitingProfileId &&
          inviteMutation.mutate({
            ...payload,
            profileId: invitingProfileId,
          })
        }
      />
      {profilePickerOpen ? (
        <div className="fixed inset-0 z-100 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="flex max-h-[78vh] w-full max-w-2xl flex-col overflow-hidden rounded-4xl bg-white shadow-2xl shadow-black/20 animate-in zoom-in-95 duration-200">
            <div className="relative p-7 pb-4">
              <button
                type="button"
                onClick={() => setProfilePickerOpen(false)}
                className="absolute top-6 right-6 rounded-full p-2 text-[#6d7a77] transition-colors hover:bg-[#e9f6f3]"
                aria-label="Đóng"
              >
                <X size={20} />
              </button>
              <div className="mb-2 flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#e9f6f3] text-[#00685f]">
                  <UserPlus size={24} />
                </div>
                <div>
                  <h3 className="text-2xl font-black text-[#121e1c]">
                    Chọn hồ sơ để chia sẻ
                  </h3>
                  <p className="mt-1 text-sm font-medium text-[#6d7a77]">
                    Chọn hồ sơ sức khỏe bạn muốn chia sẻ cho người khác xem.
                  </p>
                </div>
              </div>
            </div>
            <div className="px-7 pb-6">
              <label
                className="mb-2 block text-sm font-bold text-[#121e1c]"
                htmlFor="share-profile-select"
              >
                Hồ sơ cần chia sẻ <span className="text-red-600">*</span>
              </label>
              <div className="relative">
                <select
                  id="share-profile-select"
                  value={selectedProfileId}
                  onChange={(event) => setSelectedProfileId(event.target.value)}
                  className="h-12 w-full cursor-pointer appearance-none rounded-2xl border border-[#b7e8e0] bg-[#f0faf8] pr-10 pl-4 text-sm font-semibold text-[#3d4947] outline-none focus:border-[#008378]"
                >
                  {profiles.map((profile) => (
                    <option key={profile.id} value={profile.id}>
                      {profile.displayName}
                    </option>
                  ))}
                </select>
                <span className="pointer-events-none absolute top-1/2 right-4 -translate-y-1/2 text-[#00685f]">
                  ▾
                </span>
              </div>
            </div>
            <div className="mt-auto flex items-center justify-end border-t border-[#e8eeec] px-7 py-4">
              <button
                type="button"
                onClick={() => setProfilePickerOpen(false)}
                className="mr-3 rounded-2xl border border-[#c5dfd9] bg-white px-5 py-2.5 text-sm font-bold text-[#3d4947] transition hover:bg-[#f6fbfa]"
              >
                Hủy
              </button>
              <button
                type="button"
                disabled={!selectedProfileId}
                onClick={() => {
                  if (!selectedProfileId) return;
                  setInvitingProfileId(selectedProfileId);
                  setProfilePickerOpen(false);
                  setInviteModalOpen(true);
                }}
                className="rounded-2xl bg-[#008378] px-6 py-3 text-sm font-bold text-white shadow-md transition hover:brightness-110"
              >
                Tiếp tục
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </DashboardPageShell>
  );
}

function StatCard({
  icon,
  value,
  label,
  detail,
  status,
}: {
  icon: ReactNode;
  value: string;
  label: string;
  detail: string;
  status: string;
}) {
  return (
    <article className="rounded-xl border-l-4 border-[#00685f] bg-white p-6 shadow-sm">
      <div className="mb-4 flex items-start justify-between">
        <div className="rounded-lg bg-[#e9f6f3] p-2 text-[#00685f]">{icon}</div>
        <span className="inline-flex items-center gap-1 rounded-full bg-[#e6f6f2] px-2 py-1 text-xs font-bold text-[#00685f]">
          {status}
        </span>
      </div>
      <p className="text-sm font-bold text-[#6d7a77]">
        {label}
      </p>
      <p className="mt-2 text-4xl font-black tracking-tight text-[#121e1c]">
        {value}
      </p>
      <p className="mt-2 text-sm font-semibold leading-5 text-[#6d7a77]">
        {detail}
      </p>
    </article>
  );
}

function formatShortDate(value?: string | null) {
  if (!value) return "Chưa có";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Chưa có";
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
}

function formatAbnormalCount(record?: HealthRecord) {
  if (!record) return "Chưa có";
  const status = record.status?.toLowerCase();
  if (status && status !== "done") return "Đang xử lý";
  return String(record.abnormalCount ?? 0);
}

function historyHrefForProfile(profileId?: string) {
  if (!profileId) return "/health-records";
  return `/profiles/${profileId}/history`;
}

function uploadHrefForProfile(profileId?: string) {
  if (!profileId) return "/health-records";
  return `/profiles/${profileId}/history?openUpload=1`;
}

function ActionTile({
  icon,
  label,
  href,
  disabled,
  onClick,
}: {
  icon: ReactNode;
  label: string;
  href?: string;
  disabled?: boolean;
  onClick?: () => void;
}) {
  const commonClass =
    "group aspect-square min-h-28 rounded-2xl bg-white p-4 shadow-sm transition-all duration-300 hover:bg-[#00685f] hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]";

  if (disabled || !href) {
    return (
      <button
        type="button"
        disabled={disabled}
        onClick={disabled ? undefined : onClick}
        className={`${commonClass} ${disabled ? "cursor-not-allowed opacity-60" : ""}`}
      >
        <div className="flex h-full flex-col items-center justify-center gap-3 text-center">
          <span className="text-[#00685f] group-hover:text-white">{icon}</span>
          <span className="text-xs font-bold uppercase leading-tight tracking-tight break-words">
            {label}
          </span>
        </div>
      </button>
    );
  }

  return (
    <Link href={href} className={commonClass}>
      <div className="flex h-full flex-col items-center justify-center gap-3 text-center">
        <span className="text-[#00685f] group-hover:text-white">{icon}</span>
        <span className="text-xs font-bold uppercase leading-tight tracking-tight break-words">
          {label}
        </span>
      </div>
    </Link>
  );
}

function recordStatusClass(status: HealthRecord["overallStatus"]) {
  if (status === "abnormal") return "bg-[#ffdad6] text-[#ba1a1a]";
  if (status === "attention") return "bg-[#ffdbce] text-[#773215]";
  if (status === "normal") return "bg-[#e6f6f2] text-[#00685f]";
  if (status === "error" || status === "failed" || status === "ocr_failed")
    return "bg-[#ffe4e6] text-[#be123c]";
  return "bg-[#f1f5f9] text-[#64748b]";
}

function recordStatusLabel(status: HealthRecord["overallStatus"]) {
  if (status === "abnormal") return "Bất thường";
  if (status === "attention") return "Cần chú ý";
  if (status === "normal") return "Bình thường";
  if (status === "error" || status === "failed" || status === "ocr_failed")
    return "Lỗi";
  return "Chưa xác thực";
}

function resolveHomeRecordStatus(record: HealthRecord): string {
  const recordStatus = record.status?.toLowerCase();
  if (recordStatus === "done") {
    if (record.overallStatus === "abnormal" || record.overallStatus === "attention") {
      return record.overallStatus;
    }
    return "normal";
  }
  if (
    recordStatus === "review_required" ||
    recordStatus === "processing" ||
    recordStatus === "pending"
  ) {
    return "unverified";
  }
  if (
    recordStatus === "ocr_failed" ||
    recordStatus === "failed" ||
    recordStatus === "error"
  ) {
    return "error";
  }
  return record.overallStatus === "abnormal" || record.overallStatus === "attention"
    ? record.overallStatus
    : "unverified";
}
