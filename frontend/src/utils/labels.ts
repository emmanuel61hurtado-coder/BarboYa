import type { Role, UserStatus } from '../services/api/types'

export const ROLE_LABELS: Record<Role, string> = {
  CUSTOMER: 'Cliente',
  COURIER: 'Domiciliario',
  MOTO_DRIVER: 'Mototaxista',
  CAR_DRIVER: 'Conductor de automóvil',
  RESTAURANT: 'Restaurante',
  STORE: 'Tienda o comercio',
  ADMIN: 'Administrador',
  SUPERADMIN: 'Superadministrador',
}

export const STATUS_LABELS: Record<UserStatus, string> = {
  ACTIVE: 'Activa',
  PENDING_VERIFICATION: 'Pendiente de revisión',
  SUSPENDED: 'Suspendida',
}

const ACTION_LABELS: Record<string, string> = {
  REGISTERED: 'Cuenta creada',
  LOGIN_SUCCESS: 'Ingreso',
  LOGIN_NEW_DEVICE: 'Ingreso desde un dispositivo nuevo',
  LOGIN_FAILED: 'Intento de ingreso fallido',
  LOGIN_LOCKED: 'Cuenta bloqueada por intentos fallidos',
  LOGIN_BLOCKED: 'Ingreso bloqueado',
  LOGOUT: 'Cierre de sesión',
  SESSION_REVOKED: 'Sesión cerrada a distancia',
  REFRESH_TOKEN_REUSED: 'Sesión cerrada por posible copia',
  EMAIL_VERIFIED: 'Correo verificado',
  PASSWORD_RESET: 'Contraseña restablecida',
  USER_STATUS_CHANGED: 'Cambio de estado de la cuenta',
  STAFF_CREATED: 'Cuenta interna creada',
}

export const actionLabel = (action: string) => ACTION_LABELS[action] ?? action

const dateFormat = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' })

export const formatDate = (iso: string) => dateFormat.format(new Date(iso))
