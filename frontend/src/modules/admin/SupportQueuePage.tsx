import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { TicketStatus, staffSupportApi } from '../../services/api/support'
import { Button } from '../../components/ui/button'
import { Card, ErrorAlert, Pager } from '../../components/ui/surface'
import { formatDate } from '../../utils/labels'
import { TICKET_CATEGORY_LABELS } from '../support/labels'
import { DueLabel } from '../support/DueLabel'
import { TicketBadge } from '../support/TicketThread'

const FILTERS: [TicketStatus, string][] = [
  ['OPEN', 'Por responder'],
  ['ANSWERED', 'Respondidos'],
  ['RESOLVED', 'Resueltos'],
]

export function SupportQueuePage() {
  const [status, setStatus] = useState<TicketStatus>('OPEN')
  const [page, setPage] = useState(0)
  const tickets = useQuery({
    queryKey: ['support', 'staff', status, page],
    queryFn: () => staffSupportApi.queue(status, page),
    placeholderData: keepPreviousData,
    refetchInterval: 30_000,
  })

  return (
    <>
      <h1 className="text-2xl font-display font-black">Casos de soporte</h1>
      <div className="flex flex-wrap gap-2" role="group" aria-label="Filtro de casos">
        {FILTERS.map(([value, label]) => (
          <Button
            key={value}
            size="sm"
            variant={status === value ? 'primary' : 'secondary'}
            aria-pressed={status === value}
            onClick={() => {
              setStatus(value)
              setPage(0)
            }}
          >
            {label}
          </Button>
        ))}
      </div>
      <Card>
        <ErrorAlert error={tickets.error} />
        {tickets.data?.total === 0 && <p className="text-sm text-slate-400">No hay casos en esta lista.</p>}
        <ul className="divide-y divide-slate-800">
          {tickets.data?.items.map((ticket) => (
            <li key={ticket.id} className="flex flex-wrap items-center justify-between gap-2 py-3">
              <div className="min-w-0">
                <Link to={`/admin/soporte/${ticket.id}`} className="text-sm font-semibold text-sky-400 hover:underline">
                  Caso {ticket.number}: {ticket.subject}
                </Link>
                <p className="text-xs text-slate-500">
                  {TICKET_CATEGORY_LABELS[ticket.category]} · {formatDate(ticket.updatedAt)}
                </p>
              </div>
              <div className="flex flex-col items-end gap-1">
                <TicketBadge status={ticket.status} staff />
                <DueLabel ticket={ticket} />
              </div>
            </li>
          ))}
        </ul>
        {tickets.data && tickets.data.total > tickets.data.size && <Pager {...tickets.data} onChange={setPage} />}
      </Card>
    </>
  )
}
