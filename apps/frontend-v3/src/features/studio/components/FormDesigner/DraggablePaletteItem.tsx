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
      className="p-3 mb-2 border border-gray-200 rounded bg-white shadow-sm cursor-move hover:border-blue-400 hover:shadow-md transition-all"
    >
      <div className="text-sm font-medium text-gray-700">{label}</div>
      <div className="text-xs text-gray-400 mt-1 capitalize">{type}</div>
    </div>
  );
};
