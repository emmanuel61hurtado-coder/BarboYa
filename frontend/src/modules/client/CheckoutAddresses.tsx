import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AddressInput, SavedAddress, addressApi } from '../../services/api/addresses'
import { addressSchema } from '../../validations/addresses'
import { SelectField, TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Alert, ErrorAlert } from '../../components/ui/surface'
import type { Point } from '../../utils/geo'

/** Rellena el pedido con una dirección guardada. No muestra nada si no hay ninguna. */
export function SavedAddressSelect({ onChoose }: { onChoose: (address: SavedAddress) => void }) {
  const addresses = useQuery({ queryKey: ['addresses'], queryFn: addressApi.list })
  const [chosen, setChosen] = useState('')
  if (!addresses.data?.length) return <ErrorAlert error={addresses.error} />

  return (
    <SelectField
      label="Mis direcciones"
      value={chosen}
      onChange={(event) => {
        setChosen(event.target.value)
        const found = addresses.data.find((a) => a.id === event.target.value)
        if (found) onChoose(found)
      }}
    >
      <option value="">Elegir una guardada…</option>
      {addresses.data.map((address) => (
        <option key={address.id} value={address.id}>
          {address.label}: {address.address}
        </option>
      ))}
    </SelectField>
  )
}

/** Guarda lo escrito para el próximo pedido, sin enviar el pedido actual. */
export function SaveAddress({ address, notes, point }: { address: string; notes: string; point: Point | null }) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [label, setLabel] = useState('')
  const [problem, setProblem] = useState<string>()
  const save = useMutation({
    mutationFn: (data: AddressInput) => addressApi.save(data),
    onSuccess: () => {
      setOpen(false)
      return queryClient.invalidateQueries({ queryKey: ['addresses'] })
    },
  })

  const submit = () => {
    const result = addressSchema.safeParse({
      label,
      address,
      notes,
      latitude: point?.latitude ?? null,
      longitude: point?.longitude ?? null,
    })
    setProblem(result.success ? undefined : result.error.issues[0].message)
    if (result.success) save.mutate(result.data)
  }

  if (!open) {
    return (
      <div className="space-y-2">
        {save.isSuccess && <Alert variant="success">Dirección guardada.</Alert>}
        <button
          type="button"
          className="text-xs text-sky-400 hover:underline"
          onClick={() => {
            setLabel('')
            save.reset()
            setOpen(true)
          }}
        >
          Guardar esta dirección
        </button>
      </div>
    )
  }
  return (
    <div className="space-y-2 rounded-xl border border-slate-700 p-3">
      <TextField
        label="Nombre para esta dirección"
        autoFocus
        maxLength={40}
        value={label}
        error={problem}
        onChange={(e) => setLabel(e.target.value)}
        onKeyDown={(e) => {
          // Enter aquí guarda la dirección; no debe confirmar el pedido
          if (e.key === 'Enter') {
            e.preventDefault()
            submit()
          }
        }}
      />
      <ErrorAlert error={save.error} />
      <div className="flex gap-2">
        <Button size="sm" loading={save.isPending} onClick={submit}>
          Guardar
        </Button>
        <Button variant="secondary" size="sm" onClick={() => setOpen(false)}>
          Cancelar
        </Button>
      </div>
    </div>
  )
}
