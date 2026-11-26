import type { Order } from '../../services/api/commerce'
import { FULFILLMENT_LABELS, ORDER_STATUS_LABELS, formatCop } from '../../utils/commerce'
import { cn } from '../../utils/cn'

export function StatusBadge({ status }: { status: Order['status'] }) {
  const tone =
    status === 'DELIVERED'
      ? 'bg-emerald-500/15 text-emerald-300'
      : status === 'REJECTED' || status === 'CANCELLED'
        ? 'bg-rose-500/15 text-rose-300'
        : 'bg-sky-500/15 text-sky-300'
  return (
    <span className={cn('shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold', tone)}>
      {ORDER_STATUS_LABELS[status]}
    </span>
  )
}

/** Productos, cargos y destino de un pedido. */
export function OrderSummary({ order }: { order: Order }) {
  return (
    <div className="space-y-3 text-sm">
      <ul className="space-y-1">
        {order.items.map((item) => (
          <li key={item.productId} className="flex justify-between gap-3">
            <span>
              {item.quantity} × {item.name}
            </span>
            <span className="shrink-0">{formatCop(item.lineTotalCop)}</span>
          </li>
        ))}
      </ul>
      <dl className="border-t border-slate-800 pt-2 space-y-1">
        <div className="flex justify-between text-slate-400">
          <dt>Subtotal</dt>
          <dd>{formatCop(order.subtotalCop)}</dd>
        </div>
        <div className="flex justify-between text-slate-400">
          <dt>Domicilio</dt>
          <dd>{formatCop(order.deliveryFeeCop)}</dd>
        </div>
        <div className="flex justify-between font-bold">
          <dt>Total en efectivo</dt>
          <dd>{formatCop(order.totalCop)}</dd>
        </div>
      </dl>
      <p className="text-slate-400">
        {FULFILLMENT_LABELS[order.fulfillment]}
        {order.deliveryAddress && `: ${order.deliveryAddress}`}
      </p>
      {order.notes && <p className="text-slate-400 break-words">Indicaciones: {order.notes}</p>}
      {order.statusReason && <p className="text-rose-300 break-words">Motivo: {order.statusReason}</p>}
    </div>
  )
}
