import React from 'react';
import type { SchDicModel } from '../types/schema';

interface FieldEditorProps {
  field: SchDicModel;
  onChange: (field: SchDicModel) => void;
  onDelete: () => void;
}

export const FieldEditor: React.FC<FieldEditorProps> = ({ field, onChange, onDelete }) => {
  const handleChange = (key: keyof SchDicModel, value: any) => {
    onChange({ ...field, [key]: value });
  };

  return (
    <div className="border p-4 rounded mb-4 bg-gray-50">
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium">Field ID</label>
          <input
            type="text"
            className="w-full p-1 border rounded"
            value={field.fieldId}
            onChange={(e) => handleChange('fieldId', e.target.value)}
          />
        </div>
        <div>
          <label className="block text-sm font-medium">Field Name</label>
          <input
            type="text"
            className="w-full p-1 border rounded"
            value={field.fieldName}
            onChange={(e) => handleChange('fieldName', e.target.value)}
          />
        </div>
        <div>
          <label className="block text-sm font-medium">Widget Type</label>
          <select
            className="w-full p-1 border rounded"
            value={field.widgetType}
            onChange={(e) => handleChange('widgetType', e.target.value)}
          >
            <option value="text">Text</option>
            <option value="textarea">Textarea</option>
            <option value="text_date">Date</option>
            <option value="checkbox">Checkbox</option>
            <option value="selectbox">Selectbox</option>
          </select>
        </div>
        <div className="flex items-center space-x-4">
          <label className="flex items-center">
            <input
              type="checkbox"
              className="mr-1"
              checked={field.isKey}
              onChange={(e) => handleChange('isKey', e.target.checked)}
            />
            Is Key
          </label>
          <label className="flex items-center">
            <input
              type="checkbox"
              className="mr-1"
              checked={field.isRequired}
              onChange={(e) => handleChange('isRequired', e.target.checked)}
            />
            Required
          </label>
          <label className="flex items-center">
            <input
              type="checkbox"
              className="mr-1"
              checked={field.isHeader}
              onChange={(e) => handleChange('isHeader', e.target.checked)}
            />
            List Header
          </label>
        </div>
      </div>
      <button
        onClick={onDelete}
        className="mt-2 text-red-600 hover:text-red-800 text-sm"
      >
        Delete Field
      </button>
    </div>
  );
};
