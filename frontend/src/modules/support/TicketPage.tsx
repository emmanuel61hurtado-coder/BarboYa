import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { supportApi } from '../../services/api/support'
import { Card, ErrorAlert } from '../../components/ui/surface'
import { TICKET_CATEGORY_LABELS } from './labels'
import { DueLabel } from './DueLabel'
import { Messages, ReplyForm, TicketBadge } from './TicketThread'

export function TicketPage() {
  const { id = '' } = useParams()
  const queryClient = useQueryClient()
  const query = useQuery({
    queryKey: ['support', 'mine', 'detail', id],
    queryFn: () => supportApi.get(id),
    refetchInterval: 30_000,
  })

  if (!query.data) {
    return query.error ? <ErrorAlert error={query.error} /> : <p className="text-sm text-slate-400">Cargando…</p>
  }
  const { ticket } = query.data

  return (
    <>
      <Link to="/soporte" className="text-sm text-sky-400 hover:underline">
        Volver a mis casos
      </Link>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-display font-black">Caso {ticket.number}</h1>
        <TicketBadge status={ticket.status} staff={false} />
      </div>
      <DueLabel ticket={ticket} />
      <Card title={ticket.subject} description={TICKET_CATEGORY_LABELS[ticket.category]}>
        <Messages ticket={query.data} staff={false} />
        <ReplyForm
          resolved={ticket.status === 'RESOLVED'}
          onSend={async (body) => {
            await supportApi.reply(id, body)
            await queryClient.invalidateQueries({ queryKey: ['support'] })
          }}
        />
      </Card>
    </>
  )
}
