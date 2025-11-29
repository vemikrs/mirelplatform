import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { schemaApi } from '../api/schemaApi';
import type { SchDicModel } from '../types/schema';
import { DynamicForm } from '../components/DynamicForm';

export const SchemaRecordDetailPage: React.FC = () => {
  const { modelId, recordId } = useParams<{ modelId: string; recordId: string }>();
  const navigate = useNavigate();
  const [fields, setFields] = useState<SchDicModel[]>([]);
  const [data, setData] = useState<Record<string, any>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (modelId) {
      loadData();
    }
  }, [modelId, recordId]);

  const loadData = async () => {
    try {
      setLoading(true);
      const schemaResponse = await schemaApi.listSchema(modelId!);
      setFields(schemaResponse.data.schemas);

      if (recordId && recordId !== 'new') {
        const recordResponse = await schemaApi.load(recordId);
        setData(recordResponse.data.record.recordData);
      }
    } catch (error) {
      console.error('Failed to load record:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    try {
      await schemaApi.save(modelId!, recordId === 'new' ? null : recordId!, data);
      navigate('/apps/schema/records');
    } catch (error) {
      console.error('Failed to save record:', error);
      alert('Failed to save record');
    }
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-4">
        {recordId === 'new' ? 'New Record' : 'Edit Record'}
      </h1>
      <DynamicForm
        fields={fields}
        data={data}
        onChange={setData}
        onSubmit={handleSubmit}
      />
    </div>
  );
};
