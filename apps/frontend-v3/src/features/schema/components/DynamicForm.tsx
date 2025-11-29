import React from 'react';
import type { SchDicModel } from '../types/schema';
import { WidgetRenderer } from './WidgetRenderer';

interface DynamicFormProps {
  fields: SchDicModel[];
  data: Record<string, any>;
  onChange: (data: Record<string, any>) => void;
  onSubmit: () => void;
}

export const DynamicForm: React.FC<DynamicFormProps> = ({ fields, data, onChange, onSubmit }) => {
  const handleFieldChange = (fieldId: string, value: any) => {
    onChange({ ...data, [fieldId]: value });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit();
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {fields.map((field) => (
        <div key={field.fieldId} className="flex flex-col">
          <label className="mb-1 font-medium">
            {field.fieldName}
            {field.isRequired && <span className="text-red-500 ml-1">*</span>}
          </label>
          <WidgetRenderer
            field={field}
            value={data[field.fieldId]}
            onChange={(value) => handleFieldChange(field.fieldId, value)}
          />
          {field.description && <span className="text-sm text-gray-500">{field.description}</span>}
        </div>
      ))}
      <button
        type="submit"
        className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
      >
        Save
      </button>
    </form>
  );
};
