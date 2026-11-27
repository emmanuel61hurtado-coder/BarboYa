import type { TicketCategory } from '../../services/api/support'

export const TICKET_CATEGORY_LABELS: Record<TicketCategory, string> = {
  ORDER_PROBLEM: 'Problema con un pedido',
  DELIVERY_FAILED: 'Entrega fallida',
  PAYMENT: 'Cobros o comisiones',
  ACCOUNT: 'Mi cuenta',
  OTHER: 'Otro',
}

/** Enlace para abrir un caso sobre un pedido concreto. */
export const claimLink = (order: { id: string; number: number }) =>
  `/soporte?pedido=${order.id}&numero=${order.number}`
