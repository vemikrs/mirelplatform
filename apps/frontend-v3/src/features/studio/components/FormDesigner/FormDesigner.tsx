import React from 'react';
import { useFormDesignerStore } from '../../stores/useFormDesignerStore';
import type { Widget, WidgetType } from '../../stores/useFormDesignerStore';
import { DndContext, type DragEndEvent, useSensor, useSensors, PointerSensor, DragOverlay, defaultDropAnimationSideEffects, type DragStartEvent } from '@dnd-kit/core';
import { WidgetPalette } from './WidgetPalette';
import { PropertyEditor } from './PropertyEditor';
import { GridFormDesigner } from './GridFormDesigner';
import { nanoid } from 'nanoid';
import { Card } from '@mirel/ui';

export const FormDesigner: React.FC = () => {
  const { widgets, addWidget, setWidgets, selectWidget, selectedWidgetId } = useFormDesignerStore();
  const [activeDragItem, setActiveDragItem] = React.useState<{ type: WidgetType; label: string } | null>(null);
  
  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    })
  );

  const handleDragStart = (event: DragStartEvent) => {
    const { active } = event;
    if (active.data.current?.isPaletteItem) {
        setActiveDragItem({
            type: active.data.current.type,
            label: active.data.current.type // Simplified label for overlay
        });
    }
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    setActiveDragItem(null);

    if (!over) return;

    // Handle drop from palette
    if (active.data.current?.isPaletteItem) {
        const type = active.data.current.type as WidgetType;
        const newWidget: Widget = {
            id: nanoid(),
            type,
            label: `New ${type}`,
            fieldCode: `fld_${nanoid(8)}`,
            required: false,
            x: 0, 
            y: 0,
            w: 1,
            h: 1
        };
        addWidget(newWidget);
        return;
    }

    // Handle reordering - NOT SUPPORTED IN GRID MODE YET via dnd-kit
    // if (active.id !== over.id) {
    //   const oldIndex = widgets.findIndex((w) => w.id === active.id);
    //   const newIndex = widgets.findIndex((w) => w.id === over.id);
    //   setWidgets(arrayMove(widgets, oldIndex, newIndex));
    // }
  };

  const dropAnimation = {
    sideEffects: defaultDropAnimationSideEffects({
      styles: {
        active: {
          opacity: '0.5',
        },
      },
    }),
  };

  return (
    <div className="flex h-full bg-gray-100">
      <DndContext 
        sensors={sensors} 
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
      >
        {/* Palette */}
        <WidgetPalette />

        {/* Canvas */}
        <div className="flex-1 p-8 overflow-auto flex justify-center">
          <div 
            className="bg-white min-h-[800px] w-full max-w-4xl shadow-sm p-8 relative rounded-lg"
            onClick={() => selectWidget(null)}
          >
             <GridFormDesigner />
          </div>
        </div>

        {/* Property Editor */}
        <PropertyEditor />

        <DragOverlay dropAnimation={dropAnimation}>
            {activeDragItem ? (
                 <Card className="p-3 text-sm font-medium bg-white shadow-lg opacity-80 w-48">
                    {activeDragItem.label}
                 </Card>
            ) : null}
        </DragOverlay>
      </DndContext>
    </div>
  );
};

