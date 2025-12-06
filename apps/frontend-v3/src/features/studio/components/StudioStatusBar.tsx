import React from 'react';
import { cn } from '@mirel/ui';
import { GitBranch, AlertCircle, CheckCircle, Smartphone, Globe } from 'lucide-react';

interface StudioStatusBarProps {
  className?: string;
  environment?: 'development' | 'staging' | 'production';
  hasErrors?: boolean;
  errorCount?: number;
  isDraft?: boolean;
}

export const StudioStatusBar: React.FC<StudioStatusBarProps> = ({
  className,
  environment = 'development',
  hasErrors = false,
  errorCount = 0,
  isDraft = false,
}) => {
  return (
    <div className={cn("h-6 bg-primary text-primary-foreground flex items-center px-4 text-xs select-none", className)}>
      <div className="flex items-center gap-4 flex-1">
        <div className="flex items-center gap-1.5 opacity-90 hover:opacity-100 cursor-pointer">
          <Globe className="size-3" />
          <span className="capitalize">{environment}</span>
        </div>
        
        {isDraft && (
          <div className="flex items-center gap-1.5 text-yellow-300">
            <GitBranch className="size-3" />
            <span>Draft Mode</span>
          </div>
        )}

        {hasErrors ? (
          <div className="flex items-center gap-1.5 text-red-300">
            <AlertCircle className="size-3" />
            <span>{errorCount} Errors</span>
          </div>
        ) : (
          <div className="flex items-center gap-1.5 text-green-300 opacity-80">
            <CheckCircle className="size-3" />
            <span>Ready</span>
          </div>
        )}
      </div>

      <div className="flex items-center gap-4">
        <div className="flex items-center gap-1.5 opacity-70 hover:opacity-100 cursor-pointer">
           <Smartphone className="size-3" />
           <span>Mobile View</span>
        </div>
        <div className="opacity-50">
           Mirel Studio v3.0.0
        </div>
      </div>
    </div>
  );
};
