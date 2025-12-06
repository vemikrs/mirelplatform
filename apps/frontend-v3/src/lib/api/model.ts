import { apiClient as client } from './client';

export const getModel = (modelId: string) => {
  return client.get<any>(`/api/studio/models/${modelId}`);
};
