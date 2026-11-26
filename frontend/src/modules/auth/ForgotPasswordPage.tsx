import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from 'react-router-dom'
import type { z } from 'zod'
import { authApi } from '../../services/api/auth'
import { useSubmit } from '../../hooks/useSubmit'
import { forgotSchema, resetSchema } from '../../validations/auth'
import { TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Alert, Card, ErrorAlert } from '../../components/ui/surface'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState<string | null>(null)

  return (
    <Card title="Recuperar contraseña">
      {email ? (
        <ResetStep email={email} onBack={() => setEmail(null)} />
      ) : (
        <RequestStep onSent={setEmail} />
      )}
      <Button asChild variant="link" size="inline">
        <Link to="/ingresar">Volver al ingreso</Link>
      </Button>
    </Card>
  )
}

function RequestStep({ onSent }: { onSent: (email: string) => void }) {
  const form = useForm<z.infer<typeof forgotSchema>>({ resolver: zodResolver(forgotSchema) })
  const { submit, error, sending } = useSubmit(form, async ({ email }) => {
    await authApi.forgotPassword(email)
    onSent(email)
  })

  return (
    <form onSubmit={submit} noValidate className="space-y-4">
      <p className="text-sm text-slate-400">Te enviaremos un código de 6 dígitos al correo de tu cuenta.</p>
      <ErrorAlert error={error} />
      <TextField
        label="Correo"
        type="email"
        autoComplete="email"
        error={form.formState.errors.email?.message}
        {...form.register('email')}
      />
      <Button type="submit" className="w-full" loading={sending}>
        Enviar código
      </Button>
    </form>
  )
}

function ResetStep({ email, onBack }: { email: string; onBack: () => void }) {
  const navigate = useNavigate()
  const form = useForm<z.infer<typeof resetSchema>>({ resolver: zodResolver(resetSchema) })
  const { submit, error, sending } = useSubmit(form, async ({ code, password }) => {
    await authApi.resetPassword(email, code, password)
    navigate('/ingresar', { replace: true, state: { notice: 'Contraseña actualizada. Ingresa con la nueva.' } })
  })
  const { errors } = form.formState

  return (
    <form onSubmit={submit} noValidate className="space-y-4">
      <Alert>
        Si <strong>{email}</strong> tiene una cuenta, le llegará un código. Vence en 10 minutos.
      </Alert>
      <ErrorAlert error={error} />
      <TextField
        label="Código"
        inputMode="numeric"
        autoComplete="one-time-code"
        maxLength={6}
        error={errors.code?.message}
        {...form.register('code')}
      />
      <TextField
        label="Nueva contraseña"
        type="password"
        autoComplete="new-password"
        hint="Mínimo 10 caracteres, con letras y números"
        error={errors.password?.message}
        {...form.register('password')}
      />
      <TextField
        label="Repite la contraseña"
        type="password"
        autoComplete="new-password"
        error={errors.confirmPassword?.message}
        {...form.register('confirmPassword')}
      />
      <Button type="submit" className="w-full" loading={sending}>
        Cambiar contraseña
      </Button>
      <Button variant="link" size="inline" onClick={onBack}>
        Usar otro correo o pedir otro código
      </Button>
    </form>
  )
}
