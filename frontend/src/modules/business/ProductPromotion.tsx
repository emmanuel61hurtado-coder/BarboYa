import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import type { z } from 'zod'
import { Product, shopApi } from '../../services/api/commerce'
import { useSubmit } from '../../hooks/useSubmit'
import { promotionSchema } from '../../validations/commerce'
import { TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { ErrorAlert } from '../../components/ui/surface'
import { formatCop } from '../../utils/commerce'
import { formatDay, todayInColombia } from '../../utils/dates'

export function ProductPromotion({ product }: { product: Product }) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['shop', 'products'] })
  const remove = useMutation({ mutationFn: () => shopApi.removePromotion(product.id), onSuccess: refresh })
  const { promotion } = product

  const describe = () => {
    if (!promotion) return null
    const range = `${formatCop(promotion.priceCop)}, del ${formatDay(promotion.startsOn)} al ${formatDay(promotion.endsOn)}`
    if (promotion.active) return `En promoción: ${range}`
    return promotion.endsOn < todayInColombia() ? `Promoción terminada: ${range}` : `Promoción programada: ${range}`
  }

  return (
    <div className="space-y-2">
      {promotion && <p className="text-xs text-amber-300">{describe()}</p>}
      {open ? (
        <PromotionForm
          product={product}
          onSaved={async () => {
            await refresh()
            setOpen(false)
          }}
          onClose={() => setOpen(false)}
        />
      ) : (
        <div className="flex flex-wrap gap-3">
          <button type="button" className="text-xs text-sky-400 hover:underline" onClick={() => setOpen(true)}>
            {promotion ? 'Cambiar promoción' : 'Crear promoción'}
          </button>
          {promotion && (
            <button
              type="button"
              className="text-xs text-slate-400 hover:underline disabled:opacity-40"
              disabled={remove.isPending}
              onClick={() => remove.mutate()}
            >
              Quitar promoción
            </button>
          )}
        </div>
      )}
      <ErrorAlert error={remove.error} />
    </div>
  )
}

function PromotionForm({ product, onSaved, onClose }: {
  product: Product
  onSaved: () => Promise<void>
  onClose: () => void
}) {
  const today = todayInColombia()
  const current = product.promotion && product.promotion.endsOn >= today ? product.promotion : null
  const form = useForm<z.infer<typeof promotionSchema>>({
    resolver: zodResolver(promotionSchema),
    defaultValues: {
      priceCop: current?.priceCop,
      startsOn: current?.startsOn ?? today,
      endsOn: current?.endsOn ?? today,
    },
  })
  const { submit, error, sending } = useSubmit(form, async (values) => {
    await shopApi.setPromotion(product.id, values)
    await onSaved()
  })
  const { errors } = form.formState

  return (
    <form onSubmit={submit} noValidate className="space-y-3 rounded-xl border border-slate-700 p-3">
      <div className="grid gap-3 sm:grid-cols-3">
        <TextField
          label="Precio rebajado (COP)"
          type="number"
          min={100}
          step={100}
          autoFocus
          hint={`Precio normal: ${formatCop(product.priceCop)}`}
          error={errors.priceCop?.message}
          {...form.register('priceCop', { valueAsNumber: true })}
        />
        <TextField label="Desde" type="date" error={errors.startsOn?.message} {...form.register('startsOn')} />
        <TextField label="Hasta (incluido)" type="date" min={today} error={errors.endsOn?.message} {...form.register('endsOn')} />
      </div>
      <ErrorAlert error={error} />
      <div className="flex gap-2">
        <Button type="submit" size="sm" loading={sending}>
          Guardar promoción
        </Button>
        <Button variant="secondary" size="sm" onClick={onClose}>
          Cancelar
        </Button>
      </div>
    </form>
  )
}
