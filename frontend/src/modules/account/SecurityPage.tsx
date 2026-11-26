import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import type { z } from 'zod'
import { authApi } from '../../services/api/auth'
import { useAuth, useCurrentUser } from '../../contexts/AuthContext'
import { useSubmit } from '../../hooks/useSubmit'
import { codeSchema } from '../../validations/auth'
import { TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Alert, Card, ErrorAlert } from '../../components/ui/surface'
import { actionLabel, formatDate } from '../../utils/labels'

export function SecurityPage() {
  return (
    <>
      <h1 className="text-2xl font-display font-black">Seguridad</h1>
      <EmailCard />
      <SessionsCard />
      <HistoryCard />
    </>
  )
}

function EmailCard() {
  const user = useCurrentUser()
  const { reloadUser } = useAuth()
  const send = useMutation({ mutationFn: authApi.requestEmailCode })
  const form = useForm<z.infer<typeof codeSchema>>({ resolver: zodResolver(codeSchema) })
  const { submit, error, sending } = useSubmit(form, async ({ code }) => {
    await authApi.verifyEmail(code)
    await reloadUser()
  })

  if (user.emailVerified) {
    return (
      <Card title="Correo">
        <Alert variant="success">{user.email} está verificado.</Alert>
      </Card>
    )
  }

  return (
    <Card title="Verifica tu correo" description={`Enviaremos un código a ${user.email}.`}>
      <ErrorAlert error={send.error ?? error} />
      {send.isSuccess && <Alert>Código enviado. Revisa tu correo.</Alert>}
      <Button variant="secondary" loading={send.isPending} onClick={() => send.mutate()}>
        Enviar código
      </Button>
      <form onSubmit={submit} noValidate className="flex flex-wrap items-end gap-3">
        <div className="w-40">
          <TextField
            label="Código recibido"
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            error={form.formState.errors.code?.message}
            {...form.register('code')}
          />
        </div>
        <Button type="submit" loading={sending}>
          Verificar
        </Button>
      </form>
    </Card>
  )
}

function SessionsCard() {
  const queryClient = useQueryClient()
  const sessions = useQuery({ queryKey: ['sessions'], queryFn: authApi.sessions })
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['sessions'] })
  const close = useMutation({ mutationFn: authApi.closeSession, onSuccess: refresh })
  const closeOthers = useMutation({ mutationFn: authApi.closeOtherSessions, onSuccess: refresh })

  return (
    <Card title="Sesiones abiertas" description="Si no reconoces alguna, ciérrala y cambia tu contraseña.">
      <ErrorAlert error={sessions.error ?? close.error ?? closeOthers.error} />
      {sessions.isPending && <p className="text-sm text-slate-400">Cargando…</p>}
      <ul className="divide-y divide-slate-800">
        {sessions.data?.map((session) => (
          <li key={session.id} className="flex items-center justify-between gap-3 py-3">
            <div className="min-w-0">
              <p className="text-sm truncate" title={session.userAgent ?? undefined}>
                {session.userAgent ?? 'Dispositivo desconocido'}
              </p>
              <p className="text-xs text-slate-500">
                {session.ip ?? 'IP desconocida'} · Último uso {formatDate(session.lastUsedAt)}
              </p>
            </div>
            {session.current ? (
              <span className="shrink-0 text-xs font-semibold text-emerald-400">Esta sesión</span>
            ) : (
              <Button
                variant="secondary"
                size="sm"
                loading={close.isPending && close.variables === session.id}
                onClick={() => close.mutate(session.id)}
              >
                Cerrar
              </Button>
            )}
          </li>
        ))}
      </ul>
      {(sessions.data?.length ?? 0) > 1 && (
        <Button variant="destructive" size="sm" loading={closeOthers.isPending} onClick={() => closeOthers.mutate()}>
          Cerrar las demás sesiones
        </Button>
      )}
    </Card>
  )
}

function HistoryCard() {
  const history = useQuery({ queryKey: ['history'], queryFn: authApi.history })

  return (
    <Card title="Actividad reciente">
      <ErrorAlert error={history.error} />
      {history.isPending && <p className="text-sm text-slate-400">Cargando…</p>}
      <ul className="divide-y divide-slate-800">
        {history.data?.map((event, index) => (
          <li key={`${event.at}-${index}`} className="flex flex-wrap justify-between gap-2 py-2 text-sm">
            <span>{actionLabel(event.action)}</span>
            <span className="text-xs text-slate-500">
              {formatDate(event.at)} · {event.ip ?? 'IP desconocida'}
            </span>
          </li>
        ))}
      </ul>
    </Card>
  )
}
