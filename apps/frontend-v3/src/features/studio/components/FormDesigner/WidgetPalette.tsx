import React from 'react';
import { useDraggable } from '@dnd-kit/core';
import { Card } from '@mirel/ui';
import { Type, Hash, Calendar, CheckSquare, List, AlignLeft, CircleDot, Clock } from 'lucide-react';
import type { WidgetType } from '../../stores/useFormDesignerStore';

interface PaletteItemProps {
  type: WidgetType;
  label: string;
  icon: React.ReactNode;
}

const PaletteItem: React.FC<PaletteItemProps> = ({ type, label, icon }) => {
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
      <Card className="p-3 text-sm font-medium hover:bg-muted transition-colors flex items-center gap-2">
        {icon}
        {label}
      </Card>
    </div>
  );
};

export const WidgetPalette: React.FC = () => {
  return (
    <div className="w-64 border-r bg-background flex flex-col h-full">
      <div className="p-4 border-b">
        <h3 className="font-semibold text-foreground">Widgets</h3>
        <p className="text-xs text-muted-foreground mt-1">Drag to add to canvas</p>
      </div>
      <div className="p-4 flex-1 overflow-y-auto">
        <PaletteItem type="text" label="Text Input" icon={<Type className="size-4" />} />
        <PaletteItem type="textarea" label="Text Area" icon={<AlignLeft className="size-4" />} />
        <PaletteItem type="number" label="Number Input" icon={<Hash className="size-4" />} />
        <PaletteItem type="date" label="Date Picker" icon={<Calendar className="size-4" />} />
        <PaletteItem type="time" label="Time Picker" icon={<Clock className="size-4" />} />
        <PaletteItem type="datetime" label="Date & Time" icon={<Calendar className="size-4" />} />
        <PaletteItem type="boolean" label="Checkbox" icon={<CheckSquare className="size-4" />} />
        <PaletteItem type="select" label="Dropdown" icon={<List className="size-4" />} />
        <PaletteItem type="radio" label="Radio Group" icon={<CircleDot className="size-4" />} />
      </div>
    </div>
  );
};
