import { describe, expect, it } from 'vitest'

import { toastVariants } from '../Toast'

describe('Toast variants', () => {
  it('supports info, success, warning, and loading variants', () => {
    const info = toastVariants({ variant: 'info' })
    const success = toastVariants({ variant: 'success' })
    const warning = toastVariants({ variant: 'warning' })
    const loading = toastVariants({ variant: 'loading' })

    expect(info).toContain('bg-info')
    expect(success).toContain('bg-success')
    expect(warning).toContain('bg-warning')
    expect(loading).toContain('animate-pulse')
  })
})
