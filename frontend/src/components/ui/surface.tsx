import * as React from 'react'
import { cva, VariantProps } from 'class-variance-authority'
import { cn } from '../../utils/cn'

export function Card({ title, description, className, children }: {
  title?: string
  description?: string
  className?: string
  children: React.ReactNode
}) {
  return (
    <section className={cn('pro-surface-elevated rounded-3xl p-5 sm:p-6 space-y-4', className)}>
      {(title || description) && (
        <header className="space-y-1">
          {title && <h2 className="text-lg font-bold text-white">{title}</h2>}
          {description && <p className="text-xs text-slate-400">{description}</p>}
        </header>
      )}
      {children}
    </section>
  )
}

const alertVariants = cva('rounded-2xl border p-3.5 text-sm', {
  variants: {
    variant: {
      info: 'border-sky-500/30 bg-sky-500/10 text-sky-200',
      success: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-200',
      warning: 'border-amber-500/30 bg-amber-500/10 text-amber-200',
      error: 'border-rose-500/30 bg-rose-500/10 text-rose-200',
    },
  },
  defaultVariants: { variant: 'info' },
})

export function Alert({ variant, className, children }: VariantProps<typeof alertVariants> & {
  className?: string
  children: React.ReactNode
}) {
  return (
    <div role={variant === 'error' ? 'alert' : 'status'} className={cn(alertVariants({ variant }), className)}>
      {children}
    </div>
  )
}

/** Muestra el mensaje de un error de la API; no dibuja nada si no hay error. */
export function ErrorAlert({ error }: { error: unknown }) {
  if (!error) return null
  return <Alert variant="error">{error instanceof Error ? error.message : 'Ocurrió un error inesperado'}</Alert>
}

export function Pager({ page, size, total, onChange }: {
  page: number
  size: number
  total: number
  onChange: (page: number) => void
}) {
  const pages = Math.max(1, Math.ceil(total / size))
  return (
    <div className="flex items-center justify-between text-xs text-slate-400">
      <span>
        Página {page + 1} de {pages} · {total} en total
      </span>
      <div className="flex gap-2">
        <button
          type="button"
          className="rounded-lg border border-slate-700 px-3 py-1.5 disabled:opacity-40"
          disabled={page === 0}
          onClick={() => onChange(page - 1)}
        >
          Anterior
        </button>
        <button
          type="button"
          className="rounded-lg border border-slate-700 px-3 py-1.5 disabled:opacity-40"
          disabled={page + 1 >= pages}
          onClick={() => onChange(page + 1)}
        >
          Siguiente
        </button>
      </div>
    </div>
  )
}
