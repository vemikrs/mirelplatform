import { useMemo, useState } from 'react';
import {
  Badge,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  FormError,
  FormField,
  FormHelper,
  FormLabel,
  FormRequiredMark,
  Input,
} from '@mirel/ui';
import type { DataElement } from '../types/api';
import type { UseParameterFormReturn } from '../hooks/useParameterForm';
import { FileUploadButton } from './FileUploadButton';

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

  const parameterCountLabel = useMemo(() => `${parameters.length} 項目`, [parameters.length]);

  return (
    <div className="space-y-6" data-testid="parameter-section">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">パラメータ入力</h3>
        <Badge variant="neutral">{parameterCountLabel}</Badge>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {parameters.map((param) => {
          const error = errors[param.id];
          const hasError = !!error;

          const validationHints: string[] = [];
          if (param.validation?.minLength) {
            validationHints.push(`最小${param.validation.minLength}文字`);
          }
          if (param.validation?.maxLength) {
            validationHints.push(`最大${param.validation.maxLength}文字`);
          }
          if (param.validation?.pattern) {
            validationHints.push(param.validation.errorMessage ?? `正規表現: ${param.validation.pattern}`);
          }

          return (
            <Card key={param.id} data-testid={`param-field-${param.id}`}>
              <CardHeader className="space-y-3">
                <div className="flex items-center justify-between gap-3">
                  <CardTitle className="text-base text-foreground">{param.name}</CardTitle>
                  <Badge variant={param.validation?.required ? 'warning' : 'neutral'}>
                    {param.validation?.required ? '必須' : '任意'}
                  </Badge>
                </div>
                {param.note ? (
                  <CardDescription data-testid={`param-${param.id}-description`}>
                    {param.note}
                  </CardDescription>
                ) : null}
              </CardHeader>
              <CardContent className="space-y-3">
                <FormField>
                  <FormLabel htmlFor={`param-${param.id}`} requiredMark={param.validation?.required ? <FormRequiredMark /> : undefined}>
                    入力値
                  </FormLabel>
                  {param.valueType === 'file' ? (
                    <div className="space-y-3">
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
                        <FormHelper data-testid={`file-name-${param.id}`}>
                          選択中: {fileNames[param.id]}
                        </FormHelper>
                      )}
                    </div>
                  ) : (
                    <Input
                      id={`param-${param.id}`}
                      data-testid={`param-${param.id}`}
                      placeholder={param.placeholder || ''}
                      disabled={disabled}
                      aria-invalid={hasError}
                      {...register(param.id)}
                    />
                  )}
                </FormField>

                {hasError ? (
                  <FormError data-testid={`error-${param.id}`}>
                    {error.message as string}
                  </FormError>
                ) : null}

                {validationHints.length > 0 ? (
                  <FormHelper>
                    入力条件: {validationHints.join(' / ')}
                  </FormHelper>
                ) : null}
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
