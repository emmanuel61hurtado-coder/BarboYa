import { describe, expect, it } from 'vitest'
import { promotionSchema } from '../validations/commerce'
import { formatDay, formatMinutes, todayInColombia } from './dates'

describe('fechas', () => {
  it('el día cambia a la medianoche de Colombia, no a la de Greenwich', () => {
    expect(todayInColombia(new Date('2026-12-01T04:59:00Z'))).toBe('2026-11-30')
    expect(todayInColombia(new Date('2026-12-01T05:00:00Z'))).toBe('2026-12-01')
  })

  it('expresa los plazos de entrega en palabras', () => {
    expect(formatMinutes(45)).toBe('45 minutos')
    expect(formatMinutes(150)).toBe('2 horas y 30 minutos')
    expect(formatMinutes(2880)).toBe('2 días')
  })

  it('una fecha sin hora no se corre de día', () => {
    expect(formatDay('2026-11-30')).toBe('30 de noviembre')
  })

  it('una promoción no puede terminar antes de empezar', () => {
    const promo = { priceCop: 5000, startsOn: '2026-12-02', endsOn: '2026-12-01' }
    expect(promotionSchema.safeParse(promo).success).toBe(false)
    expect(promotionSchema.safeParse({ ...promo, endsOn: '2026-12-02' }).success).toBe(true)
    expect(promotionSchema.safeParse({ ...promo, endsOn: '' }).success).toBe(false)
  })
})
