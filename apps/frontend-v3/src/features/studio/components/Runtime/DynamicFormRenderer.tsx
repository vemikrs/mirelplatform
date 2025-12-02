import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Button, Input } from '@mirel/ui';
import type { Widget } from '../../stores/useFormDesignerStore';

interface DynamicFormRendererProps {
  widgets: Widget[];
  defaultValues?: Record<string, any>;
  onSubmit: (data: any) => void;
}

export const DynamicFormRenderer: React.FC<DynamicFormRendererProps> = ({ widgets, defaultValues, onSubmit }) => {
  const schema = React.useMemo(() => {
    const shape: any = {};
    widgets.forEach(w => {
      let validator: any;
      switch (w.type) {
        case 'text':
        case 'select':
          validator = z.string();
          if (w.required) validator = validator.min(1, { message: 'Required' });
          else validator = validator.optional();
          break;
        case 'number':
          validator = z.coerce.number(); // Handle string input for numbers
          if (w.required) validator = validator.min(1, { message: 'Required' });
          else validator = validator.optional();
          break;
        case 'boolean':
          validator = z.boolean();
          if (!w.required) validator = validator.optional();
          break;
        case 'date':
           validator = z.string(); // Date input returns string
           if (w.required) validator = validator.min(1, { message: 'Required' });
           else validator = validator.optional();
           break;
        default:
          validator = z.any();
      }
      // Use fieldCode for validation schema key if available, else label (fallback)
      // Ideally we should always use fieldCode or ID.
      // For now, let's use fieldCode if available, otherwise fallback to ID to ensure uniqueness.
      const key = (w as any).fieldCode || w.id; // Cast to any to allow fieldCode if not in Widget type
      shape[key] = validator;
    });
    return z.object(shape);
  }, [widgets]);

  type FormData = z.infer<typeof schema>;

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: defaultValues || {},
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      {widgets.map((widget) => (
        <div key={widget.id} className="space-y-2">
          <label className="text-sm font-medium">
            {widget.label}
            {widget.required && <span className="text-red-500 ml-1">*</span>}
          </label>
          
          {/* Render input based on type */}
          {(() => {
            const key = (widget as any).fieldCode || widget.id;
            switch (widget.type) {
              case 'text':
                return <Input {...register(key)} placeholder={widget.label} />;
              case 'number':
                return <Input type="number" {...register(key)} placeholder={widget.label} />;
              case 'date':
                return <Input type="date" {...register(key)} />;
              case 'boolean':
                // Note: Checkbox component is not imported, assuming it exists or needs to be added.
                // For a simple boolean input, a native checkbox can be used.
                return <input type="checkbox" {...register(key)} />;
              case 'select':
                // Note: This uses a native HTML select, not the @mirel/ui Select component structure.
                // If @mirel/ui Select is intended, its specific structure for options would be needed.
                return (
                  <select {...register(key)}>
                    <option value="">Select...</option>
                    <option value="Option 1">Option 1</option>
                    <option value="Option 2">Option 2</option>
                  </select>
                );
              default:
                return <Input {...register(key)} />;
            }
          })()}
          
          {errors[(widget as any).fieldCode || widget.id] && (
            <p className="text-xs text-red-500">
              {errors[(widget as any).fieldCode || widget.id]?.message as string}
            </p>
          )}
        </div>
      ))}

      <Button type="submit" className="mt-4">
        Submit
      </Button>
    </form>
  );
};
