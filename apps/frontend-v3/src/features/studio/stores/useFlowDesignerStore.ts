import { create } from 'zustand';
import {
  addEdge,
  applyNodeChanges,
  applyEdgeChanges,
  type Connection,
  type Edge,
  type EdgeChange,
  type Node,
  type NodeChange,
  type OnNodesChange,
  type OnEdgesChange,
  type OnConnect,
} from 'reactflow';
import { createFlow, updateFlow, getFlows, type Flow } from '../../../lib/api/flow';

interface FlowDesignerState {
  flowId: string | null;
  nodes: Node[];
  edges: Edge[];
  onNodesChange: OnNodesChange;
  onEdgesChange: OnEdgesChange;
  onConnect: OnConnect;
  addNode: (node: Node) => void;
  setNodes: (nodes: Node[]) => void;
  setEdges: (edges: Edge[]) => void;
  
  // API Actions
  loadFlow: (modelId: string) => Promise<void>;
  saveFlow: (modelId: string, name: string) => Promise<void>;
}

const initialNodes: Node[] = [
  { id: '1', position: { x: 0, y: 0 }, data: { label: 'Start' }, type: 'input' },
  { id: '2', position: { x: 0, y: 100 }, data: { label: 'Process' } },
];
const initialEdges: Edge[] = [{ id: 'e1-2', source: '1', target: '2' }];

export const useFlowDesignerStore = create<FlowDesignerState>((set, get) => ({
  flowId: null,
  nodes: initialNodes,
  edges: initialEdges,
  
  onNodesChange: (changes: NodeChange[]) => {
    set({
      nodes: applyNodeChanges(changes, get().nodes),
    });
  },
  
  onEdgesChange: (changes: EdgeChange[]) => {
    set({
      edges: applyEdgeChanges(changes, get().edges),
    });
  },
  
  onConnect: (connection: Connection) => {
    set({
      edges: addEdge(connection, get().edges),
    });
  },

  addNode: (node: Node) => {
    set({
      nodes: [...get().nodes, node],
    });
  },

  setNodes: (nodes) => set({ nodes }),
  setEdges: (edges) => set({ edges }),

  loadFlow: async (modelId: string) => {
    try {
      const response = await getFlows(modelId);
      const flows = response.data;
      if (flows && flows.length > 0) {
        // For now, just load the first one
        const flow = flows[0];
        const definition = JSON.parse(flow.definition);
        
        set({
          flowId: flow.flowId,
          nodes: definition.nodes || initialNodes,
          edges: definition.edges || initialEdges,
        });
      } else {
        // Reset if no flow exists
        set({
          flowId: null,
          nodes: initialNodes,
          edges: initialEdges,
        });
      }
    } catch (error) {
      console.error('Failed to load flow', error);
    }
  },

  saveFlow: async (modelId: string, name: string) => {
    const { flowId, nodes, edges } = get();
    const definition = JSON.stringify({ nodes, edges });
    
    try {
      if (flowId) {
        await updateFlow(flowId, { definition });
      } else {
        const response = await createFlow({
          modelId,
          name: name + ' Flow',
          triggerType: 'MANUAL',
        });
        // Update with definition immediately
        if (response.data) {
           await updateFlow(response.data.flowId, { definition });
           set({ flowId: response.data.flowId });
        }
      }
    } catch (error) {
      console.error('Failed to save flow', error);
      throw error;
    }
  },
}));
