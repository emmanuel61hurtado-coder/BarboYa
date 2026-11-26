import { z } from 'zod'

const money = (min: number, max: number) =>
  z
    .number({ error: 'Escribe un valor en pesos' })
    .int('Sin decimales')
    .min(min, `Mínimo ${min}`)
    .max(max, `Máximo ${max}`)

export const businessSchema = z
  .object({
    name: z.string().trim().min(2, 'Escribe el nombre del negocio').max(120),
    legalName: z.string().trim().max(160),
    nit: z.string().trim().regex(/^$|^[0-9.]{6,15}(-[0-9])?$/, 'NIT no válido, por ejemplo 900123456-7'),
    category: z.enum(['RESTAURANT', 'GROCERY', 'PHARMACY', 'BAKERY', 'OTHER']),
    description: z.string().trim().max(500),
    address: z.string().trim().min(5, 'Escribe la dirección').max(200),
    phone: z.string().trim().regex(/^\+?[0-9 ()-]{7,16}$/, 'Teléfono no válido'),
    pickupEnabled: z.boolean(),
    deliveryEnabled: z.boolean(),
    deliveryFeeCop: money(0, 100000),
  })
  .refine((data) => data.pickupEnabled || data.deliveryEnabled, {
    message: 'Activa al menos una forma de entrega',
    path: ['deliveryEnabled'],
  })

export const productSchema = z.object({
  name: z.string().trim().min(2, 'Escribe el nombre').max(120),
  description: z.string().trim().max(500),
  category: z.string().trim().min(1, 'Escribe una categoría').max(60),
  priceCop: money(100, 10000000),
  stock: z
    .number({ error: 'Escribe un número o deja el campo vacío' })
    .int('Sin decimales')
    .min(0, 'No puede ser negativo')
    .max(100000)
    .nullable(),
  available: z.boolean(),
})

export const checkoutSchema = z
  .object({
    fulfillment: z.enum(['PICKUP', 'DELIVERY']),
    deliveryAddress: z.string().trim().max(200),
    notes: z.string().trim().max(300),
  })
  .refine((data) => data.fulfillment === 'PICKUP' || data.deliveryAddress.length >= 5, {
    message: 'Escribe la dirección de entrega',
    path: ['deliveryAddress'],
  })

export const prepSchema = z.object({
  prepMinutes: z.number({ error: 'Indica los minutos' }).int().min(1, 'Mínimo 1').max(240, 'Máximo 240'),
})

export const pinSchema = z.object({ pin: z.string().trim().regex(/^\d{4}$/, 'El PIN tiene 4 dígitos') })
