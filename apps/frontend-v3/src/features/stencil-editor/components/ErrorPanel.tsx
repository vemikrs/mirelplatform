import React from 'react';
import { Alert, AlertDescription } from '@mirel/ui';

export type ErrorSeverity = 'error' | 'warning' | 'info';

export interface ValidationError {
  severity: ErrorSeverity;
  message: string;
  line?: number;
  column?: number;
  file?: string;
  code?: string;
}

interface ErrorPanelProps {
  errors: ValidationError[];
  onErrorClick?: (error: ValidationError) => void;
}

const severityConfig = {
  error: {
    variant: 'destructive' as const,
    icon: '❌',
    label: 'エラー',
    color: 'text-red-600',
  },
  warning: {
    variant: 'default' as const,
    icon: '⚠️',
    label: '警告',
    color: 'text-yellow-600',
  },
  info: {
    variant: 'default' as const,
    icon: 'ℹ️',
    label: '情報',
    color: 'text-blue-600',
  },
};

export function ErrorPanel({ errors, onErrorClick }: ErrorPanelProps) {
  if (errors.length === 0) {
    return null;
  }

  const errorCount = errors.filter((e) => e.severity === 'error').length;
  const warningCount = errors.filter((e) => e.severity === 'warning').length;
  const infoCount = errors.filter((e) => e.severity === 'info').length;

  return (
    <div className="border-t border-gray-200 bg-gray-50 p-4">
      <div className="mb-3 flex items-center gap-4 text-sm">
        <span className="font-semibold">検証結果</span>
        {errorCount > 0 && (
          <span className="text-red-600">
            ❌ エラー: {errorCount}
          </span>
        )}
        {warningCount > 0 && (
          <span className="text-yellow-600">
            ⚠️ 警告: {warningCount}
          </span>
        )}
        {infoCount > 0 && (
          <span className="text-blue-600">
            ℹ️ 情報: {infoCount}
          </span>
        )}
      </div>

      <div className="max-h-48 space-y-2 overflow-y-auto">
        {errors.map((error, index) => {
          const config = severityConfig[error.severity];
          const location =
            error.line !== undefined
              ? ` (行 ${error.line}${error.column !== undefined ? `:${error.column}` : ''})`
              : '';

          return (
            <Alert
              key={index}
              variant={config.variant}
              className={`cursor-pointer transition-colors hover:bg-gray-100 ${
                error.severity === 'error' ? 'border-red-300' : ''
              }`}
              onClick={() => onErrorClick?.(error)}
            >
              <AlertDescription className="flex items-start gap-2">
                <span className="text-lg">{config.icon}</span>
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className={`text-xs font-semibold ${config.color}`}>
                      {config.label}
                    </span>
                    {error.file && (
                      <span className="text-xs text-gray-500">{error.file}</span>
                    )}
                    {location && (
                      <span className="text-xs text-gray-500">{location}</span>
                    )}
                  </div>
                  <p className="mt-1 text-sm">{error.message}</p>
                  {error.code && (
                    <code className="mt-1 block text-xs text-gray-600">
                      {error.code}
                    </code>
                  )}
                </div>
              </AlertDescription>
            </Alert>
          );
        })}
      </div>
    </div>
  );
}
