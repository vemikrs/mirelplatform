import type { SchemaApiResponse } from '../types/schema';

const BASE_URL = '/apps/schema/api';

async function request(path: string, content: Record<string, any>): Promise<SchemaApiResponse> {
  const response = await fetch(`${BASE_URL}/${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ content }),
  });

  if (!response.ok) {
    throw new Error(`API Error: ${response.status} ${response.statusText}`);
  }

  return response.json();
}

export const schemaApi = {
  list: (modelId: string, page: number = 1, size: number = 20, query: string = '') => {
    // In a real implementation, these params would be sent to the backend.
    // For now, we'll simulate it or pass them if backend supports it.
    // Assuming backend might support query params like ?page=1&size=20&q=...
    return request('list', { modelId, page, size, query });
  },
  load: (recordId: string) => request('load', { recordId }),
  save: (modelId: string, recordId: string | null, record: Record<string, any>) =>
    request('save', { modelId, recordId, record }),
  deleteRecord: (recordId: string) => request('deleteRecord', { recordId }),
  listSchema: (modelId: string) => request('listSchema', { modelId }),
  saveSchema: (modelId: string, modelName: string, isHiddenModel: boolean, modelType: 'transaction' | 'master', fields: any[]) =>
    request('saveSchema', { modelId, modelName, isHiddenModel, modelType, fields }),
  deleteModel: (modelId: string) => request('deleteModel', { modelId }),
  listModels: () => request('listModels', {}),
  listCodeGroups: () => request('listCodeGroups', {}),
  listCode: (groupId: string) => request('listCode', { id: groupId }),
  saveCode: (groupId: string, details: any[]) => request('saveCode', { groupId, details }),
  deleteCode: (codeGroupId: string) => request('deleteCode', { codeGroupId }),
};
