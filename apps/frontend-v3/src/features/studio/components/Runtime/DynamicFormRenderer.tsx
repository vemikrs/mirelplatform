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
        case 'textarea':
        case 'select':
        case 'radio':
          validator = z.string();
          if (w.required) validator = validator.min(1, { message: 'Required' });
          else validator = validator.optional();
          
          if (w.minLength) validator = (validator as z.ZodString).min(w.minLength, { message: `Min length is ${w.minLength}` });
          if (w.maxLength) validator = (validator as z.ZodString).max(w.maxLength, { message: `Max length is ${w.maxLength}` });
          if (w.validationRegex) {
             try {
               new RegExp(w.validationRegex);
               validator = (validator as z.ZodString).regex(new RegExp(w.validationRegex), { message: 'Invalid format' });
             } catch (e) {
               console.warn('Invalid regex:', w.validationRegex);
             }
          }
          break;
        // ... (number, boolean, date cases remain same)
        case 'number':
          validator = z.coerce.number();
          if (w.required) validator = validator.min(1, { message: 'Required' });
          
          if (w.minValue !== undefined) validator = (validator as z.ZodNumber).min(w.minValue, { message: `Min value is ${w.minValue}` });
          if (w.maxValue !== undefined) validator = (validator as z.ZodNumber).max(w.maxValue, { message: `Max value is ${w.maxValue}` });
          
          if (!w.required) validator = validator.optional();
          break;
        case 'boolean':
          validator = z.boolean();
          if (!w.required) validator = validator.optional();
          break;
        case 'date':
           validator = z.string();
           if (w.required) validator = validator.min(1, { message: 'Required' });
           else validator = validator.optional();
           break;
        default:
          validator = z.any();
      }
      // ...
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
          
          {(() => {
            const key = (widget as any).fieldCode || widget.id;
            switch (widget.type) {
              case 'text':
                return <Input {...register(key)} placeholder={widget.label} />;
              case 'textarea':
                return <textarea {...register(key)} className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50" placeholder={widget.label} />;
              case 'number':
                return <Input type="number" {...register(key)} placeholder={widget.label} />;
              case 'date':
                return <Input type="date" {...register(key)} />;
              case 'boolean':
                return (
                    <div className="flex items-center space-x-2">
                        <input type="checkbox" {...register(key)} className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary" />
                        <span className="text-sm text-muted-foreground">Yes</span>
                    </div>
                );
              case 'select':
                return (
                  <select {...register(key)} className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50">
                    <option value="">Select...</option>
                    {(widget.options || []).map((opt, i) => (
                        <option key={i} value={opt.value}>{opt.label}</option>
                    ))}
                  </select>
                );
              case 'radio':
                return (
                    <div className="space-y-2">
                        {(widget.options || []).map((opt, i) => (
                            <div key={i} className="flex items-center space-x-2">
                                <input 
                                    type="radio" 
                                    value={opt.value} 
                                    {...register(key)} 
                                    id={`${key}-${opt.value}`}
                                    className="aspect-square h-4 w-4 rounded-full border border-primary text-primary ring-offset-background focus:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                                />
                                <label htmlFor={`${key}-${opt.value}`} className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
                                    {opt.label}
                                </label>
                            </div>
                        ))}
                    </div>
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
