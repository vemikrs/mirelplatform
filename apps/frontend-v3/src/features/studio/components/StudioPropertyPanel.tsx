import React from 'react';
import { cn } from '@mirel/ui';
import { ChevronDown, ChevronRight } from 'lucide-react';

// Types
export interface PropertySection {
  id: string;
  title: string;
  groups: PropertyGroupProps[];
}

export interface PropertyGroupProps {
  id: string;
  title: string;
  defaultOpen?: boolean;
  children?: React.ReactNode;
}

export interface PropertyFieldProps {
  label: string;
  description?: string;
  error?: string;
  required?: boolean;
  className?: string;
  children: React.ReactNode;
}

interface StudioPropertyPanelProps {
  title?: string;
  children?: React.ReactNode;
  className?: string;
  emptyMessage?: string;
}

// Components

export const StudioPropertyPanel: React.FC<StudioPropertyPanelProps> = ({
  title = 'Properties',
  children,
  className,
  emptyMessage = 'Select an item to view properties',
}) => {
  if (!children) {
    return (
      <div className={cn("flex h-full flex-col bg-background text-sm", className)}>
        <div className="flex h-10 items-center border-b border-border px-4 font-medium">
          {title}
        </div>
        <div className="flex flex-1 items-center justify-center p-4 text-center text-muted-foreground">
          {emptyMessage}
        </div>
      </div>
    );
  }

  return (
    <div className={cn("flex h-full flex-col bg-background text-sm", className)}>
      <div className="flex h-10 shrink-0 items-center justify-between border-b border-border px-4 font-medium bg-muted/5">
        <span>{title}</span>
      </div>
      <div className="flex-1 overflow-y-auto p-4 space-y-6">
        {children}
      </div>
    </div>
  );
};

export const PropertyGroup: React.FC<PropertyGroupProps> = ({
  title,
  defaultOpen = true,
  children,
}) => {
  const [isOpen, setIsOpen] = React.useState(defaultOpen);

  return (
    <div className="space-y-3">
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="flex w-full items-center gap-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground hover:text-foreground transition-colors"
      >
        {isOpen ? <ChevronDown className="size-3" /> : <ChevronRight className="size-3" />}
        {title}
      </button>
      {isOpen && <div className="space-y-4 pl-2">{children}</div>}
    </div>
  );
};

export const PropertyField: React.FC<PropertyFieldProps> = ({
  label,
  description,
  error,
  required,
  className,
  children,
}) => {
  return (
    <div className={cn("space-y-1.5", className)}>
      <div className="flex items-center justify-between gap-2">
        <label className="text-xs font-medium text-foreground flex items-center gap-1">
          {label}
          {required && <span className="text-destructive">*</span>}
        </label>
      </div>
      
      {children}
      
      {description && !error && (
        <div className="text-[11px] text-muted-foreground leading-tight">
          {description}
        </div>
      )}
      
      {error && (
        <div className="text-[11px] font-medium text-destructive leading-tight">
          {error}
        </div>
      )}
    </div>
  );
};
