import type { ComponentType } from 'react'
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import type { NavigationConfig } from '@/app/navigation.schema'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useLoaderData: () => mockNavigation,
  }
})

const mockNavigation: NavigationConfig = {
  brand: {
    name: 'mirelplatform',
    shortName: 'Mirel',
  },
  primary: [
    { id: 'home', label: 'ホーム', path: '/' },
    { id: 'promarker', label: 'ProMarker', path: '/promarker' },
    { id: 'catalog', label: 'UIカタログ', path: '/catalog' },
    { id: 'sitemap', label: 'サイトマップ', path: '/sitemap' },
  ],
  secondary: [],
  quickLinks: [],
  globalActions: [
    { id: 'theme-toggle', type: 'theme' },
  ],
}

let RootLayout: ComponentType

describe('RootLayout', () => {
  beforeAll(async () => {
    RootLayout = (await import('../RootLayout')).RootLayout
  })

  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    cleanup()
  })

  afterAll(() => {
    vi.resetModules()
  })

  it('renders navigation links from configuration with active state', () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/catalog']}>
        <Routes>
          <Route element={<RootLayout />}>
            <Route path="/catalog" element={<div>Catalog page</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    )

    const catalogLinks = screen.getAllByRole('link', { name: 'UIカタログ' })
    expect(catalogLinks.some((link) => link.getAttribute('aria-current') === 'page')).toBe(true)
    expect(container.querySelector('header')).toBeTruthy()
    expect(screen.getAllByLabelText('テーマ切替').length).toBeGreaterThan(0)
  })
})
