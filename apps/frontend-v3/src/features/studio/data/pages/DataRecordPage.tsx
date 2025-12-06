import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { StudioLayout } from '../../layouts';
import { StudioContextBar } from '../../components';
import { StudioNavigation } from '../../components/StudioNavigation';
import { modelerApi } from '../../modeler/api/modelerApi';
import type { SchDicModel } from '../../modeler/types/modeler';
import { DynamicForm } from '../../modeler/components/DynamicForm';

export const DataRecordPage: React.FC = () => {
  const { modelId, recordId } = useParams<{ modelId: string; recordId: string }>();
  const navigate = useNavigate();
  const [fields, setFields] = useState<SchDicModel[]>([]);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [data, setData] = useState<Record<string, any>>({});
  const [loading, setLoading] = useState(true);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      if (!modelId) return;

      const modelerResponse = await modelerApi.listModel(modelId);
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
  }, [modelId, recordId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleSubmit = async () => {
    try {
      await modelerApi.save(modelId!, recordId === 'new' ? null : recordId!, data);
      navigate('/apps/studio/data');
    } catch (error) {
      console.error('Failed to save record:', error);
      alert('レコードの保存に失敗しました');
    }
  };

  if (loading) {
    return (
      <StudioLayout 
        navigation={<StudioNavigation className="h-full border-r" />}
        hideContextBar={true}
      >
        <div className="flex items-center justify-center h-full text-muted-foreground">
          読み込み中...
        </div>
      </StudioLayout>
    );
  }

  const pageTitle = recordId === 'new' ? '新規レコード作成' : 'レコード編集';

  return (
    <StudioLayout 
      navigation={<StudioNavigation className="h-full border-r" />}
      hideContextBar={true}
    >
      <div className="flex flex-col h-full overflow-hidden">
        <StudioContextBar
          breadcrumbs={[
            { label: 'Studio', href: '/apps/studio' },
            { label: 'データ', href: '/apps/studio/data' },
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
