import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { Avatar } from '../Avatar';

describe('Avatar', () => {
  it('画像URLがある場合: 画像を表示', async () => {
    // Given: 有効な画像URL
    const testSrc = 'https://example.com/avatar.jpg';
    
    // When: Avatarコンポーネントをレンダリング
    render(<Avatar src={testSrc} alt="Test Avatar" />);
    
    // Then: 画像要素が表示される
    const img = screen.getByAlt('Test Avatar');
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', testSrc);
  });
  
  it('画像読み込みエラー: フォールバック文字を表示', async () => {
    // Given: 無効な画像URL + フォールバック文字
    const testSrc = 'https://invalid.com/notfound.jpg';
    const fallback = 'AB';
    
    // When: Avatarコンポーネントをレンダリング
    const { container } = render(<Avatar src={testSrc} fallback={fallback} />);
    
    // Then: 画像要素が存在
    const img = screen.getByAlt('Avatar');
    
    // Then: エラーイベントをトリガー
    img.dispatchEvent(new Event('error'));
    
    // Then: フォールバック文字が表示される
    await waitFor(() => {
      expect(screen.getByText(fallback)).toBeInTheDocument();
    });
  });
  
  it('画像URLがnull: フォールバック文字を表示', () => {
    // Given: 画像URLがnull
    const fallback = 'CD';
    
    // When: Avatarコンポーネントをレンダリング
    render(<Avatar src={null} fallback={fallback} />);
    
    // Then: フォールバック文字が表示される
    expect(screen.getByText(fallback)).toBeInTheDocument();
  });
  
  it('画像URLもフォールバック文字もない場合: デフォルトアイコンを表示', () => {
    // When: Avatarコンポーネントをレンダリング
    const { container } = render(<Avatar />);
    
    // Then: SVGアイコンが表示される
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });
  
  it('サイズ指定: smサイズ', () => {
    // When: smサイズで表示
    const { container } = render(<Avatar size="sm" />);
    
    // Then: smサイズクラスが適用される
    const avatarDiv = container.querySelector('div');
    expect(avatarDiv).toHaveClass('h-8', 'w-8', 'text-xs');
  });
  
  it('サイズ指定: mdサイズ（デフォルト）', () => {
    // When: サイズ未指定（デフォルトmd）
    const { container } = render(<Avatar />);
    
    // Then: mdサイズクラスが適用される
    const avatarDiv = container.querySelector('div');
    expect(avatarDiv).toHaveClass('h-10', 'w-10', 'text-sm');
  });
  
  it('サイズ指定: lgサイズ', () => {
    // When: lgサイズで表示
    const { container } = render(<Avatar size="lg" />);
    
    // Then: lgサイズクラスが適用される
    const avatarDiv = container.querySelector('div');
    expect(avatarDiv).toHaveClass('h-12', 'w-12', 'text-base');
  });
  
  it('サイズ指定: xlサイズ', () => {
    // When: xlサイズで表示
    const { container } = render(<Avatar size="xl" />);
    
    // Then: xlサイズクラスが適用される
    const avatarDiv = container.querySelector('div');
    expect(avatarDiv).toHaveClass('h-16', 'w-16', 'text-lg');
  });
  
  it('カスタムクラス名: 適用される', () => {
    // When: カスタムクラス名を指定
    const { container } = render(<Avatar className="custom-avatar" />);
    
    // Then: カスタムクラスが適用される
    const avatarDiv = container.querySelector('div');
    expect(avatarDiv).toHaveClass('custom-avatar');
  });
  
  it('画像読み込み完了: opacity-100が適用される', async () => {
    // Given: 有効な画像URL
    const testSrc = 'https://example.com/avatar.jpg';
    
    // When: Avatarコンポーネントをレンダリング
    render(<Avatar src={testSrc} />);
    
    // Then: 画像要素が存在
    const img = screen.getByAlt('Avatar');
    
    // Then: 読み込み完了イベントをトリガー
    img.dispatchEvent(new Event('load'));
    
    // Then: opacity-100クラスが適用される
    await waitFor(() => {
      expect(img).toHaveClass('opacity-100');
    });
  });
  
  it('src変更時: エラー状態をリセット', async () => {
    // Given: 初回レンダリング
    const { rerender } = render(<Avatar src="https://invalid.com/1.jpg" fallback="A" />);
    
    // Then: エラーイベントをトリガー
    const img1 = screen.getByAlt('Avatar');
    img1.dispatchEvent(new Event('error'));
    
    // Then: フォールバックが表示される
    await waitFor(() => {
      expect(screen.getByText('A')).toBeInTheDocument();
    });
    
    // When: src変更
    rerender(<Avatar src="https://example.com/2.jpg" fallback="A" />);
    
    // Then: 新しい画像要素が表示される
    const img2 = screen.getByAlt('Avatar');
    expect(img2).toHaveAttribute('src', 'https://example.com/2.jpg');
  });
});
