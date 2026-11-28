import { describe, expect, it } from 'vitest'
import { ratingSchema } from '../../validations/ratings'
import { formatRating } from './format'

describe('calificaciones', () => {
  it('no inventa una nota cuando no hay calificaciones', () => {
    expect(formatRating({ average: null, count: 0 })).toBe('Sin calificaciones')
  })

  it('muestra la nota con un decimal y la cantidad', () => {
    expect(formatRating({ average: 4, count: 1 })).toBe('★ 4,0 · 1 calificación')
    expect(formatRating({ average: 3.5, count: 2 })).toBe('★ 3,5 · 2 calificaciones')
  })

  it('exige elegir las estrellas', () => {
    expect(ratingSchema.safeParse({ stars: null, comment: '' }).success).toBe(false)
    expect(ratingSchema.safeParse({ stars: '5', comment: '' }).success).toBe(true)
  })
})
