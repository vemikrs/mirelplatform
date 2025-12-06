import React from 'react';
import { WorkspaceDashboard } from '../components/WorkspaceDashboard';
import { QuickActions } from '../components/QuickActions';
import { RecentWorkList } from '../components/RecentWorkList';
import { WorkflowStepper } from '../components/WorkflowStepper';

export const StudioHomePage: React.FC = () => {
  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Studio Workspace</h1>
        <p className="text-muted-foreground">Manage your application lifecycle</p>
      </div>

      <WorkspaceDashboard />

      <WorkflowStepper />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2">
          <RecentWorkList />
        </div>
        <div>
          <QuickActions />
          
          {/* Helper / Tips Section */}
          <div className="mt-8 p-4 bg-muted/30 rounded-lg border border-border">
            <h4 className="font-semibold mb-2">Did you know?</h4>
            <p className="text-sm text-muted-foreground">
              You can use the Modeler to define your data structure before creating forms.
              Check out the "How to build" guide in documentation.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
