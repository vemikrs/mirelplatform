import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Database } from 'lucide-react';
import { Button, Card, CardHeader, CardTitle, CardContent } from '@mirel/ui';
import { StudioLayout } from '../../layouts';
import { StudioContextBar } from '../../components';
import { modelerApi } from '../api/modelerApi';

import { StudioNavigation } from '../../components/StudioNavigation';
import { ModelerTree } from '../components/ModelerTree';

export const EntityListPage: React.FC = () => {
  const navigate = useNavigate();
  const [models, setModels] = useState<{ value: string; text: string }[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchModels = async () => {
      try {
        setLoading(true);
        const response = await modelerApi.listModels();
        setModels(response.data.models);
      } catch (error) {
        console.error('Failed to fetch models:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchModels();
  }, []);

  return (
    <StudioLayout
      navigation={<StudioNavigation className="h-auto shrink-0 max-h-[40%] border-b" />}
      explorer={<ModelerTree className="flex-1" />}
    >
      <div className="flex flex-col h-full overflow-hidden">
        <StudioContextBar
          title="エンティティ一覧"
          breadcrumbs={[
            { label: 'Studio', href: '/apps/studio' },
            { label: 'Modeler', href: '/apps/studio/modeler' },
            { label: 'エンティティ', href: '/apps/studio/modeler/entities' },
          ]}
          actions={
            <Button onClick={() => navigate('/apps/studio/modeler/entities/new')}>
              <Plus className="mr-2 h-4 w-4" />
              新規作成
            </Button>
          }
        />
        <div className="flex-1 overflow-y-auto p-6">
          {loading ? (
             <div className="flex items-center justify-center h-full text-muted-foreground">
               読み込み中...
             </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {models.map((model) => (
                <Card 
                  key={model.value} 
                  className="cursor-pointer hover:bg-accent hover:text-accent-foreground transition-colors"
                  onClick={() => navigate(`/apps/studio/modeler/entities/${model.value}`)}
                >
                  <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                    <CardTitle className="text-sm font-medium">
                      {model.text}
                    </CardTitle>
                    <Database className="h-4 w-4 text-muted-foreground" />
                  </CardHeader>
                  <CardContent>
                    <div className="text-xs text-muted-foreground">ID: {model.value}</div>
                  </CardContent>
                </Card>
              ))}
              {models.length === 0 && (
                <div className="col-span-full text-center p-12 text-muted-foreground bg-muted/10 rounded-lg border border-dashed">
                  モデルが定義されていません。<br />
                  「新規作成」ボタンから新しいモデルを作成してください。
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </StudioLayout>
  );
};
