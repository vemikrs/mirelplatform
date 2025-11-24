import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { OAuthCallbackPage } from '../OAuthCallbackPage';
import { useAuthStore } from '@/stores/authStore';

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

describe('OAuthCallbackPage', () => {
  const mockNavigate = vi.fn();
  const mockSetToken = vi.fn();
  const mockSetAuthenticated = vi.fn();
  
  beforeEach(async () => {
    vi.clearAllMocks();
    
    // useNavigateモック
    const { useNavigate } = await import('react-router-dom');
    vi.mocked(useNavigate).mockReturnValue(mockNavigate);
    
    // useAuthStoreモック
    vi.mocked(useAuthStore).mockReturnValue({
      setToken: mockSetToken,
      setAuthenticated: mockSetAuthenticated,
    } as any);
  });
  
  it('トークンが渡された場合: ストアに保存してダッシュボードに遷移', async () => {
    // Given: トークン付きURLパラメータ
    const { useSearchParams } = await import('react-router-dom');
    const testToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token';
    const mockSearchParams = new URLSearchParams({ token: testToken });
    vi.mocked(useSearchParams).mockReturnValue([mockSearchParams, vi.fn()] as any);
    
    // When: コンポーネントをレンダリング
    render(
      <BrowserRouter>
        <OAuthCallbackPage />
      </BrowserRouter>
    );
    
    // Then: ローディング表示
    expect(screen.getByText('ログイン処理中...')).toBeInTheDocument();
    
    // Then: トークンをストアに保存
    await waitFor(() => {
      expect(mockSetToken).toHaveBeenCalledWith(testToken);
      expect(mockSetAuthenticated).toHaveBeenCalledWith(true);
    });
    
    // Then: ダッシュボードに遷移
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/');
    });
  });
  
  it('エラーが渡された場合: ログインページに戻る', async () => {
    // Given: エラー付きURLパラメータ
    const { useSearchParams } = await import('react-router-dom');
    const mockSearchParams = new URLSearchParams({ error: 'oauth2' });
    vi.mocked(useSearchParams).mockReturnValue([mockSearchParams, vi.fn()] as any);
    
    // When: コンポーネントをレンダリング
    render(
      <BrowserRouter>
        <OAuthCallbackPage />
      </BrowserRouter>
    );
    
    // Then: ログインページにエラー付きで遷移
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/login?error=oauth2');
    });
    
    // Then: トークン保存されない
    expect(mockSetToken).not.toHaveBeenCalled();
    expect(mockSetAuthenticated).not.toHaveBeenCalled();
  });
  
  it('トークンがない場合: ログインページに戻る', async () => {
    // Given: パラメータなし
    const { useSearchParams } = await import('react-router-dom');
    const mockSearchParams = new URLSearchParams({});
    vi.mocked(useSearchParams).mockReturnValue([mockSearchParams, vi.fn()] as any);
    
    // When: コンポーネントをレンダリング
    render(
      <BrowserRouter>
        <OAuthCallbackPage />
      </BrowserRouter>
    );
    
    // Then: ログインページにエラー付きで遷移
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/login?error=no_token');
    });
    
    // Then: トークン保存されない
    expect(mockSetToken).not.toHaveBeenCalled();
  });
});
