import type { DataElement } from '../types/api';
import type { UseParameterFormReturn } from '../hooks/useParameterForm';
import { FileUploadButton } from './FileUploadButton';
import { useState } from 'react';

interface ParameterFieldsProps {
  parameters: DataElement[];
  form: UseParameterFormReturn;
  disabled?: boolean;
}

/**
 * ParameterFields Component
 * 
 * Dynamically generates input fields based on stencil parameter configuration.
 * Supports text and file type parameters.
 * Integrated with React Hook Form + Zod validation (Step 6)
 * File upload support added in Step 7
 */
export function ParameterFields({
  parameters,
  form,
  disabled = false,
}: ParameterFieldsProps) {
  const { register, errors, setValue: formSetValue, getValue } = form;
  const [fileNames, setFileNames] = useState<Record<string, string>>({});

  const handleFileUploaded = (parameterId: string, fileId: string, fileName: string) => {
    formSetValue(parameterId, fileId);
    setFileNames((prev) => ({ ...prev, [parameterId]: fileName }));
  };

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
        {parameters.map((param) => {
          const error = errors[param.id];
          const hasError = !!error;

          return (
            <div key={param.id} className="space-y-2" data-testid={`param-field-${param.id}`}>
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
                <div className="space-y-2">
                  <input
                    type="hidden"
                    id={`param-${param.id}`}
                    data-testid={`param-${param.id}`}
                    {...register(param.id)}
                  />
                  <FileUploadButton
                    parameterId={param.id}
                    value={(getValue(param.id) as string) || ''}
                    onFileUploaded={handleFileUploaded}
                    disabled={disabled}
                  />
                  {fileNames[param.id] && (
                    <p 
                      className="text-xs text-muted-foreground"
                      data-testid={`file-name-${param.id}`}
                    >
                      📎 {fileNames[param.id]}
                    </p>
                  )}
                </div>
              ) : (
                <input
                  type="text"
                  id={`param-${param.id}`}
                  data-testid={`param-${param.id}`}
                  className={`w-full rounded-md border px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 ${
                    hasError 
                      ? 'border-red-500 bg-red-50' 
                      : 'border-input bg-background'
                  }`}
                  placeholder={param.placeholder || ''}
                  disabled={disabled}
                  {...register(param.id)}
                />
              )}

              {hasError && (
                <p 
                  className="text-xs text-red-600" 
                  data-testid={`error-${param.id}`}
                >
                  {error.message as string}
                </p>
              )}

              {param.note && !hasError && (
                <p 
                  className="text-xs text-muted-foreground" 
                  data-testid={`param-${param.id}-description`}
                >
                  {param.note}
                </p>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
