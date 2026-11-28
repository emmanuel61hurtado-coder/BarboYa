import { apiClient } from './client'
import type { Page, Role } from './types'

const { http } = apiClient

export type TicketCategory =
  | 'ORDER_PROBLEM'
  | 'DELIVERY_FAILED'
  | 'PAYMENT'
  | 'ACCOUNT'
  | 'PERSONAL_DATA'
  | 'OTHER'
export type TicketStatus = 'OPEN' | 'ANSWERED' | 'RESOLVED'

export interface TicketSummary {
  id: string
  number: number
  category: TicketCategory
  subject: string
  status: TicketStatus
  orderId: string | null
  createdAt: string
  updatedAt: string
  /** Fecha límite legal de respuesta. */
  dueAt: string
}

export interface Ticket {
  ticket: TicketSummary
  /** Solo llega al equipo. */
  opener: { name: string; phone: string; email: string; role: Role } | null
  resolution: string | null
  messages: { staff: boolean; body: string; at: string }[]
}

export interface NewTicket {
  category: TicketCategory
  subject: string
  message: string
  orderId?: string
}

export const supportApi = {
  list: async (page: number) =>
    (await http.get<Page<TicketSummary>>('/support/tickets', { params: { page } })).data,
  get: async (id: string) => (await http.get<Ticket>(`/support/tickets/${id}`)).data,
  open: async (data: NewTicket) => (await http.post<Ticket>('/support/tickets', data)).data,
  reply: async (id: string, body: string) =>
    (await http.post<Ticket>(`/support/tickets/${id}/messages`, { body })).data,
}

export const staffSupportApi = {
  queue: async (status: TicketStatus, page: number) =>
    (await http.get<Page<TicketSummary>>('/admin/support/tickets', { params: { status, page } })).data,
  get: async (id: string) => (await http.get<Ticket>(`/admin/support/tickets/${id}`)).data,
  reply: async (id: string, body: string) =>
    (await http.post<Ticket>(`/admin/support/tickets/${id}/messages`, { body })).data,
  resolve: async (id: string, resolution: string) =>
    (await http.post<Ticket>(`/admin/support/tickets/${id}/resolve`, { resolution })).data,
}
