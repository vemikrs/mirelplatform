import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getReleases, createRelease } from '@/lib/api/release';
import { ReleaseList } from '../components/ReleaseCenter/ReleaseList';
import { Button, toast } from '@mirel/ui';
import { ArrowLeft, Plus } from 'lucide-react';

import { StudioLayout } from '../../layouts';
import { StudioContextBar } from '../../components';
import { StudioNavigation } from '../../components/StudioNavigation';
import React from 'react';

export const ReleasePage: React.FC = () => {
  const { modelId } = useParams<{ modelId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: releases, isLoading } = useQuery({
    queryKey: ['releases', modelId],
    queryFn: () => modelId ? getReleases(modelId) : Promise.resolve(null),
    enabled: !!modelId,
  });

  const createMutation = useMutation({
    mutationFn: () => createRelease(modelId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['releases', modelId] });
      toast({
        title: 'リリース作成完了',
        description: 'リリースを作成しました',
        variant: 'success',
      });
    },
    onError: () => {
      toast({
        title: 'エラー',
        description: 'リリースの作成に失敗しました',
        variant: 'destructive',
      });
    },
  });

  if (!modelId) return <div>モデルIDが無効です</div>;

  return (
    <StudioLayout 
        navigation={<StudioNavigation className="h-full border-r" />}
        hideContextBar={true}
    >
        <div className="flex flex-col h-full overflow-hidden">
            <StudioContextBar
                breadcrumbs={[
                    { label: 'Studio', href: '/apps/studio' },
                    { label: 'フォーム', href: '..' }, // Go up to form/model
                    { label: modelId },
                    { label: 'リリース' },
                ]}
                title="リリースセンター"
            >
                <Button onClick={() => createMutation.mutate()} disabled={createMutation.isPending} className="gap-2">
                    <Plus className="size-4" />
                    リリース作成
                </Button>
            </StudioContextBar>

            <div className="flex-1 overflow-auto p-8 bg-muted/30">
                <div className="max-w-4xl mx-auto">
                {isLoading ? (
                    <div>読み込み中...</div>
                ) : (
                    <ReleaseList releases={releases?.data || []} />
                )}
                </div>
            </div>
        </div>
    </StudioLayout>
  );
};
