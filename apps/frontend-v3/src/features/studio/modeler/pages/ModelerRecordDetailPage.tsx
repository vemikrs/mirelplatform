import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { modelerApi } from '../api/modelerApi';
import type { SchDicModel } from '../types/modeler';
import { DynamicForm } from '../components/DynamicForm';
import { ModelerLayout } from '../components/layout/ModelerLayout';

export const ModelerRecordDetailPage: React.FC = () => {
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
      const modelerResponse = await modelerApi.listModel(modelId!);
      setFields(modelerResponse.data.modelers);

      if (recordId && recordId !== 'new') {
        const recordResponse = await modelerApi.load(recordId);
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
      await modelerApi.save(modelId!, recordId === 'new' ? null : recordId!, data);
      navigate('/apps/modeler/records');
    } catch (error) {
      console.error('Failed to save record:', error);
      alert('レコードの保存に失敗しました');
    }
  };

  if (loading) return <div className="p-6 text-muted-foreground">読み込み中...</div>;

  return (
    <ModelerLayout>
      <div className="p-6 space-y-6">
        <div className="flex items-center gap-4 mb-6">
          <Link to="/apps/modeler/records" className="text-muted-foreground hover:text-foreground">
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
    </ModelerLayout>
  );
};
