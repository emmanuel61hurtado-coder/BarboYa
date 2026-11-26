import { z } from 'zod'

// Mismas reglas que valida el backend; este las vuelve a comprobar siempre
const email = z.string().trim().min(1, 'Escribe tu correo').max(254).email('Correo no válido')
const phone = z
  .string()
  .trim()
  .regex(/^\+?[0-9 ()-]{10,20}$/, 'Escribe un celular válido, por ejemplo 300 123 4567')
const fullName = z.string().trim().min(2, 'Escribe tu nombre').max(120, 'Máximo 120 caracteres')
const password = z
  .string()
  .regex(/^(?=.*\p{L})(?=.*\d).{10,128}$/u, 'Usa entre 10 y 128 caracteres, con al menos una letra y un número')
const code = z.string().trim().regex(/^\d{6}$/, 'El código tiene 6 dígitos')

const passwordsMatch = {
  check: (data: { password: string; confirmPassword: string }) => data.password === data.confirmPassword,
  error: { message: 'Las contraseñas no coinciden', path: ['confirmPassword'] },
}

export const loginSchema = z.object({
  identifier: z.string().trim().min(1, 'Escribe tu correo o celular').max(254),
  password: z.string().min(1, 'Escribe tu contraseña').max(128),
})

export const registerSchema = z
  .object({
    role: z.enum(['CUSTOMER', 'COURIER', 'MOTO_DRIVER', 'CAR_DRIVER', 'RESTAURANT', 'STORE']),
    fullName,
    email,
    phone,
    password,
    confirmPassword: z.string(),
  })
  .refine(passwordsMatch.check, passwordsMatch.error)

export const forgotSchema = z.object({ email })

export const resetSchema = z
  .object({ code, password, confirmPassword: z.string() })
  .refine(passwordsMatch.check, passwordsMatch.error)

export const codeSchema = z.object({ code })

export const reasonSchema = z.object({
  reason: z.string().trim().min(5, 'Explica el motivo (mínimo 5 caracteres)').max(300),
})

export const newAdminSchema = z.object({
  fullName,
  email,
  phone,
  password: password.refine((value) => value.length >= 12, 'Los administradores necesitan 12 caracteres o más'),
})
