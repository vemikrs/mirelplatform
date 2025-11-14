import * as React from 'react'

import { cn } from '../lib/utils'

const FormField = React.forwardRef<HTMLDivElement, React.HTMLAttributes<HTMLDivElement>>(
  ({ className, ...props }, ref) => (
    <div ref={ref} className={cn('space-y-2', className)} {...props} />
  )
)
FormField.displayName = 'FormField'

const FormLabel = React.forwardRef<
  HTMLLabelElement,
  React.LabelHTMLAttributes<HTMLLabelElement> & { requiredMark?: React.ReactNode }
>(({ className, requiredMark, children, ...props }, ref) => (
  <label
    ref={ref}
    className={cn(
      'flex items-center gap-2 text-sm font-medium text-foreground',
      className
    )}
    {...props}
  >
    <span>{children}</span>
    {requiredMark ?? null}
  </label>
))
FormLabel.displayName = 'FormLabel'

const FormHelper = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => (
  <p ref={ref} className={cn('text-xs text-muted-foreground', className)} {...props} />
))
FormHelper.displayName = 'FormHelper'

const FormError = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => (
  <p ref={ref} className={cn('flex items-center gap-1 text-xs text-destructive', className)} {...props} />
))
FormError.displayName = 'FormError'

const FormRequiredMark = ({
  children = '*',
  className,
}: {
  children?: React.ReactNode
  className?: string
}) => (
  <span className={cn('text-destructive', className)} aria-hidden>
    {children}
  </span>
)
FormRequiredMark.displayName = 'FormRequiredMark'

export { FormError, FormField, FormHelper, FormLabel, FormRequiredMark }
