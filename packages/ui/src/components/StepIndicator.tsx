import * as React from 'react'

import { cn } from '../lib/utils'
import { Badge } from './Badge'

export type StepState = 'complete' | 'current' | 'upcoming'

export interface StepIndicatorStep {
  id: string
  title: string
  description?: string
  state?: StepState
}

export interface StepIndicatorProps extends React.HTMLAttributes<HTMLOListElement> {
  steps: StepIndicatorStep[]
}

const stateStyles: Record<StepState, string> = {
  complete: 'border-success/40 bg-success/15 text-success-foreground',
  current: 'border-primary/40 bg-primary/10 text-primary',
  upcoming: 'border-outline/60 bg-surface-subtle text-muted-foreground',
}

const stateBadge: Record<StepState, React.ReactNode> = {
  complete: <Badge variant="success">完了</Badge>,
  current: <Badge variant="info">進行中</Badge>,
  upcoming: <Badge variant="neutral">準備中</Badge>,
}

const StepIndicator = React.forwardRef<HTMLOListElement, StepIndicatorProps>(
  ({ steps, className, ...props }, ref) => {
    return (
      <ol
        ref={ref}
        className={cn(
          'grid grid-cols-3 gap-3 rounded-xl border border-outline/60 bg-surface-subtle p-4 shadow-sm',
          className
        )}
        {...props}
      >
        {steps.map((step) => {
          const state: StepState = step.state ?? 'upcoming'

          return (
            <li
              key={step.id}
              className={cn(
                'rounded-lg border p-4 transition-colors',
                stateStyles[state]
              )}
              data-state={state}
            >
              <div className="flex items-center justify-between gap-2">
                <span className="text-sm font-semibold tracking-wide text-foreground">
                  {step.title}
                </span>
                {stateBadge[state]}
              </div>
              {step.description ? (
                <p className="mt-2 text-xs text-muted-foreground">{step.description}</p>
              ) : null}
            </li>
          )
        })}
      </ol>
    )
  }
)
StepIndicator.displayName = 'StepIndicator'

export { StepIndicator }
