// Contratos de la API /api/v1 (backend Spring Boot)

export type Role =
  | 'CUSTOMER'
  | 'COURIER'
  | 'MOTO_DRIVER'
  | 'CAR_DRIVER'
  | 'RESTAURANT'
  | 'STORE'
  | 'ADMIN'
  | 'SUPERADMIN'

export type PublicRole = Exclude<Role, 'ADMIN' | 'SUPERADMIN'>

export type UserStatus = 'ACTIVE' | 'PENDING_VERIFICATION' | 'SUSPENDED'

export interface User {
  id: string
  fullName: string
  email: string
  phone: string
  role: Role
  status: UserStatus
  emailVerified: boolean
  createdAt: string
}

export interface TokenResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresInSeconds: number
  user: User
}

export interface Session {
  id: string
  current: boolean
  ip: string | null
  userAgent: string | null
  createdAt: string
  lastUsedAt: string
}

export interface AccessEvent {
  action: string
  ip: string | null
  userAgent: string | null
  at: string
}

export interface AuditEvent {
  id: string
  actorId: string | null
  targetId: string | null
  action: string
  detail: string | null
  ip: string | null
  at: string
}

export interface Page<T> {
  items: T[]
  page: number
  size: number
  total: number
}
