import React from 'react';
import { StudioLayout } from '../../layouts';
// import { FormDesigner } from '../../components/FormDesigner/FormDesigner'; // Assuming this exists or will be moved

export const FormDesignerPage: React.FC = () => {
  return (
    <StudioLayout>
      <div className="flex flex-col h-full">
        {/* Placeholder for Context Bar - will be properly integrated in Task 2.3 */}
        <div className="p-6">
          <h1 className="text-2xl font-bold">Form Designer</h1>
          <p className="text-muted-foreground mt-2">Form designer implementation coming soon (migration from StudioPage).</p>
        </div>
      </div>
    </StudioLayout>
  );
};
