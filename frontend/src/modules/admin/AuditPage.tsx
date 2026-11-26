import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { adminApi } from '../../services/api/admin'
import { Card, ErrorAlert, Pager } from '../../components/ui/surface'
import { actionLabel, formatDate } from '../../utils/labels'

export function AuditPage() {
  const [page, setPage] = useState(0)
  const audit = useQuery({
    queryKey: ['admin', 'audit', page],
    queryFn: () => adminApi.audit(page),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <h1 className="text-2xl font-display font-black">Auditoría</h1>
      <Card description="Este historial no se puede modificar ni borrar.">
        <ErrorAlert error={audit.error} />
        {audit.isPending && <p className="text-sm text-slate-400">Cargando…</p>}
        <ul className="divide-y divide-slate-800">
          {audit.data?.items.map((event) => (
            <li key={event.id} className="py-2.5 space-y-0.5">
              <div className="flex flex-wrap justify-between gap-2 text-sm">
                <span className="font-semibold">{actionLabel(event.action)}</span>
                <span className="text-xs text-slate-500">{formatDate(event.at)}</span>
              </div>
              {event.detail && <p className="text-xs text-slate-300 break-words">{event.detail}</p>}
              <p className="text-[11px] font-mono text-slate-500 break-all">
                Autor: {event.actorId ?? 'sin identificar'} · Cuenta: {event.targetId ?? 'ninguna'} ·{' '}
                {event.ip ?? 'sin IP'}
              </p>
            </li>
          ))}
        </ul>
        {audit.data && <Pager {...audit.data} onChange={setPage} />}
      </Card>
    </>
  )
}
