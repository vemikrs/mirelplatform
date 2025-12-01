import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
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
    <div className="p-6 space-y-6">
      <div className="flex items-center gap-4 mb-6">
        <Link to="/apps/schema" className="text-muted-foreground hover:text-foreground">
          ← 戻る
        </Link>
        <h1 className="text-2xl font-bold text-foreground">データ管理</h1>
      </div>

      <div className="flex justify-between items-center mb-4 p-4 border border-border rounded-lg bg-card shadow-sm">
        <ModelSelector
          models={models}
          selectedModelId={selectedModelId}
          onSelect={setSelectedModelId}
        />
        <button
          onClick={handleCreateNew}
          disabled={!selectedModelId}
          className="px-4 py-2 bg-primary text-primary-foreground rounded hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-sm"
        >
          新規作成
        </button>
      </div>
      {selectedModelId && (
        <div className="border border-border rounded-lg bg-card shadow-sm overflow-hidden">
          <RecordGrid fields={fields} records={records} onRowClick={handleRowClick} />
        </div>
      )}
    </div>
  );
};
