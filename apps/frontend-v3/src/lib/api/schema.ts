import { apiClient } from './client';
import type { ApiResponse } from './types';

export interface StuField {
  fieldId: string;
  modelId?: string;
  fieldName: string;
  fieldCode: string;
  fieldType: string;
  isRequired: boolean;
  validationRegex?: string;
  minValue?: number;
  maxValue?: number;
  minLength?: number;
  maxLength?: number;
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

export interface SchemaSummary {
  modelId: string;
  modelName: string;
  description?: string;
  status: string;
  version: number;
  updatedAt: string;
}

export interface SchemaDetail extends SchemaSummary {
  fields: StuField[];
}

export const getSchemas = async () => {
  const response = await apiClient.get<ApiResponse<SchemaSummary[]>>('/api/studio/schema');
  return response.data;
};

export const getSchema = async (modelId: string) => {
  const response = await apiClient.get<ApiResponse<SchemaDetail>>(`/api/studio/schema/${modelId}`);
  return response.data;
};

// Data API
export const listData = async (modelId: string) => {
  const response = await apiClient.get<any[]>(`/api/studio/data/${modelId}`);
  return response.data;
};

export const getData = async (modelId: string, recordId: string) => {
  const response = await apiClient.get<any>(`/api/studio/data/${modelId}/${recordId}`);
  return response.data;
};

export const createData = async (modelId: string, data: any) => {
  const response = await apiClient.post<void>(`/api/studio/data/${modelId}`, data);
  return response;
};

export const updateData = async (modelId: string, recordId: string, data: any) => {
  const response = await apiClient.put<void>(`/api/studio/data/${modelId}/${recordId}`, data);
  return response;
};

export const deleteData = async (modelId: string, recordId: string) => {
  const response = await apiClient.delete<void>(`/api/studio/data/${modelId}/${recordId}`);
  return response;
};
