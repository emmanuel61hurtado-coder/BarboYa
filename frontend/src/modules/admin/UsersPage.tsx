import { useState } from 'react'
import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import type { z } from 'zod'
import { adminApi } from '../../services/api/admin'
import { shopApi } from '../../services/api/commerce'
import type { User, UserStatus } from '../../services/api/types'
import { useCurrentUser } from '../../contexts/AuthContext'
import { useSubmit } from '../../hooks/useSubmit'
import { reasonSchema } from '../../validations/auth'
import { SelectField, TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Card, ErrorAlert, Pager } from '../../components/ui/surface'
import { ROLE_LABELS, STATUS_LABELS, formatDate } from '../../utils/labels'
import { CATEGORY_LABELS, formatCop } from '../../utils/commerce'

type Decision = 'ACTIVE' | 'SUSPENDED'

export function UsersPage() {
  const [status, setStatus] = useState<UserStatus | ''>('PENDING_VERIFICATION')
  const [page, setPage] = useState(0)
  const users = useQuery({
    queryKey: ['admin', 'users', status, page],
    queryFn: () => adminApi.users(page, status || undefined),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <h1 className="text-2xl font-display font-black">Usuarios</h1>
      <Card>
        <div className="max-w-xs">
          <SelectField
            label="Mostrar cuentas"
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as UserStatus | '')
              setPage(0)
            }}
          >
            <option value="">Todas</option>
            {(Object.keys(STATUS_LABELS) as UserStatus[]).map((value) => (
              <option key={value} value={value}>
                {STATUS_LABELS[value]}
              </option>
            ))}
          </SelectField>
        </div>
        <ErrorAlert error={users.error} />
        {users.isPending && <p className="text-sm text-slate-400">Cargando…</p>}
        {users.data?.items.length === 0 && <p className="text-sm text-slate-400">No hay cuentas en esta lista.</p>}
        <ul className="divide-y divide-slate-800">
          {users.data?.items.map((user) => <UserRow key={user.id} user={user} />)}
        </ul>
        {users.data && <Pager {...users.data} onChange={setPage} />}
      </Card>
    </>
  )
}

function UserRow({ user }: { user: User }) {
  const actor = useCurrentUser()
  const [decision, setDecision] = useState<Decision | null>(null)
  const staff = user.role === 'ADMIN' || user.role === 'SUPERADMIN'
  const merchant = user.role === 'RESTAURANT' || user.role === 'STORE'
  const [reviewing, setReviewing] = useState(false)
  // El backend aplica estas mismas reglas; aquí solo se ocultan acciones imposibles
  const manageable = user.id !== actor.id && (!staff || actor.role === 'SUPERADMIN')

  return (
    <li className="py-3 space-y-3" aria-label={user.fullName}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-semibold">{user.fullName}</p>
          <p className="text-xs text-slate-400 break-words">
            {ROLE_LABELS[user.role]} · {user.email} · {user.phone}
          </p>
          <p className="text-xs text-slate-500">
            {STATUS_LABELS[user.status]} · Registro {formatDate(user.createdAt)}
          </p>
        </div>
        {manageable && (
          <div className="flex gap-2">
            {user.status !== 'ACTIVE' && (
              <Button size="sm" onClick={() => setDecision('ACTIVE')}>
                {user.status === 'PENDING_VERIFICATION' ? 'Aprobar' : 'Reactivar'}
              </Button>
            )}
            {user.status !== 'SUSPENDED' && (
              <Button variant="destructive" size="sm" onClick={() => setDecision('SUSPENDED')}>
                Suspender
              </Button>
            )}
          </div>
        )}
      </div>
      {merchant && (
        <Button variant="link" size="inline" aria-expanded={reviewing} onClick={() => setReviewing(!reviewing)}>
          {reviewing ? 'Ocultar comercio' : 'Ver comercio'}
        </Button>
      )}
      {reviewing && <BusinessReview ownerId={user.id} />}
      {decision && <DecisionForm user={user} decision={decision} onClose={() => setDecision(null)} />}
    </li>
  )
}

function BusinessReview({ ownerId }: { ownerId: string }) {
  const business = useQuery({
    queryKey: ['admin', 'business', ownerId],
    queryFn: () => shopApi.reviewBusiness(ownerId),
  })
  if (business.error) return <ErrorAlert error={business.error} />
  if (business.data === undefined) return <p className="text-sm text-slate-400">Cargando…</p>
  if (business.data === null) {
    return <p className="text-sm text-amber-300">Todavía no ha registrado los datos de su comercio.</p>
  }
  const data = business.data
  const rows = [
    ['Nombre', data.name],
    ['Categoría', CATEGORY_LABELS[data.category]],
    ['Razón social', data.legalName ?? 'No indicada'],
    ['NIT', data.nit ?? 'No indicado'],
    ['Dirección', data.address],
    ['Teléfono', data.phone],
    ['Domicilio', data.deliveryEnabled ? formatCop(data.deliveryFeeCop) : 'No ofrece'],
  ]
  return (
    <dl className="grid gap-2 rounded-2xl border border-slate-700 p-3 sm:grid-cols-2">
      {rows.map(([term, value]) => (
        <div key={term}>
          <dt className="text-xs text-slate-500">{term}</dt>
          <dd className="text-sm break-words">{value}</dd>
        </div>
      ))}
    </dl>
  )
}

function DecisionForm({ user, decision, onClose }: { user: User; decision: Decision; onClose: () => void }) {
  const queryClient = useQueryClient()
  const form = useForm<z.infer<typeof reasonSchema>>({ resolver: zodResolver(reasonSchema) })
  const { submit, error, sending } = useSubmit(form, async ({ reason }) => {
    await adminApi.changeStatus(user.id, decision, reason)
    onClose()
    await queryClient.invalidateQueries({ queryKey: ['admin'] })
  })

  return (
    <form onSubmit={submit} noValidate className="space-y-3 rounded-2xl border border-slate-700 p-3">
      <ErrorAlert error={error} />
      <TextField
        label={decision === 'ACTIVE' ? 'Motivo de la activación' : 'Motivo de la suspensión'}
        hint="Queda registrado en la auditoría"
        autoFocus
        error={form.formState.errors.reason?.message}
        {...form.register('reason')}
      />
      <div className="flex gap-2">
        <Button type="submit" size="sm" variant={decision === 'ACTIVE' ? 'primary' : 'destructive'} loading={sending}>
          Confirmar
        </Button>
        <Button variant="secondary" size="sm" onClick={onClose}>
          Cancelar
        </Button>
      </div>
    </form>
  )
}
