import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { financeApi } from '../../services/api/finance'
import { Card, ErrorAlert } from '../../components/ui/surface'
import { StatementView } from './StatementView'

export function MyAccountPage() {
  const [page, setPage] = useState(0)
  const statement = useQuery({
    queryKey: ['finance', 'mine', page],
    queryFn: () => financeApi.mine(page),
    placeholderData: keepPreviousData,
    staleTime: 0,
  })

  return (
    <>
      <h1 className="text-2xl font-display font-black">Comisiones</h1>
      <Card description="Los pedidos se pagan en efectivo, así que la comisión queda como saldo por pagar a BarboYa. Para ponerte al día, coordina el pago con soporte: se registra al recibir el comprobante.">
        <ErrorAlert error={statement.error} />
        {statement.isPending && <p className="text-sm text-slate-400">Cargando…</p>}
        {statement.data && <StatementView statement={statement.data} onPage={setPage} />}
      </Card>
    </>
  )
}
