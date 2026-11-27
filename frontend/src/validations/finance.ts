import { z } from 'zod'

const amount = z
  .number({ error: 'Escribe un valor en pesos' })
  .int('Sin decimales')
  .min(1, 'Debe ser mayor que cero')

export const paymentSchema = z.object({
  amountCop: amount,
  method: z.enum(['BANK_TRANSFER', 'NEQUI', 'CASH']),
  reference: z.string().trim().regex(/^[A-Za-z0-9 ._#-]{4,80}$/, 'Escribe el número del comprobante'),
  note: z.string().trim().max(300),
})

export const waiveSchema = z.object({
  amountCop: amount,
  reason: z.string().trim().min(10, 'Explica el motivo (mínimo 10 caracteres)').max(300),
})
