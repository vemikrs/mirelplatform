import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getDataById, createData, updateData } from '@/lib/api/data';
import { getModel } from '@/lib/api/model';
import { Button, Card } from '@mirel/ui';
import { ArrowLeft } from 'lucide-react';
import { DynamicFormRenderer } from '../components/Runtime/DynamicFormRenderer';
import { toast } from 'sonner';

export const StudioDataEditPage: React.FC = () => {
  const { modelId, recordId } = useParams<{ modelId: string; recordId: string }>();
  const isNew = !recordId || recordId === 'new';
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Fetch Model Definition
  const { data: modelResponse } = useQuery({
    queryKey: ['model', modelId],
    queryFn: () => modelId ? getModel(modelId) : Promise.resolve(null),
    enabled: !!modelId,
  });

  // Fetch Data if editing
  const { data: recordResponse, isLoading } = useQuery({
    queryKey: ['data', modelId, recordId],
    queryFn: () => (!isNew && modelId && recordId) ? getDataById(modelId, recordId) : Promise.resolve(null),
    enabled: !isNew && !!modelId && !!recordId,
  });

  const mutation = useMutation({
    mutationFn: (data: any) => {
      if (isNew) {
        return createData(modelId!, data);
      } else {
        return updateData(modelId!, recordId!, data);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['data', modelId] });
      toast.success(isNew ? 'Record created' : 'Record updated');
      navigate(`/apps/studio/${modelId}/data`);
    },
    onError: () => {
      toast.error('Failed to save record');
    },
  });

  const model = modelResponse?.data;
  const record = recordResponse?.data;


  const onSubmit = (data: any) => {
    mutation.mutate(data);
  };

  if (!modelId) return <div>Invalid Model ID</div>;
  if (isLoading) return <div>Loading...</div>;

  return (
    <div className="h-[calc(100vh-4rem)] flex flex-col">
      <div className="h-14 border-b bg-background flex items-center justify-between px-4">
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="icon" onClick={() => navigate(`/apps/studio/${modelId}/data`)}>
            <ArrowLeft className="size-4" />
          </Button>
          <h1 className="font-semibold text-lg">
            {isNew ? 'New Record' : 'Edit Record'}
          </h1>
        </div>
        {/* <Button onClick={form.handleSubmit(onSubmit)} disabled={mutation.isPending} className="gap-2">
          <Save className="size-4" />
          Save
        </Button> */}
      </div>

      <div className="flex-1 overflow-auto p-8 bg-muted/30">
        <Card className="max-w-3xl mx-auto p-6">
          {model && (
            <DynamicFormRenderer
              widgets={model.fields.map((f: any) => ({
                id: f.fieldCode,
                type: f.fieldType === 'NUMBER' ? 'number' : 'text', // Simple mapping for now
                label: f.fieldName,
                x: 0, y: 0, w: 12, h: 1, // Default layout
                ...f, // Pass other props like validation
              }))}
              defaultValues={record}
              onSubmit={onSubmit}
            />
          )}
        </Card>
      </div>
    </div>
  );
};
