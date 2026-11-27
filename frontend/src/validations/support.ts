import { z } from 'zod'

export const ticketSchema = z.object({
  category: z.enum(['ORDER_PROBLEM', 'DELIVERY_FAILED', 'PAYMENT', 'ACCOUNT', 'OTHER']),
  subject: z.string().trim().min(5, 'Resume el problema (mínimo 5 caracteres)').max(120),
  message: z.string().trim().min(10, 'Cuéntanos qué pasó (mínimo 10 caracteres)').max(2000),
})

export const replySchema = z.object({
  body: z.string().trim().min(2, 'Escribe tu mensaje').max(2000),
})

export const resolutionSchema = z.object({
  resolution: z.string().trim().min(10, 'Explica la solución (mínimo 10 caracteres)').max(500),
})

/** El servidor antepone "Cerrado por soporte: " y todo debe caber en el historial. */
export const closeOrderSchema = z.object({
  reason: z.string().trim().min(5, 'Explica el motivo (mínimo 5 caracteres)').max(270, 'Máximo 270 caracteres'),
})
