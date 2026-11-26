import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import type { z } from 'zod'
import { adminApi } from '../../services/api/admin'
import { useSubmit } from '../../hooks/useSubmit'
import { newAdminSchema } from '../../validations/auth'
import { TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Alert, Card, ErrorAlert } from '../../components/ui/surface'

export function NewAdminPage() {
  const queryClient = useQueryClient()
  const [created, setCreated] = useState<string | null>(null)
  const form = useForm<z.infer<typeof newAdminSchema>>({
    resolver: zodResolver(newAdminSchema),
    defaultValues: { fullName: '', email: '', phone: '', password: '' },
  })
  const { submit, error, sending } = useSubmit(form, async (values) => {
    setCreated(null)
    const admin = await adminApi.createAdmin(values)
    setCreated(admin.email)
    form.reset()
    await queryClient.invalidateQueries({ queryKey: ['admin'] })
  })
  const { errors } = form.formState

  return (
    <>
      <h1 className="text-2xl font-display font-black">Nuevo administrador</h1>
      <Card description="Entrega la contraseña por un canal seguro.">
        {created && <Alert variant="success">Se creó la cuenta de administrador {created}.</Alert>}
        <ErrorAlert error={error} />
        <form onSubmit={submit} noValidate className="max-w-md space-y-4">
          <TextField label="Nombre completo" autoComplete="off" error={errors.fullName?.message} {...form.register('fullName')} />
          <TextField label="Correo" type="email" autoComplete="off" error={errors.email?.message} {...form.register('email')} />
          <TextField label="Celular" type="tel" autoComplete="off" error={errors.phone?.message} {...form.register('phone')} />
          <TextField
            label="Contraseña inicial"
            type="password"
            autoComplete="new-password"
            hint="Mínimo 12 caracteres, con letras y números"
            error={errors.password?.message}
            {...form.register('password')}
          />
          <Button type="submit" loading={sending}>
            Crear administrador
          </Button>
        </form>
      </Card>
    </>
  )
}
