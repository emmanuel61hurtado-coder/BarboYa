import { describe, expect, it } from 'vitest'
import { dueText } from './due'

describe('plazo de los reclamos', () => {
  // Fin del 18 de diciembre en Colombia
  const dueAt = '2026-12-19T04:59:59Z'

  it('muestra la fecha límite en hora de Colombia', () => {
    expect(dueText({ status: 'OPEN', dueAt }, new Date('2026-12-01T00:00:00Z'))).toEqual({
      overdue: false,
      text: 'Plazo de respuesta: 18 de diciembre',
    })
  })

  it('marca los vencidos y no muestra plazo a los resueltos', () => {
    expect(dueText({ status: 'ANSWERED', dueAt }, new Date('2026-12-20T00:00:00Z'))?.overdue).toBe(true)
    expect(dueText({ status: 'RESOLVED', dueAt })).toBeNull()
  })
})
