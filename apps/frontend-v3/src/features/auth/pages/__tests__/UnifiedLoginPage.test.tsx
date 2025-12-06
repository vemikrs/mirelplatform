import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { UnifiedLoginPage } from '../UnifiedLoginPage';
import { useAuthStore } from '@/stores/authStore';
import { useRequestOtp } from '@/lib/hooks/useOtp';

// モック
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: vi.fn(),
    useSearchParams: vi.fn(),
  };
});

vi.mock('@/stores/authStore', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('@/lib/hooks/useOtp', () => ({
  useRequestOtp: vi.fn(),
}));

vi.mock('@/lib/hooks/useTheme', () => ({
  useTheme: vi.fn(),
}));

describe('UnifiedLoginPage', () => {
  const mockNavigate = vi.fn();
  const mockSetOtpState = vi.fn();
  const mockLogin = vi.fn();
  const mockRequestOtp = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();

    // useNavigateモック
    const { useNavigate, useSearchParams } = await import('react-router-dom');
    vi.mocked(useNavigate).mockReturnValue(mockNavigate);
    vi.mocked(useSearchParams).mockReturnValue([new URLSearchParams(), vi.fn()]);

    // useAuthStoreモック
    vi.mocked(useAuthStore).mockReturnValue({
      setOtpState: mockSetOtpState,
      login: mockLogin,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any);

    // useRequestOtpモック
    vi.mocked(useRequestOtp).mockReturnValue({
      mutate: mockRequestOtp,
      isPending: false,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any);
  });

  it('OTPフォームが表示される', () => {
    render(
      <BrowserRouter>
        <UnifiedLoginPage />
      </BrowserRouter>
    );

    expect(screen.getByLabelText('メールアドレスでログイン')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '認証コードを送信' })).toBeInTheDocument();
  });

  it('GitHubログインボタンが表示される', () => {
    render(
      <BrowserRouter>
        <UnifiedLoginPage />
      </BrowserRouter>
    );

    expect(screen.getByRole('button', { name: 'GitHubでログイン' })).toBeInTheDocument();
  });

  it('パスワードログインが初期状態では非表示', () => {
    render(
      <BrowserRouter>
        <UnifiedLoginPage />
      </BrowserRouter>
    );

    expect(screen.queryByLabelText('ユーザー名 または メールアドレス')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('パスワード')).not.toBeInTheDocument();
  });

  it('パスワードログイン展開ボタンをクリックするとフォームが表示される', async () => {
    render(
      <BrowserRouter>
        <UnifiedLoginPage />
      </BrowserRouter>
    );

    const toggleButton = screen.getByRole('button', { name: /パスワードでログイン/ });
    fireEvent.click(toggleButton);

    await waitFor(() => {
      expect(screen.getByLabelText('ユーザー名 または メールアドレス')).toBeInTheDocument();
      expect(screen.getByLabelText('パスワード')).toBeInTheDocument();
    });
  });

  it('OTPフォーム送信が機能する', async () => {
    render(
      <BrowserRouter>
        <UnifiedLoginPage />
      </BrowserRouter>
    );

    const emailInput = screen.getByLabelText('メールアドレスでログイン');
    const submitButton = screen.getByRole('button', { name: '認証コードを送信' });

    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockRequestOtp).toHaveBeenCalledWith({
        email: 'test@example.com',
        purpose: 'LOGIN',
      });
    });
  });

  it('空のメールアドレスで送信ボタンがある', () => {
    render(
      <BrowserRouter>
        <UnifiedLoginPage />
      </BrowserRouter>
    );

    // メールアドレス入力フィールドがrequired属性を持つことを確認
    const emailInput = screen.getByLabelText('メールアドレスでログイン');
    expect(emailInput).toHaveAttribute('required');
  });

  it('新規登録リンクが表示される', () => {
    render(
      <BrowserRouter>
        <UnifiedLoginPage />
      </BrowserRouter>
    );

    const signupLink = screen.getByRole('link', { name: '新規登録' });
    expect(signupLink).toBeInTheDocument();
    expect(signupLink).toHaveAttribute('href', '/signup');
  });
});
