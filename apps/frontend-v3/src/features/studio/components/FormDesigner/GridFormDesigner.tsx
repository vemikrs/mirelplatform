import React from 'react';
import GridLayout from 'react-grid-layout';
import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';
import { useFormDesignerStore } from '../../stores/useFormDesignerStore';
import { Card } from '@mirel/ui';

export const GridFormDesigner: React.FC = () => {
  const { widgets, setWidgets } = useFormDesignerStore();

  const layout = widgets.map((w) => ({
    i: w.id,
    x: w.x,
    y: w.y,
    w: w.w,
    h: w.h,
  }));

  const onLayoutChange = (newLayout: GridLayout.Layout[]) => {
    const updatedWidgets = widgets.map((w) => {
      const l = newLayout.find((nl) => nl.i === w.id);
      if (l) {
        return {
          ...w,
          x: l.x,
          y: l.y,
          w: l.w,
          h: l.h,
        };
      }
      return w;
    });
    setWidgets(updatedWidgets);
  };

  return (
    <div className="flex-1 p-8 overflow-auto bg-gray-100">
      <div className="bg-white min-h-[800px] w-full max-w-4xl shadow-sm p-8 relative rounded-lg mx-auto">
        <GridLayout
          className="layout"
          layout={layout}
          cols={12}
          rowHeight={30}
          width={800}
          onLayoutChange={onLayoutChange}
          draggableHandle=".drag-handle"
        >
          {widgets.map((widget) => (
            <div key={widget.id} className="border border-gray-200 bg-white rounded shadow-sm group relative">
               {/* Drag Handle */}
              <div className="drag-handle absolute top-0 left-0 right-0 h-6 bg-gray-50 cursor-move flex items-center px-2 border-b border-gray-100 opacity-0 group-hover:opacity-100 transition-opacity z-10">
                 <span className="text-xs text-gray-400">Drag</span>
              </div>
              
              <div className="p-4 h-full flex flex-col pt-8">
                <label className="text-sm font-medium text-gray-700 mb-1 block">
                  {widget.label} {widget.required && <span className="text-red-500">*</span>}
                </label>
                <div className="flex-1 bg-gray-50 rounded border border-dashed border-gray-200 flex items-center justify-center text-xs text-gray-400">
                  {widget.type}
                </div>
              </div>
            </div>
          ))}
        </GridLayout>
      </div>
    </div>
  );
};
