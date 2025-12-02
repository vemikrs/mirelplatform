import * as React from 'react'
import { cva, type VariantProps } from 'class-variance-authority'

import { cn } from '../lib/utils'

const badgeVariants = cva(
  'inline-flex items-center gap-1 rounded-full px-3 py-0.5 text-xs font-medium tracking-wide uppercase',
  {
    variants: {
      variant: {
        neutral: 'bg-surface-subtle text-muted-foreground ring-1 ring-inset ring-outline/60',
        outline:
          'border border-dashed border-outline/80 bg-transparent text-muted-foreground shadow-none',
        info: 'bg-info text-info-foreground',
        success: 'bg-success text-success-foreground',
        warning: 'bg-warning text-warning-foreground',
        destructive: 'bg-destructive text-destructive-foreground',
      },
    },
    defaultVariants: {
      variant: 'neutral',
    },
  }
)

export interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {}

const Badge = React.forwardRef<HTMLSpanElement, BadgeProps>(
  ({ className, variant, ...props }, ref) => {
    return (
      <span ref={ref} className={cn('select-none', badgeVariants({ variant, className }))} {...props} />
    )
  }
)
Badge.displayName = 'Badge'

export { Badge, badgeVariants }
