import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { UiCatalogPage } from './UiCatalogPage'

describe('UiCatalogPage', () => {
  it('displays component catalog sections', () => {
    render(<UiCatalogPage />)

    expect(screen.getByRole('heading', { name: 'UI コンポーネントカタログ' })).toBeInTheDocument()
    const demoCards = screen.getAllByTestId('catalog-demo-card')
    expect(demoCards.length).toBeGreaterThanOrEqual(3)
  })
})
