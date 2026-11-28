import { describe, expect, it } from 'vitest'
import { MAX_FILE_BYTES, courierProfileSchema, uploadProblem } from './courier'

const soat = { type: 'SOAT' as const, label: 'SOAT', required: true, expires: true }
const profile = { documentNumber: '52000111', vehicleType: 'MOTORCYCLE', plate: 'ABC12D', acceptsDataProcessing: true }

describe('datos del domiciliario', () => {
  it('exige placa salvo en bicicleta', () => {
    expect(courierProfileSchema.safeParse({ ...profile, plate: '' }).success).toBe(false)
    expect(courierProfileSchema.safeParse({ ...profile, vehicleType: 'BICYCLE', plate: '' }).success).toBe(true)
  })

  it('exige la autorización', () => {
    expect(courierProfileSchema.safeParse({ ...profile, acceptsDataProcessing: false }).success).toBe(false)
  })

  it('revisa tamaño y vencimiento antes de subir', () => {
    expect(uploadProblem(soat, undefined, '', '2026-01-01')).toBe('Elige un archivo')
    expect(uploadProblem(soat, { size: MAX_FILE_BYTES + 1 }, '2027-01-01', '2026-01-01')).toMatch(/5 MB/)
    expect(uploadProblem(soat, { size: 10 }, '', '2026-01-01')).toMatch(/vencimiento/)
    expect(uploadProblem(soat, { size: 10 }, '2025-12-31', '2026-01-01')).toMatch(/vencido/)
    expect(uploadProblem(soat, { size: 10 }, '2026-01-01', '2026-01-01')).toBeNull()
  })
})
