import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { getSchemas, type SchemaSummary } from '@/lib/api/schema';
import { Card, Button, Badge } from '@mirel/ui';
import { Plus, FileText, Clock } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const StudioHomePage: React.FC = () => {
  const navigate = useNavigate();
  const { data: schemas, isLoading, error } = useQuery({
    queryKey: ['studio-schemas'],
    queryFn: getSchemas,
  });

  if (isLoading) {
    return <div className="p-8 text-center">Loading...</div>;
  }

  if (error) {
    return <div className="p-8 text-center text-red-500">Error loading schemas</div>;
  }

  const handleCreateNew = () => {
    navigate('/apps/studio/new');
  };

  const handleEdit = (modelId: string) => {
    navigate(`/apps/studio/${modelId}`);
  };

  return (
    <div className="p-8 max-w-7xl mx-auto">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold">Mirel Studio</h1>
          <p className="text-muted-foreground">Manage your forms and schemas</p>
        </div>
        <Button onClick={handleCreateNew} className="gap-2">
          <Plus className="size-4" />
          Create New Form
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {schemas?.data?.map((schema: SchemaSummary) => (
          <Card 
            key={schema.modelId} 
            className="p-6 hover:shadow-md transition-shadow cursor-pointer group"
            onClick={() => handleEdit(schema.modelId)}
          >
            <div className="flex items-start justify-between mb-4">
              <div className="p-2 bg-primary/10 rounded-lg text-primary">
                <FileText className="size-6" />
              </div>
              <Badge variant={schema.status === 'PUBLISHED' ? 'success' : 'neutral'}>
                {schema.status}
              </Badge>
            </div>
            
            <h3 className="font-semibold text-lg mb-2 group-hover:text-primary transition-colors">
              {schema.modelName}
            </h3>
            <p className="text-sm text-muted-foreground mb-4 line-clamp-2 h-10">
              {schema.description || 'No description'}
            </p>

            <div className="flex items-center text-xs text-muted-foreground gap-4 pt-4 border-t">
              <div className="flex items-center gap-1">
                <Clock className="size-3" />
                {new Date(schema.updatedAt).toLocaleDateString()}
              </div>
              <div>v{schema.version}</div>
            </div>
          </Card>
        ))}

        {/* Empty State */}
        {(!schemas?.data || schemas.data.length === 0) && (
          <div className="col-span-full py-12 text-center bg-gray-50 rounded-lg border border-dashed">
            <p className="text-muted-foreground mb-4">No forms created yet</p>
            <Button variant="outline" onClick={handleCreateNew}>
              Create your first form
            </Button>
          </div>
        )}
      </div>
    </div>
  );
};
