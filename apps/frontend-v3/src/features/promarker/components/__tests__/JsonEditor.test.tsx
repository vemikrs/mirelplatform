import { describe, expect, it, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';

vi.mock('@mirel/ui', async () => {
  const actual = await vi.importActual<typeof import('@mirel/ui')>('@mirel/ui');
  return {
    ...actual,
    toast: vi.fn(),
  };
});

import { toast } from '@mirel/ui';
import { JsonEditor } from '../JsonEditor';
import { toastMessages } from '../../constants/toastMessages';
import type { DataElement } from '../../types/api';

const sampleParameters: DataElement[] = [
  {
    id: 'packageName',
    name: 'パッケージ名',
    valueType: 'text',
    value: 'com.example',
    placeholder: '',
    note: '',
    nodeType: 'ELEMENT',
  },
];

describe('JsonEditor toast handling', () => {
  beforeEach(() => {
    vi.mocked(toast).mockClear();
  });

  it('notifies success when apply completes', async () => {
    const onApply = vi.fn().mockResolvedValue(undefined);

    render(
      <JsonEditor
        open
        onOpenChange={() => undefined}
        category="samples"
        stencil="hello-world"
        serial="A1"
        parameters={sampleParameters}
        onApply={onApply}
      />
    );

    fireEvent.click(screen.getByTestId('json-apply-btn'));

    await waitFor(() => {
      expect(onApply).toHaveBeenCalled();
      expect(toast).toHaveBeenCalledWith(expect.objectContaining(toastMessages.jsonApplySuccess));
    });
  });

  it('notifies error when apply throws', async () => {
    const onApply = vi.fn().mockRejectedValue(new Error('apply failed'));

    render(
      <JsonEditor
        open
        onOpenChange={() => undefined}
        category="samples"
        stencil="hello-world"
        serial="A1"
        parameters={sampleParameters}
        onApply={onApply}
      />
    );

    fireEvent.click(screen.getByTestId('json-apply-btn'));

    await waitFor(() => {
      expect(onApply).toHaveBeenCalled();
      expect(toast).toHaveBeenCalledWith(
        expect.objectContaining({
          ...toastMessages.jsonApplyError,
          description: 'apply failed',
        })
      );
    });
  });

  it('shows validation error toast for invalid JSON', () => {
    const onApply = vi.fn();

    render(
      <JsonEditor
        open
        onOpenChange={() => undefined}
        category="samples"
        stencil="hello-world"
        serial="A1"
        parameters={sampleParameters}
        onApply={onApply}
      />
    );

    const textarea = screen.getByTestId('json-textarea');
    fireEvent.change(textarea, { target: { value: 'invalid json' } });
    fireEvent.click(screen.getByTestId('json-apply-btn'));

    expect(onApply).not.toHaveBeenCalled();
    expect(toast).toHaveBeenCalledWith(
      expect.objectContaining({
        title: toastMessages.jsonApplyError.title,
        variant: toastMessages.jsonApplyError.variant,
        description: expect.stringContaining('JSONフォーマットが不正です'),
      })
    );
    expect(screen.getByTestId('json-error-message')).toHaveTextContent('JSONフォーマットが不正です');
  });
});
