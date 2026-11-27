import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import type { z } from 'zod'
import type { Ticket, TicketStatus } from '../../services/api/support'
import { useSubmit } from '../../hooks/useSubmit'
import { replySchema } from '../../validations/support'
import { TextAreaField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Alert, ErrorAlert } from '../../components/ui/surface'
import { cn } from '../../utils/cn'
import { formatDate } from '../../utils/labels'

export function TicketBadge({ status, staff }: { status: TicketStatus; staff: boolean }) {
  const waiting: TicketStatus = staff ? 'OPEN' : 'ANSWERED'
  const label =
    status === 'RESOLVED'
      ? 'Resuelto'
      : status === 'OPEN'
        ? staff
          ? 'Sin responder'
          : 'En revisión'
        : staff
          ? 'Esperando a la persona'
          : 'Respondido'
  return (
    <span
      className={cn(
        'shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold',
        status === 'RESOLVED'
          ? 'bg-emerald-500/15 text-emerald-300'
          : status === waiting
            ? 'bg-amber-500/15 text-amber-300'
            : 'bg-sky-500/15 text-sky-300',
      )}
    >
      {label}
    </span>
  )
}

export function Messages({ ticket, staff }: { ticket: Ticket; staff: boolean }) {
  return (
    <>
      <ol aria-label="Conversación" className="space-y-3">
        {ticket.messages.map((message, index) => {
          const mine = message.staff === staff
          const author = message.staff ? 'Soporte BarboYa' : staff ? (ticket.opener?.name ?? 'Persona') : 'Tú'
          return (
            <li
              key={`${message.at}-${index}`}
              className={cn(
                'max-w-[85%] rounded-2xl border p-3 text-sm',
                mine ? 'ml-auto border-sky-500/30 bg-sky-500/10' : 'border-slate-700',
              )}
            >
              <p className="text-xs text-slate-400">
                {author} · {formatDate(message.at)}
              </p>
              <p className="whitespace-pre-wrap break-words">{message.body}</p>
            </li>
          )
        })}
      </ol>
      {ticket.resolution && <Alert variant="success">Caso resuelto: {ticket.resolution}</Alert>}
    </>
  )
}

export function ReplyForm({ resolved, onSend }: { resolved: boolean; onSend: (body: string) => Promise<unknown> }) {
  const form = useForm<z.infer<typeof replySchema>>({ resolver: zodResolver(replySchema), defaultValues: { body: '' } })
  const { submit, error, sending } = useSubmit(form, async ({ body }) => {
    await onSend(body)
    form.reset()
  })

  return (
    <form onSubmit={submit} noValidate className="space-y-2">
      <ErrorAlert error={error} />
      <TextAreaField
        label="Mensaje"
        hint={resolved ? 'Al responder, el caso se vuelve a abrir' : undefined}
        rows={3}
        maxLength={2000}
        error={form.formState.errors.body?.message}
        {...form.register('body')}
      />
      <Button type="submit" size="sm" loading={sending}>
        Enviar mensaje
      </Button>
    </form>
  )
}
