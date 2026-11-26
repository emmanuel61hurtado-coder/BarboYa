import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from 'react-router-dom'
import type { z } from 'zod'
import { useAuth } from '../../contexts/AuthContext'
import { useSubmit } from '../../hooks/useSubmit'
import { registerSchema } from '../../validations/auth'
import { SelectField, TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Alert, Card, ErrorAlert } from '../../components/ui/surface'
import { ROLE_LABELS } from '../../utils/labels'

export function RegisterPage() {
  const { signUp } = useAuth()
  const form = useForm<z.infer<typeof registerSchema>>({
    resolver: zodResolver(registerSchema),
    defaultValues: { role: 'CUSTOMER' },
  })
  const { submit, error, sending } = useSubmit(form, ({ confirmPassword: _, ...data }) => signUp(data))
  const { errors } = form.formState
  const role = form.watch('role')
  const transport = role === 'MOTO_DRIVER' || role === 'CAR_DRIVER'

  return (
    <Card title="Crear cuenta">
      <ErrorAlert error={error} />
      <form onSubmit={submit} noValidate className="space-y-4">
        <SelectField label="Tipo de cuenta" error={errors.role?.message} {...form.register('role')}>
          {registerSchema.shape.role.options.map((option) => (
            <option key={option} value={option}>
              {ROLE_LABELS[option]}
            </option>
          ))}
        </SelectField>
        {role !== 'CUSTOMER' && (
          <Alert variant="warning">
            Tu cuenta quedará pendiente hasta que una persona la revise.
            {transport && ' Registrarte no significa que esta modalidad de transporte ya esté habilitada.'}
          </Alert>
        )}
        <TextField label="Nombre completo" autoComplete="name" error={errors.fullName?.message} {...form.register('fullName')} />
        <TextField label="Correo" type="email" autoComplete="email" error={errors.email?.message} {...form.register('email')} />
        <TextField label="Celular" type="tel" autoComplete="tel" error={errors.phone?.message} {...form.register('phone')} />
        <TextField
          label="Contraseña"
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
          Crear cuenta
        </Button>
      </form>
      <Button asChild variant="link" size="inline">
        <Link to="/ingresar">Ya tengo cuenta</Link>
      </Button>
    </Card>
  )
}
