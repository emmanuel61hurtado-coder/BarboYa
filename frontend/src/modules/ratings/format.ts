import type { RatingSummary } from '../../services/api/ratings'

export function formatRating({ average, count }: RatingSummary) {
  if (average === null || average === undefined || count === 0) return 'Sin calificaciones'
  const value = average.toLocaleString('es-CO', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
  return `★ ${value} · ${count} ${count === 1 ? 'calificación' : 'calificaciones'}`
}
