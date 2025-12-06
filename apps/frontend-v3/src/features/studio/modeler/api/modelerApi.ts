import type { SchemaApiResponse } from '../types/modeler';

const BASE_URL = '/mapi/api/studio/modeler';

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

export const modelerApi = {
  list: async (modelId: string, page: number = 1, size: number = 20, query: string = '') => {
      // Use DataController (REST)
      // GET /api/studio/data/{modelId}?page=...
      const params = new URLSearchParams({ page: page.toString(), size: size.toString(), q: query });
      const response = await fetch(`/mapi/api/studio/data/${modelId}?${params}`);
      if (!response.ok) throw new Error('Data API Error');
      const data = await response.json();
      return { data }; // Wrap to match expected SchemaApiResponse structure if needed, or update callers
  },
  load: (recordId: string) => request('load', { recordId }), // Legacy or DataController? DataController has get(recordId)
  save: (modelId: string, recordId: string | null, record: Record<string, any>) =>
    request('save', { modelId, recordId, record }), // DataController has POST/PUT
  deleteRecord: (recordId: string) => request('deleteRecord', { recordId }), // DataController has DELETE

  // Model Operations (REST)
  listModels: async () => {
    const response = await fetch('/mapi/api/studio/models');
    if (!response.ok) throw new Error('Failed to list models');
    const result = await response.json(); // ApiResponse<List<StuModelHeader>>
    // Transform to expected format { models: { value, text }[] }
    const models = (result.data || []).map((h: any) => ({
      value: h.modelId,
      text: h.modelName
    }));
    return { data: { models } };
  },

  listModel: async (modelId: string) => {
      const response = await fetch(`/mapi/api/studio/models/${modelId}`);
      if (!response.ok) throw new Error('Failed to get model');
      const result = await response.json();
      // Transform to expected format if necessary. 
      // Previous RPC 'listModel' returned something specific?
      // Check EntityListPage usage? EntityListPage lists models. 
      // FormDesigner uses loadModel/getModel?
      // Let's assume result.data (ModelDetailsDto) is what we need or adaptable.
      return { data: result.data };
  },

  saveModel: async (modelId: string, modelName: string, isHiddenModel: boolean, _modelType: 'transaction' | 'master', fields: any[]) => {
      // 1. Update Header
      await fetch(`/mapi/api/studio/models/${modelId}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ modelName, isHidden: isHiddenModel })
      });
      // 2. Update Draft (Fields)
      await fetch(`/mapi/api/studio/models/${modelId}/draft`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ headerName: modelName, fields })
      });
      return { data: { modelId } };
  },

  deleteModel: async (modelId: string) => {
      await fetch(`/mapi/api/studio/models/${modelId}`, { method: 'DELETE' });
      return { data: { deleted: true } };
  },

  // Code Operations (Legacy RPC)
  listCodeGroups: () => request('listCodeGroups', {}),
  listCode: (groupId: string) => request('listCode', { id: groupId }),
  saveCode: (groupId: string, details: any[]) => request('saveCode', { groupId, details }),
  deleteCode: (codeGroupId: string) => request('deleteCode', { codeGroupId }),
};
