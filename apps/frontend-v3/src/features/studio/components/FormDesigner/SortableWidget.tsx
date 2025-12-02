import React from 'react';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import type { Widget } from '../../stores/useFormDesignerStore';

interface SortableWidgetProps {
  widget: Widget;
  isSelected: boolean;
  onClick: () => void;
}

export const SortableWidget: React.FC<SortableWidgetProps> = ({ widget, isSelected, onClick }) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
  } = useSortable({ id: widget.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
      onClick={(e) => {
        e.stopPropagation();
        onClick();
      }}
      className={`
        p-4 mb-2 border rounded cursor-move bg-white
        ${isSelected ? 'border-blue-500 ring-2 ring-blue-200' : 'border-gray-200 hover:border-gray-300'}
      `}
    >
      <div className="flex justify-between items-center">
        <span className="font-medium">{widget.label}</span>
        <span className="text-xs text-gray-400 uppercase">{widget.type}</span>
      </div>
      {widget.required && <span className="text-xs text-red-500 mt-1 block">Required</span>}
    </div>
  );
};
