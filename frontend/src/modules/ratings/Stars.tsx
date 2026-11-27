import type { RatingSummary } from '../../services/api/ratings'
import { formatRating } from './format'

export function Stars({ value }: { value: number }) {
  return (
    <span role="img" aria-label={`${value} de 5 estrellas`} className="text-amber-300">
      {'★'.repeat(value)}
      <span className="text-slate-600">{'★'.repeat(5 - value)}</span>
    </span>
  )
}

export function RatingBadge({ summary }: { summary: RatingSummary }) {
  return <span className="text-xs text-amber-300">{formatRating(summary)}</span>
}
