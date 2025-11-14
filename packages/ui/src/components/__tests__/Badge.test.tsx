import { describe, expect, it } from 'vitest'

import { badgeVariants } from '../Badge'

describe('Badge variants', () => {
  it('supports semantic variants for enterprise use', () => {
    expect(badgeVariants({ variant: 'neutral' })).toContain('bg-surface-subtle')
    expect(badgeVariants({ variant: 'info' })).toContain('bg-info')
    expect(badgeVariants({ variant: 'success' })).toContain('bg-success')
    expect(badgeVariants({ variant: 'warning' })).toContain('bg-warning')
  })
})
