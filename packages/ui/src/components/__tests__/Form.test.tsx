import { describe, expect, it } from 'vitest'
import { render } from '@testing-library/react'

import { FormError, FormField, FormHelper, FormLabel } from '../Form'

describe('Form primitives', () => {
  it('apply consistent spacing and typography tokens', () => {
    const { container, getByText } = render(
      <FormField>
        <FormLabel htmlFor="id">Label</FormLabel>
        <FormHelper>Helper</FormHelper>
        <FormError>Error</FormError>
      </FormField>
    )

    expect(container.firstChild).toHaveClass('space-y-2')
    expect(getByText('Label').closest('label')?.className).toContain('text-sm')
    expect(getByText('Helper').className).toContain('text-muted-foreground')
    expect(getByText('Error').className).toContain('text-destructive')
  })
})
