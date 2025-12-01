import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
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
      alert('レコードの保存に失敗しました');
    }
  };

  if (loading) return <div className="p-6 text-muted-foreground">読み込み中...</div>;

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center gap-4 mb-6">
        <Link to="/apps/schema/records" className="text-muted-foreground hover:text-foreground">
          ← 戻る
        </Link>
        <h1 className="text-2xl font-bold text-foreground">
          {recordId === 'new' ? '新規レコード作成' : 'レコード編集'}
        </h1>
      </div>
      <div className="p-6 border border-border rounded-lg bg-card shadow-sm">
        <DynamicForm
          fields={fields}
          data={data}
          onChange={setData}
          onSubmit={handleSubmit}
        />
      </div>
    </div>
  );
};
