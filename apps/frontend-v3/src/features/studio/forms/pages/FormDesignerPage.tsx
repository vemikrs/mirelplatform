import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { FormDesigner } from '../../components/FormDesigner/FormDesigner';
import { DynamicFormRenderer } from '../../components/Runtime/DynamicFormRenderer';
import { useFormDesignerStore, type Widget } from '../../stores/useFormDesignerStore';
import { Button, Card, toast } from '@mirel/ui';
import { Eye, Edit, Workflow, Database, Rocket } from 'lucide-react';
import { createDraft, updateDraft, getSchema } from '@/lib/api/schema';
import { FlowDesignerContainer } from '../../components/FlowDesigner/FlowDesignerContainer';
import { useFlowDesignerStore } from '../../stores/useFlowDesignerStore';
import { StudioLayout } from '../../layouts';
import { StudioContextBar, ModeSwitcher } from '../../components';

export const FormDesignerPage: React.FC = () => {
  const { formId } = useParams<{ formId: string }>();
  // Handle 'new' as a special ID indicating creation mode, or undefined if previously route based
  const paramModelId = formId === 'new' ? undefined : formId;
  
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialMode = searchParams.get('mode') as 'edit' | 'preview' | 'flow' | null;
  const [mode, setMode] = useState<'edit' | 'preview' | 'flow'>(
    (initialMode && ['edit', 'preview', 'flow'].includes(initialMode)) ? initialMode : 'edit'
  );
  const { widgets, modelId, modelName, setModelInfo, setWidgets } = useFormDesignerStore();
  const { loadFlow, saveFlow } = useFlowDesignerStore();

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
      
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const loadedWidgets: Widget[] = data.fields.map((field: any) => {
        let layout = { x: 0, y: 0, w: 4, h: 2 };
        try {
          if (field.layout) {
            layout = JSON.parse(field.layout);
          }
        } catch {
          console.warn('Failed to parse layout for field', field.fieldId);
        }

        return {
          id: field.fieldId,
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          type: field.fieldType.toLowerCase() as any, // TODO: strict type mapping
          label: field.fieldName,
          fieldCode: field.fieldCode,
          required: field.isRequired,
          validationRegex: field.validationRegex,
          minValue: field.minValue,
          maxValue: field.maxValue,
          minLength: field.minLength,
          maxLength: field.maxLength,
          options: field.options ? JSON.parse(field.options) : undefined,
          ...layout,
        };
      });
      setWidgets(loadedWidgets);
      
      // Load flow if in flow mode
      if (mode === 'flow') {
        loadFlow(data.modelId);
      }
    } else if (!paramModelId) {
      // Reset for new form
      setModelInfo(null, 'Untitled Form');
      setWidgets([]);
    }
  }, [paramModelId, schema, setModelInfo, setWidgets, mode, loadFlow]);

  const handleSave = async () => {
    try {
      const fields = widgets.map((w, index) => ({
        fieldId: w.id,
        fieldName: w.label,
        fieldCode: w.fieldCode,
        fieldType: w.type.toUpperCase(),
        isRequired: w.required,
        validationRegex: w.validationRegex,
        minValue: w.minValue,
        maxValue: w.maxValue,
        minLength: w.minLength,
        maxLength: w.maxLength,
        options: w.options ? JSON.stringify(w.options) : undefined,
        sortOrder: index,
        layout: JSON.stringify({ x: w.x, y: w.y, w: w.w, h: w.h }),
      }));

      if (modelId) {
        // Save Form
        await updateDraft(modelId, {
          name: modelName,
          fields,
        });
        
        // Save Flow
        await saveFlow(modelId, modelName);
        
        toast({
          title: 'Success',
          description: 'Form and Flow saved successfully',
          variant: 'success',
        });
      } else {
        const name = prompt('Enter form name', modelName);
        if (!name) return;
        const result = await createDraft({
          name,
        });
        if (result.data) {
           setModelInfo(result.data, name);
           // Update with fields
           await updateDraft(result.data, {
             name,
             fields,
           });
           
           // Create initial Flow
           await saveFlow(result.data, name);
           
           navigate(`/apps/studio/forms/${result.data}`);
           toast({
             title: 'Success',
             description: 'Form created successfully',
             variant: 'success',
           });
        }
      }
    } catch (error) {
      console.error('Failed to save', error);
      toast({
        title: 'Error',
        description: 'Failed to save form',
        variant: 'destructive',
      });
    }
  };

  if (isLoading) {
    return (
      <StudioLayout hideProperties={true} hideContextBar={true}>
        <div className="h-full flex items-center justify-center">Loading...</div>
      </StudioLayout>
    );
  }

  // Mode definitions for the switcher
  const modes = [
    { id: 'edit', label: 'Form', icon: Edit },
    { id: 'flow', label: 'Flow', icon: Workflow },
    { id: 'preview', label: 'Preview', icon: Eye },
  ];

  // Quick action buttons
  const quickActions = (
    <>
      <ModeSwitcher
        modes={modes}
        activeMode={mode}
        onModeChange={(m) => setMode(m as 'edit' | 'flow' | 'preview')}
      />
      <div className="w-px h-6 bg-border mx-2" />
      <Button 
        size="sm" 
        variant="ghost" 
        onClick={() => navigate(`/apps/studio/${modelId}/data`)} 
        disabled={!modelId}
        className="gap-1.5"
      >
        <Database className="size-4" />
        <span className="hidden md:inline">Data</span>
      </Button>
      <Button 
        size="sm" 
        variant="ghost" 
        onClick={() => navigate(`/apps/studio/${modelId}/releases`)} 
        disabled={!modelId}
        className="gap-1.5"
      >
        <Rocket className="size-4" />
        <span className="hidden md:inline">Releases</span>
      </Button>
    </>
  );

  return (
    <StudioLayout hideProperties={mode !== 'edit'} hideContextBar={true}>
      <div className="flex flex-col h-full">
        {/* Context Bar with mode switcher */}
        <StudioContextBar
          title={modelName}
          subtitle={modelId ? `v${schema?.data?.version || 1}` : 'New'}
          breadcrumbs={[
            { label: 'Studio', href: '/apps/studio' },
            { label: 'Forms', href: '/apps/studio/forms' },
            { label: modelName, href: `/apps/studio/forms/${modelId || 'new'}` },
          ]}
          actions={quickActions}
          onSave={handleSave}
          showSave={true}
          showPreview={false}
        />

        {/* Content */}
        <div className="flex-1 overflow-hidden">
          {mode === 'edit' && <FormDesigner />}
          {mode === 'flow' && <FlowDesignerContainer />}
          {mode === 'preview' && (
            <div className="h-full overflow-auto p-8 bg-muted/30 flex justify-center">
              <Card className="w-full max-w-2xl p-8 bg-card shadow-sm h-fit">
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
    </StudioLayout>
  );
};
