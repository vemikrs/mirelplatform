import { apiClient as client } from './client';

export interface Flow {
  flowId: string;
  modelId: string;
  flowName: string;
  triggerType: string;
  definition: string; // JSON string
  createdAt: string;
  updatedAt: string;
}

export const getFlows = async (modelId: string) => {
  return client.get<Flow[]>(`/api/studio/flows/${modelId}`);
};

export const createFlow = async (data: { modelId: string; name: string; triggerType: string }) => {
  return client.post<Flow>('/api/studio/flows', data);
};

export const updateFlow = async (flowId: string, data: { name?: string; triggerType?: string; definition?: string }) => {
  return client.put<Flow>(`/api/studio/flows/${flowId}`, data);
};

export const deleteFlow = async (flowId: string) => {
  return client.delete(`/api/studio/flows/${flowId}`);
};

export const executeFlow = async (flowId: string, context: Record<string, any>) => {
  return client.post(`/api/studio/flows/${flowId}/execute`, context);
};
