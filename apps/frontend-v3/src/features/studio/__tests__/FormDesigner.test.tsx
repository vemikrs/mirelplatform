import { render, screen } from '@testing-library/react';
import { FormDesigner } from '../components/FormDesigner/FormDesigner';
import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock dnd-kit to avoid complex drag simulation issues in unit tests if possible,
// or use it as is if it works with jsdom.
// For now, let's try to test the integration.

// We need to mock the store to have a clean state
const { mockAddWidget, mockSetWidgets, mockSelectWidget } = vi.hoisted(() => ({
  mockAddWidget: vi.fn(),
  mockSetWidgets: vi.fn(),
  mockSelectWidget: vi.fn(),
}));

vi.mock('../stores/useFormDesignerStore', () => ({
  useFormDesignerStore: () => ({
    widgets: [],
    selectedWidgetId: null,
    addWidget: mockAddWidget,
    setWidgets: mockSetWidgets,
    selectWidget: mockSelectWidget,
  }),
}));

describe('FormDesigner', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders palette and empty canvas', () => {
    render(<FormDesigner />);
    
    // Check Palette items
    expect(screen.getByText('Text Input')).toBeInTheDocument();
    expect(screen.getByText('Number Input')).toBeInTheDocument();
    
    // Check Property Editor empty state
    expect(screen.getByText('Select a widget to edit its properties')).toBeInTheDocument();
  });

  // Testing Drag and Drop with dnd-kit in jsdom is notoriously difficult without specific setup.
  // Instead of full DND simulation which might be flaky, we can verify that the components are rendered correctly
  // and that the store interactions work if we were to trigger them (which we mocked).
  
  // However, to test the actual "Add Widget" logic, we might need to integration test the store or 
  // mock the DndContext's onDragEnd handler if we can access it.
  
  // For this unit test, let's focus on rendering and ensuring the structure is correct.
  // The actual DND logic is handled by dnd-kit, so we assume dnd-kit works if configured correctly.
  // We can verify that the drop handler calls addWidget if we extract the logic or test the store separately.
  
  // Let's test the store logic separately if we want to verify "Add Widget" logic.
  // But here we are testing the UI.
});
