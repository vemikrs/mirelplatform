import React from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight, Home } from 'lucide-react';
import { cn } from '@mirel/ui';
import { useStudioContextOptional } from '../contexts';

interface StudioContextBarProps {
  className?: string;
  // Legacy props for compatibility with existing Modeler pages
  title?: string;
  subtitle?: string;
  breadcrumbs?: { label: string; href?: string; path?: string }[];
  onSave?: () => Promise<void> | void;
  actions?: React.ReactNode;
  showSave?: boolean;
  showPreview?: boolean;
  children?: React.ReactNode;
}

export const StudioContextBar: React.FC<StudioContextBarProps> = ({ 
  className,
  title,
  subtitle,
  breadcrumbs: propBreadcrumbs,
  onSave,
  actions,
  // showSave, showPreview are largely handled by presence of onSave/children/actions in this new implementation
  children
}) => {
  const context = useStudioContextOptional();
  // Prefer context breadcrumbs if available, otherwise use props
  const breadcrumbs = context?.breadcrumbs?.length ? context.breadcrumbs : (propBreadcrumbs || []);

  const hasContent = breadcrumbs.length > 0 || title || children || onSave;

  if (!hasContent) {
    return null;
  }

  return (
    <div className={`flex h-10 items-center justify-between border-b border-border bg-background px-4 text-sm ${className || ''}`}>
      <div className="flex items-center gap-4">
        <nav className="flex items-center gap-1 text-muted-foreground">
          <Link to="/studio" className="flex items-center hover:text-foreground transition-colors">
            <Home className="size-4" />
          </Link>
          
          {breadcrumbs.map((item, index) => (
            <React.Fragment key={item.path || item.href || index}>
              <ChevronRight className="size-4 text-muted-foreground/50" />
              {(item.path || item.href) ? (
                <Link to={(item.path || item.href)!} className="hover:text-foreground transition-colors">
                  {item.label}
                </Link>
              ) : (
                <span className="text-foreground font-medium">{item.label}</span>
              )}
            </React.Fragment>
          ))}
        </nav>
        
        {title && (
          <>
            {breadcrumbs.length > 0 && <span className="text-muted-foreground/20">|</span>}
            <span className="font-medium">{title}</span>
            {subtitle && <span className="text-muted-foreground text-xs">{subtitle}</span>}
          </>
        )}
      </div>

      <div className="flex items-center gap-2">
        {actions}
        {children}
        {onSave && (
          <button onClick={onSave} className="text-xs bg-primary text-primary-foreground px-2 py-1 rounded hover:bg-primary/90">
            Save
          </button>
        )}
      </div>
    </div>
  );
};

export interface ModeSwitcherProps {
  modes: { id: string; label: string; icon?: React.ElementType }[];
  activeMode: string;
  onModeChange: (modeId: string) => void;
  className?: string;
}

export const ModeSwitcher: React.FC<ModeSwitcherProps> = ({ modes, activeMode, onModeChange, className }) => {
  return (
    <div className={cn("flex items-center bg-muted/50 rounded-md p-0.5", className)}>
      {modes.map((mode) => {
        const Icon = mode.icon;
        const isActive = activeMode === mode.id;
        return (
          <button
            key={mode.id}
            onClick={() => onModeChange(mode.id)}
            className={cn(
              "flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium rounded-sm transition-all",
              isActive 
                ? "bg-background text-foreground shadow-sm" 
                : "text-muted-foreground hover:text-foreground hover:bg-muted"
            )}
          >
            {Icon && <Icon className="size-3.5" />}
            <span>{mode.label}</span>
          </button>
        );
      })}
    </div>
  );
};
