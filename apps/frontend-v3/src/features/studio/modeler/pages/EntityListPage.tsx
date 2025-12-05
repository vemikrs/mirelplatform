import React from 'react';
import { StudioLayout } from '../../layouts';
import { StudioContextBar } from '../../components';

export const EntityListPage: React.FC = () => {
  return (
    <StudioLayout>
      <div className="flex flex-col h-full">
        <StudioContextBar
          title="エンティティ一覧"
          breadcrumbs={[
            { label: 'Studio', href: '/apps/studio' },
            { label: 'Modeler', href: '/apps/studio/modeler' },
            { label: 'Entities', href: '/apps/studio/modeler/entities' },
          ]}
        />
        <div className="p-6">
          <h1 className="text-2xl font-bold">Entities</h1>
          <p className="text-muted-foreground mt-2">Entity list implementation coming soon.</p>
        </div>
      </div>
    </StudioLayout>
  );
};
