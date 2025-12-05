import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Workflow } from 'lucide-react';
import { Button, Card, CardHeader, CardTitle, CardContent } from '@mirel/ui';
import { StudioLayout } from '../../layouts';
import { StudioContextBar } from '../../components';
import { StudioNavigation } from '../../components/StudioNavigation';
import { modelerApi } from '../../modeler/api/modelerApi';

export const FlowListPage: React.FC = () => {
  const navigate = useNavigate();
  const [models, setModels] = useState<{ value: string; text: string }[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchModels = async () => {
      try {
        setLoading(true);
        // Flows are typically attached to models. Using listModels for now.
        const response = await modelerApi.listModels();
        setModels(response.data.models);
      } catch (error) {
        console.error('Failed to fetch models for flows:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchModels();
  }, []);

  return (
    <StudioLayout
      navigation={<StudioNavigation className="h-full border-r" />}
    >
      <div className="flex flex-col h-full overflow-hidden">
        <StudioContextBar
          title="フロー一覧"
          breadcrumbs={[
            { label: 'Studio', href: '/apps/studio' },
            { label: 'Flows', href: '/apps/studio/flows' },
          ]}
          actions={
            <Button onClick={() => navigate('/apps/studio/flows/new')}>
              <Plus className="mr-2 h-4 w-4" />
              新規フロー
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
                  onClick={() => navigate(`/apps/studio/flows/${model.value}`)}
                >
                  <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                    <CardTitle className="text-sm font-medium">
                      {model.text}
                    </CardTitle>
                    <Workflow className="h-4 w-4 text-muted-foreground" />
                  </CardHeader>
                  <CardContent>
                    <div className="text-xs text-muted-foreground">ID: {model.value}</div>
                  </CardContent>
                </Card>
              ))}
              {models.length === 0 && (
                <div className="col-span-full text-center p-12 text-muted-foreground bg-muted/10 rounded-lg border border-dashed">
                  フロー（モデル）が見つかりません。
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </StudioLayout>
  );
};
