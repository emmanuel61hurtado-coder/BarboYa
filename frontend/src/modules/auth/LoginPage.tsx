import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useLocation } from 'react-router-dom'
import type { z } from 'zod'
import { useAuth } from '../../contexts/AuthContext'
import { useSubmit } from '../../hooks/useSubmit'
import { loginSchema } from '../../validations/auth'
import { TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Alert, Card, ErrorAlert } from '../../components/ui/surface'

export function LoginPage() {
  const { signIn } = useAuth()
  const notice = (useLocation().state as { notice?: string } | null)?.notice
  const form = useForm<z.infer<typeof loginSchema>>({ resolver: zodResolver(loginSchema) })
  // Al ingresar, las rutas redirigen solas a la pantalla pedida
  const { submit, error, sending } = useSubmit(form, (values) => signIn(values.identifier, values.password))
  const { errors } = form.formState

  return (
    <Card title="Iniciar sesión">
      {notice && <Alert variant="success">{notice}</Alert>}
      <ErrorAlert error={error} />
      <form onSubmit={submit} noValidate className="space-y-4">
        <TextField
          label="Correo o celular"
          autoComplete="username"
          error={errors.identifier?.message}
          {...form.register('identifier')}
        />
        <TextField
          label="Contraseña"
          type="password"
          autoComplete="current-password"
          error={errors.password?.message}
          {...form.register('password')}
        />
        <Button type="submit" className="w-full" loading={sending}>
          Ingresar
        </Button>
      </form>
      <div className="flex justify-between text-sm">
        <Button asChild variant="link" size="inline">
          <Link to="/recuperar">Olvidé mi contraseña</Link>
        </Button>
        <Button asChild variant="link" size="inline">
          <Link to="/registro">Crear cuenta</Link>
        </Button>
      </div>
    </Card>
  )
}
