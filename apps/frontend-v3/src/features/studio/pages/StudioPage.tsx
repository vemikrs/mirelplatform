import React, { useState } from 'react';
import { FormDesigner } from '../components/FormDesigner/FormDesigner';
import { DynamicFormRenderer } from '../components/Runtime/DynamicFormRenderer';
import { useFormDesignerStore } from '../stores/useFormDesignerStore';
import { Button, Card } from '@mirel/ui';
import { Eye, Edit, Save } from 'lucide-react';
import { createDraft, updateDraft } from '@/lib/api/schema';

export const StudioPage: React.FC = () => {
  const [mode, setMode] = useState<'edit' | 'preview'>('edit');
  const { widgets, modelId, modelName, setModelInfo } = useFormDesignerStore();

  const handleSave = async () => {
    try {
      const fields = widgets.map((w, index) => ({
        fieldId: w.id,
        fieldName: w.label,
        fieldType: w.type.toUpperCase(),
        isRequired: w.required,
        sortOrder: index,
        layout: JSON.stringify({ x: w.x, y: w.y, w: w.w, h: w.h }),
      }));

      if (modelId) {
        const res = await updateDraft(modelId, {
          name: modelName,
          fields,
        });
        if (res.errors && res.errors.length > 0) {
            alert(`Failed to save: ${res.errors.join(', ')}`);
        } else {
            alert('Saved successfully!');
        }
      } else {
        const name = window.prompt('Enter form name', modelName);
        if (!name) return;
        
        const res = await createDraft({
          name,
        });
        
        if (res.data) {
            setModelInfo(res.data, name);
            // After creating, we should also update with fields because createDraft only takes name/desc
            await updateDraft(res.data, {
                name,
                fields
            });
            alert('Created and saved successfully!');
        } else {
             alert(`Failed to create: ${res.errors?.join(', ') || 'Unknown error'}`);
        }
      }
    } catch (e) {
      console.error(e);
      alert('An error occurred while saving.');
    }
  };

  return (
    <div className="h-[calc(100vh-4rem)] flex flex-col">
      {/* Toolbar */}
      <div className="h-14 border-b bg-white flex items-center justify-between px-4">
        <div className="flex items-center gap-2">
          <h1 className="font-semibold text-lg">Form Designer</h1>
          <span className="text-xs bg-gray-100 px-2 py-1 rounded text-gray-500">v1.0</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="bg-gray-100 p-1 rounded-lg flex gap-1">
            <Button 
              variant={mode === 'edit' ? 'secondary' : 'ghost'} 
              size="sm"
              onClick={() => setMode('edit')}
              className="gap-2"
            >
              <Edit className="size-4" />
              Edit
            </Button>
            <Button 
              variant={mode === 'preview' ? 'secondary' : 'ghost'} 
              size="sm"
              onClick={() => setMode('preview')}
              className="gap-2"
            >
              <Eye className="size-4" />
              Preview
            </Button>
          </div>
          <div className="w-px h-6 bg-gray-200 mx-2" />
          <Button size="sm" onClick={handleSave} className="gap-2">
            <Save className="size-4" />
            Save
          </Button>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-hidden">
        {mode === 'edit' ? (
          <FormDesigner />
        ) : (
          <div className="h-full overflow-auto p-8 bg-gray-50 flex justify-center">
            <Card className="w-full max-w-2xl p-8 bg-white shadow-sm h-fit">
              <h2 className="text-xl font-bold mb-6">Preview Form</h2>
              <DynamicFormRenderer 
                widgets={widgets} 
                onSubmit={(data) => alert(JSON.stringify(data, null, 2))} 
              />
            </Card>
          </div>
        )}
      </div>
    </div>
  );
};

