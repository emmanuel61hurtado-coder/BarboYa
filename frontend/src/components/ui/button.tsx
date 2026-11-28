import * as React from 'react'
import { Slot } from '@radix-ui/react-slot'
import { cva, VariantProps } from 'class-variance-authority'
import { cn } from '../../utils/cn'

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 rounded-xl text-sm font-semibold transition cursor-pointer ' +
    'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400 disabled:opacity-50 disabled:cursor-not-allowed',
  {
    variants: {
      variant: {
        primary: 'bg-sky-600 text-white hover:bg-sky-500',
        secondary: 'bg-slate-800 text-slate-200 border border-slate-700 hover:bg-slate-700',
        destructive: 'bg-rose-600 text-white hover:bg-rose-500',
        link: 'text-sky-400 underline-offset-4 hover:underline',
      },
      size: {
        sm: 'px-3 py-1.5 text-xs',
        md: 'px-4 py-2.5',
        inline: 'p-0',
      },
    },
    defaultVariants: { variant: 'primary', size: 'md' },
  },
)

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean
  loading?: boolean
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild, loading, disabled, children, type = 'button', ...props }, ref) => {
    const classes = cn(buttonVariants({ variant, size }), className)
    if (asChild) {
      return (
        <Slot ref={ref} className={classes} {...props}>
          {children}
        </Slot>
      )
    }
    return (
      <button ref={ref} type={type} className={classes} disabled={disabled || loading} aria-busy={loading} {...props}>
        {loading ? 'Un momento…' : children}
      </button>
    )
  },
)
Button.displayName = 'Button'
