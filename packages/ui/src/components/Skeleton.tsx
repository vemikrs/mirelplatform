import * as React from 'react'

import { cn } from '../lib/utils'

const Skeleton = React.forwardRef<HTMLDivElement, React.HTMLAttributes<HTMLDivElement>>(
  ({ className, ...props }, ref) => (
    <div
      ref={ref}
      className={cn('animate-shimmer rounded-md bg-surface-subtle/80 bg-gradient-to-r from-surface-subtle via-surface-raised/80 to-surface-subtle', className)}
      {...props}
    />
  )
)
Skeleton.displayName = 'Skeleton'

export { Skeleton }
