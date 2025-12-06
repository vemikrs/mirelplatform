import { apiClient as client } from './client';

export interface AnnouncementSearchCondition {
  title?: string;
  categories?: string[];
  statuses?: string[];
}

export interface AnnouncementSaveDto {
  title: string;
  content: string;
  summary?: string;
  category: string;
  priority: string;
  status: string;
  publishAt?: string;
  expireAt?: string;
  isPinned: boolean;
  requiresAcknowledgment: boolean;
  targets: TargetDto[];
}

export interface TargetDto {
  targetType: 'ALL' | 'TENANT' | 'USER' | 'ROLE';
  targetId: string;
}

export const searchAnnouncements = async (condition: AnnouncementSearchCondition, page = 0, size = 20) => {
  const response = await client.post(`/api/admin/announcements/search?page=${page}&size=${size}`, condition);
  return response.data;
};

export const getAnnouncement = async (id: string) => {
  const response = await client.get(`/api/admin/announcements/${id}`);
  return response.data;
};

export const createAnnouncement = async (dto: AnnouncementSaveDto) => {
  const response = await client.post('/api/admin/announcements', dto);
  return response.data;
};

export const updateAnnouncement = async (id: string, dto: AnnouncementSaveDto) => {
  const response = await client.put(`/api/admin/announcements/${id}`, dto);
  return response.data;
};

export const deleteAnnouncement = async (id: string) => {
  await client.delete(`/api/admin/announcements/${id}`);
};
