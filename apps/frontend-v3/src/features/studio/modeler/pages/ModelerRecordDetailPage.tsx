import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { modelerApi } from '../api/modelerApi';
import type { SchDicModel } from '../types/modeler';
import { DynamicForm } from '../components/DynamicForm';
import { StudioLayout } from '../../layouts';
import { StudioContextBar } from '../../components';

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

  if (loading) {
    return (
      <StudioLayout>
        <div className="flex items-center justify-center h-full text-muted-foreground">
          読み込み中...
        </div>
      </StudioLayout>
    );
  }

  const pageTitle = recordId === 'new' ? '新規レコード作成' : 'レコード編集';

  return (
    <StudioLayout>
      <div className="flex flex-col h-full overflow-hidden">
        <StudioContextBar
          breadcrumbs={[
            { label: 'Studio', href: '/apps/studio' },
            { label: 'Modeler', href: '/apps/studio/modeler' },
            { label: 'データ管理', href: '/apps/studio/modeler/records' },
            { label: modelId || '' },
            { label: pageTitle },
          ]}
          title={pageTitle}
          onSave={handleSubmit}
        />

        <div className="flex-1 overflow-y-auto p-6">
          <div className="max-w-4xl mx-auto p-6 border border-border rounded-lg bg-card shadow-sm">
            <DynamicForm
              fields={fields}
              data={data}
              onChange={setData}
              onSubmit={handleSubmit}
            />
          </div>
        </div>
      </div>
    </StudioLayout>
  );
};
