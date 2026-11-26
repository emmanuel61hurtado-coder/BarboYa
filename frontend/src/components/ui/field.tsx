import * as React from 'react'
import * as LabelPrimitive from '@radix-ui/react-label'
import { cn } from '../../utils/cn'

const control = 'w-full rounded-xl pro-input px-3.5 py-2.5 text-sm aria-[invalid=true]:border-rose-500'

interface FieldProps {
  label: string
  error?: string
  hint?: string
}

function Wrapper({ id, label, error, hint, children }: FieldProps & { id: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1.5">
      <LabelPrimitive.Root htmlFor={id} className="block text-xs font-medium text-slate-300">
        {label}
      </LabelPrimitive.Root>
      {children}
      {error ? (
        <p id={`${id}-message`} role="alert" className="text-xs text-rose-400">
          {error}
        </p>
      ) : (
        hint && (
          <p id={`${id}-message`} className="text-xs text-slate-500">
            {hint}
          </p>
        )
      )}
    </div>
  )
}

export const TextField = React.forwardRef<HTMLInputElement, FieldProps & React.InputHTMLAttributes<HTMLInputElement>>(
  ({ label, error, hint, className, ...props }, ref) => {
    const id = React.useId()
    return (
      <Wrapper id={id} label={label} error={error} hint={hint}>
        <input
          ref={ref}
          id={id}
          className={cn(control, className)}
          aria-invalid={Boolean(error)}
          aria-describedby={error || hint ? `${id}-message` : undefined}
          {...props}
        />
      </Wrapper>
    )
  },
)
TextField.displayName = 'TextField'

export const SelectField = React.forwardRef<
  HTMLSelectElement,
  FieldProps & React.SelectHTMLAttributes<HTMLSelectElement>
>(({ label, error, hint, className, children, ...props }, ref) => {
  const id = React.useId()
  return (
    <Wrapper id={id} label={label} error={error} hint={hint}>
      <select
        ref={ref}
        id={id}
        className={cn(control, className)}
        aria-invalid={Boolean(error)}
        aria-describedby={error || hint ? `${id}-message` : undefined}
        {...props}
      >
        {children}
      </select>
    </Wrapper>
  )
})
SelectField.displayName = 'SelectField'
