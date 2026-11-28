import { z } from 'zod'

export const ratingSchema = z.object({
  stars: z.enum(['1', '2', '3', '4', '5'], { error: 'Elige de 1 a 5 estrellas' }),
  comment: z.string().trim().max(500, 'Máximo 500 caracteres'),
})

export const moderationSchema = z.object({
  reason: z.string().trim().min(10, 'Explica el motivo (mínimo 10 caracteres)').max(300),
})
