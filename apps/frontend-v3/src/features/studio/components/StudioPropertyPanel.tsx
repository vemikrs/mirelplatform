import { useState, type ReactNode } from 'react';
import { cn, Button } from '@mirel/ui';
import {
  ChevronRight,
  ChevronDown,
  PanelRightClose,
  PanelRightOpen,
  X,
} from 'lucide-react';

/**
 * Property panel section interface
 */
export interface PropertySection {
  id: string;
  title: string;
  content: ReactNode;
  defaultOpen?: boolean;
}

interface StudioPropertyPanelProps {
  title?: string;
  sections?: PropertySection[];
  children?: ReactNode;
  className?: string;
  collapsed?: boolean;
  onToggle?: () => void;
  onClose?: () => void;
  emptyMessage?: string;
}

/**
 * Studio Property Panel Component
 * Right panel for displaying and editing properties of selected elements
 */
export function StudioPropertyPanel({
  title = 'Properties',
  sections,
  children,
  className,
  collapsed = false,
  onToggle,
  onClose,
  emptyMessage = 'Select an element to view its properties',
}: StudioPropertyPanelProps) {
  if (collapsed) {
    return (
      <div
        className={cn(
          'w-10 flex flex-col items-center py-2 bg-surface border-l border-outline/20',
          className
        )}
      >
        {onToggle && (
          <Button
            variant="ghost"
            size="icon"
            onClick={onToggle}
            aria-label="Expand properties panel"
          >
            <PanelRightOpen className="size-4" />
          </Button>
        )}
      </div>
    );
  }

  const hasContent = children || (sections && sections.length > 0);

  return (
    <div
      className={cn(
        'w-80 flex flex-col bg-surface border-l border-outline/20',
        className
      )}
    >
      {/* Header */}
      <div className="h-12 flex items-center justify-between px-4 border-b border-outline/20 shrink-0">
        <h3 className="text-sm font-semibold text-foreground">{title}</h3>
        <div className="flex items-center gap-1">
          {onToggle && (
            <Button
              variant="ghost"
              size="icon"
              onClick={onToggle}
              aria-label="Collapse properties panel"
              className="size-7"
            >
              <PanelRightClose className="size-4" />
            </Button>
          )}
          {onClose && (
            <Button
              variant="ghost"
              size="icon"
              onClick={onClose}
              aria-label="Close properties panel"
              className="size-7"
            >
              <X className="size-4" />
            </Button>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto">
        {hasContent ? (
          <>
            {sections?.map((section) => (
              <CollapsibleSection
                key={section.id}
                title={section.title}
                defaultOpen={section.defaultOpen ?? true}
              >
                {section.content}
              </CollapsibleSection>
            ))}
            {children}
          </>
        ) : (
          <div className="flex flex-col items-center justify-center h-full p-4 text-center">
            <p className="text-sm text-muted-foreground">{emptyMessage}</p>
          </div>
        )}
      </div>
    </div>
  );
}

interface CollapsibleSectionProps {
  title: string;
  children: ReactNode;
  defaultOpen?: boolean;
}

/**
 * Collapsible section within the property panel
 */
function CollapsibleSection({
  title,
  children,
  defaultOpen = true,
}: CollapsibleSectionProps) {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  return (
    <div className="border-b border-outline/20">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="w-full flex items-center gap-2 px-4 py-3 text-sm font-medium text-foreground hover:bg-surface-raised transition-colors"
      >
        {isOpen ? (
          <ChevronDown className="size-4 text-muted-foreground" />
        ) : (
          <ChevronRight className="size-4 text-muted-foreground" />
        )}
        <span>{title}</span>
      </button>
      {isOpen && (
        <div className="px-4 pb-4">
          {children}
        </div>
      )}
    </div>
  );
}

/**
 * Property field component for consistent styling
 */
export interface PropertyFieldProps {
  label: string;
  children: ReactNode;
  description?: string;
}

export function PropertyField({ label, children, description }: PropertyFieldProps) {
  return (
    <div className="space-y-1.5">
      <label className="text-xs font-medium text-muted-foreground">
        {label}
      </label>
      {children}
      {description && (
        <p className="text-xs text-muted-foreground/70">{description}</p>
      )}
    </div>
  );
}

/**
 * Property group component for grouping related fields
 */
export interface PropertyGroupProps {
  children: ReactNode;
  className?: string;
}

export function PropertyGroup({ children, className }: PropertyGroupProps) {
  return (
    <div className={cn('space-y-4', className)}>
      {children}
    </div>
  );
}

export default StudioPropertyPanel;
