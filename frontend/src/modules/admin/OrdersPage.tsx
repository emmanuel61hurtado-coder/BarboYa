import { useState } from 'react'
import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useSearchParams } from 'react-router-dom'
import type { z } from 'zod'
import { adminApi } from '../../services/api/admin'
import type { Order } from '../../services/api/commerce'
import { useSubmit } from '../../hooks/useSubmit'
import { closeOrderSchema } from '../../validations/support'
import { TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Card, ErrorAlert, Pager } from '../../components/ui/surface'
import { ORDER_STATUS_LABELS } from '../../utils/commerce'
import { formatDate } from '../../utils/labels'
import { OrderSummary, StatusBadge } from '../orders/OrderSummary'

const ACTIVE: Order['status'][] = ['PLACED', 'PREPARING', 'READY', 'OUT_FOR_DELIVERY']

export function OrdersPage() {
  const orderId = useSearchParams()[0].get('pedido')
  return (
    <>
      <h1 className="text-2xl font-display font-black">Pedidos</h1>
      {orderId ? <OneOrder id={orderId} /> : <OrderSearch />}
    </>
  )
}

function OneOrder({ id }: { id: string }) {
  const order = useQuery({ queryKey: ['orders', 'staff', 'detail', id], queryFn: () => adminApi.order(id) })
  return (
    <>
      <Link to="/admin/pedidos" className="text-sm text-sky-400 hover:underline">
        Ver todos los pedidos
      </Link>
      <ErrorAlert error={order.error} />
      {order.data && <StaffOrder order={order.data} />}
    </>
  )
}

function OrderSearch() {
  const [finished, setFinished] = useState(false)
  const [number, setNumber] = useState<number | undefined>()
  const [text, setText] = useState('')
  const [page, setPage] = useState(0)
  const orders = useQuery({
    queryKey: ['orders', 'staff', finished, number, page],
    queryFn: () => adminApi.orders({ finished, number, page }),
    placeholderData: keepPreviousData,
    refetchInterval: 30_000,
  })

  return (
    <>
      <form
        className="flex flex-wrap items-end gap-3"
        onSubmit={(event) => {
          event.preventDefault()
          setNumber(text ? Number(text) : undefined)
          setPage(0)
        }}
      >
        <div className="w-52">
          <TextField
            label="Número de pedido"
            inputMode="numeric"
            value={text}
            onChange={(e) => setText(e.target.value.replace(/\D/g, '').slice(0, 12))}
          />
        </div>
        <Button type="submit">Buscar</Button>
      </form>
      {number === undefined && (
        <div className="flex gap-2" role="group" aria-label="Filtro de pedidos">
          {[false, true].map((value) => (
            <Button
              key={String(value)}
              size="sm"
              variant={finished === value ? 'primary' : 'secondary'}
              aria-pressed={finished === value}
              onClick={() => {
                setFinished(value)
                setPage(0)
              }}
            >
              {value ? 'Terminados' : 'En curso'}
            </Button>
          ))}
        </div>
      )}
      <ErrorAlert error={orders.error} />
      {orders.data?.total === 0 && <p className="text-sm text-slate-400">No se encontraron pedidos.</p>}
      <ul className="space-y-4">
        {orders.data?.items.map((order) => (
          <li key={order.id} aria-label={`Pedido ${order.number}`}>
            <StaffOrder order={order} />
          </li>
        ))}
      </ul>
      {orders.data && orders.data.total > orders.data.size && <Pager {...orders.data} onChange={setPage} />}
    </>
  )
}

function StaffOrder({ order }: { order: Order }) {
  const people = [
    ['Comercio', order.businessName, order.businessPhone],
    ['Cliente', order.customer?.name, order.customer?.phone],
    ['Domiciliario', order.courier?.name, order.courier?.phone],
  ] as const

  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="font-bold">Pedido #{order.number}</h2>
          <p className="text-xs text-slate-500">{formatDate(order.createdAt)}</p>
        </div>
        <StatusBadge status={order.status} />
      </div>
      <dl className="grid gap-2 sm:grid-cols-3 text-sm">
        {people.map(
          ([label, name, phone]) =>
            name && (
              <div key={label}>
                <dt className="text-xs text-slate-500">{label}</dt>
                <dd>
                  {name}
                  <a href={`tel:${phone}`} className="block text-sky-400 hover:underline">
                    {phone}
                  </a>
                </dd>
              </div>
            ),
        )}
      </dl>
      <OrderSummary order={order} />
      <details>
        <summary className="cursor-pointer text-sm text-slate-400">Historial</summary>
        <ol className="mt-2 space-y-1">
          {order.history.map((step, index) => (
            <li key={`${step.at}-${index}`} className="flex flex-wrap justify-between gap-2 text-sm">
              <span>
                {ORDER_STATUS_LABELS[step.status]}
                {step.note && <span className="text-slate-500"> · {step.note}</span>}
              </span>
              <span className="text-xs text-slate-500">{formatDate(step.at)}</span>
            </li>
          ))}
        </ol>
      </details>
      {ACTIVE.includes(order.status) && <CloseOrder order={order} />}
    </Card>
  )
}

function CloseOrder({ order }: { order: Order }) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const form = useForm<z.infer<typeof closeOrderSchema>>({
    resolver: zodResolver(closeOrderSchema),
    defaultValues: { reason: '' },
  })
  const { submit, error, sending } = useSubmit(form, async ({ reason }) => {
    try {
      await adminApi.closeOrder(order.id, reason)
    } finally {
      await queryClient.invalidateQueries({ queryKey: ['orders'] })
    }
  })

  if (!open) {
    return (
      <Button variant="secondary" size="sm" onClick={() => setOpen(true)}>
        Cerrar este pedido
      </Button>
    )
  }
  return (
    <form onSubmit={submit} noValidate className="space-y-2 border-t border-slate-800 pt-3">
      <p className="text-xs text-slate-400">
        {order.status === 'OUT_FOR_DELIVERY'
          ? 'El pedido ya salió: quedará como entrega fallida, sin comisiones.'
          : 'El pedido quedará cancelado y las existencias volverán al comercio.'}
      </p>
      <ErrorAlert error={error} />
      <TextField
        label="Motivo del cierre"
        hint="Lo verán el cliente y el comercio"
        autoFocus
        maxLength={270}
        error={form.formState.errors.reason?.message}
        {...form.register('reason')}
      />
      <div className="flex gap-2">
        <Button type="submit" variant="destructive" size="sm" loading={sending}>
          Confirmar cierre
        </Button>
        <Button variant="secondary" size="sm" onClick={() => setOpen(false)}>
          Volver
        </Button>
      </div>
    </form>
  )
}
