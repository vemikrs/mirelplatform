import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { HomePage } from './HomePage';

describe('HomePage', () => {
  it('renders the welcome heading', () => {
    render(<HomePage />);
    const heading = screen.getByRole('heading', { name: /welcome to mirelplatform/i });
    expect(heading).toBeInTheDocument();
  });

  it('renders the description text', () => {
    render(<HomePage />);
    const description = screen.getByText(/エンタープライズ向けの軽量な開発支援プラットフォーム/i);
    expect(description).toBeInTheDocument();
  });

  it('renders ProMarker button', () => {
    render(<HomePage />);
    const button = screen.getByRole('button', { name: /ProMarker を開く/i });
    expect(button).toBeInTheDocument();
  });

  it('renders toast button', () => {
    render(<HomePage />);
    const button = screen.getByRole('button', { name: /トースト表示/i });
    expect(button).toBeInTheDocument();
  });

  it('calls navigation function when ProMarker button is clicked', () => {
    // Spy on window.location.href setter
    const hrefSpy = vi.fn();
    Object.defineProperty(window, 'location', {
      value: { href: '' },
      writable: true,
      configurable: true,
    });
    
    Object.defineProperty(window.location, 'href', {
      set: hrefSpy,
      get: () => '',
    });

    render(<HomePage />);
    const button = screen.getByRole('button', { name: /ProMarker を開く/i });
    button.click();

    expect(hrefSpy).toHaveBeenCalledWith('/promarker');
  });
});
