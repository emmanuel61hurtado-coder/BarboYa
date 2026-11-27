import { describe, expect, it } from 'vitest'
import { newAdminSchema, registerSchema } from './auth'

const valid = {
  role: 'CUSTOMER',
  fullName: 'Ana Prueba',
  email: 'ana@barboya.test',
  phone: '300 123 4567',
  password: 'Segura12345',
  confirmPassword: 'Segura12345',
  adult: true,
  acceptTerms: true,
  authorizeData: true,
}

describe('validaciones de cuenta', () => {
  it('acepta un registro correcto', () => {
    expect(registerSchema.safeParse(valid).success).toBe(true)
  })

  it.each([
    ['rol administrativo', { role: 'ADMIN' }],
    ['contraseña corta', { password: 'Corta1', confirmPassword: 'Corta1' }],
    ['contraseña sin números', { password: 'SoloLetrasLargas', confirmPassword: 'SoloLetrasLargas' }],
    ['contraseñas distintas', { confirmPassword: 'Otra1234567' }],
    ['correo inválido', { email: 'ana' }],
    ['celular inválido', { phone: '123' }],
    ['un menor de edad', { adult: false }],
    ['términos sin aceptar', { acceptTerms: false }],
    ['datos sin autorizar', { authorizeData: false }],
  ])('rechaza %s', (_, change) => {
    expect(registerSchema.safeParse({ ...valid, ...change }).success).toBe(false)
  })

  it('exige 12 caracteres a los administradores', () => {
    expect(newAdminSchema.safeParse({ ...valid, password: 'Segura12345' }).success).toBe(false)
    expect(newAdminSchema.safeParse({ ...valid, password: 'Segura123456' }).success).toBe(true)
  })
})
