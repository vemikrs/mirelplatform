import React from 'react';
import { useFormDesignerStore } from '../../stores/useFormDesignerStore';
import { Input, Button } from '@mirel/ui';

export const PropertyEditor: React.FC = () => {
  const { widgets, selectedWidgetId, updateWidget, removeWidget } = useFormDesignerStore();
  
  const selectedWidget = widgets.find(w => w.id === selectedWidgetId);

  if (!selectedWidget) {
    return (
      <div className="w-80 border-l bg-white flex flex-col h-full p-6 items-center justify-center text-gray-400 text-center">
        <p>Select a widget to edit its properties</p>
      </div>
    );
  }

  return (
    <div className="w-80 border-l bg-white flex flex-col h-full">
      <div className="p-4 border-b">
        <h3 className="font-semibold text-gray-900">Properties</h3>
        <p className="text-xs text-gray-500 mt-1">ID: {selectedWidget.id}</p>
      </div>
      
      <div className="p-4 flex-1 overflow-y-auto space-y-6">
        <div className="space-y-2">
          <label className="text-sm font-medium text-gray-700">Label</label>
          <Input 
            value={selectedWidget.label} 
            onChange={(e) => updateWidget(selectedWidget.id, { label: e.target.value })}
          />
        </div>

        <div className="space-y-2">
            <label className="text-sm font-medium text-gray-700">Type</label>
             <div className="p-2 bg-gray-100 rounded text-sm text-gray-600">
                {selectedWidget.type}
             </div>
        </div>

        <div className="flex items-center space-x-2">
            <input 
                type="checkbox" 
                id="required"
                checked={selectedWidget.required}
                onChange={(e) => updateWidget(selectedWidget.id, { required: e.target.checked })}
                className="h-4 w-4 text-blue-600 rounded border-gray-300 focus:ring-blue-500"
            />
            <label htmlFor="required" className="text-sm font-medium text-gray-700">Required</label>
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
