import React, { type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { cn, Button } from '@mirel/ui';
import { ChevronRight, Save, Eye } from 'lucide-react';
import { useStudioContextOptional, type BreadcrumbItem } from '../contexts';

interface StudioContextBarProps {
  className?: string;
  /**
   * Override breadcrumbs from context
   */
  breadcrumbs?: BreadcrumbItem[];
  /**
   * Current editing target title
   */
  title?: string;
  /**
   * Subtitle or description
   */
  subtitle?: string;
  /**
   * Quick actions to display
   */
  actions?: ReactNode;
  /**
   * Callback for save action
   */
  onSave?: () => void;
  /**
   * Callback for preview action
   */
  onPreview?: () => void;
  /**
   * Whether save is disabled
   */
  saveDisabled?: boolean;
  /**
   * Whether to show save button
   */
  showSave?: boolean;
  /**
   * Whether to show preview button
   */
  /**
   * Whether to show preview button
   */
  showPreview?: boolean;
  /**
   * Children are rendered in the actions area (alias for actions)
   */
  children?: ReactNode;
}

/**
 * Studio Context Bar Component
 * Displays breadcrumbs, current editing context, and quick actions
 */
export function StudioContextBar({
  className,
  breadcrumbs: propsBreadcrumbs,
  title,
  subtitle,
  actions,
  onSave,
  onPreview,
  saveDisabled = false,
  showSave = true,
  showPreview = true,
  children,
}: StudioContextBarProps) {
  const context = useStudioContextOptional();
  const breadcrumbs = propsBreadcrumbs ?? context?.breadcrumbs ?? [];

  return (
    <div
      className={cn(
        'h-12 flex items-center justify-between px-4 border-b border-outline/10 bg-surface-subtle',
        className
      )}
    >
      {/* Left: Breadcrumbs + Title */}
      <div className="flex items-center gap-3 min-w-0">
        {/* Breadcrumbs */}
        {breadcrumbs.length > 0 && (
          <nav className="flex items-center gap-1" aria-label="Breadcrumb">
            {breadcrumbs.map((item, index) => {
              const isLast = index === breadcrumbs.length - 1;
              const target = item.path || item.href;
              
              return (
                <React.Fragment key={index}>
                  {index > 0 && (
                    <ChevronRight className="size-3 text-muted-foreground shrink-0" />
                  )}
                  {target && !isLast ? (
                    <Link
                      to={target}
                      className={cn(
                        'text-xs transition-colors truncate max-w-32',
                        'text-muted-foreground hover:text-foreground'
                      )}
                    >
                      {item.label}
                    </Link>
                  ) : (
                    <span
                      className={cn(
                        'text-xs truncate max-w-32',
                        isLast ? 'text-foreground font-medium' : 'text-muted-foreground'
                      )}
                    >
                      {item.label}
                    </span>
                  )}
                </React.Fragment>
              );
            })}
          </nav>
        )}

        {/* Title */}
        {title && (
          <>
            {breadcrumbs.length > 0 && (
              <div className="w-px h-4 bg-border" />
            )}
            <div className="flex items-center gap-2 min-w-0">
              <h1 className="text-sm font-semibold text-foreground truncate">
                {title}
              </h1>
              {subtitle && (
                <span className="text-xs text-muted-foreground truncate">
                  {subtitle}
                </span>
              )}
            </div>
          </>
        )}
      </div>

      {/* Right: Actions */}
      <div className="flex items-center gap-2">
        {actions}
        {children}
        
        {showPreview && onPreview && (
          <Button
            variant="ghost"
            size="sm"
            onClick={onPreview}
            className="gap-1.5"
          >
            <Eye className="size-4" />
            <span className="hidden sm:inline">Preview</span>
          </Button>
        )}
        
        {showSave && onSave && (
          <Button
            variant="default"
            size="sm"
            onClick={onSave}
            disabled={saveDisabled}
            className="gap-1.5"
          >
            <Save className="size-4" />
            <span className="hidden sm:inline">Save</span>
          </Button>
        )}
      </div>
    </div>
  );
}

/**
 * Mode switcher for context bar (e.g., Form/Flow/Preview tabs)
 */
interface ModeSwitcherProps {
  modes: Array<{
    id: string;
    label: string;
    icon?: React.ElementType;
  }>;
  activeMode: string;
  onModeChange: (mode: string) => void;
  className?: string;
}

export function ModeSwitcher({
  modes,
  activeMode,
  onModeChange,
  className,
}: ModeSwitcherProps) {
  return (
    <div className={cn('flex items-center bg-muted rounded-lg p-1', className)}>
      {modes.map((mode) => {
        const Icon = mode.icon;
        const isActive = mode.id === activeMode;
        
        return (
          <button
            key={mode.id}
            onClick={() => onModeChange(mode.id)}
            className={cn(
              'flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium rounded-md transition-colors',
              isActive
                ? 'bg-background text-foreground shadow-sm'
                : 'text-muted-foreground hover:text-foreground'
            )}
          >
            {Icon && <Icon className="size-4" />}
            <span>{mode.label}</span>
          </button>
        );
      })}
    </div>
  );
}

export default StudioContextBar;
