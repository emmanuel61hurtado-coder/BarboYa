import { useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import type { Order } from '../../services/api/commerce'
import { DeliveryOffer, deliveryApi } from '../../services/api/delivery'
import { useCurrentUser } from '../../contexts/AuthContext'
import { Button } from '../../components/ui/button'
import { TextField } from '../../components/ui/field'
import { Alert, Card, ErrorAlert } from '../../components/ui/surface'
import { Map } from '../../components/map/Map'
import type { Marker } from '../../components/map/LeafletMap'
import { ORDER_STATUS_LABELS, formatCop } from '../../utils/commerce'
import { Point, currentPosition } from '../../utils/geo'
import { formatDate } from '../../utils/labels'
import { StatusBadge } from '../orders/OrderSummary'
import { useLocationSharing } from './useLocationSharing'

const km = (value: number) => `${value.toLocaleString('es-CO', { maximumFractionDigits: 1 })} km`

function useRefreshWork() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: ['work'] })
}

export function DeliveriesPage() {
  const user = useCurrentUser()
  if (user.status !== 'ACTIVE') {
    return (
      <Card title="Todavía no puedes recibir pedidos">
        <p className="text-sm text-slate-400">Tu cuenta debe estar aprobada.</p>
        <Button asChild>
          <Link to="/verificacion">Ir a mi verificación</Link>
        </Button>
      </Card>
    )
  }
  return <Work />
}

function Work() {
  const refresh = useRefreshWork()
  const presence = useQuery({ queryKey: ['work', 'presence'], queryFn: deliveryApi.presence })
  const delivery = useQuery({ queryKey: ['work', 'delivery'], queryFn: deliveryApi.current, refetchInterval: 20_000 })
  const online = presence.data?.online ?? false
  const location = useLocationSharing(online || Boolean(delivery.data), online)

  const connect = useMutation({
    mutationFn: async (next: boolean) => {
      if (!next) return deliveryApi.setPresence(false)
      const position = await currentPosition().catch(() => {
        throw new Error('Activa el permiso de ubicación para conectarte.')
      })
      return deliveryApi.setPresence(true, position)
    },
    onSettled: refresh,
  })

  return (
    <>
      <h1 className="text-2xl font-display font-black">Entregas</h1>
      <Card
        title={online ? 'Estás conectado' : 'Estás desconectado'}
        description="Mantén esta pantalla abierta: la ubicación no se comparte en segundo plano."
      >
        <ErrorAlert error={presence.error ?? connect.error} />
        {location.problem && <Alert variant="warning">{location.problem}</Alert>}
        <Button
          variant={online ? 'secondary' : 'primary'}
          loading={connect.isPending}
          onClick={() => connect.mutate(!online)}
        >
          {online ? 'Desconectarme' : 'Conectarme'}
        </Button>
      </Card>
      <ErrorAlert error={delivery.error} />
      {delivery.data ? (
        <ActiveDelivery order={delivery.data} position={location.position} />
      ) : (
        online && <Offers />
      )}
      <History />
    </>
  )
}

function Offers() {
  const refresh = useRefreshWork()
  const offers = useQuery({ queryKey: ['work', 'offers'], queryFn: deliveryApi.offers, refetchInterval: 20_000 })
  const accept = useMutation({ mutationFn: deliveryApi.accept, onSettled: refresh })

  return (
    <Card title="Pedidos cercanos" description="Distancias en línea recta.">
      <ErrorAlert error={offers.error ?? accept.error} />
      {offers.data?.length === 0 && <p className="text-sm text-slate-400">No hay pedidos cerca por ahora.</p>}
      <ul className="space-y-3">
        {offers.data?.map((offer) => (
          <Offer
            key={offer.id}
            offer={offer}
            taking={accept.isPending && accept.variables === offer.id}
            onTake={() => accept.mutate(offer.id)}
          />
        ))}
      </ul>
    </Card>
  )
}

function Offer({ offer, taking, onTake }: { offer: DeliveryOffer; taking: boolean; onTake: () => void }) {
  return (
    <li aria-label={`Pedido ${offer.number}`} className="rounded-2xl border border-slate-700 p-4 space-y-2">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <p className="font-semibold">{offer.businessName}</p>
          <p className="text-xs text-slate-400">{offer.businessAddress}</p>
        </div>
        <p className="text-lg font-bold text-emerald-300">Ganas {formatCop(offer.feeCop)}</p>
      </div>
      <p className="text-xs text-slate-400">
        A {km(offer.pickupDistanceKm)} del comercio · {km(offer.deliveryDistanceKm)} hasta el cliente ·{' '}
        {ORDER_STATUS_LABELS[offer.status]}
      </p>
      <p className="text-sm">
        Pagas {formatCop(offer.payAtPickupCop)} al comercio y cobras {formatCop(offer.collectCop)} al cliente.
      </p>
      <Button loading={taking} onClick={onTake}>
        Tomar pedido
      </Button>
    </li>
  )
}

function ActiveDelivery({ order, position }: { order: Order; position: Point | null }) {
  const refresh = useRefreshWork()
  const picked = order.status === 'OUT_FOR_DELIVERY'
  const markers: Marker[] = [
    ...(order.businessLocation ? [{ position: order.businessLocation, label: 'Comercio', color: '#f97316' }] : []),
    ...(order.deliveryLocation ? [{ position: order.deliveryLocation, label: 'Cliente', color: '#10b981' }] : []),
    ...(position ? [{ position, label: 'Tú', color: '#0ea5e9' }] : []),
  ]
  const release = useMutation({ mutationFn: (reason: string) => deliveryApi.release(order.id, reason), onSettled: refresh })
  const pickUp = useMutation({ mutationFn: (code: string) => deliveryApi.pickUp(order.id, code), onSettled: refresh })
  const deliver = useMutation({ mutationFn: (pin: string) => deliveryApi.deliver(order.id, pin), onSettled: refresh })
  const fail = useMutation({ mutationFn: (reason: string) => deliveryApi.fail(order.id, reason), onSettled: refresh })

  return (
    <section aria-label="Entrega en curso" className="pro-surface-elevated rounded-3xl p-5 space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-lg font-bold">Entrega en curso · #{order.number}</h2>
        <StatusBadge status={order.status} />
      </div>
      <Map markers={markers} />
      <dl className="grid gap-3 sm:grid-cols-2 text-sm">
        <div>
          <dt className="text-xs text-slate-500">Recoge en</dt>
          <dd>
            {order.businessName}, {order.businessAddress}
            <a href={`tel:${order.businessPhone}`} className="block text-sky-400">
              {order.businessPhone}
            </a>
          </dd>
        </div>
        <div>
          <dt className="text-xs text-slate-500">Entrega a</dt>
          <dd>
            {order.customer?.name}, {order.deliveryAddress}
            <a href={`tel:${order.customer?.phone}`} className="block text-sky-400">
              {order.customer?.phone}
            </a>
            {order.notes && <span className="block text-slate-400">{order.notes}</span>}
          </dd>
        </div>
      </dl>
      <ul className="text-sm space-y-1">
        {order.items.map((item) => (
          <li key={item.productId}>
            {item.quantity} × {item.name}
          </li>
        ))}
      </ul>
      <Alert>
        Paga <strong>{formatCop(order.subtotalCop)}</strong> al comercio y cobra{' '}
        <strong>{formatCop(order.totalCop)}</strong> al cliente. Tu ganancia: {formatCop(order.deliveryFeeCop)}.
      </Alert>

      {order.status === 'PREPARING' && <p className="text-sm text-slate-400">El comercio todavía lo está preparando.</p>}
      {order.status === 'READY' && (
        <CodeForm
          label="Código de recogida"
          action="Confirmar recogida"
          pending={pickUp.isPending}
          onSubmit={pickUp.mutate}
        />
      )}
      {picked && (
        <>
          <EvidenceUpload order={order} />
          {order.pinLocked ? (
            <Alert variant="warning">PIN bloqueado. Pide al cliente que genere uno nuevo.</Alert>
          ) : (
            <CodeForm label="PIN del cliente" action="Confirmar entrega" pending={deliver.isPending} onSubmit={deliver.mutate} />
          )}
        </>
      )}
      <ErrorAlert error={release.error ?? pickUp.error ?? deliver.error ?? fail.error} />
      {picked ? (
        <ReasonAction label="No pude entregar" confirm="Registrar entrega fallida" onConfirm={fail.mutate} />
      ) : (
        <ReasonAction label="Soltar pedido" confirm="Confirmar" onConfirm={release.mutate} />
      )}
    </section>
  )
}

function CodeForm({ label, action, pending, onSubmit }: {
  label: string
  action: string
  pending: boolean
  onSubmit: (code: string) => void
}) {
  const [code, setCode] = useState('')
  const [invalid, setInvalid] = useState(false)

  return (
    <form
      noValidate
      className="flex flex-wrap items-end gap-3"
      onSubmit={(event) => {
        event.preventDefault()
        const valid = /^\d{4}$/.test(code)
        setInvalid(!valid)
        if (valid) onSubmit(code)
      }}
    >
      <div className="w-44">
        <TextField
          label={label}
          inputMode="numeric"
          autoComplete="off"
          maxLength={4}
          value={code}
          error={invalid ? 'Son 4 dígitos' : undefined}
          onChange={(e) => setCode(e.target.value)}
        />
      </div>
      <Button type="submit" loading={pending}>
        {action}
      </Button>
    </form>
  )
}

function EvidenceUpload({ order }: { order: Order }) {
  const refresh = useRefreshWork()
  const input = useRef<HTMLInputElement>(null)
  const upload = useMutation({
    mutationFn: (photo: File) => deliveryApi.addEvidence(order.id, photo),
    onSettled: refresh,
  })

  return (
    <div className="space-y-2">
      <label className="block text-xs text-slate-300">
        Foto de la entrega (opcional)
        <input
          ref={input}
          type="file"
          accept="image/jpeg,image/png"
          capture="environment"
          className="mt-1 block text-sm"
          onChange={(e) => e.target.files?.[0] && upload.mutate(e.target.files[0])}
        />
      </label>
      {upload.isPending && <p className="text-xs text-slate-400">Subiendo…</p>}
      {order.hasEvidence && !upload.isPending && <p className="text-xs text-emerald-400">Foto guardada.</p>}
      <ErrorAlert error={upload.error} />
    </div>
  )
}

function ReasonAction({ label, confirm, onConfirm }: {
  label: string
  confirm: string
  onConfirm: (reason: string) => void
}) {
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState('')
  const [invalid, setInvalid] = useState(false)

  if (!open) {
    return (
      <Button variant="secondary" size="sm" onClick={() => setOpen(true)}>
        {label}
      </Button>
    )
  }
  return (
    <div className="space-y-2">
      <TextField
        label="Motivo"
        autoFocus
        maxLength={300}
        value={reason}
        error={invalid ? 'Explica el motivo (mínimo 5 caracteres)' : undefined}
        onChange={(e) => setReason(e.target.value)}
      />
      <div className="flex gap-2">
        <Button
          variant="destructive"
          size="sm"
          onClick={() => {
            const valid = reason.trim().length >= 5
            setInvalid(!valid)
            if (valid) onConfirm(reason.trim())
          }}
        >
          {confirm}
        </Button>
        <Button variant="secondary" size="sm" onClick={() => setOpen(false)}>
          Volver
        </Button>
      </div>
    </div>
  )
}

function History() {
  const earnings = useQuery({ queryKey: ['work', 'earnings'], queryFn: deliveryApi.earnings })
  const history = useQuery({ queryKey: ['work', 'history'], queryFn: () => deliveryApi.history(0) })
  const totals = earnings.data
    ? ([
        ['Hoy', earnings.data.todayCop],
        ['Últimos 7 días', earnings.data.last7DaysCop],
        ['Últimos 30 días', earnings.data.last30DaysCop],
      ] as const)
    : []

  return (
    <Card title="Mis ganancias">
      <ErrorAlert error={earnings.error ?? history.error} />
      <dl className="grid grid-cols-3 gap-2">
        {totals.map(([label, value]) => (
          <div key={label} aria-label={label} className="rounded-xl border border-slate-700 p-3">
            <dt className="text-xs text-slate-500">{label}</dt>
            <dd className="font-bold">{formatCop(value)}</dd>
          </div>
        ))}
      </dl>
      <ul className="divide-y divide-slate-800">
        {history.data?.items.map((order) => (
          <li key={order.id} className="flex flex-wrap justify-between gap-2 py-2 text-sm">
            <span>
              #{order.number} · {order.businessName}
              <span className="block text-xs text-slate-500">{formatDate(order.createdAt)}</span>
            </span>
            <span className="text-right">
              {ORDER_STATUS_LABELS[order.status]}
              {order.status === 'DELIVERED' && (
                <span className="block text-emerald-300">{formatCop(order.deliveryFeeCop)}</span>
              )}
            </span>
          </li>
        ))}
      </ul>
    </Card>
  )
}
