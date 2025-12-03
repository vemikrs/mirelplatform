import { apiClient as client } from './client';

export const getData = (modelId: string) => {
  return client.get<any[]>(`/api/studio/data/${modelId}`);
};

export const getDataById = (modelId: string, id: string) => {
  return client.get<any>(`/api/studio/data/${modelId}/${id}`);
};

export const createData = (modelId: string, data: any) => {
  return client.post(`/api/studio/data/${modelId}`, { content: data });
};

export const updateData = (modelId: string, id: string, data: any) => {
  return client.put(`/api/studio/data/${modelId}/${id}`, { content: data });
};

export const deleteData = (modelId: string, id: string) => {
  return client.delete(`/api/studio/data/${modelId}/${id}`);
};
