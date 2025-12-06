import React from 'react';
import ReactFlow, {
  MiniMap,
  Controls,
  Background,
} from 'reactflow';
import 'reactflow/dist/style.css';
import { useFlowDesignerStore } from '../../stores/useFlowDesignerStore';

export const FlowDesigner: React.FC = () => {
  const { nodes, edges, onNodesChange, onEdgesChange, onConnect } = useFlowDesignerStore();

  return (
    <div className="flex-1 h-full w-full">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
      >
        <Controls />
        <MiniMap />
        <Background gap={12} size={1} />
      </ReactFlow>
    </div>
  );
};
