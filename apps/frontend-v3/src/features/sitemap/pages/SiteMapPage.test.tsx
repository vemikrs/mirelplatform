import type { NavigationConfig } from '@/app/navigation.schema'
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'

const mockNavigation: NavigationConfig = {
  brand: { name: 'Mirel Platform' },
  primary: [
    { id: 'home', label: 'ホーム', path: '/' },
    { id: 'promarker', label: 'ProMarker', path: '/promarker' },
    { id: 'catalog', label: 'UIカタログ', path: '/catalog' }
  ],
  secondary: [],
  quickLinks: [],
  globalActions: [],
}

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useRouteLoaderData: () => mockNavigation,
  }
})

describe('SiteMapPage', () => {
  afterEach(() => {
    cleanup()
  })

  it('lists navigation destinations', async () => {
    const { SiteMapPage } = await import('./SiteMapPage')
    render(
      <MemoryRouter>
        <SiteMapPage />
      </MemoryRouter>
    )

    expect(screen.getByRole('heading', { name: 'サイトマップ' })).toBeInTheDocument()
    expect(screen.getByText('ProMarker')).toBeInTheDocument()
    expect(screen.getAllByRole('link').length).toBeGreaterThanOrEqual(3)
  })
})
