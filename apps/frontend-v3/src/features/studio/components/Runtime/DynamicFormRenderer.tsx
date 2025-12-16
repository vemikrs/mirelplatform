import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Button, Input } from '@mirel/ui';
import type { Widget } from '../../stores/useFormDesignerStore';
import GridLayout from 'react-grid-layout';
import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';

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

          if (w.required) {
            validator = (validator as z.ZodString).min(1, { message: 'Required' });
          } else {
            validator = validator.optional().or(z.literal(''));
          }
          break;
        case 'number':
          validator = z.coerce.number();
          
          if (w.minValue !== undefined) validator = (validator as z.ZodNumber).min(w.minValue, { message: `Min value is ${w.minValue}` });
          if (w.maxValue !== undefined) validator = (validator as z.ZodNumber).max(w.maxValue, { message: `Max value is ${w.maxValue}` });
          
          if (w.required) {
             validator = validator.min(1, { message: 'Required' });
          } else {
             validator = validator.optional();
          }
          break;
        case 'boolean':
          validator = z.boolean();
          if (!w.required) validator = validator.optional();
          break;
        case 'date':
        case 'time':
        case 'datetime':
           validator = z.string();
           if (w.required) validator = validator.min(1, { message: 'Required' });
           else validator = validator.optional().or(z.literal(''));
           break;
        default:
          validator = z.any();
      }
      
      const key = (w as any).fieldCode || w.id;
      shape[key] = validator;
    });
    return z.object(shape);
  }, [widgets]);

  type FormData = z.infer<typeof schema>;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: defaultValues || {},
  });

  React.useEffect(() => {
    if (defaultValues) {
      reset(defaultValues);
    }
  }, [defaultValues, reset]);

  const layout = widgets.map((w) => ({
    i: w.id,
    x: w.x,
    y: w.y,
    w: w.w,
    h: w.h,
    static: true // Make items static (non-draggable/resizable)
  }));

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <GridLayout
        className="layout"
        layout={layout}
        // @ts-ignore
        cols={12}
        rowHeight={30}
        width={800}
        isDraggable={false}
        isResizable={false}
      >
      {widgets.map((widget) => {
        const key = (widget as any).fieldCode || widget.id;
        return (
        <div key={widget.id} className="space-y-1 bg-white">
          <label htmlFor={key} className="text-sm font-medium block">
            {widget.label}
            {widget.required && <span className="text-red-500 ml-1">*</span>}
          </label>
          
          {(() => {
            switch (widget.type) {
              case 'text':
                return <Input id={key} {...register(key)} placeholder={widget.label} />;
              case 'textarea':
                return <textarea id={key} {...register(key)} className="flex min-h-[60px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50" placeholder={widget.label} />;
              case 'number':
                return <Input id={key} type="number" {...register(key)} placeholder={widget.label} />;
              case 'date':
                return <Input id={key} type="date" {...register(key)} />;
              case 'time':
                return <Input id={key} type="time" {...register(key)} />;
              case 'datetime':
                return <Input id={key} type="datetime-local" {...register(key)} />;
              case 'boolean':
                return (
                    <div className="flex items-center space-x-2">
                        <input id={key} type="checkbox" {...register(key)} className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary" />
                        <span className="text-sm text-muted-foreground">Yes</span>
                    </div>
                );
              case 'select':
                return (
                  <select id={key} {...register(key)} className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50">
                    <option value="">Select...</option>
                    {(widget.options || []).map((opt, i) => (
                        <option key={i} value={opt.value}>{opt.label}</option>
                    ))}
                  </select>
                );
              case 'radio':
                return (
                    <div className="space-y-1">
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
                return <Input id={key} {...register(key)} />;
            }
          })()}
          
          {errors[(widget as any).fieldCode || widget.id] && (
            <p className="text-xs text-red-500">
              {errors[(widget as any).fieldCode || widget.id]?.message as string}
            </p>
          )}
        </div>
        );
      })}
      </GridLayout>

      <div className="mt-8 pt-4 border-t">
        <Button type="submit">
            Submit
        </Button>
      </div>
    </form>
  );
};
