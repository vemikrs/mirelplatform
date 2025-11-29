import React from 'react';
import type { SchDicModel } from '../types/schema';

interface ModelSelectorProps {
  models: { value: string; text: string }[];
  selectedModelId: string;
  onSelect: (modelId: string) => void;
}

export const ModelSelector: React.FC<ModelSelectorProps> = ({ models, selectedModelId, onSelect }) => {
  return (
    <select
      className="p-2 border rounded"
      value={selectedModelId}
      onChange={(e) => onSelect(e.target.value)}
    >
      <option value="">Select Model...</option>
      {models.map((model) => (
        <option key={model.value} value={model.value}>
          {model.text}
        </option>
      ))}
    </select>
  );
};
