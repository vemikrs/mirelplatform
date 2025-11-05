import { describe, expect, it } from 'vitest'

import {
  colors,
  surfaces,
  typography,
  shadows,
  motion,
  spacing,
  borderRadius,
} from '../tokens'

describe('design tokens', () => {
  it('exposes semantic color tokens for enterprise use cases', () => {
    expect(colors).toMatchObject({
      border: expect.any(String),
      outline: expect.any(String),
      focus: expect.any(Object),
      info: expect.any(Object),
      success: expect.any(Object),
      warning: expect.any(Object),
    })
  })

  it('provides surface hierarchy tokens', () => {
    expect(surfaces).toMatchObject({
      base: expect.any(String),
      raised: expect.any(String),
      overlay: expect.any(String),
    })
  })

  it('provides typography scale entries', () => {
    expect(typography.text).toMatchObject({
      xs: expect.any(Object),
      sm: expect.any(Object),
      md: expect.any(Object),
      lg: expect.any(Object),
      xl: expect.any(Object),
    })
  })

  it('provides motion durations and easing curves', () => {
    expect(motion).toMatchObject({
      duration: expect.objectContaining({
        fast: expect.any(String),
        normal: expect.any(String),
        slow: expect.any(String),
      }),
      easing: expect.objectContaining({
        standard: expect.any(String),
        emphasized: expect.any(String),
      }),
    })
  })

  it('retains spacing scale and radius tokens', () => {
    expect(spacing.md).toBeDefined()
    expect(borderRadius.lg).toBeDefined()
  })

  it('provides elevated shadow tokens', () => {
    expect(shadows).toMatchObject({
      sm: expect.any(String),
      md: expect.any(String),
      lg: expect.any(String),
      xl: expect.any(String),
    })
  })
})
