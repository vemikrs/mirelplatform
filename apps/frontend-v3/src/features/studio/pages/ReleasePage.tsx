import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getReleases, createRelease } from '@/lib/api/release';
import { ReleaseList } from '../components/ReleaseCenter/ReleaseList';
import { Button } from '@mirel/ui';
import { ArrowLeft, Plus } from 'lucide-react';
import { toast } from 'sonner';

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
      toast.success('Release created successfully');
    },
    onError: () => {
      toast.error('Failed to create release');
    },
  });

  if (!modelId) return <div>Invalid Model ID</div>;

  return (
    <div className="h-[calc(100vh-4rem)] flex flex-col">
      {/* Toolbar */}
      <div className="h-14 border-b bg-white flex items-center justify-between px-4">
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="icon" onClick={() => navigate(`/apps/studio/${modelId}`)}>
            <ArrowLeft className="size-4" />
          </Button>
          <h1 className="font-semibold text-lg">Release Center</h1>
        </div>
        <div>
          <Button onClick={() => createMutation.mutate()} disabled={createMutation.isPending} className="gap-2">
            <Plus className="size-4" />
            Create Release
          </Button>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-8 bg-gray-50">
        <div className="max-w-4xl mx-auto">
          {isLoading ? (
            <div>Loading...</div>
          ) : (
            <ReleaseList releases={releases?.data || []} />
          )}
        </div>
      </div>
    </div>
  );
};
