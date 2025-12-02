import React from 'react';
import { SchemaSidebar } from './SchemaSidebar';

interface SchemaLayoutProps {
  children: React.ReactNode;
}

export const SchemaLayout: React.FC<SchemaLayoutProps> = ({ children }) => {
  return (
    <div className="flex h-screen overflow-hidden bg-background">
      <SchemaSidebar className="hidden md:block w-64 flex-shrink-0" />
      <main className="flex-1 overflow-y-auto">
        {children}
      </main>
    </div>
  );
};
