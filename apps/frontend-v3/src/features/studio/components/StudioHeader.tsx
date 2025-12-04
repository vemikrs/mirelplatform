import React from 'react';
import { Link } from 'react-router-dom';
import { cn, Button, Badge } from '@mirel/ui';
import {
  PanelLeftClose,
  PanelLeftOpen,
  Search,
  Bell,
  HelpCircle,
  Save,
  ChevronRight,
} from 'lucide-react';
import { useStudioContextOptional } from '../contexts';
import { UserMenu } from '@/components/header/UserMenu';

interface StudioHeaderProps {
  className?: string;
  onToggleNavigation?: () => void;
  isNavigationCollapsed?: boolean;
}

/**
 * Studio Header Component
 * Displays workspace info, draft status, environment badge, and actions
 */
export function StudioHeader({
  className,
  onToggleNavigation,
  isNavigationCollapsed,
}: StudioHeaderProps) {
  const context = useStudioContextOptional();
  
  const workspace = context?.workspace;
  const draft = context?.draft;
  const environment = context?.environment ?? 'dev';
  const breadcrumbs = context?.breadcrumbs ?? [];

  return (
    <header
      className={cn(
        'h-14 flex items-center justify-between px-4 border-b border-outline/20 bg-surface',
        className
      )}
    >
      {/* Left Section: Navigation Toggle + Workspace + Breadcrumbs */}
      <div className="flex items-center gap-3">
        {/* Navigation Toggle */}
        {onToggleNavigation && (
          <Button
            variant="ghost"
            size="icon"
            onClick={onToggleNavigation}
            className="shrink-0"
            aria-label={isNavigationCollapsed ? 'Expand navigation' : 'Collapse navigation'}
          >
            {isNavigationCollapsed ? (
              <PanelLeftOpen className="size-4" />
            ) : (
              <PanelLeftClose className="size-4" />
            )}
          </Button>
        )}

        {/* Workspace Info */}
        {workspace && (
          <div className="flex items-center gap-2">
            <span className="text-sm font-medium text-foreground">
              {workspace.name}
            </span>
            <span className="text-muted-foreground">/</span>
          </div>
        )}

        {/* Breadcrumbs */}
        {breadcrumbs.length > 0 && (
          <nav className="flex items-center gap-1" aria-label="Breadcrumb">
            {breadcrumbs.map((item, index) => (
              <React.Fragment key={item.path}>
                {index > 0 && (
                  <ChevronRight className="size-4 text-muted-foreground" />
                )}
                {index === breadcrumbs.length - 1 ? (
                  <span className="text-sm text-foreground font-medium">
                    {item.label}
                  </span>
                ) : (item.path || item.href) ? (
                  <Link
                    to={(item.path || item.href)!}
                    className="text-sm text-muted-foreground hover:text-foreground transition-colors"
                  >
                    {item.label}
                  </Link>
                ) : (
                  <span className="text-sm text-muted-foreground">
                    {item.label}
                  </span>
                )}
              </React.Fragment>
            ))}
          </nav>
        )}
      </div>

      {/* Center Section: Draft Status */}
      <div className="flex items-center gap-2">
        {draft && (
          <>
            <DraftStatusIndicator status={draft.status} />
            <span className="text-xs text-muted-foreground">
              v{draft.version}
            </span>
            {draft.lastSaved && (
              <span className="text-xs text-muted-foreground">
                Last saved: {formatRelativeTime(draft.lastSaved)}
              </span>
            )}
          </>
        )}
      </div>

      {/* Right Section: Environment + Actions */}
      <div className="flex items-center gap-2">
        {/* Environment Badge */}
        <EnvironmentBadge environment={environment} />

        {/* Search */}
        <Button variant="ghost" size="icon" aria-label="Search">
          <Search className="size-4" />
        </Button>

        {/* Help */}
        <Button variant="ghost" size="icon" aria-label="Help">
          <HelpCircle className="size-4" />
        </Button>

        {/* Notifications */}
        <Button variant="ghost" size="icon" aria-label="Notifications">
          <Bell className="size-4" />
        </Button>

        {/* User Menu */}
        <UserMenu />
      </div>
    </header>
  );
}

interface DraftStatusIndicatorProps {
  status: 'saved' | 'unsaved' | 'saving';
}

function DraftStatusIndicator({ status }: DraftStatusIndicatorProps) {
  const statusConfig = {
    saved: {
      label: 'Saved',
      className: 'bg-success/10 text-success',
      icon: null,
    },
    unsaved: {
      label: 'Unsaved changes',
      className: 'bg-warning/10 text-warning',
      icon: null,
    },
    saving: {
      label: 'Saving...',
      className: 'bg-info/10 text-info animate-pulse',
      icon: <Save className="size-3 animate-spin" />,
    },
  };

  const config = statusConfig[status];

  return (
    <div
      className={cn(
        'flex items-center gap-1 px-2 py-1 rounded text-xs font-medium',
        config.className
      )}
    >
      {config.icon}
      <span>{config.label}</span>
    </div>
  );
}

interface EnvironmentBadgeProps {
  environment: 'dev' | 'stg' | 'prod';
}

function EnvironmentBadge({ environment }: EnvironmentBadgeProps) {
  const envConfig = {
    dev: { label: 'DEV', variant: 'info' as const },
    stg: { label: 'STG', variant: 'warning' as const },
    prod: { label: 'PROD', variant: 'destructive' as const },
  };

  const config = envConfig[environment];

  return (
    <Badge variant={config.variant} className="uppercase text-xs">
      {config.label}
    </Badge>
  );
}

/**
 * Format date to relative time string
 */
function formatRelativeTime(date: Date): string {
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSeconds = Math.floor(diffMs / 1000);
  const diffMinutes = Math.floor(diffSeconds / 60);
  const diffHours = Math.floor(diffMinutes / 60);

  if (diffSeconds < 60) {
    return 'Just now';
  }
  if (diffMinutes < 60) {
    return `${diffMinutes}m ago`;
  }
  if (diffHours < 24) {
    return `${diffHours}h ago`;
  }
  return date.toLocaleDateString();
}

export default StudioHeader;
