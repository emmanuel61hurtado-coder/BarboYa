import type { TicketSummary } from '../../services/api/support'
import { formatDay, todayInColombia } from '../../utils/dates'

/** Fecha límite de un caso abierto; vacío si ya se resolvió. */
export function dueText(ticket: Pick<TicketSummary, 'status' | 'dueAt'>, now = new Date()) {
  if (ticket.status === 'RESOLVED') return null
  const day = formatDay(todayInColombia(new Date(ticket.dueAt)))
  return new Date(ticket.dueAt) < now
    ? { overdue: true, text: `Venció el ${day}` }
    : { overdue: false, text: `Plazo de respuesta: ${day}` }
}
