import { describe, it, expect, beforeEach } from 'vitest';
import { useFormDesignerStore } from '../useFormDesignerStore';
import type { Widget } from '../useFormDesignerStore';

describe('useFormDesignerStore', () => {
  beforeEach(() => {
    useFormDesignerStore.setState({
      widgets: [],
      selectedWidgetId: null,
      modelId: null,
      modelName: 'Untitled Form',
    });
  });

  it('adds a widget', () => {
    const widget: Widget = {
      id: '1',
      type: 'text',
      label: 'Test Widget',
      fieldCode: 'test',
      required: false,
      x: 0,
      y: 0,
      w: 4,
      h: 2,
    };

    useFormDesignerStore.getState().addWidget(widget);

    const state = useFormDesignerStore.getState();
    expect(state.widgets).toHaveLength(1);
    expect(state.widgets[0]).toEqual(widget);
    expect(state.selectedWidgetId).toBe('1');
  });

  it('removes a widget', () => {
    const widget: Widget = {
      id: '1',
      type: 'text',
      label: 'Test Widget',
      fieldCode: 'test',
      required: false,
      x: 0,
      y: 0,
      w: 4,
      h: 2,
    };

    useFormDesignerStore.getState().addWidget(widget);
    useFormDesignerStore.getState().removeWidget('1');

    const state = useFormDesignerStore.getState();
    expect(state.widgets).toHaveLength(0);
    expect(state.selectedWidgetId).toBeNull();
  });

  it('updates a widget', () => {
    const widget: Widget = {
      id: '1',
      type: 'text',
      label: 'Test Widget',
      fieldCode: 'test',
      required: false,
      x: 0,
      y: 0,
      w: 4,
      h: 2,
    };

    useFormDesignerStore.getState().addWidget(widget);
    useFormDesignerStore.getState().updateWidget('1', { label: 'Updated Label' });

    const state = useFormDesignerStore.getState();
    expect(state.widgets[0].label).toBe('Updated Label');
  });

  it('selects a widget', () => {
    const widget: Widget = {
      id: '1',
      type: 'text',
      label: 'Test Widget',
      fieldCode: 'test',
      required: false,
      x: 0,
      y: 0,
      w: 4,
      h: 2,
    };

    useFormDesignerStore.getState().addWidget(widget);
    useFormDesignerStore.getState().selectWidget(null);
    expect(useFormDesignerStore.getState().selectedWidgetId).toBeNull();

    useFormDesignerStore.getState().selectWidget('1');
    expect(useFormDesignerStore.getState().selectedWidgetId).toBe('1');
  });
});
