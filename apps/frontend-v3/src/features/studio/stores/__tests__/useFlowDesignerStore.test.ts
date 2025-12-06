import { renderHook, act } from '@testing-library/react';
import { useFlowDesignerStore } from '../useFlowDesignerStore';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import * as flowApi from '../../../../lib/api/flow';

// Mock the API client
vi.mock('../../../../lib/api/flow', () => ({
  getFlows: vi.fn(),
  createFlow: vi.fn(),
  updateFlow: vi.fn(),
}));

describe('useFlowDesignerStore', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Reset store state
    useFlowDesignerStore.setState({
      flowId: null,
      nodes: [],
      edges: [],
    });
  });

  it('should add a node', () => {
    const { result } = renderHook(() => useFlowDesignerStore());
    const newNode = { id: '3', position: { x: 0, y: 0 }, data: { label: 'Test' } };

    act(() => {
      result.current.addNode(newNode);
    });

    expect(result.current.nodes).toContainEqual(newNode);
  });

  it('should load flow from API', async () => {
    const mockFlow = {
      flowId: 'flow-1',
      definition: JSON.stringify({
        nodes: [{ id: '1', position: { x: 0, y: 0 }, data: { label: 'Loaded Node' } }],
        edges: [],
      }),
    };
    vi.mocked(flowApi.getFlows).mockResolvedValue({ data: [mockFlow] } as any);

    const { result } = renderHook(() => useFlowDesignerStore());

    await act(async () => {
      await result.current.loadFlow('model-1');
    });

    expect(result.current.flowId).toBe('flow-1');
    expect(result.current.nodes[0]?.data.label).toBe('Loaded Node');
  });

  it('should save new flow to API', async () => {
    const { result } = renderHook(() => useFlowDesignerStore());
    
    vi.mocked(flowApi.createFlow).mockResolvedValue({ 
      data: { flowId: 'new-flow-id' } 
    } as any);
    vi.mocked(flowApi.updateFlow).mockResolvedValue({} as any);

    await act(async () => {
      await result.current.saveFlow('model-1', 'Test Model');
    });

    expect(flowApi.createFlow).toHaveBeenCalledWith({
      modelId: 'model-1',
      name: 'Test Model Flow',
      triggerType: 'MANUAL',
    });
    expect(flowApi.updateFlow).toHaveBeenCalledWith('new-flow-id', expect.objectContaining({
      definition: expect.any(String),
    }));
    expect(result.current.flowId).toBe('new-flow-id');
  });

  it('should update existing flow to API', async () => {
    const { result } = renderHook(() => useFlowDesignerStore());
    
    // Set initial state with flowId
    // We can't easily set state directly without an action, so we'll simulate a load first or just mock the internal state if possible,
    // but here we'll just use the loadFlow to set it up.
    const mockFlow = {
      flowId: 'existing-flow-id',
      definition: '{}',
    };
    vi.mocked(flowApi.getFlows).mockResolvedValue({ data: [mockFlow] } as any);
    await act(async () => {
      await result.current.loadFlow('model-1');
    });

    vi.mocked(flowApi.updateFlow).mockResolvedValue({} as any);

    await act(async () => {
      await result.current.saveFlow('model-1', 'Test Model');
    });

    expect(flowApi.createFlow).not.toHaveBeenCalled();
    expect(flowApi.updateFlow).toHaveBeenCalledWith('existing-flow-id', expect.objectContaining({
      definition: expect.any(String),
    }));
  });
});
