import { describe, expect, it } from 'vitest'
import { render } from '@testing-library/react'

import { StepIndicator } from '../StepIndicator'

describe('StepIndicator', () => {
  it('marks steps with semantic states', () => {
    const { getAllByRole } = render(
      <StepIndicator
        steps={[
          { id: 'select', title: '選択', state: 'complete' },
          { id: 'details', title: '詳細', state: 'current' },
          { id: 'execute', title: '実行', state: 'upcoming' },
        ]}
      />
    )

    const items = getAllByRole('listitem')

    expect(items[0]).toHaveAttribute('data-state', 'complete')
    expect(items[1]).toHaveAttribute('data-state', 'current')
    expect(items[2]).toHaveAttribute('data-state', 'upcoming')
  })
})
