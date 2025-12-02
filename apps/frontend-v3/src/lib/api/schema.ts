import { apiClient } from './client';
import type { ApiResponse } from './types';

export interface StuField {
  fieldId: string;
  modelId?: string;
  fieldName: string;
  fieldType: string;
  isRequired: boolean;
  sortOrder: number;
  layout?: string; // JSON string
}

export interface CreateDraftRequest {
  name: string;
  description?: string;
}

export interface UpdateDraftRequest {
  name: string;
  description?: string;
  fields: StuField[];
}

export const createDraft = async (payload: CreateDraftRequest) => {
  const response = await apiClient.post<ApiResponse<string>>('/api/studio/schema/draft', payload);
  return response.data;
};

export const updateDraft = async (modelId: string, payload: UpdateDraftRequest) => {
  const response = await apiClient.put<ApiResponse<void>>(`/api/studio/schema/draft/${modelId}`, payload);
  return response.data;
};

export const publishSchema = async (modelId: string) => {
  const response = await apiClient.post<ApiResponse<void>>(`/api/studio/schema/${modelId}/publish`);
  return response.data;
};
