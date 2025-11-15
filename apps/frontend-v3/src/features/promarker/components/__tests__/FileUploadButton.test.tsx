import { describe, expect, it, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';

vi.mock('@mirel/ui', async () => {
  const actual = await vi.importActual<typeof import('@mirel/ui')>('@mirel/ui');
  return {
    ...actual,
    toast: vi.fn(),
  };
});

const mutateAsync = vi.fn();
const useFileUploadMock = () => ({ mutateAsync, isPending: false });

vi.mock('../../hooks/useFileUpload', () => ({
  useFileUpload: () => useFileUploadMock(),
}));

import { toast } from '@mirel/ui';
import { FileUploadButton } from '../FileUploadButton';
import { toastMessages } from '../../constants/toastMessages';

describe('FileUploadButton toast handling', () => {
  beforeEach(() => {
    mutateAsync.mockReset();
    vi.mocked(toast).mockClear();
  });

  it('calls success toast when upload succeeds', async () => {
    mutateAsync.mockResolvedValue({
      data: [{ fileId: 'file-1', name: 'config.yaml' }],
      errors: [],
    });
    const onFileUploaded = vi.fn();

    render(
      <FileUploadButton
        parameterId="config"
        value=""
        onFileUploaded={onFileUploaded}
      />
    );

    const input = screen.getByTestId('file-input-config') as HTMLInputElement;
    const file = new File(['content'], 'config.yaml', { type: 'text/yaml' });

    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(onFileUploaded).toHaveBeenCalledWith('config', 'file-1', 'config.yaml');
      expect(toast).toHaveBeenCalledWith(
        expect.objectContaining({
          ...toastMessages.fileUploadSuccess,
          description: expect.stringContaining('config.yaml'),
        })
      );
    });
  });

  it('shows error toast when API returns errors', async () => {
    mutateAsync.mockResolvedValue({
      data: [],
      errors: ['サイズ上限を超えています'],
    });

    render(
      <FileUploadButton
        parameterId="config"
        value=""
        onFileUploaded={vi.fn()}
      />
    );

    const input = screen.getByTestId('file-input-config') as HTMLInputElement;
    const file = new File(['content'], 'config.yaml', { type: 'text/yaml' });

    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(toast).toHaveBeenCalledWith(
        expect.objectContaining({
          ...toastMessages.fileUploadError,
          description: 'サイズ上限を超えています',
        })
      );
    });
  });

  it('shows error toast when upload throws', async () => {
    mutateAsync.mockRejectedValue(new Error('network error'));

    render(
      <FileUploadButton
        parameterId="config"
        value=""
        onFileUploaded={vi.fn()}
      />
    );

    const input = screen.getByTestId('file-input-config') as HTMLInputElement;
    const file = new File(['content'], 'config.yaml', { type: 'text/yaml' });

    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(toast).toHaveBeenCalledWith(
        expect.objectContaining({
          ...toastMessages.fileUploadError,
          description: 'network error',
        })
      );
    });
  });
});
