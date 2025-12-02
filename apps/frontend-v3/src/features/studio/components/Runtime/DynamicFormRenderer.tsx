import React from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Button, Input, Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '@mirel/ui';
import type { Widget } from '../../stores/useFormDesignerStore';

interface DynamicFormRendererProps {
  widgets: Widget[];
  onSubmit: (data: any) => void;
}

export const DynamicFormRenderer: React.FC<DynamicFormRendererProps> = ({ widgets, onSubmit }) => {
  // Generate Zod schema dynamically
  const schemaShape: Record<string, z.ZodTypeAny> = {};
  
  widgets.forEach((widget) => {
    let validator: z.ZodTypeAny;

    switch (widget.type) {
      case 'number':
        validator = z.coerce.number(); // Handle string input as number
        break;
      case 'boolean':
        validator = z.boolean();
        break;
      case 'date':
        validator = z.string(); // Simplify date as string for now
        break;
      default:
        validator = z.string();
    }

    if (widget.required) {
        // For strings, min(1) is usually what we want for "required"
        if (widget.type === 'text' || widget.type === 'select' || widget.type === 'date') {
             validator = (validator as z.ZodString).min(1, { message: 'Required' });
        }
        // For numbers/booleans, base type check is often enough, but can add refinements
    } else {
        validator = validator.optional();
    }

    schemaShape[widget.id] = validator;
  });

  const schema = z.object(schemaShape);
  type FormData = z.infer<typeof schema>;

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      {widgets.map((widget) => (
        <div key={widget.id} className="space-y-2">
          <label className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
            {widget.label}
            {widget.required && <span className="text-red-500 ml-1">*</span>}
          </label>
          
          <Controller
            control={control}
            name={widget.id}
            render={({ field }) => {
              switch (widget.type) {
                case 'text':
                  return <Input {...field} value={field.value as string || ''} />;
                case 'number':
                    return <Input type="number" {...field} value={field.value as number || ''} />;
                case 'boolean':
                    return (
                        <input 
                            type="checkbox" 
                            checked={!!field.value} 
                            onChange={field.onChange}
                            className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                        />
                    );
                case 'select':
                    return (
                        <Select onValueChange={field.onChange} defaultValue={field.value as string}>
                            <SelectTrigger>
                                <SelectValue placeholder="Select an option" />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="option1">Option 1</SelectItem>
                                <SelectItem value="option2">Option 2</SelectItem>
                                <SelectItem value="option3">Option 3</SelectItem>
                            </SelectContent>
                        </Select>
                    );
                case 'date':
                     return <Input type="date" {...field} value={field.value as string || ''} />;
                default:
                  return <Input {...field} value={field.value as string || ''} />;
              }
            }}
          />
          
          {errors[widget.id] && (
            <p className="text-sm font-medium text-red-500">
              {errors[widget.id]?.message as string}
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
