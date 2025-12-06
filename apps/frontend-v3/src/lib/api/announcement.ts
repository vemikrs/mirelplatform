import { apiClient as client } from './client';

export interface Announcement {
  announcementId: string;
  tenantId: string | null;
  title: string;
  content: string;
  summary: string;
  category: 'MAINTENANCE' | 'RELEASE' | 'INFO' | 'ALERT' | 'TERMS' | 'EVENT';
  priority: 'CRITICAL' | 'HIGH' | 'NORMAL' | 'LOW';
  status: 'DRAFT' | 'SCHEDULED' | 'PUBLISHED' | 'ARCHIVED';
  publishAt: string;
  expireAt: string | null;
  isPinned: boolean;
  requiresAcknowledgment: boolean;
  authorId: string;
  createdAt: string;
  updatedAt: string;
}

export interface AnnouncementResponse {
  items: Announcement[];
  readIds: string[];
}

export interface UnreadCountResponse {
  count: number;
}

export const getAnnouncements = async (): Promise<AnnouncementResponse> => {
  const response = await client.get<AnnouncementResponse>('/api/announcements');
  return response.data;
};

export const getUnreadCount = async (): Promise<UnreadCountResponse> => {
  const response = await client.get<UnreadCountResponse>('/api/announcements/unread-count');
  return response.data;
};

export const markAsRead = async (announcementId: string): Promise<void> => {
  await client.post(`/api/announcements/${announcementId}/read`);
};

export const markAsUnread = async (announcementId: string): Promise<void> => {
  await client.delete(`/api/announcements/${announcementId}/read`);
};
