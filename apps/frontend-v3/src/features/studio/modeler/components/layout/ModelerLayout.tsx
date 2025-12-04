import React from 'react';
import { ModelerSidebar } from './ModelerSidebar';

interface ModelerLayoutProps {
  children: React.ReactNode;
  explorer?: React.ReactNode;
  properties?: React.ReactNode;
}

export const ModelerLayout: React.FC<ModelerLayoutProps> = ({ children, explorer, properties }) => {
  return (
    <div className="flex h-screen overflow-hidden bg-background">
      {/* Global Sidebar (Navigation) */}
      <ModelerSidebar className="hidden md:block w-64 flex-shrink-0" />
      
      {/* 3-Pane Content Area */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left Pane: Explorer */}
        {explorer && (
          <div className="w-64 flex-shrink-0 border-r bg-background overflow-hidden">
            {explorer}
          </div>
        )}

        {/* Center Pane: Canvas / Main Content */}
        <main className="flex-1 overflow-y-auto bg-muted/10 relative">
          {children}
        </main>

        {/* Right Pane: Properties */}
        {properties && (
          <div className="w-80 flex-shrink-0 border-l bg-background overflow-hidden">
            {properties}
          </div>
        )}
      </div>
    </div>
  );
};
