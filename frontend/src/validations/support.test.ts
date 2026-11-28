import { describe, expect, it } from 'vitest'
import { closeOrderSchema, ticketSchema } from './support'

describe('soporte', () => {
  it('no acepta un reclamo solo con espacios', () => {
    const result = ticketSchema.safeParse({ category: 'OTHER', subject: '        ', message: '            ' })
    expect(result.success).toBe(false)
  })

  it('limita el motivo de cierre a lo que cabe en el historial', () => {
    expect(closeOrderSchema.safeParse({ reason: 'x'.repeat(270) }).success).toBe(true)
    expect(closeOrderSchema.safeParse({ reason: 'x'.repeat(271) }).success).toBe(false)
  })
})
