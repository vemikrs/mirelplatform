import * as React from 'react'
import { cva, type VariantProps } from 'class-variance-authority'

import { cn } from '../lib/utils'

const badgeVariants = cva(
  'inline-flex items-center gap-1 rounded-full font-medium tracking-wide uppercase',
  {
    variants: {
      variant: {
        default: 'bg-primary text-primary-foreground hover:bg-primary/80',
        secondary: 'bg-secondary text-secondary-foreground hover:bg-secondary/80',
        neutral: 'bg-surface-subtle text-muted-foreground ring-1 ring-inset ring-outline/60',
        outline:
          'border border-dashed border-outline/80 bg-transparent text-muted-foreground shadow-none',
        info: 'bg-info text-info-foreground',
        success: 'bg-success text-success-foreground',
        warning: 'bg-warning text-warning-foreground',
        destructive: 'bg-destructive text-destructive-foreground',
      },
      size: {
        default: 'px-3 py-0.5 text-xs',
        sm: 'px-2 py-0.5 text-[10px]',
        lg: 'px-4 py-1 text-sm',
      },
    },
    defaultVariants: {
      variant: 'neutral',
      size: 'default',
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
