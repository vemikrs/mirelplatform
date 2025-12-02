import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { listData, deleteData, getSchema } from '@/lib/api/schema';
import { Button, Card } from '@mirel/ui';
import { Plus, Edit2, Trash2, ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';

export const StudioDataListPage: React.FC = () => {
  const { modelId } = useParams<{ modelId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: schema } = useQuery({
    queryKey: ['studio-schema', modelId],
    queryFn: () => modelId ? getSchema(modelId) : Promise.resolve(null),
    enabled: !!modelId,
  });

  const { data: records, isLoading } = useQuery({
    queryKey: ['studio-data', modelId],
    queryFn: () => modelId ? listData(modelId) : Promise.resolve([]),
    enabled: !!modelId,
  });



  const deleteMutation = useMutation({
    mutationFn: (recordId: string) => deleteData(modelId!, recordId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['studio-data', modelId] });
      toast.success('Record deleted successfully');
    },
    onError: (error) => {
        console.error('Failed to delete record', error);
        toast.error('Failed to delete record');
    }
  });

  if (isLoading || !schema) {
    return <div className="p-8 text-center">Loading...</div>;
  }

  const fields = schema.data?.fields || [];
  const displayFields = fields.slice(0, 5); // Show first 5 fields

  return (
    <div className="p-8 max-w-7xl mx-auto">
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="icon" onClick={() => navigate('/apps/studio')}>
            <ArrowLeft className="size-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-bold">{schema.data?.modelName} Data</h1>
            <p className="text-muted-foreground">Manage records</p>
          </div>
        </div>
        <Button onClick={() => navigate(`/apps/studio/${modelId}/data/new`)} className="gap-2">
          <Plus className="size-4" />
          Add Record
        </Button>
      </div>

      <Card className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead className="bg-gray-50 text-gray-500 font-medium border-b">
              <tr>
                {displayFields.map(field => (
                  <th key={field.fieldId} className="px-6 py-3">{field.fieldName}</th>
                ))}
                <th className="px-6 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {records?.map((record: any) => (
                <tr key={record.id} className="hover:bg-gray-50">
                  {displayFields.map(field => (
                    <td key={field.fieldId} className="px-6 py-3">
                      {String(record[(field as any).fieldCode] || '')}
                    </td>
                  ))}
                  <td className="px-6 py-3 text-right">
                    <div className="flex justify-end gap-2">
                      <Button variant="ghost" size="icon" onClick={() => navigate(`/apps/studio/${modelId}/data/${record.id}`)}>
                        <Edit2 className="size-4" />
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => {
                        if (confirm('Are you sure you want to delete this record?')) {
                          deleteMutation.mutate(record.id);
                        }
                      }}>
                        <Trash2 className="size-4 text-destructive" />
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
              {records?.length === 0 && (
                <tr>
                  <td colSpan={displayFields.length + 1} className="px-6 py-12 text-center text-muted-foreground">
                    No records found
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
};
