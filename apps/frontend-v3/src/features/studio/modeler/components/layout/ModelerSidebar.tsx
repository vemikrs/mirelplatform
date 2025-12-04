import React, { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { cn } from '@mirel/ui';
import { modelerApi } from '../../api/modelerApi';

interface ModelerSidebarProps {
  className?: string;
}

interface ModelSummary {
  modelId: string;
  modelName: string;
  modelType: 'transaction' | 'master';
}

export const ModelerSidebar: React.FC<ModelerSidebarProps> = ({ className }) => {
  const location = useLocation();
  const pathname = location.pathname;
  const [transactionModels, setTransactionModels] = useState<ModelSummary[]>([]);
  const [masterModels, setMasterModels] = useState<ModelSummary[]>([]);

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
      console.error('Failed to load models for sidebar:', error);
      // Fallback for demo/dev if API fails or not implemented
      setTransactionModels([]);
      setMasterModels([]);
    }
  };

  const isActive = (path: string) => pathname.startsWith(path);

  const renderModelLink = (model: ModelSummary) => (
    <Link
      key={model.modelId}
      to={`/apps/studio/modeler/records/${model.modelId}`}
      className={cn(
        "flex items-center rounded-md px-4 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground",
        isActive(`/apps/studio/modeler/records/${model.modelId}`) ? "bg-accent text-accent-foreground" : "transparent"
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
        className="mr-2 h-4 w-4"
      >
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
      </svg>
      {model.modelName || model.modelId}
    </Link>
  );

  return (
    <div className={cn("pb-12 w-64 border-r bg-background overflow-y-auto", className)}>
      <div className="space-y-4 py-4">
        <div className="px-3 py-2">
          <h2 className="mb-2 px-4 text-lg font-semibold tracking-tight">
            Mirel Modeler
          </h2>
          <div className="space-y-1">
            <Link
              to="/apps/studio/modeler"
              className={cn(
                "flex items-center rounded-md px-4 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground",
                pathname === "/apps/studio/modeler" ? "bg-accent text-accent-foreground" : "transparent"
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
                className="mr-2 h-4 w-4"
              >
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                <polyline points="9 22 9 12 15 12 15 22" />
              </svg>
              ホーム
            </Link>
          </div>
        </div>

        {transactionModels.length > 0 && (
          <div className="px-3 py-2">
            <h2 className="mb-2 px-4 text-lg font-semibold tracking-tight">
              アプリケーション
            </h2>
            <div className="space-y-1">
              {transactionModels.map(renderModelLink)}
            </div>
          </div>
        )}

        <div className="px-3 py-2">
          <h2 className="mb-2 px-4 text-lg font-semibold tracking-tight">
            マスタデータ
          </h2>
          <div className="space-y-1">
            {masterModels.map(renderModelLink)}
             <Link
              to="/apps/studio/modeler/records"
              className={cn(
                "flex items-center rounded-md px-4 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground",
                isActive("/apps/studio/modeler/records") && !transactionModels.some(m => isActive(`/apps/studio/modeler/records/${m.modelId}`)) && !masterModels.some(m => isActive(`/apps/studio/modeler/records/${m.modelId}`)) ? "bg-accent text-accent-foreground" : "transparent"
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
                className="mr-2 h-4 w-4"
              >
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
                <line x1="16" y1="13" x2="8" y2="13" />
                <line x1="16" y1="17" x2="8" y2="17" />
                <polyline points="10 9 9 9 8 9" />
              </svg>
              全データ管理
            </Link>
          </div>
        </div>

        <div className="px-3 py-2">
          <h2 className="mb-2 px-4 text-lg font-semibold tracking-tight">
            設定
          </h2>
          <div className="space-y-1">
            <Link
              to="/apps/studio/modeler/models"
              className={cn(
                "flex items-center rounded-md px-4 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground",
                isActive("/apps/studio/modeler/models") ? "bg-accent text-accent-foreground" : "transparent"
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
                className="mr-2 h-4 w-4"
              >
                <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" />
              </svg>
              モデル定義
            </Link>
            <Link
              to="/apps/studio/modeler/codes"
              className={cn(
                "flex items-center rounded-md px-4 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground",
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
                className="mr-2 h-4 w-4"
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
              コードマスタ
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
