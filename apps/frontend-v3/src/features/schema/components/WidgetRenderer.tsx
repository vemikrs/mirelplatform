import React from 'react';
import type { SchDicModel } from '../types/schema';

interface WidgetRendererProps {
  field: SchDicModel;
  value: any;
  onChange: (value: any) => void;
  disabled?: boolean;
}

export const WidgetRenderer: React.FC<WidgetRendererProps> = ({ field, value, onChange, disabled }) => {
  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    onChange(e.target.value);
  };

  switch (field.widgetType) {
    case 'text':
      return (
        <input
          type="text"
          className="w-full p-2 border rounded"
          value={value || ''}
          onChange={handleChange}
          disabled={disabled}
        />
      );
    case 'textarea':
      return (
        <textarea
          className="w-full p-2 border rounded"
          value={value || ''}
          onChange={handleChange}
          disabled={disabled}
        />
      );
    case 'text_date':
      return (
        <input
          type="date"
          className="w-full p-2 border rounded"
          value={value || ''}
          onChange={handleChange}
          disabled={disabled}
        />
      );
    case 'checkbox':
      return (
        <input
          type="checkbox"
          className="p-2 border rounded"
          checked={!!value}
          onChange={(e) => onChange(e.target.checked)}
          disabled={disabled}
        />
      );
    case 'selectbox':
      // TODO: Implement selectbox with code master
      return (
        <select
          className="w-full p-2 border rounded"
          value={value || ''}
          onChange={handleChange}
          disabled={disabled}
        >
          <option value="">Select...</option>
          {/* Options should be loaded from code master */}
        </select>
      );
    default:
      return (
        <input
          type="text"
          className="w-full p-2 border rounded"
          value={value || ''}
          onChange={handleChange}
          disabled={disabled}
        />
      );
  }
};
