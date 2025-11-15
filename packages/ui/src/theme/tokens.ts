/**
 * Design tokens for @mirel/ui
 * Aligned with shadcn/ui CSS variable conventions
 */

export const colors = {
  border: 'hsl(var(--border))',
  input: 'hsl(var(--input))',
  ring: 'hsl(var(--ring))',
  outline: 'hsl(var(--outline))',
  background: 'hsl(var(--background))',
  foreground: 'hsl(var(--foreground))',
  primary: {
    DEFAULT: 'hsl(var(--primary))',
    foreground: 'hsl(var(--primary-foreground))',
  },
  secondary: {
    DEFAULT: 'hsl(var(--secondary))',
    foreground: 'hsl(var(--secondary-foreground))',
  },
  destructive: {
    DEFAULT: 'hsl(var(--destructive))',
    foreground: 'hsl(var(--destructive-foreground))',
  },
  info: {
    DEFAULT: 'hsl(var(--info))',
    foreground: 'hsl(var(--info-foreground))',
  },
  success: {
    DEFAULT: 'hsl(var(--success))',
    foreground: 'hsl(var(--success-foreground))',
  },
  warning: {
    DEFAULT: 'hsl(var(--warning))',
    foreground: 'hsl(var(--warning-foreground))',
  },
  muted: {
    DEFAULT: 'hsl(var(--muted))',
    foreground: 'hsl(var(--muted-foreground))',
  },
  accent: {
    DEFAULT: 'hsl(var(--accent))',
    foreground: 'hsl(var(--accent-foreground))',
  },
  popover: {
    DEFAULT: 'hsl(var(--popover))',
    foreground: 'hsl(var(--popover-foreground))',
  },
  card: {
    DEFAULT: 'hsl(var(--card))',
    foreground: 'hsl(var(--card-foreground))',
  },
  focus: {
    ring: 'hsl(var(--focus-ring))',
    inner: 'hsl(var(--focus-inner))',
  },
} as const

export const surfaces = {
  base: 'hsl(var(--surface))',
  subtle: 'hsl(var(--surface-subtle))',
  raised: 'hsl(var(--surface-raised))',
  overlay: 'hsl(var(--surface-overlay))',
} as const

export const borderRadius = {
  lg: 'var(--radius)',
  md: 'calc(var(--radius) - 2px)',
  sm: 'calc(var(--radius) - 4px)',
} as const

export const spacing = {
  xxs: '0.125rem',
  xs: '0.25rem',
  sm: '0.5rem',
  md: '1rem',
  lg: '1.5rem',
  xl: '2rem',
  '2xl': '3rem',
} as const

export const typography = {
  fontFamily: {
    sans:
      "'Inter', 'Noto Sans JP', 'Helvetica Neue', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
    mono: "'JetBrains Mono', 'Fira Code', ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace",
  },
  text: {
    xs: { size: '0.75rem', lineHeight: '1.125rem', tracking: '0.02em' },
    sm: { size: '0.875rem', lineHeight: '1.25rem', tracking: '0.01em' },
    md: { size: '1rem', lineHeight: '1.5rem', tracking: '0' },
    lg: { size: '1.125rem', lineHeight: '1.75rem', tracking: '-0.005em' },
    xl: { size: '1.25rem', lineHeight: '1.85rem', tracking: '-0.01em' },
  },
  heading: {
    sm: { size: '1.5rem', lineHeight: '2.1rem', weight: 600, tracking: '-0.02em' },
    md: { size: '1.875rem', lineHeight: '2.35rem', weight: 600, tracking: '-0.025em' },
    lg: { size: '2.25rem', lineHeight: '2.6rem', weight: 600, tracking: '-0.03em' },
  },
} as const

export const shadows = {
  sm: '0 1px 2px rgba(15, 23, 42, 0.08)',
  md: '0 8px 16px rgba(15, 23, 42, 0.08)',
  lg: '0 12px 32px rgba(15, 23, 42, 0.12)',
  xl: '0 20px 45px rgba(15, 23, 42, 0.16)',
} as const

export const motion = {
  duration: {
    instant: '80ms',
    fast: '160ms',
    normal: '240ms',
    slow: '320ms',
  },
  easing: {
    standard: 'cubic-bezier(0.2, 0, 0, 1)',
    emphasized: 'cubic-bezier(0.32, 0.72, 0, 1)',
    entrance: 'cubic-bezier(0, 0, 0.2, 1)',
    exit: 'cubic-bezier(0.4, 0, 1, 1)',
  },
} as const
