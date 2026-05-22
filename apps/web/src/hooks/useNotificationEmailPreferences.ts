"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { apiClient } from "@/lib/api/apiClient";
import { API_ROUTES } from "@/lib/api/routes";
import { notify } from "@/lib/notify";

export type NotificationEmailPreferences = {
  shareInvite: boolean;
  shareAccepted: boolean;
  followUpReminder: boolean;
  security: boolean;
};

const QUERY_KEY = ["notificationEmailPreferences"] as const;

export function useNotificationEmailPreferences() {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: QUERY_KEY,
    queryFn: async () => {
      const response = await apiClient.get(API_ROUTES.USERS.ME_NOTIFICATION_PREFERENCES);
      return response.data.data as NotificationEmailPreferences;
    },
  });

  const saveMutation = useMutation({
    mutationFn: async (prefs: NotificationEmailPreferences) => {
      const response = await apiClient.put(API_ROUTES.USERS.ME_NOTIFICATION_PREFERENCES, {
        shareInvite: prefs.shareInvite,
        shareAccepted: prefs.shareAccepted,
        followUpReminder: prefs.followUpReminder,
        security: true,
      });
      return response.data.data as NotificationEmailPreferences;
    },
    onSuccess: (data) => {
      queryClient.setQueryData(QUERY_KEY, data);
      notify.success("Đã lưu tùy chọn email");
    },
    onError: () => {
      notify.error("Không thể lưu tùy chọn email. Vui lòng thử lại.");
    },
  });

  return {
    preferences: query.data,
    isLoading: query.isLoading,
    isError: query.isError,
    refetch: query.refetch,
    savePreferences: saveMutation.mutate,
    isSaving: saveMutation.isPending,
  };
}
