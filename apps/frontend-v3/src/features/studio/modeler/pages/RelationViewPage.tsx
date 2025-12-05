import React from 'react';
import { StudioLayout } from '../../layouts';
import { StudioContextBar } from '../../components';
import { StudioNavigation } from '../../components/StudioNavigation';
import { ModelerTree } from '../components/ModelerTree';

export const RelationViewPage: React.FC = () => {
  return (
    <StudioLayout
      navigation={<StudioNavigation className="h-auto shrink-0 max-h-[40%] border-b" />}
      explorer={<ModelerTree className="flex-1" />}
    >
      <div className="flex flex-col h-full">
        <StudioContextBar
          title="リレーションビュー"
          breadcrumbs={[
            { label: 'Studio', href: '/apps/studio' },
            { label: 'Modeler', href: '/apps/studio/modeler' },
            { label: 'Relations', href: '/apps/studio/modeler/relations' },
          ]}
        />
        <div className="p-6">
          <h1 className="text-2xl font-bold">Relations</h1>
          <p className="text-muted-foreground mt-2">Relation view implementation coming soon.</p>
        </div>
      </div>
    </StudioLayout>
  );
};
