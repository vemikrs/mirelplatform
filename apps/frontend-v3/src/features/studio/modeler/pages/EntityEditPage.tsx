import React from 'react';
import { StudioLayout } from '../../layouts';
import { StudioContextBar } from '../../components';

export const EntityEditPage: React.FC = () => {
  return (
    <StudioLayout>
      <div className="flex flex-col h-full">
        <StudioContextBar
          title="エンティティ編集"
          breadcrumbs={[
            { label: 'Studio', href: '/apps/studio' },
            { label: 'Modeler', href: '/apps/studio/modeler' },
            { label: 'Entities', href: '/apps/studio/modeler/entities' },
            { label: 'Edit' },
          ]}
        />
        <div className="p-6">
          <h1 className="text-2xl font-bold">Entity Edit</h1>
          <p className="text-muted-foreground mt-2">Entity editor implementation coming soon.</p>
        </div>
      </div>
    </StudioLayout>
  );
};
