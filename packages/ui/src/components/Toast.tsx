import * as React from 'react'
import * as ToastPrimitives from '@radix-ui/react-toast'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '../lib/utils'

const ToastProvider = ToastPrimitives.Provider

const ToastViewport = React.forwardRef<
  React.ElementRef<typeof ToastPrimitives.Viewport>,
  React.ComponentPropsWithoutRef<typeof ToastPrimitives.Viewport>
>(({ className, ...props }, ref) => (
  <ToastPrimitives.Viewport
    ref={ref}
    className={cn(
      'fixed inset-x-4 bottom-4 z-[100] flex max-h-[calc(100vh-2rem)] flex-col gap-3 p-0 sm:inset-auto sm:right-6 sm:bottom-6 sm:w-[380px]',
      className
    )}
    {...props}
  />
))
ToastViewport.displayName = ToastPrimitives.Viewport.displayName

const toastVariants = cva(
  'group pointer-events-auto relative flex w-full items-start gap-4 overflow-hidden rounded-xl border border-outline/60 bg-surface-raised p-4 shadow-lg backdrop-blur data-[state=open]:animate-toast-in data-[state=closed]:animate-toast-out data-[swipe=end]:animate-toast-out',
  {
    variants: {
      variant: {
        default: 'text-foreground',
        destructive: 'border-destructive/40 bg-destructive text-destructive-foreground',
        info: 'border-info/40 bg-info text-info-foreground',
        success: 'border-success/50 bg-success text-success-foreground',
        warning: 'border-warning/50 bg-warning text-warning-foreground',
        loading:
          'border-outline/50 bg-surface-raised/90 text-foreground shadow-md animate-pulse',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  }
)

const Toast = React.forwardRef<
  React.ElementRef<typeof ToastPrimitives.Root>,
  React.ComponentPropsWithoutRef<typeof ToastPrimitives.Root> & VariantProps<typeof toastVariants>
>(({ className, variant, ...props }, ref) => {
  return (
    <ToastPrimitives.Root ref={ref} className={cn(toastVariants({ variant }), className)} {...props} />
  )
})
Toast.displayName = ToastPrimitives.Root.displayName

const ToastAction = React.forwardRef<
  React.ElementRef<typeof ToastPrimitives.Action>,
  React.ComponentPropsWithoutRef<typeof ToastPrimitives.Action>
>(({ className, ...props }, ref) => (
  <ToastPrimitives.Action
    ref={ref}
    className={cn(
      'inline-flex h-9 shrink-0 items-center justify-center gap-2 rounded-full border border-outline/60 bg-transparent px-3 text-sm font-medium transition-colors hover:bg-foreground/10 focus:outline-none focus:ring-2 focus:ring-focus-ring focus:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 group-[.destructive]:border-destructive/40 group-[.destructive]:hover:bg-destructive/20 group-[.destructive]:focus:ring-destructive group-[.info]:border-info/50 group-[.success]:border-success/50 group-[.warning]:border-warning/60',
      className
    )}
    {...props}
  />
))
ToastAction.displayName = ToastPrimitives.Action.displayName

const ToastClose = React.forwardRef<
  React.ElementRef<typeof ToastPrimitives.Close>,
  React.ComponentPropsWithoutRef<typeof ToastPrimitives.Close>
>(({ className, ...props }, ref) => (
  <ToastPrimitives.Close
    ref={ref}
    className={cn(
      'absolute right-3 top-3 rounded-full p-1.5 text-foreground/60 transition-opacity hover:text-foreground focus:outline-none focus:ring-2 focus:ring-focus-ring focus:ring-offset-2 group-hover:opacity-100 group-data-[state=open]:opacity-0 group-hover:data-[state=open]:opacity-100 group-[.destructive]:text-destructive-foreground group-[.destructive]:hover:text-destructive-foreground/80 group-[.info]:text-info-foreground group-[.success]:text-success-foreground group-[.warning]:text-warning-foreground',
      className
    )}
    toast-close=""
    {...props}
  >
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="16"
      height="16"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-4 w-4"
    >
      <line x1="18" y1="6" x2="6" y2="18" />
      <line x1="6" y1="6" x2="18" y2="18" />
    </svg>
  </ToastPrimitives.Close>
))
ToastClose.displayName = ToastPrimitives.Close.displayName

const ToastTitle = React.forwardRef<
  React.ElementRef<typeof ToastPrimitives.Title>,
  React.ComponentPropsWithoutRef<typeof ToastPrimitives.Title>
>(({ className, ...props }, ref) => (
  <ToastPrimitives.Title ref={ref} className={cn('flex items-center gap-2 text-sm font-semibold', className)} {...props} />
))
ToastTitle.displayName = ToastPrimitives.Title.displayName

const ToastDescription = React.forwardRef<
  React.ElementRef<typeof ToastPrimitives.Description>,
  React.ComponentPropsWithoutRef<typeof ToastPrimitives.Description>
>(({ className, ...props }, ref) => (
  <ToastPrimitives.Description ref={ref} className={cn('text-sm text-foreground/80', className)} {...props} />
))
ToastDescription.displayName = ToastPrimitives.Description.displayName

type ToastProps = React.ComponentPropsWithoutRef<typeof Toast>

type ToastActionElement = React.ReactElement<typeof ToastAction>

export {
  type ToastProps,
  type ToastActionElement,
  ToastProvider,
  ToastViewport,
  Toast,
  ToastTitle,
  ToastDescription,
  ToastClose,
  ToastAction,
  toastVariants,
}
