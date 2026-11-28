import { apiClient } from './client'
import type { Order, OrderStatus } from './commerce'
import type { Page } from './types'
import type { Point } from '../../utils/geo'

const { http } = apiClient

export interface DeliveryOffer {
  id: string
  number: number
  status: OrderStatus
  businessName: string
  businessAddress: string
  pickupDistanceKm: number
  deliveryDistanceKm: number
  feeCop: number
  commissionCop: number
  payAtPickupCop: number
  collectCop: number
}

export interface Earnings {
  todayCop: number
  last7DaysCop: number
  last30DaysCop: number
}

const post = async (path: string, body: unknown = {}) => (await http.post<Order>(`/courier/${path}`, body)).data

export const deliveryApi = {
  presence: async () => (await http.get<{ online: boolean }>('/courier/presence')).data,
  setPresence: async (online: boolean, position?: Point) =>
    (await http.put<{ online: boolean }>('/courier/presence', { online, ...position })).data,
  offers: async () => (await http.get<DeliveryOffer[]>('/courier/offers')).data,
  accept: (id: string) => post(`offers/${id}/accept`),
  /** null cuando no hay ninguna entrega en curso. */
  current: async () => {
    const response = await http.get<Order>('/courier/delivery')
    return response.status === 204 ? null : response.data
  },
  release: async (id: string, reason: string) =>
    void (await http.post(`/courier/delivery/${id}/release`, { reason })),
  pickUp: (id: string, code: string) => post(`delivery/${id}/pickup`, { code }),
  addEvidence: async (id: string, photo: File) => {
    const form = new FormData()
    form.append('file', photo)
    return (await http.post<Order>(`/courier/delivery/${id}/evidence`, form, { timeout: 60_000 })).data
  },
  deliver: (id: string, pin: string) => post(`delivery/${id}/deliver`, { pin }),
  fail: (id: string, reason: string) => post(`delivery/${id}/fail`, { reason }),
  history: async (page: number) => (await http.get<Page<Order>>('/courier/history', { params: { page } })).data,
  earnings: async () => (await http.get<Earnings>('/courier/earnings')).data,
}
