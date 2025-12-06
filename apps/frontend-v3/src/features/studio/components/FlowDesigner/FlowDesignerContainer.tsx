import React, { useRef, useCallback } from 'react';
import { ReactFlowProvider, useReactFlow } from 'reactflow';
import { FlowDesigner } from './FlowDesigner';
import { NodePalette } from './NodePalette';
import { nanoid } from 'nanoid';
import { useFlowDesignerStore } from '../../stores/useFlowDesignerStore';

const FlowDesignerContent: React.FC = () => {
  const reactFlowWrapper = useRef<HTMLDivElement>(null);
  const { project } = useReactFlow();
  const { addNode } = useFlowDesignerStore();

  const onDragOver = useCallback((event: React.DragEvent) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  }, []);

  const onDrop = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault();

      const type = event.dataTransfer.getData('application/reactflow');

      // check if the dropped element is valid
      if (typeof type === 'undefined' || !type) {
        return;
      }

      const position = reactFlowWrapper.current?.getBoundingClientRect();
      if (!position) return;

      const { top, left } = position;
      const clientX = event.clientX;
      const clientY = event.clientY;

      // project was renamed to screenToFlowPosition in v11.3
      // fallback to project for older versions or use screenToFlowPosition if available
      const p = project({
        x: clientX - left,
        y: clientY - top,
      });

      const newNode = {
        id: nanoid(),
        type,
        position: p,
        data: { label: `${type} node` },
      };

      addNode(newNode);
    },
    [project, addNode],
  );

  return (
    <div className="flex h-full">
      <NodePalette />
      <div className="flex-1 h-full" ref={reactFlowWrapper} onDrop={onDrop} onDragOver={onDragOver}>
        <FlowDesigner />
      </div>
    </div>
  );
};

export const FlowDesignerContainer: React.FC = () => {
  return (
    <ReactFlowProvider>
      <FlowDesignerContent />
    </ReactFlowProvider>
  );
};
