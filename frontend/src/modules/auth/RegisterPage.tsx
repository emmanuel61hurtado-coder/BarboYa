import * as React from 'react'
import { useQuery } from '@tanstack/react-query'
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
import { legalApi } from '../../services/api/legal'

const Consent = React.forwardRef<
  HTMLInputElement,
  React.InputHTMLAttributes<HTMLInputElement> & { error?: string; children: React.ReactNode }
>(({ error, children, ...props }, ref) => (
  <div>
    <label className="flex items-start gap-2">
      <input ref={ref} type="checkbox" className="mt-1" aria-invalid={Boolean(error)} {...props} />
      <span>{children}</span>
    </label>
    {error && (
      <p role="alert" className="mt-1 text-xs text-rose-400">
        {error}
      </p>
    )}
  </div>
))
Consent.displayName = 'Consent'

/** Se abre aparte para no perder lo escrito en el formulario. */
const DocumentLink = ({ to, children }: { to: string; children: React.ReactNode }) => (
  <a href={to} target="_blank" rel="noreferrer" className="text-sky-400 underline">
    {children}
  </a>
)

export function RegisterPage() {
  const { signUp } = useAuth()
  const legal = useQuery({ queryKey: ['legal', 'info'], queryFn: legalApi.info })
  const form = useForm<z.infer<typeof registerSchema>>({
    resolver: zodResolver(registerSchema),
    defaultValues: { role: 'CUSTOMER', adult: false, acceptTerms: false, authorizeData: false },
  })
  const { submit, error, sending } = useSubmit(
    form,
    ({ confirmPassword: _, acceptTerms: __, authorizeData: ___, ...data }) =>
      signUp({ ...data, termsVersion: legal.data!.termsVersion, privacyVersion: legal.data!.privacyVersion }),
  )
  const { errors } = form.formState
  const role = form.watch('role')
  const transport = role === 'MOTO_DRIVER' || role === 'CAR_DRIVER'
  // El transporte de pasajeros solo aparece si el servidor lo tiene habilitado
  const roles = registerSchema.shape.role.options.filter(
    (option) => legal.data?.driverRegistration || (option !== 'MOTO_DRIVER' && option !== 'CAR_DRIVER'),
  )

  return (
    <Card title="Crear cuenta">
      <ErrorAlert error={error ?? legal.error} />
      <form onSubmit={submit} noValidate className="space-y-4">
        <SelectField label="Tipo de cuenta" error={errors.role?.message} {...form.register('role')}>
          {roles.map((option) => (
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
        <div className="space-y-3 text-sm text-slate-300">
          <Consent error={errors.adult?.message} {...form.register('adult')}>
            Soy mayor de 18 años.
          </Consent>
          <Consent error={errors.acceptTerms?.message} {...form.register('acceptTerms')}>
            Acepto los <DocumentLink to="/terminos">términos y condiciones</DocumentLink>.
          </Consent>
          <Consent error={errors.authorizeData?.message} {...form.register('authorizeData')}>
            Autorizo el tratamiento de mis datos personales según la{' '}
            <DocumentLink to="/privacidad">política de datos</DocumentLink>.
          </Consent>
        </div>
        <Button type="submit" className="w-full" loading={sending} disabled={!legal.data}>
          Crear cuenta
        </Button>
      </form>
      <Button asChild variant="link" size="inline">
        <Link to="/ingresar">Ya tengo cuenta</Link>
      </Button>
    </Card>
  )
}
