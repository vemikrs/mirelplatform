import React from 'react';
import { ModelerSidebar } from './ModelerSidebar';

interface ModelerLayoutProps {
  children: React.ReactNode;
}

export const ModelerLayout: React.FC<ModelerLayoutProps> = ({ children }) => {
  return (
    <div className="flex h-screen overflow-hidden bg-background">
      <ModelerSidebar className="hidden md:block w-64 flex-shrink-0" />
      <main className="flex-1 overflow-y-auto">
        {children}
      </main>
    </div>
  );
};
