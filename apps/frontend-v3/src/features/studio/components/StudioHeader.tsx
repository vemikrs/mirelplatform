import React from 'react';
import { Badge, Button } from '@mirel/ui';
import { Menu, Cloud, Check } from 'lucide-react';
import { UserMenu } from '@/components/header/UserMenu';
import { GlobalSearch } from '@/components/header/GlobalSearch';
// Import context optionally or fallback if not ready
// import { useStudioContext } from '../contexts/StudioContext';

interface StudioHeaderProps {
  workspaceName?: string;
  environment?: 'dev' | 'stg' | 'prod';
  draftStatus?: 'saved' | 'unsaved' | 'saving';
  onToggleNavigation?: () => void;
  isNavigationCollapsed?: boolean;
}

export const StudioHeader: React.FC<StudioHeaderProps> = ({
  workspaceName,
  environment = 'dev',
  draftStatus = 'saved',
  onToggleNavigation,
  isNavigationCollapsed,
}) => {
  // Mock context values for now as Task 1.4 is not yet done
  const finalWorkspaceName = workspaceName || 'Workspace';
  const finalEnvironment = environment || 'dev';
  const finalDraftStatus = draftStatus; 

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
    <header className="flex h-14 items-center gap-4 border-b border-border bg-background px-4 shrink-0 justify-between">
      <div className="flex items-center gap-4 min-w-0 font-semibold">
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
    </header>
  );
};
