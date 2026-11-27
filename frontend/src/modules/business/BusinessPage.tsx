import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import type { z } from 'zod'
import { Business, shopApi } from '../../services/api/commerce'
import { useCurrentUser } from '../../contexts/AuthContext'
import { useSubmit } from '../../hooks/useSubmit'
import { businessSchema } from '../../validations/commerce'
import { SelectField, TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Alert, Card, ErrorAlert } from '../../components/ui/surface'
import { LocationPicker } from '../../components/map/Map'
import { CATEGORY_LABELS } from '../../utils/commerce'
import { HoursCard } from './HoursCard'

export function BusinessPage() {
  const business = useQuery({ queryKey: ['shop', 'business'], queryFn: shopApi.business })

  return (
    <>
      <h1 className="text-2xl font-display font-black">Mi comercio</h1>
      <ErrorAlert error={business.error} />
      {business.isPending && <p className="text-sm text-slate-400">Cargando…</p>}
      {business.data && <OpenCard business={business.data} />}
      {business.data !== undefined && <BusinessForm business={business.data} />}
      {business.data && <HoursCard business={business.data} />}
    </>
  )
}

function OpenCard({ business }: { business: Business }) {
  const user = useCurrentUser()
  const queryClient = useQueryClient()
  const toggle = useMutation({
    mutationFn: shopApi.setAccepting,
    onSuccess: (updated) => queryClient.setQueryData(['shop', 'business'], updated),
  })

  return (
    <Card title={business.acceptingOrders ? 'Estás recibiendo pedidos' : 'No estás recibiendo pedidos'}>
      {user.status !== 'ACTIVE' && (
        <Alert variant="warning">
          Podrás abrir cuando aprueben tu cuenta. Mientras tanto completa tus datos y productos.
        </Alert>
      )}
      <ErrorAlert error={toggle.error} />
      <Button
        variant={business.acceptingOrders ? 'secondary' : 'primary'}
        disabled={user.status !== 'ACTIVE' && !business.acceptingOrders}
        loading={toggle.isPending}
        onClick={() => toggle.mutate(!business.acceptingOrders)}
      >
        {business.acceptingOrders ? 'Dejar de recibir pedidos' : 'Empezar a recibir pedidos'}
      </Button>
    </Card>
  )
}

function BusinessForm({ business }: { business: Business | null }) {
  const queryClient = useQueryClient()
  const form = useForm<z.infer<typeof businessSchema>>({
    resolver: zodResolver(businessSchema),
    defaultValues: {
      name: business?.name ?? '',
      legalName: business?.legalName ?? '',
      nit: business?.nit ?? '',
      category: business?.category ?? 'RESTAURANT',
      description: business?.description ?? '',
      address: business?.address ?? '',
      phone: business?.phone ?? '',
      pickupEnabled: business?.pickupEnabled ?? true,
      deliveryEnabled: business?.deliveryEnabled ?? false,
      deliveryFeeCop: business?.deliveryFeeCop ?? 0,
      courierDeliveryEnabled: business?.courierDeliveryEnabled ?? false,
      latitude: business?.latitude ?? null,
      longitude: business?.longitude ?? null,
    },
  })
  const [saved, setSaved] = useState(false)
  const { submit, error, sending } = useSubmit(form, async (values) => {
    setSaved(false)
    queryClient.setQueryData(['shop', 'business'], await shopApi.saveBusiness(values))
    form.reset(values)
    setSaved(true)
  })
  const { errors, isDirty } = form.formState
  const delivers = form.watch('deliveryEnabled')
  const [latitude, longitude] = form.watch(['latitude', 'longitude'])

  return (
    <Card title="Datos del comercio" description="El equipo de BarboYa los revisa antes de aprobar tu cuenta.">
      <form onSubmit={submit} noValidate className="space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField label="Nombre del negocio" error={errors.name?.message} {...form.register('name')} />
          <SelectField label="Categoría" {...form.register('category')}>
            {businessSchema.shape.category.options.map((value) => (
              <option key={value} value={value}>
                {CATEGORY_LABELS[value]}
              </option>
            ))}
          </SelectField>
          <TextField label="Razón social (si aplica)" error={errors.legalName?.message} {...form.register('legalName')} />
          <TextField label="NIT (si aplica)" error={errors.nit?.message} {...form.register('nit')} />
          <TextField label="Dirección" error={errors.address?.message} {...form.register('address')} />
          <TextField label="Teléfono del negocio" type="tel" error={errors.phone?.message} {...form.register('phone')} />
        </div>
        <TextField label="Descripción (opcional)" maxLength={500} {...form.register('description')} />
        <fieldset className="space-y-2">
          <legend className="text-xs font-medium text-slate-300 mb-1">Formas de entrega</legend>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" {...form.register('pickupEnabled')} /> El cliente recoge en el local
          </label>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" {...form.register('deliveryEnabled')} /> Entrego a domicilio con mi personal
          </label>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" {...form.register('courierDeliveryEnabled')} /> Usar domiciliarios de BarboYa
          </label>
          {errors.deliveryEnabled && (
            <p role="alert" className="text-xs text-rose-400">
              {errors.deliveryEnabled.message}
            </p>
          )}
        </fieldset>
        {delivers && (
          <div className="max-w-xs">
            <TextField
              label="Valor del domicilio (COP)"
              type="number"
              min={0}
              step={100}
              error={errors.deliveryFeeCop?.message}
              {...form.register('deliveryFeeCop', { valueAsNumber: true })}
            />
          </div>
        )}
        <LocationPicker
          label="Ubicación del comercio (necesaria para los domiciliarios)"
          value={latitude !== null && longitude !== null ? { latitude, longitude } : null}
          onChange={(point) => {
            form.setValue('latitude', point.latitude, { shouldDirty: true })
            form.setValue('longitude', point.longitude, { shouldDirty: true, shouldValidate: true })
          }}
        />
        {errors.latitude && (
          <p role="alert" className="text-xs text-rose-400">
            {errors.latitude.message}
          </p>
        )}
        <ErrorAlert error={error} />
        {saved && !isDirty && <Alert variant="success">Datos guardados.</Alert>}
        <Button type="submit" loading={sending}>
          Guardar datos
        </Button>
      </form>
    </Card>
  )
}
