import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { orderApi } from '../../services/api/commerce'
import { Card, ErrorAlert, Pager } from '../../components/ui/surface'
import { Button } from '../../components/ui/button'
import { formatCop } from '../../utils/commerce'
import { formatDate } from '../../utils/labels'
import { StatusBadge } from '../orders/OrderSummary'

export function MyOrdersPage() {
  const [page, setPage] = useState(0)
  const orders = useQuery({
    queryKey: ['orders', 'mine', page],
    queryFn: () => orderApi.list(page),
    placeholderData: keepPreviousData,
    // Respaldo por si se pierde un aviso en vivo
    refetchInterval: 30_000,
  })

  return (
    <>
      <h1 className="text-2xl font-display font-black">Mis pedidos</h1>
      <ErrorAlert error={orders.error} />
      {orders.isPending && <p className="text-sm text-slate-400">Cargando…</p>}
      {orders.data?.total === 0 && (
        <Card>
          <p className="text-sm text-slate-400">Todavía no has hecho pedidos.</p>
          <Button asChild>
            <Link to="/comercios">Explorar comercios</Link>
          </Button>
        </Card>
      )}
      <ul className="space-y-3">
        {orders.data?.items.map((order) => (
          <li key={order.id}>
            <Link to={`/pedidos/${order.id}`} className="flex items-center justify-between gap-3 pro-card rounded-2xl p-4">
              <div className="min-w-0">
                <p className="font-semibold">{order.businessName}</p>
                <p className="text-xs text-slate-400">
                  Pedido #{order.number} · {formatDate(order.createdAt)} · {formatCop(order.totalCop)}
                </p>
              </div>
              <StatusBadge status={order.status} />
            </Link>
          </li>
        ))}
      </ul>
      {orders.data && orders.data.total > orders.data.size && <Pager {...orders.data} onChange={setPage} />}
    </>
  )
}
