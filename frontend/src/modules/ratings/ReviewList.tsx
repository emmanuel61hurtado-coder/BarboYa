import type { Reviews } from '../../services/api/ratings'
import { formatDate } from '../../utils/labels'
import { Stars } from './Stars'

export function ReviewList({ items }: { items: Reviews['items'] }) {
  return (
    <ul aria-label="Opiniones" className="divide-y divide-slate-800">
      {items.map((review, index) => (
        <li key={`${review.at}-${index}`} className="py-2 text-sm">
          <div className="flex flex-wrap justify-between gap-2">
            <Stars value={review.stars} />
            <span className="text-xs text-slate-500">{formatDate(review.at)}</span>
          </div>
          {review.comment && <p className="whitespace-pre-wrap break-words text-slate-300">{review.comment}</p>}
        </li>
      ))}
    </ul>
  )
}
