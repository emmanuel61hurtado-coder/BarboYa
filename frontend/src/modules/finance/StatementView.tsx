import type { ReactNode } from 'react'
import type { Movement, Statement } from '../../services/api/finance'
import { Alert, Pager } from '../../components/ui/surface'
import { formatCop } from '../../utils/commerce'
import { formatDate } from '../../utils/labels'
import { METHOD_LABELS, MOVEMENT_LABELS } from './labels'

/** Deuda, tope y movimientos de una cuenta. */
export function StatementView({ statement, onPage, actions }: {
  statement: Statement
  onPage: (page: number) => void
  actions?: (movement: Movement) => ReactNode
}) {
  return (
    <div className="space-y-4">
      <dl className="grid gap-2 sm:grid-cols-3">
        {[
          ['Deuda pendiente', formatCop(statement.balanceCop)],
          ['Tope permitido', formatCop(statement.debtLimitCop)],
          ['Comisión', `${(statement.commissionBps / 100).toLocaleString('es-CO')} %`],
        ].map(([term, value]) => (
          <div key={term} aria-label={term} className="rounded-xl border border-slate-700 p-3">
            <dt className="text-xs text-slate-500">{term}</dt>
            <dd className="text-lg font-bold">{value}</dd>
          </div>
        ))}
      </dl>
      {statement.blocked && (
        <Alert variant="warning">Superó el tope: no recibe pedidos nuevos hasta ponerse al día.</Alert>
      )}
      {statement.total === 0 && <p className="text-sm text-slate-400">Todavía no hay movimientos.</p>}
      <ul className="divide-y divide-slate-800">
        {statement.movements.map((movement) => (
          <li key={movement.id} aria-label={MOVEMENT_LABELS[movement.kind]} className="py-3 space-y-1">
            <div className="flex flex-wrap justify-between gap-2 text-sm">
              <span className="font-semibold">
                {MOVEMENT_LABELS[movement.kind]}
                {movement.reversed && <span className="text-rose-300"> (reversado)</span>}
              </span>
              <span className={movement.effectCop > 0 ? 'text-amber-300' : 'text-emerald-300'}>
                {movement.effectCop > 0 ? '+' : '−'}
                {formatCop(Math.abs(movement.effectCop))}
              </span>
            </div>
            <p className="text-xs text-slate-500 break-words">
              {formatDate(movement.at)}
              {movement.method && ` · ${METHOD_LABELS[movement.method]} ${movement.reference}`}
              {movement.note && ` · ${movement.note}`}
            </p>
            {actions?.(movement)}
          </li>
        ))}
      </ul>
      {statement.total > statement.size && <Pager {...statement} onChange={onPage} />}
    </div>
  )
}
