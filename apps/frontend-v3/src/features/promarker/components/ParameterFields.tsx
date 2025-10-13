import type { DataElement } from '../types/api';

interface ParameterFieldsProps {
  parameters: DataElement[];
  values: Record<string, string>;
  onValueChange: (parameterId: string, value: string) => void;
  disabled?: boolean;
}

/**
 * ParameterFields Component
 * 
 * Dynamically generates input fields based on stencil parameter configuration.
 * Supports text and file type parameters.
 */
export function ParameterFields({
  parameters,
  values,
  onValueChange,
  disabled = false,
}: ParameterFieldsProps) {
  if (parameters.length === 0) {
    return null;
  }

  return (
    <div className="space-y-4" data-testid="parameter-section">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">パラメータ入力</h3>
        <span className="text-sm text-muted-foreground">
          {parameters.length} 項目
        </span>
      </div>

      <div className="space-y-4">
        {parameters.map((param) => (
          <div key={param.id} className="space-y-2">
            <label 
              htmlFor={`param-${param.id}`}
              className="block text-sm font-medium"
            >
              {param.name}
              {param.note && (
                <span className="ml-2 text-xs text-muted-foreground">
                  {param.note}
                </span>
              )}
            </label>

            {param.valueType === 'file' ? (
              <div className="flex gap-2">
                <input
                  type="text"
                  id={`param-${param.id}`}
                  data-testid={`param-${param.id}`}
                  className="flex-1 rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
                  value={values[param.id] || ''}
                  onChange={(e) => onValueChange(param.id, e.target.value)}
                  placeholder={param.placeholder || 'ファイルIDを入力または選択'}
                  disabled={disabled}
                  readOnly
                />
                <button
                  type="button"
                  data-testid="file-upload-btn"
                  className="inline-flex items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground ring-offset-background transition-colors hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
                  disabled={disabled}
                  onClick={() => {
                    // TODO: Step 7 - Implement file upload dialog
                    alert('ファイルアップロード機能はStep 7で実装予定');
                  }}
                >
                  📎 選択
                </button>
              </div>
            ) : (
              <input
                type="text"
                id={`param-${param.id}`}
                data-testid={`param-${param.id}`}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
                value={values[param.id] || ''}
                onChange={(e) => onValueChange(param.id, e.target.value)}
                placeholder={param.placeholder || ''}
                disabled={disabled}
              />
            )}

            {param.note && (
              <p 
                className="text-xs text-muted-foreground" 
                data-testid={`param-${param.id}-description`}
              >
                {param.note}
              </p>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
