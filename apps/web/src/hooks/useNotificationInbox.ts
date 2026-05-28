"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { apiClient } from "@/lib/api/apiClient";
import { ApiPaths } from "@/lib/api/routes";
import type { NotificationInboxItem, PaginationMeta } from "@healthlens/shared/types";

const INBOX_QUERY_KEY = ["notification-inbox"] as const;
const INBOX_REFETCH_MS = 30_000;
const SUMMARY_LIMIT = 5;
const PAGED_LIMIT = 10;

type NotificationInboxMode = "summary" | "paged";

type UseNotificationInboxOptions = {
  enabled?: boolean;
  mode?: NotificationInboxMode;
  page?: number;
  limit?: number;
};

type InboxQueryData = {
  items: NotificationInboxItem[];
  unreadCount: number;
  pagination: PaginationMeta;
};

const EMPTY_PAGINATION: PaginationMeta = {
  page: 0,
  limit: PAGED_LIMIT,
  total: 0,
  totalPages: 0,
};

function mapInboxResponse(data: unknown): NotificationInboxItem[] {
  if (!Array.isArray(data)) {
    return [];
  }
  return data as NotificationInboxItem[];
}

function mapPagination(data: unknown, fallbackLimit: number): PaginationMeta {
  if (!data || typeof data !== "object") {
    return { ...EMPTY_PAGINATION, limit: fallbackLimit };
  }
  const pagination = data as Partial<PaginationMeta>;
  return {
    page: typeof pagination.page === "number" ? pagination.page : 0,
    limit: typeof pagination.limit === "number" ? pagination.limit : fallbackLimit,
    total: typeof pagination.total === "number" ? pagination.total : 0,
    totalPages: typeof pagination.totalPages === "number" ? pagination.totalPages : 0,
  };
}

function mapUnreadCount(meta: unknown, items: NotificationInboxItem[]): number {
  if (meta && typeof meta === "object" && typeof (meta as { unreadCount?: unknown }).unreadCount === "number") {
    return (meta as { unreadCount: number }).unreadCount;
  }
  return items.filter((item) => !item.read).length;
}

function patchItemRead(
  data: InboxQueryData,
  itemId: string,
  decrementUnreadCount: boolean,
): InboxQueryData {
  return {
    ...data,
    items: data.items.map((item) => (item.id === itemId ? { ...item, read: true } : item)),
    unreadCount: decrementUnreadCount ? Math.max(0, data.unreadCount - 1) : data.unreadCount,
  };
}

function patchAllRead(data: InboxQueryData): InboxQueryData {
  return {
    ...data,
    items: data.items.map((item) => ({ ...item, read: true })),
    unreadCount: 0,
  };
}

export function useNotificationInbox(options: UseNotificationInboxOptions | boolean = true) {
  const queryClient = useQueryClient();
  const normalizedOptions = typeof options === "boolean" ? { enabled: options } : options;
  const mode = normalizedOptions.mode ?? "summary";
  const page = mode === "paged" ? Math.max(0, normalizedOptions.page ?? 0) : 0;
  const limit = normalizedOptions.limit ?? (mode === "paged" ? PAGED_LIMIT : SUMMARY_LIMIT);
  const enabled = normalizedOptions.enabled ?? true;
  const queryKey = [...INBOX_QUERY_KEY, mode, page, limit] as const;

  const query = useQuery({
    queryKey,
    queryFn: async () => {
      const response = await apiClient.get(ApiPaths.NOTIFICATIONS.INBOX, {
        params: { page, limit },
      });
      const items = mapInboxResponse(response.data?.data);
      return {
        items,
        unreadCount: mapUnreadCount(response.data?.meta, items),
        pagination: mapPagination(response.data?.pagination, limit),
      };
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
      const previous = queryClient.getQueriesData<InboxQueryData>({
        queryKey: INBOX_QUERY_KEY,
      });
      const decrementUnreadCount = previous.some(([, data]) =>
        Boolean(data?.items.some((item) => item.id === itemId && !item.read)),
      );
      queryClient.setQueriesData<InboxQueryData>({ queryKey: INBOX_QUERY_KEY }, (current) =>
        current ? patchItemRead(current, itemId, decrementUnreadCount) : current,
      );
      return { previous };
    },
    onError: (_error, _itemId, context) => {
      if (context?.previous) {
        context.previous.forEach(([cacheKey, data]) => {
          queryClient.setQueryData(cacheKey, data);
        });
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: INBOX_QUERY_KEY });
    },
  });

  const markAllAsReadMutation = useMutation({
    mutationFn: async () => {
      await apiClient.post(ApiPaths.NOTIFICATIONS.INBOX_READ_ALL);
    },
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: INBOX_QUERY_KEY });
      const previous = queryClient.getQueriesData<InboxQueryData>({
        queryKey: INBOX_QUERY_KEY,
      });
      queryClient.setQueriesData<InboxQueryData>({ queryKey: INBOX_QUERY_KEY }, (current) =>
        current ? patchAllRead(current) : current,
      );
      return { previous };
    },
    onError: (_error, _vars, context) => {
      if (context?.previous) {
        context.previous.forEach(([cacheKey, data]) => {
          queryClient.setQueryData(cacheKey, data);
        });
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: INBOX_QUERY_KEY });
    },
  });

  const items = query.data?.items ?? [];
  const unreadCount = query.data?.unreadCount ?? 0;
  const pagination = query.data?.pagination ?? { ...EMPTY_PAGINATION, page, limit };

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
    pagination,
    markAsRead,
    markAsReadAsync,
    markAllAsRead,
    isMarkingRead: markAsReadMutation.isPending,
    isMarkingAllRead: markAllAsReadMutation.isPending,
  };
}
