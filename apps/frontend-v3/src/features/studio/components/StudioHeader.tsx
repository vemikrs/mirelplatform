import React from 'react';
import { Badge, Button } from '@mirel/ui';
import { Menu, Cloud, Check } from 'lucide-react';
import { UserMenu } from '@/components/header/UserMenu';
import { GlobalSearch } from '@/components/header/GlobalSearch';
import { useStudioContextOptional } from '../contexts/index';

interface StudioHeaderProps {
  workspaceName?: string;
  environment?: 'dev' | 'stg' | 'prod';
  draftStatus?: 'saved' | 'unsaved' | 'saving';
  onToggleNavigation?: () => void;
  isNavigationCollapsed?: boolean;
}

export const StudioHeader: React.FC<StudioHeaderProps> = ({
  workspaceName,
  environment,
  draftStatus,
  onToggleNavigation,
  isNavigationCollapsed,
}) => {
  const context = useStudioContextOptional();
  
  const finalWorkspaceName = workspaceName || context?.workspace?.name || 'Workspace';
  const finalEnvironment = environment || context?.environment || 'dev';
  const finalDraftStatus = draftStatus || context?.draft?.status || 'saved';

  const getEnvBadgeVariant = (env: string) => {
    switch (env) {
      case 'prod': return 'destructive'; // Red for prod
      case 'stg': return 'warning';      // Yellow/Orange for stg
      default: return 'neutral';       // Blue/Gray for dev
    }
  };

  const getSaveStatusIcon = () => {
    switch (finalDraftStatus) {
      case 'saving': return <Cloud className="size-4 animate-pulse text-muted-foreground" />;
      case 'unsaved': return <span className="size-2 rounded-full bg-yellow-500" />;
      case 'saved': return <Check className="size-4 text-green-500" />;
    }
  };

  return (
    <div className="h-14 border-b bg-background flex items-center justify-between px-4 border-t-4 border-t-indigo-500">
      <div className="flex items-center gap-4">
        <div className="font-bold text-lg flex items-center gap-2">
          <span>Mirel</span>
          <span className="bg-indigo-100 text-indigo-700 px-2 py-0.5 rounded text-xs">Studio</span>
        </div>
        
        {/* Workspace/Project Selector could go here */}
        
        <div className="h-6 w-px bg-border" />

        {onToggleNavigation && (
          <Button 
            variant="ghost" 
            size="icon" 
            onClick={onToggleNavigation} 
            className="shrink-0 md:hidden"
            aria-label={isNavigationCollapsed ? "Expand navigation" : "Collapse navigation"}
          >
            <Menu className="size-5" />
          </Button>
        )}
        
        <div className="flex items-center gap-2 truncate">
          <span className="text-primary hidden md:inline">Studio</span>
          <span className="text-muted-foreground hidden md:inline">/</span>
          <span className="truncate">{finalWorkspaceName}</span>
          <Badge variant={getEnvBadgeVariant(finalEnvironment)} className="text-[10px] h-5 px-1.5 uppercase">
            {finalEnvironment}
          </Badge>
        </div>
      </div>


      
      {/* Center: Search */}
      <div className="flex-1 flex justify-center max-w-xl mx-auto px-4">
        <GlobalSearch />
      </div>

      {/* Right: Actions & User */}
      <div className="flex items-center gap-3 shrink-0">
        {/* Save Status Indicator */}
        <div className="hidden md:flex items-center gap-2 text-xs text-muted-foreground mr-2">
          {getSaveStatusIcon()}
          <span>{finalDraftStatus === 'saving' ? 'Saving...' : finalDraftStatus === 'unsaved' ? 'Unsaved' : 'Saved'}</span>
        </div>

        <UserMenu />
      </div>
    </div>
  );
};
