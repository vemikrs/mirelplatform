import React from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight, Home } from 'lucide-react';
import { useStudioContextOptional } from '../contexts';

interface StudioContextBarProps {
  className?: string;
  // Legacy props for compatibility with existing Modeler pages
  title?: string;
  breadcrumbs?: { label: string; href?: string; path?: string }[];
  onSave?: () => Promise<void> | void;
  children?: React.ReactNode;
}

export const StudioContextBar: React.FC<StudioContextBarProps> = ({ 
  className,
  title,
  breadcrumbs: propBreadcrumbs,
  onSave,
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
          </>
        )}
      </div>

      <div className="flex items-center gap-2">
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
