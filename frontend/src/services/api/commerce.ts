import { ApiError, apiClient } from './client'
import type { Page } from './types'
import type { VehicleType } from './courier'
import type { Point } from '../../utils/geo'
import type { Shift } from '../../utils/hours'

const { http } = apiClient

export type Category = 'RESTAURANT' | 'GROCERY' | 'PHARMACY' | 'BAKERY' | 'OTHER'
export type Fulfillment = 'PICKUP' | 'DELIVERY' | 'COURIER'
export type OrderStatus =
  | 'PLACED'
  | 'PREPARING'
  | 'READY'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'FAILED'

export interface BusinessInput {
  name: string
  legalName: string
  nit: string
  category: Category
  description: string
  address: string
  phone: string
  pickupEnabled: boolean
  deliveryEnabled: boolean
  deliveryFeeCop: number
  courierDeliveryEnabled: boolean
  latitude: number | null
  longitude: number | null
}

export type Business = Omit<BusinessInput, 'legalName' | 'nit' | 'description'> & {
  id: string
  legalName: string | null
  nit: string | null
  description: string | null
  acceptingOrders: boolean
  /** Vacío: no usa horario. */
  hours: Shift[]
}

export interface ProductInput {
  name: string
  description: string
  category: string
  priceCop: number
  stock: number | null
  available: boolean
}

export type Product = Omit<ProductInput, 'description'> & {
  id: string
  description: string | null
  photoUrl: string | null
}

export interface StoreSummary {
  id: string
  name: string
  category: Category
  description: string | null
  address: string
  pickupEnabled: boolean
  deliveryEnabled: boolean
  deliveryFeeCop: number
  open: boolean
  courierDeliveryEnabled: boolean
  latitude: number | null
  longitude: number | null
}

export interface MenuItem {
  id: string
  name: string
  description: string | null
  category: string
  priceCop: number
  soldOut: boolean
  photoUrl: string | null
}

export interface Store {
  business: StoreSummary
  hours: Shift[]
  products: MenuItem[]
}

export interface Order {
  id: string
  number: number
  status: OrderStatus
  fulfillment: Fulfillment
  businessId: string
  businessName: string
  businessPhone: string
  businessAddress: string
  businessLocation: Point | null
  customer: { name: string; phone: string } | null
  deliveryAddress: string | null
  deliveryLocation: Point | null
  distanceKm: number | null
  notes: string | null
  items: { productId: string; name: string; unitPriceCop: number; quantity: number; lineTotalCop: number }[]
  subtotalCop: number
  deliveryFeeCop: number
  totalCop: number
  paymentMethod: 'CASH'
  prepMinutes: number | null
  statusReason: string | null
  deliveryPin: string | null
  pinLocked: boolean
  pickupCode: string | null
  courier: {
    name: string
    phone: string | null
    vehicleType: VehicleType
    plate: string | null
    location: Point | null
  } | null
  hasEvidence: boolean
  /** La comisión de quien consulta; nula para el cliente. */
  commissionCop: number | null
  createdAt: string
  /** Solo mientras espera confirmación: después se cancela solo. */
  respondBy: string | null
  deliveredAt: string | null
  history: { status: OrderStatus; note: string | null; at: string }[]
}

export interface NewOrder {
  businessId: string
  items: { productId: string; quantity: number }[]
  fulfillment: Fulfillment
  deliveryAddress: string
  notes: string
  paymentMethod: 'CASH'
  expectedTotalCop: number
  deliveryLatitude?: number
  deliveryLongitude?: number
}

export type ShopAction =
  | { action: 'accept'; body: { prepMinutes: number } }
  | { action: 'reject' | 'cancel'; body: { reason: string } }
  | { action: 'ready' | 'dispatch' | 'unassign'; body?: undefined }
  | { action: 'deliver'; body: { pin: string } }

/** Un comercio sin datos todavía no es un error. */
async function optional<T>(request: Promise<{ data: T }>): Promise<T | null> {
  try {
    return (await request).data
  } catch (error) {
    if (error instanceof ApiError && error.code === 'BUSINESS_NOT_FOUND') return null
    throw error
  }
}

export const shopApi = {
  business: () => optional(http.get<Business>('/merchant/business')),
  saveBusiness: async (data: BusinessInput) => (await http.put<Business>('/merchant/business', data)).data,
  setAccepting: async (accepting: boolean) =>
    (await http.patch<Business>('/merchant/business/accepting', { accepting })).data,
  setHours: async (hours: Shift[]) => (await http.put<Business>('/merchant/business/hours', { hours })).data,
  products: async () => (await http.get<Product[]>('/merchant/products')).data,
  setPhoto: async (id: string, photo: File) => {
    const form = new FormData()
    form.append('file', photo)
    return (await http.post<Product>(`/merchant/products/${id}/photo`, form, { timeout: 60_000 })).data
  },
  removePhoto: async (id: string) => (await http.delete<Product>(`/merchant/products/${id}/photo`)).data,
  saveProduct: async (data: ProductInput, id?: string) =>
    (id
      ? await http.put<Product>(`/merchant/products/${id}`, data)
      : await http.post<Product>('/merchant/products', data)
    ).data,
  archiveProduct: async (id: string) => void (await http.delete(`/merchant/products/${id}`)),
  orders: async (finished: boolean, page: number) =>
    (await http.get<Page<Order>>('/merchant/orders', { params: { finished, page, size: 20 } })).data,
  act: async (orderId: string, { action, body }: ShopAction) =>
    (await http.post<Order>(`/merchant/orders/${orderId}/${action}`, body ?? {})).data,
  evidence: async (orderId: string) =>
    (await http.get<Blob>(`/merchant/orders/${orderId}/evidence`, { responseType: 'blob' })).data,
  reviewBusiness: (ownerId: string) => optional(http.get<Business>(`/admin/users/${ownerId}/business`)),
}

export const catalogApi = {
  search: async (q: string, category: Category | '', page: number) =>
    (
      await http.get<Page<StoreSummary>>('/catalog/businesses', {
        params: { q: q || undefined, category: category || undefined, page, size: 20 },
      })
    ).data,
  store: async (id: string) => (await http.get<Store>(`/catalog/businesses/${id}`)).data,
  quote: async (id: string, point: Point) =>
    (
      await http.get<{ distanceKm: number; feeCop: number }>(`/catalog/businesses/${id}/delivery-quote`, {
        params: { lat: point.latitude, lng: point.longitude },
      })
    ).data,
}

export const orderApi = {
  /** Repetir con la misma clave devuelve el mismo pedido, nunca crea otro. */
  place: async (order: NewOrder, idempotencyKey: string) =>
    (await http.post<Order>('/orders', order, { headers: { 'Idempotency-Key': idempotencyKey } })).data,
  list: async (page: number) => (await http.get<Page<Order>>('/orders', { params: { page, size: 10 } })).data,
  get: async (id: string) => (await http.get<Order>(`/orders/${id}`)).data,
  cancel: async (id: string) => (await http.post<Order>(`/orders/${id}/cancel`)).data,
  newPin: async (id: string) => (await http.post<Order>(`/orders/${id}/new-pin`)).data,
  evidence: async (id: string) => (await http.get<Blob>(`/orders/${id}/evidence`, { responseType: 'blob' })).data,
}
