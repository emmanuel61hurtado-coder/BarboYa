import { apiClient } from './client'
import type { AccessEvent, PublicRole, Session, TokenResponse, User } from './types'

const { http } = apiClient

export interface Registration {
  adult: boolean
  /** Versiones que la persona vio al aceptar. */
  termsVersion: string
  privacyVersion: string
  fullName: string
  email: string
  phone: string
  password: string
  role: PublicRole
}

async function openSession(url: string, body: unknown): Promise<User> {
  const { data } = await http.post<TokenResponse>(url, body)
  apiClient.setAccessToken(data.accessToken)
  return data.user
}

export const authApi = {
  register: (data: Registration) => openSession('/auth/register', data),
  login: (identifier: string, password: string) => openSession('/auth/login', { identifier, password }),
  restore: async () => (await apiClient.refresh()).user,
  logout: async () => {
    await http.post('/auth/logout')
    apiClient.setAccessToken(null)
  },
  me: async () => (await http.get<User>('/users/me')).data,

  sessions: async () => (await http.get<Session[]>('/auth/sessions')).data,
  closeSession: async (id: string) => void (await http.delete(`/auth/sessions/${id}`)),
  closeOtherSessions: async () => void (await http.delete('/auth/sessions')),
  history: async () => (await http.get<AccessEvent[]>('/auth/history')).data,

  requestEmailCode: async () => void (await http.post('/auth/email/verification')),
  verifyEmail: async (code: string) => void (await http.post('/auth/email/verify', { code })),
  forgotPassword: async (email: string) => void (await http.post('/auth/password/forgot', { email })),
  resetPassword: async (email: string, code: string, newPassword: string) =>
    void (await http.post('/auth/password/reset', { email, code, newPassword })),
}
