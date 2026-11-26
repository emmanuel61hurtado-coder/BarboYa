import type { Category, Fulfillment, OrderStatus } from '../services/api/commerce'

const cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

export const formatCop = (value: number) => cop.format(value)

export const CATEGORY_LABELS: Record<Category, string> = {
  RESTAURANT: 'Restaurante',
  GROCERY: 'Tienda o supermercado',
  PHARMACY: 'Farmacia',
  BAKERY: 'Panadería',
  OTHER: 'Otro',
}

export const FULFILLMENT_LABELS: Record<Fulfillment, string> = {
  DELIVERY: 'A domicilio',
  PICKUP: 'Recoger en el local',
}

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PLACED: 'Esperando confirmación',
  PREPARING: 'En preparación',
  READY: 'Listo',
  OUT_FOR_DELIVERY: 'En camino',
  DELIVERED: 'Entregado',
  REJECTED: 'Rechazado',
  CANCELLED: 'Cancelado',
}
