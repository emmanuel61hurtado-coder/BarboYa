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
  DOCUMENT_UPLOADED: 'Documento subido',
  DOCUMENT_DELETED: 'Documento eliminado',
  DOCUMENT_VIEWED: 'Documento consultado por el equipo',
  DOCUMENT_REVIEWED: 'Documento revisado',
  VERIFICATION_REOPENED: 'Cuenta de vuelta a revisión',
  PAYMENT_REGISTERED: 'Pago de comisiones registrado',
  DEBT_ADJUSTED: 'Deuda perdonada',
  ENTRY_REVERSED: 'Movimiento reversado',
  ORDER_CLOSED_BY_STAFF: 'Pedido cerrado por soporte',
  TICKET_RESOLVED: 'Caso de soporte resuelto',
  RATING_HIDDEN: 'Calificación ocultada',
  RATING_RESTORED: 'Calificación visible de nuevo',
  ACCOUNT_DELETED: 'Cuenta eliminada por su titular',
}

export const actionLabel = (action: string) => ACTION_LABELS[action] ?? action

const dateFormat = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' })

export const formatDate = (iso: string) => dateFormat.format(new Date(iso))

const timeFormat = new Intl.DateTimeFormat('es-CO', { timeStyle: 'short' })

export const formatTime = (iso: string) => timeFormat.format(new Date(iso))
