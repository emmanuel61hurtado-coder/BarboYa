import { z } from 'zod'

export const addressSchema = z.object({
  label: z.string().trim().min(2, 'Ponle un nombre, por ejemplo Casa').max(40, 'Máximo 40 caracteres'),
  address: z.string().trim().min(5, 'Escribe la dirección').max(200),
  notes: z.string().trim().max(300),
  latitude: z.number().min(-90).max(90).nullable(),
  longitude: z.number().min(-180).max(180).nullable(),
})
