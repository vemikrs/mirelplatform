import React from 'react';
import type { SchDicModel } from '../types/schema';
import { CodeSelect } from './CodeSelect';
import type { UseFormRegisterReturn } from 'react-hook-form';

interface WidgetRendererProps {
  field: SchDicModel;
  value?: any; // Keep for backward compatibility or controlled mode if needed
  onChange?: (value: any) => void; // Keep for backward compatibility
  disabled?: boolean;
  registration?: UseFormRegisterReturn; // For React Hook Form
  error?: string;
}

export const WidgetRenderer: React.FC<WidgetRendererProps> = ({
  field,
  value,
  onChange,
  disabled,
  registration,
  error,
}) => {
  // Helper to handle change if not using RHF (backward compatibility)
  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    if (onChange) {
      onChange(e.target.value);
    }
    registration?.onChange(e); // Call RHF onChange
  };

  const commonProps = {
    className: `w-full p-2 border rounded ${error ? 'border-red-500' : 'border-input'} bg-background text-foreground`,
    disabled,
    ...registration,
    onChange: handleChange, // Override RHF onChange to support local handler if needed
  };

  // If using RHF, value is managed by it, so we don't pass value prop unless controlled manually
  // But for now, we assume RHF is primary if registration is present.
  // If registration is NOT present, we use value/onChange (legacy mode).
  const inputProps = registration ? commonProps : { ...commonProps, value: value || '' };

  switch (field.widgetType) {
    case 'title':
      return (
        <div className="py-2 mt-4 border-b border-border">
          <h3 className="text-lg font-semibold text-foreground">{field.fieldName}</h3>
          {field.description && <p className="text-sm text-muted-foreground">{field.description}</p>}
        </div>
      );
    case 'text':
      return (
        <>
          <input type="text" {...inputProps} />
          {error && <span className="text-xs text-red-500">{error}</span>}
        </>
      );
    case 'textarea':
      return (
        <>
          <textarea {...inputProps} />
          {error && <span className="text-xs text-red-500">{error}</span>}
        </>
      );
    case 'text_date':
      return (
        <>
          <input type="date" {...inputProps} />
          {error && <span className="text-xs text-red-500">{error}</span>}
        </>
      );
    case 'text_time':
      return (
        <>
          <input type="time" {...inputProps} />
          {error && <span className="text-xs text-red-500">{error}</span>}
        </>
      );
    case 'checkbox':
      return (
        <>
          <input
            type="checkbox"
            className="p-2 border rounded border-input bg-background"
            disabled={disabled}
            {...registration}
            checked={registration ? undefined : !!value}
            onChange={(e) => {
              if (onChange) onChange(e.target.checked);
              registration?.onChange(e);
            }}
          />
          {error && <span className="text-xs text-red-500">{error}</span>}
        </>
      );
    case 'selectbox':
      return (
        <>
          <CodeSelect
            codeGroupId={field.relationCodeGroup || ''}
            {...inputProps}
          />
          {error && <span className="text-xs text-red-500">{error}</span>}
        </>
      );
    default:
      return (
        <>
          <input type="text" {...inputProps} />
          {error && <span className="text-xs text-red-500">{error}</span>}
        </>
      );
  }
};
