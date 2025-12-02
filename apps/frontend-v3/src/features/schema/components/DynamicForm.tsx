import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import type { SchDicModel } from '../types/schema';
import { WidgetRenderer } from './WidgetRenderer';

interface DynamicFormProps {
  fields: SchDicModel[];
  data: Record<string, any>;
  onChange: (data: Record<string, any>) => void;
  onSubmit: () => void;
}

export const DynamicForm: React.FC<DynamicFormProps> = ({ fields, data, onChange, onSubmit }) => {
  // Generate Zod schema dynamically
  const generateSchema = (fields: SchDicModel[]) => {
    const shape: Record<string, any> = {};
    
    fields.forEach((field) => {
      if (field.widgetType === 'title' || field.widgetType === 'break') return;

      let schema = z.string();

      if (field.minLength) {
        schema = schema.min(field.minLength, { message: `Min length is ${field.minLength}` });
      }

      if (field.maxLength) {
        schema = schema.max(field.maxLength, { message: `Max length is ${field.maxLength}` });
      }

      if (field.regexPattern) {
        try {
          const regex = new RegExp(field.regexPattern);
          schema = schema.regex(regex, { message: `Invalid format` });
        } catch (e) {
          console.warn(`Invalid regex pattern for field ${field.fieldName}: ${field.regexPattern}`);
        }
      }

      if (field.isRequired) {
        schema = schema.min(1, { message: `${field.fieldName} is required` });
        shape[field.fieldId] = schema;
      } else {
        // For optional fields, we allow empty string or undefined
        shape[field.fieldId] = schema.optional().or(z.literal(''));
      }
    });

    return z.object(shape);
  };

  const schema = generateSchema(fields);

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
    watch,
  } = useForm({
    resolver: zodResolver(schema),
    defaultValues: data,
  });

  // Watch all fields to propagate changes to parent (if needed for live preview or other logic)
  // Note: RHF manages state, so onChange might not be strictly needed for all cases,
  // but we keep it to maintain compatibility with parent's expectation of data updates.
  const watchedValues = watch();
  useEffect(() => {
    onChange(watchedValues);
  }, [watchedValues]);

  // Reset form when data prop changes (e.g. loading new record)
  useEffect(() => {
    reset(data);
  }, [data, reset]);

  const onValidSubmit = (formData: Record<string, any>) => {
    // Merge formData back to parent state via onChange before submitting?
    // Or just call onSubmit. Parent likely uses 'data' prop which we updated via useEffect.
    // However, RHF's formData is the most current valid state.
    onChange(formData);
    onSubmit();
  };

  return (
    <form onSubmit={handleSubmit(onValidSubmit)} className="space-y-4">
      {fields.map((field) => (
        <div key={field.fieldId} className="flex flex-col">
          {field.widgetType !== 'title' && field.widgetType !== 'break' && (
            <label className="mb-1 font-medium">
              {field.fieldName}
              {field.isRequired && <span className="text-red-500 ml-1">*</span>}
            </label>
          )}
          <WidgetRenderer
            field={field}
            registration={register(field.fieldId)}
            error={errors[field.fieldId]?.message as string}
          />
          {field.description && <span className="text-sm text-gray-500">{field.description}</span>}
        </div>
      ))}
      <button
        type="submit"
        className="px-4 py-2 bg-primary text-primary-foreground rounded hover:bg-primary/90 transition-colors"
      >
        Save
      </button>
    </form>
  );
};
