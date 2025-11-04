import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { RootLayout } from './RootLayout';

describe('RootLayout', () => {
  it('renders the mirelplatform header', () => {
    render(
      <MemoryRouter>
        <RootLayout />
      </MemoryRouter>
    );
    
    const heading = screen.getByRole('heading', { name: /mirelplatform/i });
    expect(heading).toBeInTheDocument();
  });

  it('renders Home navigation link', () => {
    render(
      <MemoryRouter>
        <RootLayout />
      </MemoryRouter>
    );
    
    const homeLink = screen.getByRole('link', { name: /home/i });
    expect(homeLink).toBeInTheDocument();
    expect(homeLink).toHaveAttribute('href', '/');
  });

  it('renders ProMarker navigation link', () => {
    render(
      <MemoryRouter>
        <RootLayout />
      </MemoryRouter>
    );
    
    const promarkerLink = screen.getByRole('link', { name: /promarker/i });
    expect(promarkerLink).toBeInTheDocument();
    expect(promarkerLink).toHaveAttribute('href', '/promarker');
  });

  it('renders footer with correct text', () => {
    render(
      <MemoryRouter>
        <RootLayout />
      </MemoryRouter>
    );
    
    const footer = screen.getByText(/© 2016-2025 mirelplatform\. All rights reserved\./i);
    expect(footer).toBeInTheDocument();
  });

  it('has proper semantic HTML structure', () => {
    const { container } = render(
      <MemoryRouter>
        <RootLayout />
      </MemoryRouter>
    );
    
    expect(container.querySelector('header')).toBeInTheDocument();
    expect(container.querySelector('main')).toBeInTheDocument();
    expect(container.querySelector('footer')).toBeInTheDocument();
  });
});
