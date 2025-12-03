import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { DynamicFormRenderer } from '../components/Runtime/DynamicFormRenderer';
import { describe, it, expect, vi } from 'vitest';
import type { Widget } from '../stores/useFormDesignerStore';

import userEvent from '@testing-library/user-event';

describe('DynamicFormRenderer', () => {
  const mockSubmit = vi.fn();

  it('renders correctly', () => {
    const widgets: Widget[] = [
      { id: '1', type: 'text', label: 'Name', fieldCode: 'name', required: true, x: 0, y: 0, w: 4, h: 2 },
    ];
    render(<DynamicFormRenderer widgets={widgets} onSubmit={mockSubmit} />);
    expect(screen.getByLabelText(/Name/i)).toBeInTheDocument();
  });

  it('validates required fields', async () => {
    const user = userEvent.setup();
    const widgets: Widget[] = [
      { id: '1', type: 'text', label: 'Name', fieldCode: 'name', required: true, x: 0, y: 0, w: 4, h: 2 },
    ];
    render(<DynamicFormRenderer widgets={widgets} onSubmit={mockSubmit} />);
    
    await user.click(screen.getByRole('button', { name: /Submit/i }));
    
    await waitFor(() => {
      expect(screen.getByText(/Required/i)).toBeInTheDocument();
    });
    expect(mockSubmit).not.toHaveBeenCalled();
  });

  it('validates min length', async () => {
    const user = userEvent.setup();
    const widgets: Widget[] = [
      { id: '1', type: 'text', label: 'Code', fieldCode: 'code', required: false, minLength: 5, x: 0, y: 0, w: 4, h: 2 },
    ];
    render(<DynamicFormRenderer widgets={widgets} onSubmit={mockSubmit} />);
    
    const input = screen.getByLabelText(/Code/i);
    await user.type(input, '123');
    await user.click(screen.getByRole('button', { name: /Submit/i }));
    
    await waitFor(() => {
      expect(screen.getByText(/Min length is 5/i)).toBeInTheDocument();
    });
  });

  it('validates regex', async () => {
    const user = userEvent.setup();
    const widgets: Widget[] = [
      { id: '1', type: 'text', label: 'Email', fieldCode: 'email', required: false, validationRegex: '^[^@]+@[^@]+\\.[^@]+$', x: 0, y: 0, w: 4, h: 2 },
    ];
    render(<DynamicFormRenderer widgets={widgets} onSubmit={mockSubmit} />);
    
    const input = screen.getByLabelText(/Email/i);
    await user.type(input, 'invalid-email');
    await user.click(screen.getByRole('button', { name: /Submit/i }));
    
    await waitFor(() => {
      expect(screen.getByText(/Invalid format/i)).toBeInTheDocument();
    });
  });

  it('submits valid data', async () => {
    const user = userEvent.setup();
    const widgets: Widget[] = [
      { id: '1', type: 'text', label: 'Name', fieldCode: 'name', required: true, x: 0, y: 0, w: 4, h: 2 },
    ];
    render(<DynamicFormRenderer widgets={widgets} onSubmit={mockSubmit} />);
    
    const input = screen.getByLabelText(/Name/i);
    await user.type(input, 'John Doe');
    await user.click(screen.getByRole('button', { name: /Submit/i }));
    
    await waitFor(() => {
      expect(mockSubmit).toHaveBeenCalledWith({ name: 'John Doe' }, expect.anything());
    });
  });
});
