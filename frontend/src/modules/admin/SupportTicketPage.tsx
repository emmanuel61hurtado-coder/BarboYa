import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useParams } from 'react-router-dom'
import type { z } from 'zod'
import { staffSupportApi } from '../../services/api/support'
import { useSubmit } from '../../hooks/useSubmit'
import { resolutionSchema } from '../../validations/support'
import { TextAreaField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Card, ErrorAlert } from '../../components/ui/surface'
import { ROLE_LABELS } from '../../utils/labels'
import { TICKET_CATEGORY_LABELS } from '../support/labels'
import { Messages, ReplyForm, TicketBadge } from '../support/TicketThread'

export function SupportTicketPage() {
  const { id = '' } = useParams()
  const queryClient = useQueryClient()
  const query = useQuery({
    queryKey: ['support', 'staff', 'detail', id],
    queryFn: () => staffSupportApi.get(id),
    refetchInterval: 30_000,
  })
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['support'] })

  if (!query.data) {
    return query.error ? <ErrorAlert error={query.error} /> : <p className="text-sm text-slate-400">Cargando…</p>
  }
  const { ticket, opener } = query.data

  return (
    <>
      <Link to="/admin/soporte" className="text-sm text-sky-400 hover:underline">
        Volver a los casos
      </Link>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-display font-black">Caso {ticket.number}</h1>
        <TicketBadge status={ticket.status} staff />
      </div>
      {opener && (
        <Card title="Quién reclama">
          <p className="text-sm">
            {opener.name} · {ROLE_LABELS[opener.role]}
          </p>
          <p className="text-sm text-slate-400 break-words">
            <a href={`tel:${opener.phone}`} className="text-sky-400 hover:underline">
              {opener.phone}
            </a>{' '}
            · {opener.email}
          </p>
          {ticket.orderId && (
            <Button variant="secondary" size="sm" asChild>
              <Link to={`/admin/pedidos?pedido=${ticket.orderId}`}>Ver el pedido</Link>
            </Button>
          )}
        </Card>
      )}
      <Card title={ticket.subject} description={TICKET_CATEGORY_LABELS[ticket.category]}>
        <Messages ticket={query.data} staff />
        <ReplyForm
          resolved={ticket.status === 'RESOLVED'}
          onSend={async (body) => {
            await staffSupportApi.reply(id, body)
            await refresh()
          }}
        />
      </Card>
      {ticket.status !== 'RESOLVED' && (
        <ResolveForm
          onResolve={async (resolution) => {
            await staffSupportApi.resolve(id, resolution)
            await refresh()
          }}
        />
      )}
    </>
  )
}

function ResolveForm({ onResolve }: { onResolve: (resolution: string) => Promise<void> }) {
  const form = useForm<z.infer<typeof resolutionSchema>>({
    resolver: zodResolver(resolutionSchema),
    defaultValues: { resolution: '' },
  })
  const { submit, error, sending } = useSubmit(form, ({ resolution }) => onResolve(resolution))

  return (
    <Card title="Cerrar el caso" description="La persona verá esta solución y quedará en la auditoría.">
      <form onSubmit={submit} noValidate className="space-y-2">
        <ErrorAlert error={error} />
        <TextAreaField
          label="Solución"
          rows={2}
          maxLength={500}
          error={form.formState.errors.resolution?.message}
          {...form.register('resolution')}
        />
        <Button type="submit" loading={sending}>
          Marcar como resuelto
        </Button>
      </form>
    </Card>
  )
}
