import { z } from 'zod'
import type { Requirement } from '../services/api/courier'

export const MAX_FILE_BYTES = 5 * 1024 * 1024

export const courierProfileSchema = z
  .object({
    documentNumber: z.string().trim().regex(/^[A-Za-z0-9]{5,20}$/, 'Solo letras y números, sin puntos'),
    vehicleType: z.enum(['BICYCLE', 'MOTORCYCLE', 'CAR']),
    plate: z.string().trim().regex(/^$|^[A-Za-z0-9-]{5,8}$/, 'Placa no válida'),
    acceptsDataProcessing: z.boolean().refine((value) => value, 'Necesitamos tu autorización para continuar'),
  })
  .refine((data) => data.vehicleType === 'BICYCLE' || data.plate !== '', {
    message: 'Escribe la placa',
    path: ['plate'],
  })

/** @returns el motivo por el que no se puede subir, o null */
export function uploadProblem(
  requirement: Requirement,
  file: { size: number } | undefined,
  expiresOn: string,
  today: string,
): string | null {
  if (!file) return 'Elige un archivo'
  if (file.size === 0 || file.size > MAX_FILE_BYTES) return 'El archivo debe pesar máximo 5 MB'
  if (requirement.expires && !expiresOn) return 'Indica la fecha de vencimiento'
  if (requirement.expires && expiresOn < today) return 'El documento está vencido'
  return null
}
