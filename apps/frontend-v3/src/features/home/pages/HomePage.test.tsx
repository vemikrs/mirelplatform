import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { HomePage } from './HomePage';

// Mock announcement API
vi.mock('@/lib/api/announcement', () => ({
  getAnnouncements: vi.fn().mockResolvedValue({ data: { announcements: [] } }),
  markAsRead: vi.fn(),
  markAsUnread: vi.fn(),
}));

// Mock system API  
vi.mock('@/lib/api/system', () => ({
  getSystemHealth: vi.fn().mockResolvedValue({ status: 'UP' }),
  getSystemStatus: vi.fn().mockResolvedValue({
    cpuUsage: 20,
    memoryUsage: 50,
    diskUsage: 30,
    services: [{ name: 'Backend API', status: 'operational', uptime: '-' }],
  }),
}));

// Mock useAuth hook
vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: { roles: ['ADMIN'] },
    isAuthenticated: true,
  }),
}));

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        {children}
      </MemoryRouter>
    </QueryClientProvider>
  );
};

describe('HomePage', () => {
  it('renders portal heading', () => {
    render(<HomePage />, { wrapper: createWrapper() });
    const heading = screen.getByRole('heading', { name: 'ポータル' });
    expect(heading).toBeInTheDocument();
  });

  it('renders mirelplatform eyebrow text', () => {
    render(<HomePage />, { wrapper: createWrapper() });
    expect(screen.getByText('mirelplatform')).toBeInTheDocument();
  });

  it('provides CTA links to products', () => {
    render(<HomePage />, { wrapper: createWrapper() });
    const link = screen.getByRole('link', { name: /製品ラインナップを見る/i });
    expect(link).toHaveAttribute('href', '/products');
  });
});
