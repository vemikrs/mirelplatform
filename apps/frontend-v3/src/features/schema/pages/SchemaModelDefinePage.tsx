import React, { useState, useEffect } from 'react';
import { schemaApi } from '../api/schemaApi';
import type { SchDicModel } from '../types/schema';
import { FieldEditor } from '../components/FieldEditor';

export const SchemaModelDefinePage: React.FC = () => {
  const [modelId, setModelId] = useState('');
  const [modelName, setModelName] = useState('');
  const [isHidden, setIsHidden] = useState(false);
  const [fields, setFields] = useState<SchDicModel[]>([]);

  const handleAddField = () => {
    setFields([
      ...fields,
      {
        modelId,
        fieldId: `field_${fields.length + 1}`,
        fieldName: `Field ${fields.length + 1}`,
        widgetType: 'text',
        tenantId: '', // Set by backend
        version: 1,
        createdAt: '',
        createdBy: '',
        updatedAt: '',
        updatedBy: '',
      },
    ]);
  };

  const handleFieldChange = (index: number, updatedField: SchDicModel) => {
    const newFields = [...fields];
    newFields[index] = updatedField;
    setFields(newFields);
  };

  const handleDeleteField = (index: number) => {
    const newFields = fields.filter((_, i) => i !== index);
    setFields(newFields);
  };

  const handleSave = async () => {
    try {
      await schemaApi.saveSchema(modelId, modelName, isHidden, fields);
      alert('Model saved successfully');
    } catch (error) {
      console.error('Failed to save model:', error);
      alert('Failed to save model');
    }
  };

  const handleLoad = async () => {
    if (!modelId) return;
    try {
      const response = await schemaApi.listSchema(modelId);
      setFields(response.data.schemas);
      // Note: Header info loading not implemented in listSchema response yet, assuming fields only
    } catch (error) {
      console.error('Failed to load model:', error);
    }
  };

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">Model Definition</h1>
      
      <div className="mb-6 p-4 border rounded bg-white">
        <div className="grid grid-cols-2 gap-4 mb-4">
          <div>
            <label htmlFor="modelId" className="block text-sm font-medium">Model ID</label>
            <div className="flex">
              <input
                id="modelId"
                type="text"
                className="flex-1 p-2 border rounded-l"
                value={modelId}
                onChange={(e) => setModelId(e.target.value)}
              />
              <button
                onClick={handleLoad}
                className="px-4 py-2 bg-gray-200 border border-l-0 rounded-r hover:bg-gray-300"
              >
                Load
              </button>
            </div>
          </div>
          <div>
            <label htmlFor="modelName" className="block text-sm font-medium">Model Name</label>
            <input
              id="modelName"
              type="text"
              className="w-full p-2 border rounded"
              value={modelName}
              onChange={(e) => setModelName(e.target.value)}
            />
          </div>
        </div>
        <div className="mb-4">
          <label className="flex items-center">
            <input
              type="checkbox"
              className="mr-2"
              checked={isHidden}
              onChange={(e) => setIsHidden(e.target.checked)}
            />
            Hidden Model
          </label>
        </div>
      </div>

      <div className="mb-6">
        <h2 className="text-xl font-semibold mb-4">Fields</h2>
        {fields.map((field, index) => (
          <FieldEditor
            key={index}
            field={field}
            onChange={(updatedField) => handleFieldChange(index, updatedField)}
            onDelete={() => handleDeleteField(index)}
          />
        ))}
        <button
          onClick={handleAddField}
          className="w-full py-2 border-2 border-dashed border-gray-300 rounded text-gray-500 hover:border-gray-400 hover:text-gray-600"
        >
          + Add Field
        </button>
      </div>

      <div className="flex justify-end">
        <button
          onClick={handleSave}
          className="px-6 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
        >
          Save Model
        </button>
      </div>
    </div>
  );
};
