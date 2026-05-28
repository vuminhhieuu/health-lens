export interface User {
  id: string;
  email: string;
}

export interface Profile {
  id: string;
  userId: string;
  displayName: string;
}

export interface HealthRecord {
  id: string;
  profileId: string;
  examDate: string;
}

export type NotificationInboxItemType =
  | 'PROFILE_INVITATION'
  | 'HEALTH_RECORD_INVITATION'
  | 'REMINDER_UPCOMING';

export interface NotificationInboxItem {
  id: string;
  type: NotificationInboxItemType;
  title: string;
  body: string;
  createdAt: string;
  actionUrl: string;
  read: boolean;
}

export interface PaginationMeta {
  page: number;
  limit: number;
  total: number;
  totalPages: number;
}
