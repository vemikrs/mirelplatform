import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getData, createData, updateData, getSchema } from '@/lib/api/schema';
import { Button, Card } from '@mirel/ui';
import { ArrowLeft } from 'lucide-react';
import { DynamicFormRenderer } from '../components/Runtime/DynamicFormRenderer';
import type { Widget } from '../stores/useFormDesignerStore';

export const StudioDataEditPage: React.FC = () => {
  const { modelId, recordId } = useParams<{ modelId: string; recordId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isNew = !recordId || recordId === 'new';

  const { data: schema } = useQuery({
    queryKey: ['studio-schema', modelId],
    queryFn: () => modelId ? getSchema(modelId) : Promise.resolve(null),
    enabled: !!modelId,
  });

  const { data: record, isLoading: isLoadingRecord } = useQuery({
    queryKey: ['studio-data', modelId, recordId],
    queryFn: () => (modelId && !isNew) ? getData(modelId, recordId!) : Promise.resolve(null),
    enabled: !!modelId && !isNew,
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
      queryClient.invalidateQueries({ queryKey: ['studio-data', modelId] });
      navigate(`/apps/studio/${modelId}/data`);
    },
  });

  if (!schema || (isLoadingRecord && !isNew)) {
    return <div className="p-8 text-center">Loading...</div>;
  }

  // Convert schema fields to widgets for renderer
  const widgets: Widget[] = schema.data?.fields.map((field: any) => {
     let layout = { x: 0, y: 0, w: 4, h: 2 };
     try {
       if (field.layout) {
         layout = JSON.parse(field.layout);
       }
     } catch (e) {
       // ignore
     }
     return {
       id: field.fieldId,
       type: field.fieldType.toLowerCase() as any,
       label: field.fieldName,
       fieldCode: field.fieldCode,
       required: field.isRequired,
       ...layout,
     };
  }) || [];

  return (
    <div className="p-8 max-w-3xl mx-auto">
      <div className="flex items-center gap-2 mb-8">
        <Button variant="ghost" size="icon" onClick={() => navigate(`/apps/studio/${modelId}/data`)}>
          <ArrowLeft className="size-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold">{isNew ? 'New Record' : 'Edit Record'}</h1>
          <p className="text-muted-foreground">{schema.data?.modelName}</p>
        </div>
      </div>

      <Card className="p-6">
        <DynamicFormRenderer 
          widgets={widgets} 
          defaultValues={record || {}}
          onSubmit={(data) => mutation.mutate(data)} 
        />
      </Card>
    </div>
  );
};
