"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { apiClient } from "@/lib/api/apiClient";
import { ApiPaths } from "@/lib/api/routes";
import type { NotificationInboxItem } from "@healthlens/shared/types";

const INBOX_QUERY_KEY = ["notification-inbox"] as const;
const INBOX_REFETCH_MS = 30_000;

function mapInboxResponse(data: unknown): NotificationInboxItem[] {
  if (!Array.isArray(data)) {
    return [];
  }
  return data as NotificationInboxItem[];
}

function patchItemRead(items: NotificationInboxItem[], itemId: string): NotificationInboxItem[] {
  return items.map((item) => (item.id === itemId ? { ...item, read: true } : item));
}

function patchAllRead(items: NotificationInboxItem[]): NotificationInboxItem[] {
  return items.map((item) => ({ ...item, read: true }));
}

export function useNotificationInbox(enabled = true) {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: INBOX_QUERY_KEY,
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.NOTIFICATIONS.INBOX);
      return mapInboxResponse(response.data?.data);
    },
    enabled,
    refetchInterval: INBOX_REFETCH_MS,
    refetchOnWindowFocus: true,
  });

  const markAsReadMutation = useMutation({
    mutationFn: async (itemId: string) => {
      await apiClient.post(ApiPaths.NOTIFICATIONS.INBOX_READ, { itemId });
    },
    onMutate: async (itemId) => {
      await queryClient.cancelQueries({ queryKey: INBOX_QUERY_KEY });
      const previous = queryClient.getQueryData<NotificationInboxItem[]>(INBOX_QUERY_KEY);
      if (previous) {
        queryClient.setQueryData(INBOX_QUERY_KEY, patchItemRead(previous, itemId));
      }
      return { previous };
    },
    onSuccess: (_data, itemId) => {
      queryClient.setQueryData<NotificationInboxItem[]>(INBOX_QUERY_KEY, (current) =>
        current ? patchItemRead(current, itemId) : current,
      );
    },
    onError: (_error, _itemId, context) => {
      if (context?.previous) {
        queryClient.setQueryData(INBOX_QUERY_KEY, context.previous);
      }
    },
  });

  const markAllAsReadMutation = useMutation({
    mutationFn: async () => {
      await apiClient.post(ApiPaths.NOTIFICATIONS.INBOX_READ_ALL);
    },
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: INBOX_QUERY_KEY });
      const previous = queryClient.getQueryData<NotificationInboxItem[]>(INBOX_QUERY_KEY);
      if (previous) {
        queryClient.setQueryData(INBOX_QUERY_KEY, patchAllRead(previous));
      }
      return { previous };
    },
    onSuccess: () => {
      queryClient.setQueryData<NotificationInboxItem[]>(INBOX_QUERY_KEY, (current) =>
        current ? patchAllRead(current) : current,
      );
    },
    onError: (_error, _vars, context) => {
      if (context?.previous) {
        queryClient.setQueryData(INBOX_QUERY_KEY, context.previous);
      }
    },
  });

  const items = query.data ?? [];
  const unreadCount = items.filter((item) => !item.read).length;

  const markAsRead = (itemId: string) => {
    const item = items.find((entry) => entry.id === itemId);
    if (item?.read) {
      return;
    }
    markAsReadMutation.mutate(itemId);
  };

  const markAsReadAsync = async (itemId: string) => {
    const item = items.find((entry) => entry.id === itemId);
    if (item?.read) {
      return;
    }
    await markAsReadMutation.mutateAsync(itemId);
  };

  const markAllAsRead = () => {
    if (unreadCount === 0) {
      return;
    }
    markAllAsReadMutation.mutate();
  };

  return {
    ...query,
    items,
    unreadCount,
    markAsRead,
    markAsReadAsync,
    markAllAsRead,
    isMarkingRead: markAsReadMutation.isPending,
    isMarkingAllRead: markAllAsReadMutation.isPending,
  };
}
