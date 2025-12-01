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
  list: (modelId: string) => request('list', { modelId }),
  load: (recordId: string) => request('load', { recordId }),
  save: (modelId: string, recordId: string | null, record: Record<string, any>) =>
    request('save', { modelId, recordId, record }),
  deleteRecord: (recordId: string) => request('deleteRecord', { recordId }),
  listSchema: (modelId: string) => request('listSchema', { modelId }),
  saveSchema: (modelId: string, modelName: string, isHiddenModel: boolean, fields: any[]) =>
    request('saveSchema', { modelId, modelName, isHiddenModel, fields }),
  deleteModel: (modelId: string) => request('deleteModel', { modelId }),
  listCodeGroups: () => request('listCodeGroups', {}),
  listCode: (groupId: string) => request('listCode', { id: groupId }),
  saveCode: (groupId: string, details: any[]) => request('saveCode', { groupId, details }),
  deleteCode: (codeGroupId: string) => request('deleteCode', { codeGroupId }),
};
