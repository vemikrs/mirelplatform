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
    <div className="border p-4 rounded mb-4 bg-card text-card-foreground shadow-sm">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
        {/* Basic Info */}
        <div className="col-span-2 md:col-span-1 space-y-2">
          <h4 className="font-semibold text-sm text-muted-foreground mb-2">基本設定</h4>
          <div>
            <label className="block text-sm font-medium mb-1">Field ID</label>
            <input
              type="text"
              className="w-full p-2 border border-input rounded bg-background"
              value={field.fieldId}
              onChange={(e) => handleChange('fieldId', e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Field Name</label>
            <input
              type="text"
              className="w-full p-2 border border-input rounded bg-background"
              value={field.fieldName}
              onChange={(e) => handleChange('fieldName', e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Widget Type</label>
            <select
              className="w-full p-2 border border-input rounded bg-background"
              value={field.widgetType}
              onChange={(e) => handleChange('widgetType', e.target.value)}
            >
              <option value="text">Text</option>
              <option value="textarea">Textarea</option>
              <option value="text_date">Date</option>
              <option value="text_time">Time</option>
              <option value="checkbox">Checkbox</option>
              <option value="selectbox">Selectbox</option>
              <option value="title">Title (Section Header)</option>
            </select>
          </div>
          {(field.widgetType === 'selectbox' || field.widgetType === 'tag') && (
            <div>
              <label className="block text-sm font-medium mb-1">Relation Code Group</label>
              <input
                type="text"
                className="w-full p-2 border border-input rounded bg-background"
                value={field.relationCodeGroup || ''}
                onChange={(e) => handleChange('relationCodeGroup', e.target.value)}
                placeholder="e.g. GENDER"
              />
            </div>
          )}
        </div>

        {/* Validation & Display */}
        <div className="col-span-2 md:col-span-1 space-y-2">
          <h4 className="font-semibold text-sm text-muted-foreground mb-2">バリデーション・表示</h4>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="block text-sm font-medium mb-1">Min Length</label>
              <input
                type="number"
                className="w-full p-2 border border-input rounded bg-background"
                value={field.minLength || ''}
                onChange={(e) => handleChange('minLength', e.target.value ? parseInt(e.target.value) : undefined)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Max Length</label>
              <input
                type="number"
                className="w-full p-2 border border-input rounded bg-background"
                value={field.maxLength || ''}
                onChange={(e) => handleChange('maxLength', e.target.value ? parseInt(e.target.value) : undefined)}
              />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Regex Pattern</label>
            <input
              type="text"
              className="w-full p-2 border border-input rounded bg-background"
              value={field.regexPattern || ''}
              onChange={(e) => handleChange('regexPattern', e.target.value)}
              placeholder="e.g. ^[0-9]+$"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Description</label>
            <input
              type="text"
              className="w-full p-2 border border-input rounded bg-background"
              value={field.description || ''}
              onChange={(e) => handleChange('description', e.target.value)}
            />
          </div>
        </div>
      </div>

      {/* Flags */}
      <div className="flex flex-wrap items-center gap-4 pt-2 border-t border-border">
        <label className="flex items-center cursor-pointer">
          <input
            type="checkbox"
            className="mr-2 rounded border-input"
            checked={field.isKey}
            onChange={(e) => handleChange('isKey', e.target.checked)}
          />
          <span className="text-sm">Is Key</span>
        </label>
        <label className="flex items-center cursor-pointer">
          <input
            type="checkbox"
            className="mr-2 rounded border-input"
            checked={field.isRequired}
            onChange={(e) => handleChange('isRequired', e.target.checked)}
          />
          <span className="text-sm">Required</span>
        </label>
        <label className="flex items-center cursor-pointer">
          <input
            type="checkbox"
            className="mr-2 rounded border-input"
            checked={field.isHeader}
            onChange={(e) => handleChange('isHeader', e.target.checked)}
          />
          <span className="text-sm">List Header</span>
        </label>
        <div className="flex-1"></div>
        <button
          onClick={onDelete}
          className="px-3 py-1 text-sm text-destructive hover:bg-destructive/10 rounded transition-colors"
        >
          Delete Field
        </button>
      </div>
    </div>
  );
};
