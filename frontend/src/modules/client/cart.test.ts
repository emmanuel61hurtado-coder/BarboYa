import { describe, expect, it } from 'vitest'
import { MAX_PRODUCTS, setQuantity, summarize } from './cart'

const item = (id: string, priceCop: number, soldOut = false) => ({
  id,
  name: id,
  description: null,
  category: 'General',
  priceCop,
  soldOut,
  photoUrl: null,
  regularPriceCop: null,
  promoEndsOn: null,
})

describe('carrito', () => {
  it('limita las cantidades y quita los productos en cero', () => {
    expect(setQuantity({}, 'a', 99)).toEqual({ a: 50 })
    expect(setQuantity({ a: 1 }, 'a', 0)).toEqual({})
    expect(setQuantity({ a: 1 }, 'a', -3)).toEqual({})
  })

  it('no admite más productos distintos de los permitidos', () => {
    let cart = {}
    for (let i = 0; i < MAX_PRODUCTS + 5; i++) cart = setQuantity(cart, `p${i}`, 1)

    expect(Object.keys(cart)).toHaveLength(MAX_PRODUCTS)
  })

  it('suma el domicilio solo cuando se pide a domicilio', () => {
    const menu = [item('a', 8000), item('b', 2000)]
    const cart = { a: 2, b: 1 }

    expect(summarize(menu, cart, 'DELIVERY', 3000)).toMatchObject({ subtotalCop: 18000, totalCop: 21000 })
    expect(summarize(menu, cart, 'PICKUP', 3000)).toMatchObject({ feeCop: 0, totalCop: 18000 })
  })

  it('no cobra productos agotados ni retirados', () => {
    const summary = summarize([item('a', 8000, true)], { a: 1, borrado: 2 }, 'PICKUP', 0)

    expect(summary.lines).toEqual([])
    expect(summary.totalCop).toBe(0)
  })
})
