import React from 'react';
import { useDraggable } from '@dnd-kit/core';
import { Card } from '@mirel/ui';
import type { WidgetType } from '../../stores/useFormDesignerStore';

interface PaletteItemProps {
  type: WidgetType;
  label: string;
}

const PaletteItem: React.FC<PaletteItemProps> = ({ type, label }) => {
  const { attributes, listeners, setNodeRef, transform } = useDraggable({
    id: `palette-${type}`,
    data: {
      type,
      isPaletteItem: true,
    },
  });

  const style = transform ? {
    transform: `translate3d(${transform.x}px, ${transform.y}px, 0)`,
  } : undefined;

  return (
    <div ref={setNodeRef} style={style} {...listeners} {...attributes} className="mb-2 cursor-move">
      <Card className="p-3 text-sm font-medium hover:bg-gray-50 transition-colors">
        {label}
      </Card>
    </div>
  );
};

export const WidgetPalette: React.FC = () => {
  const widgets: { type: WidgetType; label: string }[] = [
    { type: 'text', label: 'Text Input' },
    { type: 'number', label: 'Number Input' },
    { type: 'date', label: 'Date Picker' },
    { type: 'boolean', label: 'Checkbox' },
    { type: 'select', label: 'Select Box' },
  ];

  return (
    <div className="w-64 border-r bg-white flex flex-col h-full">
      <div className="p-4 border-b">
        <h3 className="font-semibold text-gray-900">Widgets</h3>
        <p className="text-xs text-gray-500 mt-1">Drag to add to canvas</p>
      </div>
      <div className="p-4 flex-1 overflow-y-auto">
        {widgets.map((w) => (
          <PaletteItem key={w.type} type={w.type} label={w.label} />
        ))}
      </div>
    </div>
  );
};
