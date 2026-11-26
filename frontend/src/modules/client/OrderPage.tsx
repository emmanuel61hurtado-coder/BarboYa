import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { Order, orderApi } from '../../services/api/commerce'
import { Button } from '../../components/ui/button'
import { Alert, Card, ErrorAlert } from '../../components/ui/surface'
import { ORDER_STATUS_LABELS } from '../../utils/commerce'
import { formatDate } from '../../utils/labels'
import { OrderSummary, StatusBadge } from '../orders/OrderSummary'

export function OrderPage() {
  const { id = '' } = useParams()
  const order = useQuery({
    queryKey: ['orders', 'mine', 'detail', id],
    queryFn: () => orderApi.get(id),
    refetchInterval: 30_000,
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
