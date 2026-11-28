import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import type { z } from 'zod'
import { SavedAddress, addressApi } from '../../services/api/addresses'
import { useSubmit } from '../../hooks/useSubmit'
import { addressSchema } from '../../validations/addresses'
import { TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Card, ErrorAlert } from '../../components/ui/surface'
import { LocationPicker } from '../../components/map/Map'

export function AddressesPage() {
  const addresses = useQuery({ queryKey: ['addresses'], queryFn: addressApi.list })
  const [editing, setEditing] = useState<SavedAddress | 'new' | null>(null)

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-display font-black">Mis direcciones</h1>
        {!editing && <Button onClick={() => setEditing('new')}>Nueva dirección</Button>}
      </div>
      <ErrorAlert error={addresses.error} />
      {editing && (
        <AddressForm
          key={editing === 'new' ? 'new' : editing.id}
          current={editing === 'new' ? undefined : editing}
          onClose={() => setEditing(null)}
        />
      )}
      <Card description="Solo tú las ves. Al pedir puedes elegir una en lugar de escribirla.">
        {addresses.data?.length === 0 && <p className="text-sm text-slate-400">Aún no has guardado direcciones.</p>}
        <ul className="divide-y divide-slate-800">
          {addresses.data?.map((item) => (
            <AddressRow key={item.id} item={item} onEdit={() => setEditing(item)} />
          ))}
        </ul>
      </Card>
    </>
  )
}

function AddressRow({ item, onEdit }: { item: SavedAddress; onEdit: () => void }) {
  const queryClient = useQueryClient()
  const [confirming, setConfirming] = useState(false)
  const remove = useMutation({
    mutationFn: () => addressApi.remove(item.id),
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['addresses'] }),
  })

  return (
    <li aria-label={item.label} className="space-y-2 py-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="min-w-0 text-sm break-words">
          <p className="font-semibold">{item.label}</p>
          <p className="text-slate-400">{item.address}</p>
          {item.notes && <p className="text-xs text-slate-500">{item.notes}</p>}
          <p className="text-xs text-slate-500">
            {item.latitude === null ? 'Sin punto en el mapa' : 'Con punto en el mapa'}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" onClick={onEdit}>
            Editar
          </Button>
          {confirming ? (
            <>
              <Button variant="destructive" size="sm" loading={remove.isPending} onClick={() => remove.mutate()}>
                Confirmar
              </Button>
              <Button variant="secondary" size="sm" onClick={() => setConfirming(false)}>
                Volver
              </Button>
            </>
          ) : (
            <Button variant="secondary" size="sm" onClick={() => setConfirming(true)}>
              Eliminar
            </Button>
          )}
        </div>
      </div>
      <ErrorAlert error={remove.error} />
    </li>
  )
}

function AddressForm({ current, onClose }: { current?: SavedAddress; onClose: () => void }) {
  const queryClient = useQueryClient()
  const form = useForm<z.infer<typeof addressSchema>>({
    resolver: zodResolver(addressSchema),
    defaultValues: {
      label: current?.label ?? '',
      address: current?.address ?? '',
      notes: current?.notes ?? '',
      latitude: current?.latitude ?? null,
      longitude: current?.longitude ?? null,
    },
  })
  const { submit, error, sending } = useSubmit(form, async (values) => {
    await addressApi.save(values, current?.id)
    await queryClient.invalidateQueries({ queryKey: ['addresses'] })
    onClose()
  })
  const { errors } = form.formState
  const [latitude, longitude] = form.watch(['latitude', 'longitude'])
  const point = latitude !== null && longitude !== null ? { latitude, longitude } : null
  const setPoint = (next: { latitude: number; longitude: number } | null) => {
    form.setValue('latitude', next?.latitude ?? null)
    form.setValue('longitude', next?.longitude ?? null)
  }

  return (
    <Card title={current ? 'Editar dirección' : 'Nueva dirección'}>
      <form onSubmit={submit} noValidate className="space-y-4">
        <TextField label="Nombre" hint="Casa, trabajo…" autoFocus error={errors.label?.message} {...form.register('label')} />
        <TextField label="Dirección" error={errors.address?.message} {...form.register('address')} />
        <TextField label="Indicaciones (opcional)" maxLength={300} {...form.register('notes')} />
        <LocationPicker label="Punto en el mapa (para pedir con domiciliario)" value={point} onChange={setPoint} />
        {point && (
          <button type="button" className="text-xs text-slate-400 hover:underline" onClick={() => setPoint(null)}>
            Quitar el punto
          </button>
        )}
        <ErrorAlert error={error} />
        <div className="flex gap-2">
          <Button type="submit" loading={sending}>
            Guardar dirección
          </Button>
          <Button variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
        </div>
      </form>
    </Card>
  )
}
