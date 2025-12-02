import React, { useState } from 'react';
import { schemaApi } from '../api/schemaApi';
import type { SchDicModel } from '../types/schema';
import { FieldEditor } from '../components/FieldEditor';
import { SchemaLayout } from '../components/layout/SchemaLayout';

import { useToast } from '@mirel/ui';

export const SchemaModelDefinePage: React.FC = () => {
  const { toast } = useToast();
  const [modelId, setModelId] = useState('');
  const [modelName, setModelName] = useState('');
  const [modelType, setModelType] = useState<'transaction' | 'master'>('master');
  const [isHidden, setIsHidden] = useState(false);
  const [fields, setFields] = useState<SchDicModel[]>([]);

  const handleAddField = () => {
    setFields([
      ...fields,
      {
        modelId,
        modelType,
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
      await schemaApi.saveSchema(modelId, modelName, isHidden, modelType, fields);
      toast({
        title: "保存しました",
        description: "モデル定義を保存しました。",
        variant: "success",
      });
    } catch (error) {
      console.error('Failed to save model:', error);
      toast({
        title: "エラー",
        description: "モデルの保存に失敗しました。",
        variant: "destructive",
      });
    }
  };

  const handleLoad = async () => {
    if (!modelId) return;
    try {
      const response = await schemaApi.listSchema(modelId);
      setFields(response.data.schemas);
      // Note: Header info loading not implemented in listSchema response yet, assuming fields only
      // TODO: Load modelType and other metadata from response once available
    } catch (error) {
      console.error('Failed to load model:', error);
    }
  };

  return (
    <SchemaLayout>
      <div className="p-6 space-y-6">
        <div className="flex items-center gap-4 mb-6">
          <h1 className="text-2xl font-bold text-foreground">モデル定義</h1>
        </div>
        
        <div className="p-6 border border-border rounded-lg bg-card shadow-sm">
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label htmlFor="modelId" className="block text-sm font-medium text-foreground mb-1">モデルID</label>
              <div className="flex">
                <input
                  id="modelId"
                  type="text"
                  className="flex-1 p-2 border border-input rounded-l bg-background text-foreground focus:ring-2 focus:ring-ring focus:border-input"
                  value={modelId}
                  onChange={(e) => setModelId(e.target.value)}
                  placeholder="例: customer"
                />
                <button
                  onClick={handleLoad}
                  className="px-4 py-2 bg-secondary text-secondary-foreground border border-l-0 border-input rounded-r hover:bg-secondary/80 transition-colors"
                >
                  読込
                </button>
              </div>
            </div>
            <div>
              <label htmlFor="modelName" className="block text-sm font-medium text-foreground mb-1">モデル名</label>
              <input
                id="modelName"
                type="text"
                className="w-full p-2 border border-input rounded bg-background text-foreground focus:ring-2 focus:ring-ring focus:border-input"
                value={modelName}
                onChange={(e) => setModelName(e.target.value)}
                placeholder="例: 顧客情報"
              />
            </div>
          </div>
          <div className="mb-4">
            <label className="block text-sm font-medium text-foreground mb-1">モデルタイプ</label>
            <div className="flex gap-4">
              <label className="flex items-center">
                <input
                  type="radio"
                  className="mr-2"
                  checked={modelType === 'transaction'}
                  onChange={() => setModelType('transaction')}
                />
                アプリケーション (Transaction)
              </label>
              <label className="flex items-center">
                <input
                  type="radio"
                  className="mr-2"
                  checked={modelType === 'master'}
                  onChange={() => setModelType('master')}
                />
                マスタ (Master)
              </label>
            </div>
          </div>
          <div className="mb-4">
            <label className="flex items-center text-foreground">
              <input
                type="checkbox"
                className="mr-2 rounded border-input bg-background text-primary focus:ring-ring"
                checked={isHidden}
                onChange={(e) => setIsHidden(e.target.checked)}
              />
              非表示モデル（一覧に表示しない）
            </label>
          </div>
        </div>

        <div className="space-y-4">
          <h2 className="text-xl font-semibold text-foreground">フィールド定義</h2>
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
            className="w-full py-3 border-2 border-dashed border-muted-foreground/25 rounded-lg text-muted-foreground hover:border-muted-foreground/50 hover:text-foreground transition-colors flex items-center justify-center gap-2"
          >
            <span>+ フィールドを追加</span>
          </button>
        </div>

        <div className="flex justify-end pt-4">
          <button
            onClick={handleSave}
            className="px-6 py-2 bg-primary text-primary-foreground rounded hover:bg-primary/90 transition-colors shadow-sm"
          >
            モデルを保存
          </button>
        </div>
      </div>
    </SchemaLayout>
  );
};
