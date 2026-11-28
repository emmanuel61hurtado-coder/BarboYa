import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { ratingApi } from '../../services/api/ratings'
import { Card, ErrorAlert, Pager } from '../../components/ui/surface'
import { formatRating } from './format'
import { ReviewList } from './ReviewList'

export function MyRatingsPage() {
  const [page, setPage] = useState(0)
  const reviews = useQuery({
    queryKey: ['ratings', 'mine', page],
    queryFn: () => ratingApi.mine(page),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <h1 className="text-2xl font-display font-black">Calificaciones</h1>
      <ErrorAlert error={reviews.error} />
      {reviews.data && (
        <Card
          title={formatRating(reviews.data.summary)}
          description="De pedidos entregados. Si una opinión incumple las normas, repórtala en Soporte."
        >
          <ReviewList items={reviews.data.items} />
          {reviews.data.total > reviews.data.size && <Pager {...reviews.data} onChange={setPage} />}
        </Card>
      )}
    </>
  )
}
