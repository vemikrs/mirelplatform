import React from 'react';
import { useFormDesignerStore } from '../../stores/useFormDesignerStore';
import { Input, Button } from '@mirel/ui';

export const PropertyEditor: React.FC = () => {
  const { widgets, selectedWidgetId, updateWidget, removeWidget } = useFormDesignerStore();
  
  const selectedWidget = widgets.find(w => w.id === selectedWidgetId);

  if (!selectedWidget) {
    return (
      <div className="w-80 border-l bg-background flex flex-col h-full p-6 items-center justify-center text-muted-foreground text-center">
        <p>Select a widget to edit its properties</p>
      </div>
    );
  }

  return (
    <div className="w-80 border-l bg-background flex flex-col h-full">
      <div className="p-4 border-b">
        <h3 className="font-semibold text-foreground">Properties</h3>
        <p className="text-xs text-muted-foreground mt-1">ID: {selectedWidget.id}</p>
      </div>
      
      <div className="p-4 flex-1 overflow-y-auto space-y-6">
        <div className="space-y-2">
          <label className="text-sm font-medium">Label</label>
          <Input 
            value={selectedWidget.label} 
            onChange={(e) => updateWidget(selectedWidget.id, { label: e.target.value })}
          />
        </div>

        <div className="space-y-2">
          <label className="text-sm font-medium">Field Code</label>
          <Input 
            value={selectedWidget.fieldCode || ''} 
            onChange={(e) => updateWidget(selectedWidget.id, { fieldCode: e.target.value })}
          />
          <p className="text-xs text-muted-foreground">Database column name (alphanumeric)</p>
        </div>

        <div className="flex items-center gap-2">
            <label className="text-sm font-medium">Type</label>
             <div className="p-2 bg-muted rounded text-sm text-muted-foreground">
                {selectedWidget.type}
             </div>
        </div>

        <div className="flex items-center gap-2">
          <input 
            type="checkbox" 
            id="required"
            checked={selectedWidget.required}
            onChange={(e) => updateWidget(selectedWidget.id, { required: e.target.checked })}
            className="h-4 w-4 rounded border-border text-primary focus:ring-primary"
          />
          <label htmlFor="required" className="text-sm font-medium text-foreground">Required</label>
        </div>

        <div className="border-t pt-4 space-y-4">
          <h4 className="text-sm font-semibold text-foreground">Validation</h4>
          
          {(selectedWidget.type === 'text' || selectedWidget.type === 'select') && (
            <>
              <div className="space-y-2">
                <label className="text-sm font-medium">Regex Pattern</label>
                <Input 
                  value={selectedWidget.validationRegex || ''} 
                  onChange={(e) => updateWidget(selectedWidget.id, { validationRegex: e.target.value })}
                  placeholder="e.g. ^[A-Z]+$"
                />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Min Length</label>
                  <Input 
                    type="number"
                    value={selectedWidget.minLength || ''} 
                    onChange={(e) => updateWidget(selectedWidget.id, { minLength: e.target.value ? Number(e.target.value) : undefined })}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Max Length</label>
                  <Input 
                    type="number"
                    value={selectedWidget.maxLength || ''} 
                    onChange={(e) => updateWidget(selectedWidget.id, { maxLength: e.target.value ? Number(e.target.value) : undefined })}
                  />
                </div>
              </div>
            </>
          )}

          {selectedWidget.type === 'number' && (
             <div className="grid grid-cols-2 gap-2">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Min Value</label>
                  <Input 
                    type="number"
                    value={selectedWidget.minValue || ''} 
                    onChange={(e) => updateWidget(selectedWidget.id, { minValue: e.target.value ? Number(e.target.value) : undefined })}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Max Value</label>
                  <Input 
                    type="number"
                    value={selectedWidget.maxValue || ''} 
                    onChange={(e) => updateWidget(selectedWidget.id, { maxValue: e.target.value ? Number(e.target.value) : undefined })}
                  />
                </div>
              </div>
          )}

          {(selectedWidget.type === 'select' || selectedWidget.type === 'radio') && (
            <div className="space-y-2">
              <label className="text-sm font-medium">Options</label>
              <div className="space-y-2">
                {(selectedWidget.options || []).map((option, index) => (
                  <div key={index} className="flex gap-2">
                    <Input 
                      placeholder="Label" 
                      value={option.label} 
                      onChange={(e) => {
                        const newOptions = [...(selectedWidget.options || [])];
                        if (newOptions[index]) {
                            newOptions[index] = { ...newOptions[index], label: e.target.value };
                            updateWidget(selectedWidget.id, { options: newOptions });
                        }
                      }}
                    />
                    <Input 
                      placeholder="Value" 
                      value={option.value} 
                      onChange={(e) => {
                        const newOptions = [...(selectedWidget.options || [])];
                        if (newOptions[index]) {
                            newOptions[index] = { ...newOptions[index], value: e.target.value };
                            updateWidget(selectedWidget.id, { options: newOptions });
                        }
                      }}
                    />
                    <Button 
                      variant="ghost" 
                      size="icon"
                      onClick={() => {
                        const newOptions = [...(selectedWidget.options || [])];
                        newOptions.splice(index, 1);
                        updateWidget(selectedWidget.id, { options: newOptions });
                      }}
                    >
                      ×
                    </Button>
                  </div>
                ))}
                <Button 
                  variant="outline" 
                  size="sm" 
                  className="w-full"
                  onClick={() => {
                    const newOptions = [...(selectedWidget.options || []), { label: 'New Option', value: 'new_option' }];
                    updateWidget(selectedWidget.id, { options: newOptions });
                  }}
                >
                  Add Option
                </Button>
              </div>
            </div>
          )}
        </div>

        <div className="pt-4 border-t">
            <Button 
                variant="destructive" 
                className="w-full"
                onClick={() => removeWidget(selectedWidget.id)}
            >
                Delete Widget
            </Button>
        </div>
      </div>
    </div>
  );
};
