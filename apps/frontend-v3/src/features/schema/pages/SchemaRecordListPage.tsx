import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { schemaApi } from '../api/schemaApi';
import type { SchDicModel, SchRecord } from '../types/schema';
import { RecordGrid } from '../components/RecordGrid';
import { ModelSelector } from '../components/ModelSelector';

export const SchemaRecordListPage: React.FC = () => {
  const navigate = useNavigate();
  const [models, setModels] = useState<{ value: string; text: string }[]>([]);
  const [selectedModelId, setSelectedModelId] = useState<string>('');
  const [fields, setFields] = useState<SchDicModel[]>([]);
  const [records, setRecords] = useState<SchRecord[]>([]);

  useEffect(() => {
    // TODO: Fetch available models from API
    // For now, hardcode or fetch if endpoint exists
    // schemaApi.getApps().then(...)
    setModels([
      { value: 'sample_model', text: 'Sample Model' },
      // Add more models as needed
    ]);
  }, []);

  useEffect(() => {
    if (selectedModelId) {
      loadModelAndRecords(selectedModelId);
    }
  }, [selectedModelId]);

  const loadModelAndRecords = async (modelId: string) => {
    try {
      const schemaResponse = await schemaApi.listSchema(modelId);
      setFields(schemaResponse.data.schemas);

      const recordsResponse = await schemaApi.list(modelId);
      setRecords(recordsResponse.data.records);
    } catch (error) {
      console.error('Failed to load data:', error);
    }
  };

  const handleRowClick = (record: SchRecord) => {
    navigate(`/apps/schema/records/${selectedModelId}/${record.id}`);
  };

  const handleCreateNew = () => {
    if (selectedModelId) {
      navigate(`/apps/schema/records/${selectedModelId}/new`);
    }
  };

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-4">Record List</h1>
      <div className="flex justify-between items-center mb-4">
        <ModelSelector
          models={models}
          selectedModelId={selectedModelId}
          onSelect={setSelectedModelId}
        />
        <button
          onClick={handleCreateNew}
          disabled={!selectedModelId}
          className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 disabled:bg-gray-400"
        >
          Create New
        </button>
      </div>
      {selectedModelId && (
        <RecordGrid fields={fields} records={records} onRowClick={handleRowClick} />
      )}
    </div>
  );
};
