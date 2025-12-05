import React from 'react';
import { StudioLayout } from '../../layouts';
import { StudioContextBar } from '../../components';

export const DataBrowserPage: React.FC = () => {
  return (
    <StudioLayout>
      <div className="flex flex-col h-full">
        <StudioContextBar
          title="データブラウザ"
          breadcrumbs={[
            { label: 'Studio', href: '/apps/studio' },
            { label: 'Data', href: '/apps/studio/data' },
          ]}
        />
        <div className="p-6">
          <h1 className="text-2xl font-bold">Data Browser</h1>
          <p className="text-muted-foreground mt-2">Data browser implementation coming soon.</p>
        </div>
      </div>
    </StudioLayout>
  );
};
