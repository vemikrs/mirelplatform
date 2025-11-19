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
    label: 'エラー',
    color: 'text-red-600',
  },
  warning: {
    variant: 'default' as const,
    label: '警告',
    color: 'text-yellow-600',
  },
  info: {
    variant: 'default' as const,
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
    <div className="border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 p-4">
      <div className="mb-3 flex items-center gap-4 text-sm">
        <span className="font-semibold dark:text-white">検証結果</span>
        {errorCount > 0 && (
          <span className="flex items-center gap-1 text-red-600 dark:text-red-400">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="10" strokeWidth={2} />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
            エラー: {errorCount}
          </span>
        )}
        {warningCount > 0 && (
          <span className="flex items-center gap-1 text-yellow-600 dark:text-yellow-400">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            警告: {warningCount}
          </span>
        )}
        {infoCount > 0 && (
          <span className="flex items-center gap-1 text-blue-600 dark:text-blue-400">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="10" strokeWidth={2} />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 16v-4m0-4h.01" />
            </svg>
            情報: {infoCount}
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
              className="cursor-pointer transition-colors hover:bg-gray-100 dark:hover:bg-gray-700"
              onClick={() => onErrorClick?.(error)}
            >
              <div className="flex items-start gap-2">
                <svg className="w-4 h-4 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  {error.severity === 'error' ? (
                    <>
                      <circle cx="12" cy="12" r="10" strokeWidth={2} />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </>
                  ) : error.severity === 'warning' ? (
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  ) : (
                    <>
                      <circle cx="12" cy="12" r="10" strokeWidth={2} />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 16v-4m0-4h.01" />
                    </>
                  )}
                </svg>
                <AlertDescription>
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
                </AlertDescription>
              </div>
            </Alert>
          );
        })}
      </div>
    </div>
  );
}
