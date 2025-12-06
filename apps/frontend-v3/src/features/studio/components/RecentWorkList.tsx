import React from 'react';
import { Card, Badge } from '@mirel/ui';
import { FileText, Clock } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { getSchemas, type SchemaSummary } from '@/lib/api/schema';
import { useNavigate } from 'react-router-dom';

export const RecentWorkList: React.FC = () => {
  const navigate = useNavigate();
  const { data: schemas, isLoading } = useQuery({
    queryKey: ['studio-schemas'],
    queryFn: getSchemas,
  });

  const recentSchemas = schemas?.data
    ?.sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
    .slice(0, 5);

  if (isLoading) {
    return <div>Loading recent work...</div>;
  }

  if (!recentSchemas || recentSchemas.length === 0) {
    return (
      <Card className="p-6 text-center text-muted-foreground">
        No recent work found.
      </Card>
    );
  }

  return (
    <Card className="p-0 overflow-hidden">
      <div className="p-4 border-b">
        <h3 className="text-lg font-semibold">Recent Work</h3>
      </div>
      <div className="divide-y">
        {recentSchemas.map((schema: SchemaSummary) => (
          <div
            key={schema.modelId}
            className="p-4 hover:bg-muted/50 transition-colors cursor-pointer flex items-center justify-between"
            onClick={() => navigate(`/apps/studio/${schema.modelId}`)}
          >
            <div className="flex items-center gap-4">
              <div className="p-2 bg-primary/10 rounded-lg text-primary">
                <FileText className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-medium">{schema.modelName}</h4>
                <p className="text-xs text-muted-foreground line-clamp-1">
                  {schema.description || 'No description'}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-4 text-sm text-muted-foreground">
              <div className="flex items-center gap-1">
                <Clock className="w-3 h-3" />
                {new Date(schema.updatedAt).toLocaleDateString()}
              </div>
              <Badge variant={schema.status === 'PUBLISHED' ? 'success' : 'neutral'}>
                {schema.status}
              </Badge>
            </div>
          </div>
        ))}
      </div>
    </Card>
  );
};
