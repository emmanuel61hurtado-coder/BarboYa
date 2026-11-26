import type { Fulfillment, MenuItem } from '../../services/api/commerce'

/** Cantidades por producto. Los precios siempre salen del menú vigente. */
export type Cart = Readonly<Record<string, number>>

export const MAX_QUANTITY = 50
export const MAX_PRODUCTS = 30

export function setQuantity(cart: Cart, productId: string, quantity: number): Cart {
  const next = { ...cart }
  if (quantity <= 0) {
    delete next[productId]
  } else if (productId in next || Object.keys(next).length < MAX_PRODUCTS) {
    next[productId] = Math.min(MAX_QUANTITY, quantity)
  }
  return next
}

export function summarize(menu: MenuItem[], cart: Cart, fulfillment: Fulfillment, deliveryFeeCop: number) {
  // Lo que dejó de estar disponible no se cobra ni se envía
  const lines = menu
    .filter((item) => !item.soldOut && (cart[item.id] ?? 0) > 0)
    .map((item) => ({ item, quantity: cart[item.id], totalCop: item.priceCop * cart[item.id] }))
  const subtotalCop = lines.reduce((sum, line) => sum + line.totalCop, 0)
  const feeCop = fulfillment === 'PICKUP' ? 0 : deliveryFeeCop
  return { lines, subtotalCop, feeCop, totalCop: subtotalCop + feeCop }
}
