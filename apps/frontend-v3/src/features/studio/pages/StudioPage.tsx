import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { FormDesigner } from '../components/FormDesigner/FormDesigner';
import { DynamicFormRenderer } from '../components/Runtime/DynamicFormRenderer';
import { useFormDesignerStore, type Widget } from '../stores/useFormDesignerStore';
import { Button, Card } from '@mirel/ui';
import { Eye, Edit, Save, ArrowLeft } from 'lucide-react';
import { createDraft, updateDraft, getSchema } from '@/lib/api/schema';
import { useNavigate } from 'react-router-dom';

export const StudioPage: React.FC = () => {
  const { modelId: paramModelId } = useParams<{ modelId: string }>();
  const navigate = useNavigate();
  const [mode, setMode] = useState<'edit' | 'preview'>('edit');
  const { widgets, modelId, modelName, setModelInfo, setWidgets } = useFormDesignerStore();

  // Load schema if modelId is present
  const { data: schema, isLoading } = useQuery({
    queryKey: ['studio-schema', paramModelId],
    queryFn: () => paramModelId ? getSchema(paramModelId) : Promise.resolve(null),
    enabled: !!paramModelId,
  });

  useEffect(() => {
    if (paramModelId && schema?.data) {
      const data = schema.data;
      setModelInfo(data.modelId, data.modelName);
      
      const loadedWidgets: Widget[] = data.fields.map((field: any) => {
        let layout = { x: 0, y: 0, w: 4, h: 2 };
        try {
          if (field.layout) {
            layout = JSON.parse(field.layout);
          }
        } catch (e) {
          console.warn('Failed to parse layout for field', field.fieldId);
        }

        return {
          id: field.fieldId,
          type: field.fieldType.toLowerCase() as any, // TODO: strict type mapping
          label: field.fieldName,
          required: field.isRequired,
          ...layout,
        };
      });
      setWidgets(loadedWidgets);
    } else if (!paramModelId) {
      // Reset for new form
      setModelInfo(null, 'Untitled Form');
      setWidgets([]);
    }
  }, [paramModelId, schema, setModelInfo, setWidgets]);

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
            navigate(`/apps/studio/${res.data}`);
        } else {
             alert(`Failed to create: ${res.errors?.join(', ') || 'Unknown error'}`);
        }
      }
    } catch (e) {
      console.error(e);
      alert('An error occurred while saving.');
    }
  };

  if (isLoading) {
    return <div className="h-full flex items-center justify-center">Loading...</div>;
  }

  return (
    <div className="h-[calc(100vh-4rem)] flex flex-col">
      {/* Toolbar */}
      <div className="h-14 border-b bg-white flex items-center justify-between px-4">
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="icon" onClick={() => navigate('/apps/studio')}>
            <ArrowLeft className="size-4" />
          </Button>
          <h1 className="font-semibold text-lg">{modelName}</h1>
          <span className="text-xs bg-gray-100 px-2 py-1 rounded text-gray-500">
            {modelId ? 'v' + (schema?.data?.version || 1) : 'New'}
          </span>
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

