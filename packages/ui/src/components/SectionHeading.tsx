import * as React from 'react'

import { cn } from '../lib/utils'

export interface SectionHeadingProps extends React.HTMLAttributes<HTMLDivElement> {
  eyebrow?: string
  title: string
  description?: string
  actions?: React.ReactNode
}

const SectionHeading = React.forwardRef<HTMLDivElement, SectionHeadingProps>(
  ({ eyebrow, title, description, actions, className, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn('flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between', className)}
        {...props}
      >
        <div className="space-y-2">
          {eyebrow ? (
            <span className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
              {eyebrow}
            </span>
          ) : null}
          <div className="space-y-2">
            <h2 className="text-3xl font-semibold tracking-tight text-foreground sm:text-4xl">{title}</h2>
            {description ? (
              <p className="max-w-2xl text-sm text-muted-foreground sm:text-base">{description}</p>
            ) : null}
          </div>
        </div>
        {actions ? <div className="flex items-center gap-3">{actions}</div> : null}
      </div>
    )
  }
)
SectionHeading.displayName = 'SectionHeading'

export { SectionHeading }
