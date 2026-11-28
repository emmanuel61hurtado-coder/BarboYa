import { useState } from 'react'
import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import type { z } from 'zod'
import { Movement, Statement, financeApi } from '../../services/api/finance'
import { useCurrentUser } from '../../contexts/AuthContext'
import { useSubmit } from '../../hooks/useSubmit'
import { paymentSchema, waiveSchema } from '../../validations/finance'
import { SelectField, TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Alert, Card, ErrorAlert } from '../../components/ui/surface'
import { formatCop } from '../../utils/commerce'
import { ROLE_LABELS } from '../../utils/labels'
import { StatementView } from './StatementView'
import { METHOD_LABELS } from './labels'

export function AdminFinancePage() {
  const [selected, setSelected] = useState<{ id: string; name: string } | null>(null)
  const summary = useQuery({ queryKey: ['admin', 'finance', 'summary'], queryFn: financeApi.summary, staleTime: 0 })
  const data = summary.data

  return (
    <>
      <h1 className="text-2xl font-display font-black">Finanzas</h1>
      <ErrorAlert error={summary.error} />
      {summary.isPending && <p className="text-sm text-slate-400">Cargando…</p>}
      {data && (
        <>
          <Card title="Pedidos entregados">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="text-left text-xs text-slate-500">
                  <tr>
                    <th className="py-1 pr-3">Periodo</th>
                    <th className="py-1 pr-3">Pedidos</th>
                    <th className="py-1 pr-3">Ventas</th>
                    <th className="py-1">Comisiones</th>
                  </tr>
                </thead>
                <tbody>
                  {(
                    [
                      ['Hoy', data.today],
                      ['7 días', data.last7Days],
                      ['30 días', data.last30Days],
                    ] as const
                  ).map(([label, period]) => (
                    <tr key={label} className="border-t border-slate-800">
                      <th scope="row" className="py-2 pr-3 text-left font-medium">
                        {label}
                      </th>
                      <td className="py-2 pr-3">{period.orders}</td>
                      <td className="py-2 pr-3">{formatCop(period.salesCop)}</td>
                      <td className="py-2">{formatCop(period.commissionsCop)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <dl className="grid gap-2 sm:grid-cols-3">
              {(
                [
                  ['Por cobrar', data.outstandingCop],
                  ['Cobrado', data.collectedCop],
                  ['Perdonado', data.waivedCop],
                ] as const
              ).map(([term, value]) => (
                <div key={term} aria-label={term} className="rounded-xl border border-slate-700 p-3">
                  <dt className="text-xs text-slate-500">{term}</dt>
                  <dd className="text-lg font-bold">{formatCop(value)}</dd>
                </div>
              ))}
            </dl>
          </Card>
          <Card title="Cuentas con deuda">
            {data.debtors.length === 0 && <p className="text-sm text-slate-400">Nadie debe comisiones.</p>}
            <ul className="divide-y divide-slate-800">
              {data.debtors.map((debtor) => (
                <li key={debtor.partyId} aria-label={debtor.name} className="flex flex-wrap items-center justify-between gap-2 py-2">
                  <div>
                    <p className="text-sm font-semibold">{debtor.name}</p>
                    <p className="text-xs text-slate-500">
                      {ROLE_LABELS[debtor.role]}
                      {debtor.blocked && <span className="text-amber-300"> · Sobre el tope</span>}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="font-bold">{formatCop(debtor.balanceCop)}</span>
                    <Button size="sm" variant="secondary" onClick={() => setSelected({ id: debtor.partyId, name: debtor.name })}>
                      Ver cuenta
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          </Card>
        </>
      )}
      {selected && <Account key={selected.id} {...selected} onClose={() => setSelected(null)} />}
    </>
  )
}

/** Tras cualquier movimiento se actualizan la cuenta y el resumen. */
function useApply(id: string) {
  const queryClient = useQueryClient()
  return async (statement: Statement) => {
    queryClient.setQueryData(['admin', 'finance', 'account', id, 0], statement)
    await queryClient.invalidateQueries({ queryKey: ['admin'] })
  }
}

function Account({ id, name, onClose }: { id: string; name: string; onClose: () => void }) {
  const actor = useCurrentUser()
  const boss = actor.role === 'SUPERADMIN'
  const [page, setPage] = useState(0)
  const statement = useQuery({
    queryKey: ['admin', 'finance', 'account', id, page],
    queryFn: () => financeApi.account(id, page),
    placeholderData: keepPreviousData,
    staleTime: 0,
  })

  return (
    <section aria-label={`Cuenta de ${name}`} className="pro-surface-elevated rounded-3xl p-5 space-y-5">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-lg font-bold">Cuenta de {name}</h2>
        <Button variant="secondary" size="sm" onClick={onClose}>
          Cerrar
        </Button>
      </div>
      <ErrorAlert error={statement.error} />
      {statement.data && (
        <>
          {statement.data.balanceCop > 0 && <PaymentForm id={id} />}
          {boss && statement.data.balanceCop > 0 && <WaiveForm id={id} />}
          <StatementView
            statement={statement.data}
            onPage={setPage}
            actions={(movement) =>
              boss && !movement.reversed && (movement.kind === 'PAYMENT' || movement.kind === 'ADJUSTMENT') ? (
                <Reversal id={id} movement={movement} />
              ) : null
            }
          />
        </>
      )}
    </section>
  )
}

function PaymentForm({ id }: { id: string }) {
  const apply = useApply(id)
  const [done, setDone] = useState(false)
  const form = useForm<z.infer<typeof paymentSchema>>({
    resolver: zodResolver(paymentSchema),
    defaultValues: { method: 'BANK_TRANSFER', reference: '', note: '' },
  })
  const { submit, error, sending } = useSubmit(form, async (values) => {
    setDone(false)
    await apply(await financeApi.registerPayment(id, values))
    form.reset()
    setDone(true)
  })
  const { errors } = form.formState

  return (
    <form onSubmit={submit} noValidate className="space-y-3 rounded-2xl border border-slate-700 p-4">
      <h3 className="font-semibold">Registrar un pago recibido</h3>
      <p className="text-xs text-slate-500">
        Verifica en el banco o en Nequi que el dinero llegó antes de registrarlo.
      </p>
      <div className="grid gap-3 sm:grid-cols-3">
        <TextField
          label="Valor recibido (COP)"
          type="number"
          min={1}
          error={errors.amountCop?.message}
          {...form.register('amountCop', { valueAsNumber: true })}
        />
        <SelectField label="Medio" {...form.register('method')}>
          {paymentSchema.shape.method.options.map((value) => (
            <option key={value} value={value}>
              {METHOD_LABELS[value]}
            </option>
          ))}
        </SelectField>
        <TextField label="Referencia del comprobante" error={errors.reference?.message} {...form.register('reference')} />
      </div>
      <ErrorAlert error={error} />
      {done && <Alert variant="success">Pago registrado.</Alert>}
      <Button type="submit" size="sm" loading={sending}>
        Registrar pago
      </Button>
    </form>
  )
}

function WaiveForm({ id }: { id: string }) {
  const apply = useApply(id)
  const [open, setOpen] = useState(false)
  const form = useForm<z.infer<typeof waiveSchema>>({ resolver: zodResolver(waiveSchema), defaultValues: { reason: '' } })
  const { submit, error, sending } = useSubmit(form, async ({ amountCop, reason }) => {
    await apply(await financeApi.waive(id, amountCop, reason))
    setOpen(false)
  })

  if (!open) {
    return (
      <Button variant="secondary" size="sm" onClick={() => setOpen(true)}>
        Perdonar deuda
      </Button>
    )
  }
  return (
    <form onSubmit={submit} noValidate className="space-y-3 rounded-2xl border border-slate-700 p-4">
      <div className="grid gap-3 sm:grid-cols-2">
        <TextField
          label="Valor a perdonar (COP)"
          type="number"
          min={1}
          error={form.formState.errors.amountCop?.message}
          {...form.register('amountCop', { valueAsNumber: true })}
        />
        <TextField label="Motivo" error={form.formState.errors.reason?.message} {...form.register('reason')} />
      </div>
      <ErrorAlert error={error} />
      <div className="flex gap-2">
        <Button type="submit" size="sm" variant="destructive" loading={sending}>
          Confirmar
        </Button>
        <Button variant="secondary" size="sm" onClick={() => setOpen(false)}>
          Volver
        </Button>
      </div>
    </form>
  )
}

function Reversal({ id, movement }: { id: string; movement: Movement }) {
  const apply = useApply(id)
  const [open, setOpen] = useState(false)
  const form = useForm<{ reason: string }>({
    resolver: zodResolver(waiveSchema.pick({ reason: true })),
    defaultValues: { reason: '' },
  })
  const { submit, error, sending } = useSubmit(form, async ({ reason }) => {
    await apply(await financeApi.reverse(movement.id, reason))
  })

  if (!open) {
    return (
      <Button variant="link" size="inline" onClick={() => setOpen(true)}>
        Reversar
      </Button>
    )
  }
  return (
    <form onSubmit={submit} noValidate className="space-y-2">
      <TextField label="Motivo de la reversión" autoFocus error={form.formState.errors.reason?.message} {...form.register('reason')} />
      <ErrorAlert error={error} />
      <div className="flex gap-2">
        <Button type="submit" size="sm" variant="destructive" loading={sending}>
          Confirmar reversión
        </Button>
        <Button variant="secondary" size="sm" onClick={() => setOpen(false)}>
          Volver
        </Button>
      </div>
    </form>
  )
}
