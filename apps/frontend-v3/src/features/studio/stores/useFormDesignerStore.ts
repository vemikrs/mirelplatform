import { create } from 'zustand';

export type WidgetType = 'text' | 'number' | 'date' | 'boolean' | 'select';

export interface Widget {
  id: string;
  type: WidgetType;
  label: string;
  required: boolean;
  // Grid layout properties
  x: number;
  y: number;
  w: number;
  h: number;
}

interface FormDesignerState {
  widgets: Widget[];
  selectedWidgetId: string | null;
  
  addWidget: (widget: Widget) => void;
  removeWidget: (id: string) => void;
  updateWidget: (id: string, updates: Partial<Widget>) => void;
  selectWidget: (id: string | null) => void;
  setWidgets: (widgets: Widget[]) => void;
}

export const useFormDesignerStore = create<FormDesignerState>((set) => ({
  widgets: [],
  selectedWidgetId: null,

  addWidget: (widget) => set((state) => ({ 
    widgets: [...state.widgets, widget],
    selectedWidgetId: widget.id 
  })),

  removeWidget: (id) => set((state) => ({ 
    widgets: state.widgets.filter((w) => w.id !== id),
    selectedWidgetId: state.selectedWidgetId === id ? null : state.selectedWidgetId
  })),

  updateWidget: (id, updates) => set((state) => ({
    widgets: state.widgets.map((w) => (w.id === id ? { ...w, ...updates } : w)),
  })),

  selectWidget: (id) => set({ selectedWidgetId: id }),
  
  setWidgets: (widgets) => set({ widgets }),
}));
