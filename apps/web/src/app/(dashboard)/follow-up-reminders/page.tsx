"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import type { FormEvent } from "react";
import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Bell,
  CalendarDays,
  ClipboardList,
  Pencil,
  Trash2,
  X,
} from "lucide-react";

import { ApiPaths } from "@healthlens/shared/constants";

import { DashboardPageShell } from "@/components/layout/DashboardPageShell";
import { apiClient } from "@/lib/api/apiClient";
import { notify } from "@/lib/notify";
import { EmptyState, ErrorState, InlineFieldError, LoadingState } from "@/components/ui";

const REMINDER_TYPES = [
  "Tái khám",
  "Xét nghiệm lại",
  "Theo dõi chỉ số",
  "Khác",
] as const;

type ReminderType = (typeof REMINDER_TYPES)[number];

type Profile = {
  id: string;
  displayName: string;
  isDefault: boolean;
};

type FollowUpReminder = {
  id: string;
  profileId: string;
  reminderDate: string;
  reminderType: ReminderType;
  note?: string | null;
  emailSentAt?: string | null;
  emailSkippedOptOutAt?: string | null;
  createdAt: string;
  updatedAt: string;
};

type FollowUpReminderPayload = {
  reminderDate: string;
  reminderType: ReminderType;
  note: string | null;
};

type FormErrors = {
  reminderDate?: string;
  reminderType?: string;
  submit?: string;
};

function formatDate(value: string) {
  const date = new Date(`${value}T00:00:00`);
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
}

function isValidDateInput(value: string) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) return false;

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const date = new Date(year, month - 1, day);

  return (
    date.getFullYear() === year &&
    date.getMonth() === month - 1 &&
    date.getDate() === day
  );
}

export default function FollowUpRemindersPage() {
  const searchParams = useSearchParams();
  const [manualProfileId, setManualProfileId] = useState("");

  const {
    data: profiles = [],
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["follow-up-reminder-profiles"],
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.PROFILES.BASE);
      return (response.data?.data ?? []) as Profile[];
    },
  });

  const profileFromQuery = searchParams.get("profileId")?.trim() ?? "";
  const hasProfiles = profiles.length > 0;
  const selectedProfileId = useMemo(() => {
    const manualProfile = profiles.find((profile) => profile.id === manualProfileId);
    if (manualProfile) return manualProfile.id;

    const queryProfile = profiles.find((profile) => profile.id === profileFromQuery);
    if (queryProfile) return queryProfile.id;

    return profiles.find((profile) => profile.isDefault)?.id ?? profiles[0]?.id ?? "";
  }, [manualProfileId, profileFromQuery, profiles]);

  const selectedProfile = useMemo(
    () => profiles.find((profile) => profile.id === selectedProfileId),
    [profiles, selectedProfileId],
  );

  const {
    data: reminders = [],
    isLoading: isRemindersLoading,
    isError: isRemindersError,
  } = useQuery({
    queryKey: ["follow-up-reminders", selectedProfileId],
    enabled: Boolean(selectedProfileId),
    queryFn: async () => {
      const response = await apiClient.get(
        ApiPaths.PROFILES.FOLLOW_UP_REMINDERS(selectedProfileId),
      );
      return (response.data?.data ?? []) as FollowUpReminder[];
    },
  });

  return (
    <DashboardPageShell
      title="Nhắc lịch tái khám"
      subtitle="Lưu lời nhắc cá nhân để bạn chủ động kiểm tra lại khi cần. HealthLens chưa đặt lịch trực tiếp với bệnh viện."
      breadcrumbs={[
        { label: "Trang chủ", href: "/home" },
        { label: "Nhắc lịch tái khám" },
      ]}
    >
      {isLoading ? (
        <LoadingState
          title="Đang tải hồ sơ sức khỏe"
          className="min-h-64 rounded-xl border-[#bcc9c6]/30 bg-white shadow-sm"
        />
      ) : null}

      {!isLoading && isError ? (
        <ErrorState
          title="Không thể tải danh sách hồ sơ"
          description="Vui lòng thử lại sau."
          className="min-h-64 rounded-xl border-[#f2b8b5] bg-[#fff8f7] shadow-sm"
        />
      ) : null}

      {!isLoading && !isError && !hasProfiles ? (
        <EmptyState
          title="Bạn chưa có hồ sơ sức khỏe"
          description="Tạo hồ sơ trước để HealthLens lưu nhắc lịch theo từng người."
          action={
            <Link
              href="/profiles"
              className="inline-flex min-h-12 items-center rounded-full bg-[#00685f] px-5 text-sm font-bold text-white transition hover:bg-[#008378] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005049]"
            >
              Quản lý hồ sơ
            </Link>
          }
          className="min-h-64 rounded-xl border-[#bcc9c6]/30 bg-white shadow-sm"
        />
      ) : null}

      {!isLoading && !isError && hasProfiles ? (
        <FollowUpReminderWorkspace
          key={selectedProfileId}
          profiles={profiles}
          selectedProfileId={selectedProfileId}
          selectedProfileName={selectedProfile?.displayName ?? "hồ sơ đã chọn"}
          reminders={reminders}
          isRemindersLoading={isRemindersLoading}
          isRemindersError={isRemindersError}
          onSelectProfile={setManualProfileId}
        />
      ) : null}
    </DashboardPageShell>
  );
}

function FollowUpReminderWorkspace({
  profiles,
  selectedProfileId,
  selectedProfileName,
  reminders,
  isRemindersLoading,
  isRemindersError,
  onSelectProfile,
}: {
  profiles: Profile[];
  selectedProfileId: string;
  selectedProfileName: string;
  reminders: FollowUpReminder[];
  isRemindersLoading: boolean;
  isRemindersError: boolean;
  onSelectProfile: (profileId: string) => void;
}) {
  const queryClient = useQueryClient();
  const [reminderDate, setReminderDate] = useState("");
  const [reminderType, setReminderType] = useState<ReminderType>("Tái khám");
  const [note, setNote] = useState("");
  const [errors, setErrors] = useState<FormErrors>({});
  const [successMessage, setSuccessMessage] = useState("");
  const [editingReminderId, setEditingReminderId] = useState<string | null>(null);
  const isEditing = Boolean(editingReminderId);
  const reminderQueryKey = ["follow-up-reminders", selectedProfileId] as const;

  const createReminderMutation = useMutation({
    mutationFn: async (payload: FollowUpReminderPayload) => {
      const response = await apiClient.post(
        ApiPaths.PROFILES.FOLLOW_UP_REMINDERS(selectedProfileId),
        payload,
      );
      return response.data?.data as FollowUpReminder;
    },
    onSuccess: (createdReminder) => {
      queryClient.setQueryData<FollowUpReminder[]>(reminderQueryKey, (current = []) =>
        sortReminders([...current, createdReminder]),
      );
      resetForm();
      setErrors({});
      setSuccessMessage("Đã lưu nhắc lịch cho hồ sơ này.");
      notify.success("Đã lưu nhắc lịch cho hồ sơ này.");
    },
    onError: () => {
      const message = "Không thể lưu nhắc lịch. Vui lòng thử lại sau.";
      setErrors({ submit: message });
      notify.error(message);
    },
  });

  const updateReminderMutation = useMutation({
    mutationFn: async ({
      reminderId,
      payload,
    }: {
      reminderId: string;
      payload: FollowUpReminderPayload;
    }) => {
      const response = await apiClient.put(
        ApiPaths.PROFILES.FOLLOW_UP_REMINDER_BY_ID(selectedProfileId, reminderId),
        payload,
      );
      return response.data?.data as FollowUpReminder;
    },
    onSuccess: (updatedReminder) => {
      queryClient.setQueryData<FollowUpReminder[]>(reminderQueryKey, (current = []) =>
        sortReminders(
          current.map((reminder) =>
            reminder.id === updatedReminder.id ? updatedReminder : reminder,
          ),
        ),
      );
      resetForm();
      setErrors({});
      setSuccessMessage("Đã cập nhật nhắc lịch cho hồ sơ này.");
      notify.success("Đã cập nhật nhắc lịch cho hồ sơ này.");
    },
    onError: () => {
      const message = "Không thể cập nhật nhắc lịch. Vui lòng thử lại sau.";
      setErrors({ submit: message });
      notify.error(message);
    },
  });

  const deleteReminderMutation = useMutation({
    mutationFn: async (reminderId: string) => {
      await apiClient.delete(
        ApiPaths.PROFILES.FOLLOW_UP_REMINDER_BY_ID(selectedProfileId, reminderId),
      );
      return reminderId;
    },
    onSuccess: (deletedReminderId) => {
      queryClient.setQueryData<FollowUpReminder[]>(reminderQueryKey, (current = []) =>
        current.filter((reminder) => reminder.id !== deletedReminderId),
      );
      if (editingReminderId === deletedReminderId) {
        resetForm();
      }
      setSuccessMessage("");
      setErrors((current) => ({ ...current, submit: undefined }));
      notify.success("Đã xóa nhắc lịch.");
    },
    onError: () => {
      const message = "Không thể xóa nhắc lịch. Vui lòng thử lại sau.";
      setErrors((current) => ({
        ...current,
        submit: message,
      }));
      notify.error(message);
    },
  });
  const isSaving = createReminderMutation.isPending || updateReminderMutation.isPending;

  function resetForm() {
    setReminderDate("");
    setReminderType("Tái khám");
    setNote("");
    setEditingReminderId(null);
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSuccessMessage("");

    const nextErrors: FormErrors = {};
    if (!reminderDate) {
      nextErrors.reminderDate = "Vui lòng chọn ngày nhắc.";
    } else if (!isValidDateInput(reminderDate)) {
      nextErrors.reminderDate = "Ngày nhắc không hợp lệ.";
    }

    if (!REMINDER_TYPES.includes(reminderType)) {
      nextErrors.reminderType = "Vui lòng chọn loại nhắc hợp lệ.";
    }

    if (!selectedProfileId) {
      nextErrors.submit = "Vui lòng tạo hồ sơ trước khi lưu nhắc lịch.";
    }

    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      return;
    }

    const payload: FollowUpReminderPayload = {
      reminderDate,
      reminderType,
      note: note.trim() || null,
    };

    if (editingReminderId) {
      updateReminderMutation.mutate({
        reminderId: editingReminderId,
        payload,
      });
      return;
    }

    createReminderMutation.mutate(payload);
  }

  function handleStartEdit(reminder: FollowUpReminder) {
    setEditingReminderId(reminder.id);
    setReminderDate(reminder.reminderDate);
    setReminderType(reminder.reminderType);
    setNote(reminder.note ?? "");
    setSuccessMessage("");
    setErrors({});
  }

  function handleCancelEdit() {
    resetForm();
    setSuccessMessage("");
    setErrors({});
  }

  function handleDelete(reminderId: string) {
    const confirmed = window.confirm("Bạn có chắc muốn xóa nhắc lịch này?");
    if (!confirmed || !selectedProfileId) return;
    deleteReminderMutation.mutate(reminderId);
  }

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
      <section className="lg:col-span-5">
        <form
          onSubmit={handleSubmit}
          className="rounded-xl border border-[#bcc9c6]/30 bg-white p-5 shadow-sm"
          noValidate
        >
          <div className="mb-5 flex items-start gap-3">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[#e9f6f3] text-[#00685f]">
              <Bell className="h-5 w-5" aria-hidden="true" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-[#121e1c]">
                {isEditing ? "Sửa nhắc lịch" : "Tạo nhắc lịch"}
              </h2>
              <p className="mt-1 text-sm leading-6 text-[#4e6360]">
                Nhắc lịch cho {selectedProfileName}.
              </p>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <label
                htmlFor="follow-up-profile"
                className="mb-2 block text-sm font-bold text-[#121e1c]"
              >
                Hồ sơ
              </label>
              <select
                id="follow-up-profile"
                value={selectedProfileId}
                onChange={(event) => {
                  onSelectProfile(event.target.value);
                  setSuccessMessage("");
                }}
                className="min-h-12 w-full rounded-xl border border-[#b7e8e0] bg-[#f7fffd] px-4 text-sm font-semibold text-[#3d4947] outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#b7e8e0]"
              >
                {profiles.map((profile) => (
                  <option key={profile.id} value={profile.id}>
                    {profile.displayName}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label
                htmlFor="follow-up-date"
                className="mb-2 block text-sm font-bold text-[#121e1c]"
              >
                Ngày nhắc <span className="text-[#ba1a1a]">*</span>
              </label>
              <input
                id="follow-up-date"
                type="date"
                value={reminderDate}
                onChange={(event) => setReminderDate(event.target.value)}
                aria-describedby={errors.reminderDate ? "follow-up-date-error" : undefined}
                aria-invalid={Boolean(errors.reminderDate)}
                className="min-h-12 w-full rounded-xl border border-[#b7e8e0] bg-[#f7fffd] px-4 text-sm font-semibold text-[#3d4947] outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#b7e8e0]"
              />
              <InlineFieldError id="follow-up-date-error" message={errors.reminderDate} />
            </div>

            <div>
              <label
                htmlFor="follow-up-type"
                className="mb-2 block text-sm font-bold text-[#121e1c]"
              >
                Loại nhắc <span className="text-[#ba1a1a]">*</span>
              </label>
              <select
                id="follow-up-type"
                value={reminderType}
                onChange={(event) => setReminderType(event.target.value as ReminderType)}
                aria-describedby={errors.reminderType ? "follow-up-type-error" : undefined}
                aria-invalid={Boolean(errors.reminderType)}
                className="min-h-12 w-full rounded-xl border border-[#b7e8e0] bg-[#f7fffd] px-4 text-sm font-semibold text-[#3d4947] outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#b7e8e0]"
              >
                {REMINDER_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>
              <InlineFieldError id="follow-up-type-error" message={errors.reminderType} />
            </div>

            <div>
              <label
                htmlFor="follow-up-note"
                className="mb-2 block text-sm font-bold text-[#121e1c]"
              >
                Ghi chú
              </label>
              <textarea
                id="follow-up-note"
                value={note}
                onChange={(event) => setNote(event.target.value)}
                rows={4}
                placeholder="Ví dụ: Mang kết quả lần trước hoặc hỏi bác sĩ về chỉ số cần theo dõi."
                className="w-full rounded-xl border border-[#b7e8e0] bg-[#f7fffd] px-4 py-3 text-sm font-medium leading-6 text-[#3d4947] outline-none transition focus:border-[#00685f] focus:ring-2 focus:ring-[#b7e8e0]"
              />
            </div>

            <InlineFieldError id="follow-up-submit-error" message={errors.submit} />

            {successMessage ? (
              <p
                role="status"
                aria-live="polite"
                className="rounded-xl border border-[#8fd8cc] bg-[#e9f6f3] px-4 py-3 text-sm font-semibold text-[#00685f]"
              >
                {successMessage}
              </p>
            ) : null}

            <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
              <button
                type="submit"
                disabled={isSaving}
                className="inline-flex min-h-12 w-full items-center justify-center rounded-full bg-[#00685f] px-5 text-sm font-bold text-white transition hover:bg-[#008378] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005049] disabled:cursor-not-allowed disabled:opacity-70 sm:w-auto"
              >
                {isSaving
                  ? "Đang lưu..."
                  : isEditing
                    ? "Cập nhật nhắc lịch"
                    : "Lưu nhắc lịch"}
              </button>
              {isEditing ? (
                <button
                  type="button"
                  onClick={handleCancelEdit}
                  className="inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-full border border-[#bcc9c6] bg-white px-5 text-sm font-bold text-[#3d4947] transition hover:bg-[#f7fffd] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f] sm:w-auto"
                >
                  <X className="h-4 w-4" aria-hidden="true" />
                  Hủy sửa
                </button>
              ) : null}
            </div>
          </div>
        </form>
      </section>

      <section className="lg:col-span-7">
        <div className="rounded-xl border border-[#bcc9c6]/30 bg-white p-5 shadow-sm">
          <div className="mb-5 flex items-start gap-3">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[#e9f6f3] text-[#00685f]">
              <ClipboardList className="h-5 w-5" aria-hidden="true" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-[#121e1c]">Danh sách nhắc lịch</h2>
              <p className="mt-1 text-sm leading-6 text-[#4e6360]">
                HealthLens sẽ gửi email nhắc vào ngày đã lưu.
              </p>
            </div>
          </div>

          {isRemindersLoading ? (
            <LoadingState
              title="Đang tải nhắc lịch"
              className="min-h-24 rounded-xl border-[#bcc9c6] bg-[#f7fffd]"
            />
          ) : null}

          {!isRemindersLoading && isRemindersError ? (
            <ErrorState
              title="Không thể tải danh sách nhắc lịch"
              description="Vui lòng thử lại sau."
              className="min-h-24 rounded-xl border-[#f2b8b5] bg-[#fff8f7]"
            />
          ) : null}

          {!isRemindersLoading && !isRemindersError && reminders.length === 0 ? (
            <EmptyState
              title="Chưa có nhắc lịch"
              description="Chưa có nhắc lịch cho hồ sơ này."
              className="min-h-24 rounded-xl border-[#bcc9c6] bg-[#f7fffd]"
            />
          ) : null}

          {!isRemindersLoading && !isRemindersError && reminders.length > 0 ? (
            <ul className="space-y-3">
              {reminders.map((reminder) => (
                <li
                  key={reminder.id}
                  className="rounded-xl border border-[#bcc9c6]/40 bg-[#f7fffd] p-4"
                >
                  <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                    <div className="min-w-0">
                      <p className="text-sm font-bold text-[#00685f]">
                        {reminder.reminderType}
                      </p>
                      <p className="mt-2 flex items-center gap-2 text-base font-bold text-[#121e1c]">
                        <CalendarDays className="h-5 w-5 shrink-0 text-[#00685f]" aria-hidden="true" />
                        {formatDate(reminder.reminderDate)}
                      </p>
                      {reminder.note ? (
                        <p className="mt-2 break-words text-sm leading-6 text-[#4e6360]">
                          {reminder.note}
                        </p>
                      ) : null}
                      {reminder.emailSentAt ? (
                        <p className="mt-2 text-xs font-semibold text-[#00685f]">
                          Email nhắc lịch đã được gửi.
                        </p>
                      ) : reminder.emailSkippedOptOutAt ? (
                        <p className="mt-2 text-xs font-semibold text-[#6d7a77]">
                          Email nhắc lịch đã tắt trong Cài đặt → Thông báo. Bật lại &quot;Nhắc tái khám&quot; để nhận email.
                        </p>
                      ) : null}
                    </div>
                    <div className="flex gap-2">
                      <button
                        type="button"
                        aria-label="Sửa nhắc lịch"
                        title="Sửa nhắc lịch"
                        onClick={() => handleStartEdit(reminder)}
                        className="inline-flex h-11 w-11 items-center justify-center rounded-full border border-[#8fd8cc] bg-white text-[#00685f] transition hover:bg-[#e9f6f3] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00685f]"
                      >
                        <Pencil className="h-5 w-5" aria-hidden="true" />
                      </button>
                      <button
                        type="button"
                        aria-label="Xóa nhắc lịch"
                        title="Xóa nhắc lịch"
                        onClick={() => handleDelete(reminder.id)}
                        className="inline-flex h-11 w-11 items-center justify-center rounded-full border border-[#f2b8b5] bg-white text-[#ba1a1a] transition hover:bg-[#fff8f7] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#ba1a1a]"
                      >
                        <Trash2 className="h-5 w-5" aria-hidden="true" />
                      </button>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          ) : null}
        </div>
      </section>
    </div>
  );
}

function sortReminders(reminders: FollowUpReminder[]) {
  return [...reminders].sort((a, b) => a.reminderDate.localeCompare(b.reminderDate));
}
