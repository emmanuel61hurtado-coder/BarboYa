import { useState } from 'react'
import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import type { z } from 'zod'
import { TicketCategory, supportApi } from '../../services/api/support'
import { useSubmit } from '../../hooks/useSubmit'
import { ticketSchema } from '../../validations/support'
import { SelectField, TextAreaField, TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Card, ErrorAlert, Pager } from '../../components/ui/surface'
import { formatDate } from '../../utils/labels'
import { TICKET_CATEGORY_LABELS } from './labels'
import { DueLabel } from './DueLabel'
import { TicketBadge } from './TicketThread'

export function SupportPage() {
  const [params] = useSearchParams()
  const orderId = params.get('pedido') ?? undefined
  const [page, setPage] = useState(0)
  const tickets = useQuery({
    queryKey: ['support', 'mine', page],
    queryFn: () => supportApi.list(page),
    placeholderData: keepPreviousData,
    refetchInterval: 30_000,
  })

  return (
    <>
      <h1 className="text-2xl font-display font-black">Soporte</h1>
      <NewTicketForm key={orderId ?? ''} orderId={orderId} orderNumber={params.get('numero')} />
      <Card title="Mis casos">
        <ErrorAlert error={tickets.error} />
        {tickets.data?.total === 0 && <p className="text-sm text-slate-400">No has abierto ningún caso.</p>}
        <ul className="divide-y divide-slate-800">
          {tickets.data?.items.map((ticket) => (
            <li key={ticket.id} className="flex flex-wrap items-center justify-between gap-2 py-3">
              <div className="min-w-0">
                <Link to={`/soporte/${ticket.id}`} className="text-sm font-semibold text-sky-400 hover:underline">
                  Caso {ticket.number}: {ticket.subject}
                </Link>
                <p className="text-xs text-slate-500">
                  {TICKET_CATEGORY_LABELS[ticket.category]} · {formatDate(ticket.updatedAt)}
                </p>
              </div>
              <div className="flex flex-col items-end gap-1">
                <TicketBadge status={ticket.status} staff={false} />
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

function NewTicketForm({ orderId, orderNumber }: { orderId?: string; orderNumber: string | null }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const form = useForm<z.infer<typeof ticketSchema>>({
    resolver: zodResolver(ticketSchema),
    defaultValues: { category: orderId ? 'ORDER_PROBLEM' : 'ACCOUNT', subject: '', message: '' },
  })
  const { submit, error, sending } = useSubmit(form, async (values) => {
    const created = await supportApi.open({ ...values, orderId })
    await queryClient.invalidateQueries({ queryKey: ['support'] })
    navigate(`/soporte/${created.ticket.id}`)
  })
  const { errors } = form.formState

  return (
    <Card
      title={orderId ? `Ayuda con el pedido #${orderNumber ?? ''}` : 'Abrir un caso'}
      description="Si hay un riesgo para tu seguridad, llama primero a la línea 123."
    >
      <form onSubmit={submit} noValidate className="space-y-3">
        <ErrorAlert error={error} />
        <SelectField label="Tema" error={errors.category?.message} {...form.register('category')}>
          {(Object.keys(TICKET_CATEGORY_LABELS) as TicketCategory[]).map((value) => (
            <option key={value} value={value}>
              {TICKET_CATEGORY_LABELS[value]}
            </option>
          ))}
        </SelectField>
        <TextField label="Asunto" maxLength={120} error={errors.subject?.message} {...form.register('subject')} />
        <TextAreaField
          label="Qué pasó"
          hint="No escribas contraseñas, códigos ni datos bancarios"
          rows={4}
          maxLength={2000}
          error={errors.message?.message}
          {...form.register('message')}
        />
        <div className="flex flex-wrap gap-2">
          <Button type="submit" loading={sending}>
            Enviar caso
          </Button>
          {orderId && (
            <Button variant="secondary" asChild>
              <Link to="/soporte">Es sobre otro tema</Link>
            </Button>
          )}
        </div>
      </form>
    </Card>
  )
}
