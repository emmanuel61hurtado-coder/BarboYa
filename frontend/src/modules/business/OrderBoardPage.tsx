import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from 'react-router-dom'
import type { z } from 'zod'
import { Order, ShopAction, shopApi } from '../../services/api/commerce'
import { useSubmit } from '../../hooks/useSubmit'
import { pinSchema, prepSchema } from '../../validations/commerce'
import { reasonSchema } from '../../validations/auth'
import { TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Alert, Card, ErrorAlert, Pager } from '../../components/ui/surface'
import { formatDate, formatTime } from '../../utils/labels'
import { claimLink } from '../support/labels'
import { OrderSummary, StatusBadge } from '../orders/OrderSummary'
import { formatCop } from '../../utils/commerce'
import { DocumentPreview } from '../couriers/DocumentPreview'
import { VEHICLE_LABELS } from '../couriers/labels'

export function OrderBoardPage() {
  const [finished, setFinished] = useState(false)
  const [page, setPage] = useState(0)
  const business = useQuery({ queryKey: ['shop', 'business'], queryFn: shopApi.business })
  const orders = useQuery({
    queryKey: ['orders', 'shop', finished, page],
    queryFn: () => shopApi.orders(finished, page),
    placeholderData: keepPreviousData,
    enabled: Boolean(business.data),
    refetchInterval: 30_000,
  })

  if (business.data === null) {
    return (
      <Card title="Primero registra tu comercio">
        <Button asChild>
          <Link to="/mi-comercio">Completar los datos</Link>
        </Button>
      </Card>
    )
  }

  return (
    <>
      <h1 className="text-2xl font-display font-black">Pedidos recibidos</h1>
      {business.data && !business.data.acceptingOrders && (
        <Alert variant="warning">
          No estás recibiendo pedidos.{' '}
          <Link to="/mi-comercio" className="underline">
            Cambiar en Mi comercio
          </Link>
        </Alert>
      )}
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
            {value ? 'Historial' : 'En curso'}
          </Button>
        ))}
      </div>
      <ErrorAlert error={business.error ?? orders.error} />
      {orders.data?.total === 0 && <p className="text-sm text-slate-400">No hay pedidos en esta lista.</p>}
      <ul className="space-y-4">
        {orders.data?.items.map((order) => (
          <li key={order.id} aria-label={`Pedido ${order.number}`}>
            <ShopOrder order={order} />
          </li>
        ))}
      </ul>
      {orders.data && orders.data.total > orders.data.size && <Pager {...orders.data} onChange={setPage} />}
    </>
  )
}

function ShopOrder({ order }: { order: Order }) {
  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="font-bold">Pedido #{order.number}</h2>
          <p className="text-xs text-slate-500">{formatDate(order.createdAt)}</p>
        </div>
        <StatusBadge status={order.status} />
      </div>
      {order.customer && (
        <p className="text-sm">
          {order.customer.name} ·{' '}
          <a href={`tel:${order.customer.phone}`} className="text-sky-400 hover:underline">
            {order.customer.phone}
          </a>
        </p>
      )}
      <OrderSummary order={order} />
      {order.respondBy && (
        <Alert variant="warning">
          Responde antes de las {formatTime(order.respondBy)} o el pedido se cancela solo.
        </Alert>
      )}
      {!!order.commissionCop && !['REJECTED', 'CANCELLED', 'FAILED'].includes(order.status) && (
        <p className="text-xs text-slate-500">Comisión BarboYa al entregar: {formatCop(order.commissionCop)}</p>
      )}
      {order.fulfillment === 'COURIER' && <CourierStatus order={order} />}
      {order.hasEvidence && (
        <DocumentPreview label="Foto de la entrega" contentType="image/*" load={() => shopApi.evidence(order.id)} />
      )}
      <Actions order={order} />
      <Link to={claimLink(order)} className="inline-block text-xs text-sky-400 hover:underline">
        Reportar un problema con este pedido
      </Link>
    </Card>
  )
}

function CourierStatus({ order }: { order: Order }) {
  if (!order.courier) {
    return order.status === 'PREPARING' || order.status === 'READY' ? (
      <Alert variant="warning">Buscando domiciliario.</Alert>
    ) : null
  }
  return (
    <div className="space-y-2 text-sm">
      <p>
        Domiciliario: {order.courier.name} · {VEHICLE_LABELS[order.courier.vehicleType]}
        {order.courier.plate && ` · Placa ${order.courier.plate}`}
        {order.courier.phone && ` · ${order.courier.phone}`}
      </p>
      {order.pickupCode && (
        <Alert>
          Cuando te pague {formatCop(order.subtotalCop)}, dale el código de recogida{' '}
          <strong aria-label="Código de recogida" className="font-mono text-lg tracking-widest">
            {order.pickupCode}
          </strong>
        </Alert>
      )}
    </div>
  )
}

function Actions({ order }: { order: Order }) {
  const queryClient = useQueryClient()
  const [stopping, setStopping] = useState(false)
  const act = useMutation({
    mutationFn: (change: ShopAction) => shopApi.act(order.id, change),
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['orders'] }),
  })
  const run = async (change: ShopAction) => void (await act.mutateAsync(change))

  if (['DELIVERED', 'REJECTED', 'CANCELLED', 'FAILED'].includes(order.status)) return null

  const courier = order.fulfillment === 'COURIER'
  // Recogido por un domiciliario: el resto lo hace esa persona
  if (courier && order.status === 'OUT_FOR_DELIVERY') return null
  const deliverable =
    order.status === 'OUT_FOR_DELIVERY' || (order.status === 'READY' && order.fulfillment === 'PICKUP')
  const stop = order.status === 'PLACED' ? 'reject' : 'cancel'

  return (
    <div className="space-y-3 border-t border-slate-800 pt-3">
      {order.status === 'PLACED' && <AcceptForm onAccept={(prepMinutes) => run({ action: 'accept', body: { prepMinutes } })} />}
      {order.status === 'PREPARING' && (
        <Button loading={act.isPending} onClick={() => act.mutate({ action: 'ready' })}>
          Marcar como listo
        </Button>
      )}
      {order.status === 'READY' && order.fulfillment === 'DELIVERY' && (
        <Button loading={act.isPending} onClick={() => act.mutate({ action: 'dispatch' })}>
          Enviar a domicilio
        </Button>
      )}
      {deliverable &&
        (order.pinLocked ? (
          <Alert variant="warning">PIN bloqueado. Pide al cliente que genere uno nuevo.</Alert>
        ) : (
          <PinForm onConfirm={(pin) => run({ action: 'deliver', body: { pin } })} />
        ))}
      {/* Los errores de los formularios se muestran dentro de cada formulario */}
      {courier && order.courier && (
        <Button variant="secondary" size="sm" loading={act.isPending} onClick={() => act.mutate({ action: 'unassign' })}>
          Pedir otro domiciliario
        </Button>
      )}
      {['ready', 'dispatch', 'unassign'].includes(act.variables?.action ?? '') && <ErrorAlert error={act.error} />}
      {stopping ? (
        <ReasonForm
          label={stop === 'reject' ? 'Motivo del rechazo' : 'Motivo de la cancelación'}
          onConfirm={(reason) => run({ action: stop, body: { reason } })}
          onClose={() => setStopping(false)}
        />
      ) : (
        <Button variant="secondary" size="sm" onClick={() => setStopping(true)}>
          {stop === 'reject' ? 'Rechazar pedido' : 'Cancelar pedido'}
        </Button>
      )}
    </div>
  )
}

function AcceptForm({ onAccept }: { onAccept: (minutes: number) => Promise<void> }) {
  const form = useForm<z.infer<typeof prepSchema>>({
    resolver: zodResolver(prepSchema),
    defaultValues: { prepMinutes: 20 },
  })
  const { submit, error, sending } = useSubmit(form, ({ prepMinutes }) => onAccept(prepMinutes))

  return (
    <form onSubmit={submit} noValidate className="space-y-2">
      <ErrorAlert error={error} />
      <div className="flex flex-wrap items-end gap-3">
        <div className="w-44">
          <TextField
            label="Minutos de preparación"
            type="number"
            min={1}
            max={240}
            error={form.formState.errors.prepMinutes?.message}
            {...form.register('prepMinutes', { valueAsNumber: true })}
          />
        </div>
        <Button type="submit" loading={sending}>
          Aceptar pedido
        </Button>
      </div>
    </form>
  )
}

function PinForm({ onConfirm }: { onConfirm: (pin: string) => Promise<void> }) {
  const form = useForm<z.infer<typeof pinSchema>>({ resolver: zodResolver(pinSchema), defaultValues: { pin: '' } })
  const { submit, error, sending } = useSubmit(form, ({ pin }) => onConfirm(pin))

  return (
    <form onSubmit={submit} noValidate className="space-y-2">
      <ErrorAlert error={error} />
      <div className="flex flex-wrap items-end gap-3">
        <div className="w-44">
          <TextField
            label="PIN del cliente"
            inputMode="numeric"
            autoComplete="off"
            maxLength={4}
            error={form.formState.errors.pin?.message}
            {...form.register('pin')}
          />
        </div>
        <Button type="submit" loading={sending}>
          Confirmar entrega
        </Button>
      </div>
    </form>
  )
}

function ReasonForm({ label, onConfirm, onClose }: {
  label: string
  onConfirm: (reason: string) => Promise<void>
  onClose: () => void
}) {
  const form = useForm<z.infer<typeof reasonSchema>>({ resolver: zodResolver(reasonSchema) })
  const { submit, error, sending } = useSubmit(form, ({ reason }) => onConfirm(reason))

  return (
    <form onSubmit={submit} noValidate className="space-y-2">
      <ErrorAlert error={error} />
      <TextField
        label={label}
        hint="El cliente verá este motivo"
        autoFocus
        error={form.formState.errors.reason?.message}
        {...form.register('reason')}
      />
      <div className="flex gap-2">
        <Button type="submit" variant="destructive" size="sm" loading={sending}>
          Confirmar
        </Button>
        <Button variant="secondary" size="sm" onClick={onClose}>
          Volver
        </Button>
      </div>
    </form>
  )
}
