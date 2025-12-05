import { describe, expect, beforeEach, vi, it } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@mirel/ui', async () => {
  const actual = await vi.importActual<typeof import('@mirel/ui')>('@mirel/ui');
  return {
    ...actual,
    toast: vi.fn(),
  };
});

const suggestMutate = vi.fn();
const reloadMutate = vi.fn();

vi.mock('../../hooks/useSuggest', () => ({
  useSuggest: () => ({
    mutateAsync: suggestMutate,
    isPending: false,
  }),
}));

vi.mock('../../hooks/useGenerate', () => ({
  useGenerate: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
    isSuccess: false,
  }),
}));

vi.mock('../../hooks/useReloadStencilMaster', () => ({
  useReloadStencilMaster: () => ({
    mutateAsync: reloadMutate,
    isPending: false,
  }),
}));

vi.mock('../../hooks/useParameterForm', () => ({
  useParameterForm: () => ({
    reset: vi.fn(),
    clearAll: vi.fn(),
    validateAll: vi.fn().mockResolvedValue(true),
    getValues: vi.fn().mockReturnValue({}),
    setValue: vi.fn(),
    isValid: true,
    form: {},
    register: vi.fn(),
    formState: { errors: {}, isDirty: false, isSubmitting: false },
    errors: {},
    isDirty: false,
    isSubmitting: false,
    handleSubmit: vi.fn(),
    watch: vi.fn(),
  }),
}));

import { toast } from '@mirel/ui';
import { toastMessages } from '../../constants/toastMessages';
import { ProMarkerPage } from '../ProMarkerPage';

const mockSuggestResponse = {
  data: {
    model: {
      fltStrStencilCategory: {
        items: [{ value: 'category-1', text: 'カテゴリ1' }],
        selected: 'category-1',
      },
      fltStrStencilCd: {
        items: [{ value: 'stencil-1', text: 'ステンシル1' }],
        selected: 'stencil-1',
      },
      fltStrSerialNo: {
        items: [{ value: 'serial-1', text: 'シリアル1' }],
        selected: 'serial-1',
      },
      params: { childs: [] },
      stencil: { config: null },
    },
  },
};

describe('ProMarkerPage toast behavior', () => {
  beforeEach(() => {
    suggestMutate.mockReset();
    reloadMutate.mockReset();
    suggestMutate.mockResolvedValue(mockSuggestResponse);
    reloadMutate.mockResolvedValue({});
    vi.mocked(toast).mockClear();
  });

  it('shows toast when clearing all inputs', async () => {
    render(
      <MemoryRouter>
        <ProMarkerPage />
      </MemoryRouter>
    );

    await waitFor(() => expect(suggestMutate).toHaveBeenCalled());

    fireEvent.click(await screen.findByTestId('clear-all-btn'));

    await waitFor(() => {
      expect(toast).toHaveBeenCalledWith(expect.objectContaining(toastMessages.clearAll));
    });
  });

  it('shows toast when clearing stencil definition', async () => {
    render(
      <MemoryRouter>
        <ProMarkerPage />
      </MemoryRouter>
    );

    await waitFor(() => expect(suggestMutate).toHaveBeenCalled());

    fireEvent.click(await screen.findByTestId('clear-stencil-btn'));

    await waitFor(() => {
      expect(toast).toHaveBeenCalledWith(expect.objectContaining(toastMessages.clearStencil));
    });
  });
});
