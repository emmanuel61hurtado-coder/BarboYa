import type { TicketSummary } from '../../services/api/support'
import { dueText } from './due'

export function DueLabel({ ticket }: { ticket: TicketSummary }) {
  const due = dueText(ticket)
  if (!due) return null
  return <span className={due.overdue ? 'text-xs font-semibold text-rose-300' : 'text-xs text-slate-400'}>{due.text}</span>
}
