import { apiClient } from './client'
import type { AuditEvent, Page, User, UserStatus } from './types'
import type { Order } from './commerce'

const { http } = apiClient

export interface NewAdmin {
  fullName: string
  email: string
  phone: string
  password: string
}

export const adminApi = {
  users: async (page: number, status?: UserStatus) =>
    (await http.get<Page<User>>('/admin/users', { params: { page, size: 20, status } })).data,
  changeStatus: async (id: string, status: 'ACTIVE' | 'SUSPENDED', reason: string) =>
    (await http.patch<User>(`/admin/users/${id}/status`, { status, reason })).data,
  orders: async ({ finished, number, page }: { finished: boolean; number?: number; page: number }) =>
    (await http.get<Page<Order>>('/admin/orders', { params: { finished, number, page } })).data,
  order: async (id: string) => (await http.get<Order>(`/admin/orders/${id}`)).data,
  closeOrder: async (id: string, reason: string) =>
    (await http.post<Order>(`/admin/orders/${id}/close`, { reason })).data,
  createAdmin: async (data: NewAdmin) => (await http.post<User>('/admin/staff', data)).data,
  audit: async (page: number) =>
    (await http.get<Page<AuditEvent>>('/admin/audit', { params: { page, size: 30 } })).data,
}
