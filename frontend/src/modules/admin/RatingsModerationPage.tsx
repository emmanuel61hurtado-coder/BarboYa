import { useState } from 'react'
import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from 'react-router-dom'
import type { z } from 'zod'
import { ModeratedRating, ratingApi } from '../../services/api/ratings'
import { useSubmit } from '../../hooks/useSubmit'
import { moderationSchema } from '../../validations/ratings'
import { TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Card, ErrorAlert, Pager } from '../../components/ui/surface'
import { formatDate } from '../../utils/labels'
import { Stars } from '../ratings/Stars'

export function RatingsModerationPage() {
  const [hidden, setHidden] = useState(false)
  const [page, setPage] = useState(0)
  const ratings = useQuery({
    queryKey: ['ratings', 'moderation', hidden, page],
    queryFn: () => ratingApi.moderation(hidden, page),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <h1 className="text-2xl font-display font-black">Calificaciones</h1>
      <div className="flex gap-2" role="group" aria-label="Filtro de calificaciones">
        {[false, true].map((value) => (
          <Button
            key={String(value)}
            size="sm"
            variant={hidden === value ? 'primary' : 'secondary'}
            aria-pressed={hidden === value}
            onClick={() => {
              setHidden(value)
              setPage(0)
            }}
          >
            {value ? 'Ocultas' : 'Visibles'}
          </Button>
        ))}
      </div>
      <Card>
        <ErrorAlert error={ratings.error} />
        {ratings.data?.total === 0 && <p className="text-sm text-slate-400">No hay calificaciones en esta lista.</p>}
        <ul className="divide-y divide-slate-800">
          {ratings.data?.items.map((rating) => <Row key={rating.id} rating={rating} />)}
        </ul>
        {ratings.data && ratings.data.total > ratings.data.size && <Pager {...ratings.data} onChange={setPage} />}
      </Card>
    </>
  )
}

function Row({ rating }: { rating: ModeratedRating }) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const form = useForm<z.infer<typeof moderationSchema>>({
    resolver: zodResolver(moderationSchema),
    defaultValues: { reason: '' },
  })
  const { submit, error, sending } = useSubmit(form, async ({ reason }) => {
    try {
      await ratingApi.moderate(rating.id, rating.hidden ? 'restore' : 'hide', reason)
    } finally {
      await queryClient.invalidateQueries({ queryKey: ['ratings'] })
    }
  })
  const action = rating.hidden ? 'Volver a mostrar' : 'Ocultar'

  return (
    <li aria-label={`${rating.authorName} sobre ${rating.targetName}`} className="space-y-2 py-3 text-sm">
      <div className="flex flex-wrap justify-between gap-2">
        <p>
          <Stars value={rating.stars} /> {rating.authorName} sobre {rating.targetName}
        </p>
        <span className="text-xs text-slate-500">{formatDate(rating.at)}</span>
      </div>
      {rating.comment && <p className="whitespace-pre-wrap break-words text-slate-300">{rating.comment}</p>}
      {rating.hiddenReason && <p className="text-xs text-rose-300">Oculta: {rating.hiddenReason}</p>}
      <div className="flex flex-wrap gap-3">
        <Link to={`/admin/pedidos?pedido=${rating.orderId}`} className="text-xs text-sky-400 hover:underline">
          Ver el pedido
        </Link>
        {!open && (
          <Button variant="secondary" size="sm" onClick={() => setOpen(true)}>
            {action}
          </Button>
        )}
      </div>
      {open && (
        <form onSubmit={submit} noValidate className="space-y-2">
          <ErrorAlert error={error} />
          <TextField
            label="Motivo"
            autoFocus
            maxLength={300}
            error={form.formState.errors.reason?.message}
            {...form.register('reason')}
          />
          <div className="flex gap-2">
            <Button type="submit" size="sm" loading={sending}>
              Confirmar
            </Button>
            <Button variant="secondary" size="sm" onClick={() => setOpen(false)}>
              Volver
            </Button>
          </div>
        </form>
      )}
    </li>
  )
}
