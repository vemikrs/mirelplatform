import { describe, expect, it } from 'vitest'

import { buttonVariants } from '../Button'

describe('Button component variants', () => {
  it('includes subtle, soft, and elevated variants for enterprise layouts', () => {
    const subtle = buttonVariants({ variant: 'subtle' })
    const soft = buttonVariants({ variant: 'soft' })
    const elevated = buttonVariants({ variant: 'elevated' })

    expect(subtle).toContain('bg-surface-subtle')
    expect(soft).toContain('bg-primary/10')
    expect(elevated).toContain('shadow-md')
  })

  it('exposes pill and square size options', () => {
    const pill = buttonVariants({ size: 'pill' })
    const square = buttonVariants({ size: 'square' })

    expect(pill).toContain('rounded-full')
    expect(square).toContain('size-9')
  })
})
