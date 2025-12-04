import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { modelerApi } from '../api/modelerApi';
import type { SchDicModel } from '../types/modeler';
import { ModelerLayout } from '../components/layout/ModelerLayout';
import { ModelerExplorer } from '../components/layout/ModelerExplorer';
import { ModelerPropertyPanel } from '../components/layout/ModelerPropertyPanel';
import { useToast, cn } from '@mirel/ui';

export const ModelerModelDefinePage: React.FC = () => {
  const { toast } = useToast();
  const [searchParams] = useSearchParams();
  const queryModelId = searchParams.get('modelId');
  const isNew = searchParams.get('new') === 'true';

  const [modelId, setModelId] = useState('');
  const [modelName, setModelName] = useState('');
  const [modelType, setModelType] = useState<'transaction' | 'master'>('master');
  const [isHidden, setIsHidden] = useState(false);
  const [fields, setFields] = useState<SchDicModel[]>([]);
  
  const [selectedFieldIndex, setSelectedFieldIndex] = useState<number | null>(null);

  useEffect(() => {
    if (queryModelId) {
      setModelId(queryModelId);
      loadModel(queryModelId);
    } else if (isNew) {
      resetForm();
    }
  }, [queryModelId, isNew]);

  const resetForm = () => {
    setModelId('');
    setModelName('');
    setModelType('master');
    setIsHidden(false);
    setFields([]);
    setSelectedFieldIndex(null);
  };

  const loadModel = async (id: string) => {
    try {
      const response = await modelerApi.listModel(id);
      setFields(response.data.modelers);
      // Assuming the first record might contain header info or separate API needed
      // For now, we just load fields. 
      // TODO: Fetch header info (name, type) correctly
      if (response.data.modelers.length > 0) {
        // Infer basic info from first field if available or just keep defaults
        // In a real app, we'd have a getModelHeader API
      }
    } catch (error) {
      console.error('Failed to load model:', error);
      toast({
        title: "Error",
        description: "Failed to load model definition.",
        variant: "destructive",
      });
    }
  };

  const handleAddField = () => {
    const newField: SchDicModel = {
      modelId,
      modelType,
      fieldId: `field_${fields.length + 1}`,
      fieldName: `Field ${fields.length + 1}`,
      widgetType: 'text',
      tenantId: '',
      version: 1,
      createdAt: '',
      createdBy: '',
      updatedAt: '',
      updatedBy: '',
    };
    setFields([...fields, newField]);
    setSelectedFieldIndex(fields.length); // Select the new field
  };

  const handleFieldUpdate = (updatedField: SchDicModel) => {
    if (selectedFieldIndex === null) return;
    const newFields = [...fields];
    newFields[selectedFieldIndex] = updatedField;
    setFields(newFields);
  };

  const handleDeleteField = () => {
    if (selectedFieldIndex === null) return;
    const newFields = fields.filter((_, i) => i !== selectedFieldIndex);
    setFields(newFields);
    setSelectedFieldIndex(null);
  };

  const handleSave = async () => {
    if (!modelId) {
      toast({ title: "Error", description: "Model ID is required.", variant: "destructive" });
      return;
    }
    try {
      await modelerApi.saveModel(modelId, modelName, isHidden, modelType, fields);
      toast({
        title: "Saved",
        description: "Model definition saved successfully.",
        variant: "success",
      });
    } catch (error) {
      console.error('Failed to save model:', error);
      toast({
        title: "Error",
        description: "Failed to save model.",
        variant: "destructive",
      });
    }
  };

  return (
    <ModelerLayout
      explorer={<ModelerExplorer />}
      properties={
        <ModelerPropertyPanel
          selectedField={selectedFieldIndex !== null && fields[selectedFieldIndex] ? fields[selectedFieldIndex] : null}
          onFieldUpdate={handleFieldUpdate}
          onDeleteField={handleDeleteField}
        />
      }
    >
      <div className="p-6 max-w-4xl mx-auto space-y-8">
        {/* Header Section */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-foreground">
              {modelName || modelId || 'New Model'}
            </h1>
            <p className="text-muted-foreground text-sm">
              {modelType === 'transaction' ? 'Transaction Entity' : 'Master Entity'}
            </p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={handleSave}
              className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors shadow-sm text-sm font-medium"
            >
              Save Model
            </button>
          </div>
        </div>

        {/* Model Settings */}
        <div className="bg-card border rounded-lg p-4 shadow-sm space-y-4">
          <h2 className="text-sm font-semibold text-foreground uppercase tracking-wider border-b pb-2">
            Model Settings
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-foreground mb-1">Model ID</label>
              <input
                type="text"
                className="w-full p-2 border border-input rounded bg-background text-foreground"
                value={modelId}
                onChange={(e) => setModelId(e.target.value)}
                placeholder="e.g. customer_order"
                disabled={!!queryModelId} // Disable ID edit if loading existing
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-foreground mb-1">Model Name</label>
              <input
                type="text"
                className="w-full p-2 border border-input rounded bg-background text-foreground"
                value={modelName}
                onChange={(e) => setModelName(e.target.value)}
                placeholder="e.g. Customer Order"
              />
            </div>
          </div>
          <div className="flex gap-6">
            <div className="flex items-center gap-4">
              <label className="text-sm font-medium text-foreground">Type:</label>
              <label className="flex items-center cursor-pointer">
                <input
                  type="radio"
                  className="mr-2 text-primary focus:ring-primary"
                  checked={modelType === 'transaction'}
                  onChange={() => setModelType('transaction')}
                />
                <span className="text-sm">Transaction</span>
              </label>
              <label className="flex items-center cursor-pointer">
                <input
                  type="radio"
                  className="mr-2 text-primary focus:ring-primary"
                  checked={modelType === 'master'}
                  onChange={() => setModelType('master')}
                />
                <span className="text-sm">Master</span>
              </label>
            </div>
            <label className="flex items-center cursor-pointer">
              <input
                type="checkbox"
                className="mr-2 rounded border-input text-primary focus:ring-primary"
                checked={isHidden}
                onChange={(e) => setIsHidden(e.target.checked)}
              />
              <span className="text-sm text-foreground">Hidden Model</span>
            </label>
          </div>
        </div>

        {/* Fields List (Canvas) */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-foreground">Fields</h2>
            <button
              onClick={handleAddField}
              className="px-3 py-1.5 text-sm bg-secondary text-secondary-foreground rounded hover:bg-secondary/80 transition-colors"
            >
              + Add Field
            </button>
          </div>

          <div className="bg-card border rounded-lg shadow-sm overflow-hidden">
            <table className="w-full text-sm text-left">
              <thead className="bg-muted/50 text-muted-foreground font-medium border-b">
                <tr>
                  <th className="px-4 py-3 w-10">#</th>
                  <th className="px-4 py-3">Field ID</th>
                  <th className="px-4 py-3">Display Name</th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3 w-20 text-center">Key</th>
                  <th className="px-4 py-3 w-20 text-center">Req</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {fields.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-4 py-8 text-center text-muted-foreground">
                      No fields defined. Click "Add Field" to start.
                    </td>
                  </tr>
                ) : (
                  fields.map((field, index) => (
                    <tr
                      key={index}
                      onClick={() => setSelectedFieldIndex(index)}
                      className={cn(
                        "cursor-pointer hover:bg-accent/50 transition-colors",
                        selectedFieldIndex === index ? "bg-accent text-accent-foreground" : "bg-card"
                      )}
                    >
                      <td className="px-4 py-3 text-muted-foreground">{index + 1}</td>
                      <td className="px-4 py-3 font-medium">{field.fieldId}</td>
                      <td className="px-4 py-3">{field.fieldName}</td>
                      <td className="px-4 py-3">
                        <span className="px-2 py-0.5 rounded-full bg-muted text-xs font-medium">
                          {field.widgetType}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-center">
                        {field.isKey && <span className="text-primary">●</span>}
                      </td>
                      <td className="px-4 py-3 text-center">
                        {field.isRequired && <span className="text-muted-foreground">●</span>}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </ModelerLayout>
  );
};
