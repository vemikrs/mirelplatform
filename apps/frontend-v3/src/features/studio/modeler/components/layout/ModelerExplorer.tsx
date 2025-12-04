import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { cn } from '@mirel/ui';
import { modelerApi } from '../../api/modelerApi';

interface ModelerExplorerProps {
  className?: string;
}

interface ModelSummary {
  modelId: string;
  modelName: string;
  modelType: 'transaction' | 'master';
}

export const ModelerExplorer: React.FC<ModelerExplorerProps> = ({ className }) => {
  const location = useLocation();
  const pathname = location.pathname;
  const [transactionModels, setTransactionModels] = useState<ModelSummary[]>([]);
  const [masterModels, setMasterModels] = useState<ModelSummary[]>([]);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    loadModels();
  }, []);

  const loadModels = async () => {
    try {
      const response = await modelerApi.listModels();
      const models: ModelSummary[] = response.data.models || [];
      
      setTransactionModels(models.filter(m => m.modelType === 'transaction'));
      setMasterModels(models.filter(m => !m.modelType || m.modelType === 'master'));
    } catch (error) {
      console.error('Failed to load models for explorer:', error);
      setTransactionModels([]);
      setMasterModels([]);
    }
  };

  const isActive = (path: string) => pathname.startsWith(path);

  const filterModels = (models: ModelSummary[]) => {
    if (!searchTerm) return models;
    return models.filter(m => 
      m.modelId.toLowerCase().includes(searchTerm.toLowerCase()) || 
      m.modelName.toLowerCase().includes(searchTerm.toLowerCase())
    );
  };

  const renderModelLink = (model: ModelSummary) => (
    <Link
      key={model.modelId}
      to={`/apps/studio/modeler/models?modelId=${model.modelId}`}
      className={cn(
        "flex items-center rounded-md px-2 py-1.5 text-sm font-medium hover:bg-accent hover:text-accent-foreground truncate",
        location.search.includes(`modelId=${model.modelId}`) ? "bg-accent text-accent-foreground" : "transparent"
      )}
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="mr-2 h-3 w-3 flex-shrink-0"
      >
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
      </svg>
      <span className="truncate">{model.modelName || model.modelId}</span>
    </Link>
  );

  return (
    <div className={cn("flex flex-col h-full border-r bg-background", className)}>
      <div className="p-4 border-b">
        <h2 className="mb-2 text-sm font-semibold tracking-tight text-muted-foreground uppercase">
          Model Explorer
        </h2>
        <div className="relative">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground"
          >
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            placeholder="Search..."
            className="w-full pl-8 pr-3 py-2 text-sm border rounded-md bg-background focus:outline-none focus:ring-1 focus:ring-ring"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-2">
        {filterModels(transactionModels).length > 0 && (
          <div className="mb-4">
            <h3 className="px-2 mb-1 text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Entity (Transaction)
            </h3>
            <div className="space-y-0.5">
              {filterModels(transactionModels).map(renderModelLink)}
            </div>
          </div>
        )}

        <div className="mb-4">
          <h3 className="px-2 mb-1 text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Entity (Master)
          </h3>
          <div className="space-y-0.5">
            {filterModels(masterModels).map(renderModelLink)}
          </div>
        </div>

        <div>
          <h3 className="px-2 mb-1 text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Code
          </h3>
          <div className="space-y-0.5">
             <Link
              to="/apps/studio/modeler/codes"
              className={cn(
                "flex items-center rounded-md px-2 py-1.5 text-sm font-medium hover:bg-accent hover:text-accent-foreground",
                isActive("/apps/studio/modeler/codes") ? "bg-accent text-accent-foreground" : "transparent"
              )}
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                className="mr-2 h-3 w-3"
              >
                <path d="M4 21v-7" />
                <path d="M4 10V3" />
                <path d="M12 21v-9" />
                <path d="M12 8V3" />
                <path d="M20 21v-5" />
                <path d="M20 12V3" />
                <path d="M1 14h6" />
                <path d="M9 8h6" />
                <path d="M17 16h6" />
              </svg>
              Code Master
            </Link>
          </div>
        </div>
      </div>

      <div className="p-4 border-t">
        <Link
          to="/apps/studio/modeler/models?new=true"
          className="flex items-center justify-center w-full px-4 py-2 text-sm font-medium text-primary-foreground bg-primary rounded-md hover:bg-primary/90 transition-colors"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="mr-2 h-4 w-4"
          >
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          New Model
        </Link>
      </div>
    </div>
  );
};
