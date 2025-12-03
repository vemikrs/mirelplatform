import React from 'react';
import { useDraggable } from '@dnd-kit/core';
import type { WidgetType } from '../../stores/useFormDesignerStore';

interface DraggablePaletteItemProps {
  type: WidgetType;
  label: string;
}

export const DraggablePaletteItem: React.FC<DraggablePaletteItemProps> = ({ type, label }) => {
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
    <div
      ref={setNodeRef}
      style={style}
      {...listeners}
      {...attributes}
      className="p-3 mb-2 border border-border rounded bg-card shadow-sm cursor-move hover:border-primary hover:shadow-md transition-all"
    >
      <div className="text-sm font-medium text-foreground">{label}</div>
      <div className="text-xs text-muted-foreground mt-1 capitalize">{type}</div>
    </div>
  );
};
