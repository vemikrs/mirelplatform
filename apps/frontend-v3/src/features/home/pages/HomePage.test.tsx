import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { HomePage } from './HomePage';

describe('HomePage', () => {
  it('renders enterprise portal heading', () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    );
    const heading = screen.getByRole('heading', { name: '業務アプリ基盤ポータル' });
    expect(heading).toBeInTheDocument();
  });

  it('lists key platform modules', () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    );
    const moduleCards = screen.getAllByTestId('home-module-card');
    expect(moduleCards.length).toBeGreaterThanOrEqual(3);
  });

  it('provides CTA links to ProMarker workspace', () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    );
    const link = screen.getByRole('link', { name: 'ProMarker を開く' });
    expect(link).toHaveAttribute('href', '/promarker');
  });
});
