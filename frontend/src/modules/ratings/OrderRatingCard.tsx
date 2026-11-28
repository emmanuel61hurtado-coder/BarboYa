import { useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import type { z } from 'zod'
import { OrderRatings, RatingTarget, ratingApi } from '../../services/api/ratings'
import { useSubmit } from '../../hooks/useSubmit'
import { ratingSchema } from '../../validations/ratings'
import { TextAreaField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Card, ErrorAlert } from '../../components/ui/surface'
import { Stars } from './Stars'

/** Calificación de un pedido entregado: una vez al comercio y otra al domiciliario. */
export function OrderRatingCard({ orderId, names, ratings }: {
  orderId: string
  names: Record<RatingTarget, string>
  ratings: OrderRatings
}) {
  // Fuera del plazo solo se muestra lo que ya se calificó
  const visible = ratings.targets.filter((t) => t.mine || ratings.openUntil)
  if (visible.length === 0) return null

  return (
    <Card title="Tu calificación" description="No se puede cambiar. Tu nombre no aparece junto al comentario.">
      {visible.map(({ target, mine }) => (
        <section key={target} aria-label={`Calificación de ${names[target]}`} className="space-y-2">
          <h3 className="text-sm font-semibold">{names[target]}</h3>
          {mine ? (
            <p className="text-sm break-words">
              <Stars value={mine.stars} />
              {mine.comment && <span className="block text-slate-400">{mine.comment}</span>}
            </p>
          ) : (
            <RatingForm orderId={orderId} target={target} />
          )}
        </section>
      ))}
    </Card>
  )
}

function RatingForm({ orderId, target }: { orderId: string; target: RatingTarget }) {
  const queryClient = useQueryClient()
  const form = useForm<z.infer<typeof ratingSchema>>({
    resolver: zodResolver(ratingSchema),
    defaultValues: { comment: '' },
  })
  const { submit, error, sending } = useSubmit(form, async ({ stars, comment }) => {
    try {
      await ratingApi.rate(orderId, { target, stars: Number(stars), comment })
    } finally {
      await queryClient.invalidateQueries({ queryKey: ['ratings'] })
    }
  })
  const { errors } = form.formState

  return (
    <form onSubmit={submit} noValidate className="space-y-2">
      <ErrorAlert error={error} />
      <fieldset>
        <legend className="text-xs text-slate-300">Estrellas</legend>
        <div className="mt-1 flex gap-2">
          {[1, 2, 3, 4, 5].map((value) => (
            <label
              key={value}
              className="flex cursor-pointer items-center gap-1 rounded-lg border border-slate-700 px-3 py-2 text-sm has-[:checked]:border-amber-400 has-[:checked]:bg-amber-500/15"
            >
              <input type="radio" value={String(value)} className="accent-amber-400" {...form.register('stars')} />
              {value}
            </label>
          ))}
        </div>
        {errors.stars && (
          <p role="alert" className="mt-1 text-xs text-rose-400">
            {errors.stars.message}
          </p>
        )}
      </fieldset>
      <TextAreaField
        label="Comentario (opcional)"
        rows={2}
        maxLength={500}
        error={errors.comment?.message}
        {...form.register('comment')}
      />
      <Button type="submit" size="sm" loading={sending}>
        Enviar calificación
      </Button>
    </form>
  )
}
