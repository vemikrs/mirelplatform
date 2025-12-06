import { apiClient as client } from './client';

export interface Release {
  releaseId: string;
  modelId: string;
  version: number;
  snapshot: string; // JSON string
  createdAt: string;
}

export const getReleases = async (modelId: string) => {
  return client.get<Release[]>(`/api/studio/releases/${modelId}`);
};

export const createRelease = async (modelId: string) => {
  return client.post<Release>('/api/studio/releases', { modelId });
};
