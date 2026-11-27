import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { Order, orderApi } from '../../services/api/commerce'
import { Button } from '../../components/ui/button'
import { Alert, Card, ErrorAlert } from '../../components/ui/surface'
import { ORDER_STATUS_LABELS } from '../../utils/commerce'
import { formatDate, formatTime } from '../../utils/labels'
import { claimLink } from '../support/labels'
import { OrderSummary, StatusBadge } from '../orders/OrderSummary'
import { Map } from '../../components/map/Map'
import { DocumentPreview } from '../couriers/DocumentPreview'
import { VEHICLE_LABELS } from '../couriers/labels'

function CourierCard({ order }: { order: Order }) {
  const courier = order.courier!
  const active = ['PREPARING', 'READY', 'OUT_FOR_DELIVERY'].includes(order.status)
  const markers = [
    ...(order.businessLocation ? [{ position: order.businessLocation, label: 'Comercio', color: '#f97316' }] : []),
    ...(order.deliveryLocation ? [{ position: order.deliveryLocation, label: 'Entrega', color: '#10b981' }] : []),
    ...(courier.location ? [{ position: courier.location, label: 'Domiciliario', color: '#0ea5e9' }] : []),
  ]

  return (
    <Card title="Tu domiciliario">
      <p className="text-sm">
        {courier.name} · {VEHICLE_LABELS[courier.vehicleType]}
        {courier.plate && ` · Placa ${courier.plate}`}
      </p>
      {courier.phone && (
        <a href={`tel:${courier.phone}`} className="text-sm text-sky-400 hover:underline">
          Llamar: {courier.phone}
        </a>
      )}
      {active && (
        <>
          <Map markers={markers} />
          {!courier.location && <p className="text-xs text-slate-500">No hay una posición reciente.</p>}
        </>
      )}
    </Card>
  )
}

export function OrderPage() {
  const { id = '' } = useParams()
  const order = useQuery({
    queryKey: ['orders', 'mine', 'detail', id],
    queryFn: () => orderApi.get(id),
    // Con domiciliario asignado se consulta más seguido para mover su posición
    refetchInterval: (query) => (query.state.data?.courier ? 10_000 : 30_000),
  })

  if (!order.data) {
    return order.error ? <ErrorAlert error={order.error} /> : <p className="text-sm text-slate-400">Cargando…</p>
  }
  return <OrderView order={order.data} />
}

function OrderView({ order }: { order: Order }) {
  const queryClient = useQueryClient()
  const [confirming, setConfirming] = useState(false)
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['orders'] })
  const cancel = useMutation({ mutationFn: () => orderApi.cancel(order.id), onSettled: refresh })
  const renew = useMutation({ mutationFn: () => orderApi.newPin(order.id), onSettled: refresh })

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-display font-black">Pedido #{order.number}</h1>
        <StatusBadge status={order.status} />
      </div>
      <ErrorAlert error={cancel.error ?? renew.error} />
      {order.respondBy && (
        <Alert>
          Si el comercio no confirma antes de las {formatTime(order.respondBy)}, el pedido se cancela solo y no pagas
          nada.
        </Alert>
      )}
      {order.prepMinutes && order.status === 'PREPARING' && (
        <Alert>El comercio calcula {order.prepMinutes} minutos de preparación.</Alert>
      )}
      {order.deliveryPin && (
        <Card title="PIN de entrega" description="Díselo a quien te entrega solo cuando tengas el pedido en tus manos.">
          <p aria-label="PIN de entrega" className="font-mono text-4xl font-bold tracking-[0.4em] text-emerald-300">
            {order.deliveryPin}
          </p>
          {order.pinLocked && (
            <Alert variant="warning">El PIN se bloqueó por intentos fallidos. Genera uno nuevo.</Alert>
          )}
          <Button variant="secondary" size="sm" loading={renew.isPending} onClick={() => renew.mutate()}>
            Generar otro PIN
          </Button>
        </Card>
      )}
      {order.courier && <CourierCard order={order} />}
      {order.hasEvidence && (
        <Card title="Foto de la entrega">
          <DocumentPreview label="Foto de la entrega" contentType="image/*" load={() => orderApi.evidence(order.id)} />
        </Card>
      )}
      <Card title={order.businessName} description={order.businessAddress}>
        <a href={`tel:${order.businessPhone}`} className="text-sm text-sky-400 hover:underline">
          Llamar al comercio: {order.businessPhone}
        </a>
        <OrderSummary order={order} />
        {order.status === 'PLACED' &&
          (confirming ? (
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-sm">¿Cancelar este pedido?</span>
              <Button variant="destructive" size="sm" loading={cancel.isPending} onClick={() => cancel.mutate()}>
                Sí, cancelar
              </Button>
              <Button variant="secondary" size="sm" onClick={() => setConfirming(false)}>
                No
              </Button>
            </div>
          ) : (
            <Button variant="secondary" size="sm" onClick={() => setConfirming(true)}>
              Cancelar pedido
            </Button>
          ))}
      </Card>
      <Link to={claimLink(order)} className="inline-block text-sm text-sky-400 hover:underline">
        Pedir ayuda con este pedido
      </Link>
      <Card title="Seguimiento">
        <ol className="space-y-2">
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
      </Card>
    </>
  )
}
