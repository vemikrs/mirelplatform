import React from 'react';
import { cn } from '@mirel/ui';
import type { SchDicModel } from '../../types/modeler';

interface ModelerPropertyPanelProps {
  className?: string;
  selectedField: SchDicModel | null;
  onFieldUpdate?: (field: SchDicModel) => void;
  onDeleteField?: () => void;
}

export const ModelerPropertyPanel: React.FC<ModelerPropertyPanelProps> = ({ 
  className, 
  selectedField,
  onFieldUpdate,
  onDeleteField
}) => {
  if (!selectedField) {
    return (
      <div className={cn("flex flex-col h-full border-l bg-background p-6 text-center text-muted-foreground", className)}>
        <p className="mt-10">Select a field to edit properties</p>
      </div>
    );
  }

  const handleChange = (key: keyof SchDicModel, value: any) => {
    if (onFieldUpdate) {
      onFieldUpdate({ ...selectedField, [key]: value });
    }
  };

  return (
    <div className={cn("flex flex-col h-full border-l bg-background overflow-y-auto", className)}>
      <div className="p-4 border-b flex justify-between items-center bg-muted/5">
        <div>
          <h2 className="text-sm font-semibold tracking-tight text-foreground uppercase">
            Properties
          </h2>
          <p className="text-xs text-muted-foreground mt-0.5 truncate max-w-[150px]">
            {selectedField.fieldName}
          </p>
        </div>
        {onDeleteField && (
          <button
            onClick={onDeleteField}
            className="text-xs text-destructive hover:bg-destructive/10 px-2 py-1 rounded transition-colors"
            title="Delete Field"
          >
            Delete
          </button>
        )}
      </div>

      <div className="p-4 space-y-6">
        {/* Basic Settings */}
        <div className="space-y-4">
          <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider border-b pb-1">
            Basic
          </h3>
          
          <div className="space-y-2">
            <label className="text-xs font-medium text-foreground">Field ID</label>
            <input
              type="text"
              className="w-full px-3 py-1.5 text-sm border rounded bg-background text-foreground focus:ring-1 focus:ring-primary"
              value={selectedField.fieldId}
              onChange={(e) => handleChange('fieldId', e.target.value)}
            />
          </div>

          <div className="space-y-2">
            <label className="text-xs font-medium text-foreground">Display Name</label>
            <input
              type="text"
              className="w-full px-3 py-1.5 text-sm border rounded bg-background text-foreground focus:ring-1 focus:ring-primary"
              value={selectedField.fieldName}
              onChange={(e) => handleChange('fieldName', e.target.value)}
            />
          </div>

          <div className="space-y-2">
            <label className="text-xs font-medium text-foreground">Widget Type</label>
            <select
              className="w-full px-3 py-1.5 text-sm border rounded bg-background text-foreground focus:ring-1 focus:ring-primary"
              value={selectedField.widgetType}
              onChange={(e) => handleChange('widgetType', e.target.value)}
            >
              <option value="text">Text</option>
              <option value="textarea">Textarea</option>
              <option value="text_date">Date</option>
              <option value="text_time">Time</option>
              <option value="checkbox">Checkbox</option>
              <option value="selectbox">Selectbox</option>
              <option value="tag">Tag Input</option>
              <option value="title">Title (Section Header)</option>
              <option value="domain">Domain (Nested)</option>
            </select>
          </div>

          {(selectedField.widgetType === 'selectbox' || selectedField.widgetType === 'tag') && (
            <div className="space-y-2">
              <label className="text-xs font-medium text-foreground">Relation Code Group</label>
              <input
                type="text"
                className="w-full px-3 py-1.5 text-sm border rounded bg-background text-foreground focus:ring-1 focus:ring-primary"
                value={selectedField.relationCodeGroup || ''}
                onChange={(e) => handleChange('relationCodeGroup', e.target.value)}
                placeholder="e.g. GENDER"
              />
            </div>
          )}
        </div>

        {/* Validation Settings */}
        <div className="space-y-4">
          <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider border-b pb-1">
            Validation
          </h3>
          
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="text-xs font-medium text-foreground">Min Length</label>
              <input
                type="number"
                className="w-full px-3 py-1.5 text-sm border rounded bg-background text-foreground focus:ring-1 focus:ring-primary"
                value={selectedField.minLength || ''}
                onChange={(e) => handleChange('minLength', e.target.value ? parseInt(e.target.value) : undefined)}
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-medium text-foreground">Max Length</label>
              <input
                type="number"
                className="w-full px-3 py-1.5 text-sm border rounded bg-background text-foreground focus:ring-1 focus:ring-primary"
                value={selectedField.maxLength || ''}
                onChange={(e) => handleChange('maxLength', e.target.value ? parseInt(e.target.value) : undefined)}
              />
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-xs font-medium text-foreground">Regex Pattern</label>
            <input
              type="text"
              className="w-full px-3 py-1.5 text-sm border rounded bg-background text-foreground focus:ring-1 focus:ring-primary"
              value={selectedField.regexPattern || ''}
              onChange={(e) => handleChange('regexPattern', e.target.value)}
              placeholder="e.g. ^[0-9]+$"
            />
          </div>
        </div>

        {/* Flags */}
        <div className="space-y-4">
          <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider border-b pb-1">
            Flags
          </h3>
          
          <div className="space-y-3">
            <label className="flex items-center space-x-2 cursor-pointer">
              <input
                type="checkbox"
                className="rounded border-input text-primary focus:ring-primary"
                checked={selectedField.isKey || false}
                onChange={(e) => handleChange('isKey', e.target.checked)}
              />
              <span className="text-sm text-foreground">Primary Key</span>
            </label>
            
            <label className="flex items-center space-x-2 cursor-pointer">
              <input
                type="checkbox"
                className="rounded border-input text-primary focus:ring-primary"
                checked={selectedField.isRequired || false}
                onChange={(e) => handleChange('isRequired', e.target.checked)}
              />
              <span className="text-sm text-foreground">Required</span>
            </label>

            <label className="flex items-center space-x-2 cursor-pointer">
              <input
                type="checkbox"
                className="rounded border-input text-primary focus:ring-primary"
                checked={selectedField.isHeader || false}
                onChange={(e) => handleChange('isHeader', e.target.checked)}
              />
              <span className="text-sm text-foreground">Show in List Header</span>
            </label>
          </div>
        </div>

        {/* Description */}
        <div className="space-y-4">
          <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider border-b pb-1">
            Other
          </h3>
          <div className="space-y-2">
            <label className="text-xs font-medium text-foreground">Description</label>
            <textarea
              className="w-full px-3 py-1.5 text-sm border rounded bg-background text-foreground focus:ring-1 focus:ring-primary min-h-[80px]"
              value={selectedField.description || ''}
              onChange={(e) => handleChange('description', e.target.value)}
            />
          </div>
        </div>
      </div>
    </div>
  );
};
