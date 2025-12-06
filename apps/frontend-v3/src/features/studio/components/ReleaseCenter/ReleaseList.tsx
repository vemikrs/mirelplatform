import React from 'react';
import type { Release } from '@/lib/api/release';
import { Card, Button } from '@mirel/ui';
import { Clock, Tag } from 'lucide-react';

interface ReleaseListProps {
  releases: Release[];
}

export const ReleaseList: React.FC<ReleaseListProps> = ({ releases }) => {
  if (!releases || releases.length === 0) {
    return <div className="text-muted-foreground text-center py-8">No releases found.</div>;
  }

  return (
    <div className="space-y-4">
      {releases.map((release) => (
        <Card key={release.releaseId} className="p-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="bg-blue-100 dark:bg-blue-900/30 p-2 rounded-full text-blue-600 dark:text-blue-400">
              <Tag className="size-5" />
            </div>
            <div>
              <h3 className="font-semibold text-lg">Version {release.version}</h3>
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Clock className="size-3" />
                <span>{new Date(release.createdAt).toLocaleString()}</span>
              </div>
            </div>
          </div>
          <Button variant="outline" size="sm">
            View Details
          </Button>
        </Card>
      ))}
    </div>
  );
};
