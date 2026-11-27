import { useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useNavigate, useParams } from 'react-router-dom'
import type { z } from 'zod'
import { NewOrder, Store, catalogApi, orderApi } from '../../services/api/commerce'
import { useSubmit } from '../../hooks/useSubmit'
import { checkoutSchema } from '../../validations/commerce'
import { SelectField, TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Alert, Card, ErrorAlert } from '../../components/ui/surface'
import { CATEGORY_LABELS, FULFILLMENT_LABELS, formatCop } from '../../utils/commerce'
import { Cart, setQuantity, summarize } from './cart'
import { LocationPicker } from '../../components/map/Map'
import type { Point } from '../../utils/geo'
import { ratingApi } from '../../services/api/ratings'
import { formatRating } from '../ratings/format'
import { ReviewList } from '../ratings/ReviewList'
import { DAY_LABELS, describeDay, toWeek } from '../../utils/hours'

export function StorePage() {
  const { id = '' } = useParams()
  const store = useQuery({ queryKey: ['catalog', 'store', id], queryFn: () => catalogApi.store(id) })

  if (store.error) return <ErrorAlert error={store.error} />
  if (!store.data) return <p className="text-sm text-slate-400">Cargando…</p>
  return <StoreView key={id} store={store.data} />
}

function StoreReviews({ businessId }: { businessId: string }) {
  const reviews = useQuery({
    queryKey: ['ratings', 'store', businessId],
    queryFn: () => ratingApi.reviews(businessId, 0),
  })
  if (!reviews.data) return <ErrorAlert error={reviews.error} />

  return (
    <Card title="Opiniones" description={formatRating(reviews.data.summary)}>
      {reviews.data.items.length === 0 ? (
        <p className="text-sm text-slate-400">Todavía no hay comentarios.</p>
      ) : (
        <ReviewList items={reviews.data.items.slice(0, 5)} />
      )}
    </Card>
  )
}

function Schedule({ hours }: { hours: Store['hours'] }) {
  if (hours.length === 0) return null
  return (
    <details className="text-sm">
      <summary className="cursor-pointer text-sky-400">Ver horario de atención</summary>
      <dl aria-label="Horario de atención" className="mt-2 max-w-sm space-y-1">
        {toWeek(hours).map((shifts, index) => (
          <div key={DAY_LABELS[index]} className="flex justify-between gap-3">
            <dt className="text-slate-400">{DAY_LABELS[index]}</dt>
            <dd className="text-right">{describeDay(shifts)}</dd>
          </div>
        ))}
      </dl>
    </details>
  )
}

function StoreView({ store }: { store: Store }) {
  const { business, products } = store
  const [cart, setCart] = useState<Cart>({})
  const categories = [...new Set(products.map((p) => p.category))]

  return (
    <>
      <header className="space-y-1">
        <h1 className="text-2xl font-display font-black">{business.name}</h1>
        <p className="text-sm text-slate-400">
          {CATEGORY_LABELS[business.category]} · {business.address}
        </p>
        {business.description && <p className="text-sm text-slate-400">{business.description}</p>}
        <Schedule hours={store.hours} />
      </header>
      {!business.open && (
        <Alert variant="warning">Este comercio no está recibiendo pedidos en este momento.</Alert>
      )}
      <div className="grid gap-5 lg:grid-cols-5 items-start">
        <div className="space-y-4 lg:col-span-3">
          {products.length === 0 && <p className="text-sm text-slate-400">Aún no hay productos publicados.</p>}
          {categories.map((category) => (
            <Card key={category} title={category}>
              <ul className="divide-y divide-slate-800">
                {products
                  .filter((p) => p.category === category)
                  .map((product) => {
                    const quantity = cart[product.id] ?? 0
                    return (
                      <li key={product.id} aria-label={product.name} className="flex items-center justify-between gap-3 py-3">
                        {product.photoUrl && (
                          <img
                            src={product.photoUrl}
                            alt={`Foto de ${product.name}`}
                            loading="lazy"
                            className="h-16 w-16 shrink-0 rounded-xl object-cover"
                          />
                        )}
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-semibold">{product.name}</p>
                          {product.description && <p className="text-xs text-slate-500">{product.description}</p>}
                          <p className="text-sm text-sky-300">{formatCop(product.priceCop)}</p>
                        </div>
                        {product.soldOut ? (
                          <span className="shrink-0 text-xs text-rose-300">Agotado</span>
                        ) : (
                          <div className="flex shrink-0 items-center gap-2">
                            {quantity > 0 && (
                              <>
                                <Button
                                  variant="secondary"
                                  size="sm"
                                  aria-label={`Quitar ${product.name}`}
                                  onClick={() => setCart(setQuantity(cart, product.id, quantity - 1))}
                                >
                                  −
                                </Button>
                                <span className="w-5 text-center text-sm" aria-live="polite">
                                  {quantity}
                                </span>
                              </>
                            )}
                            <Button
                              size="sm"
                              disabled={!business.open}
                              aria-label={`Agregar ${product.name}`}
                              onClick={() => setCart(setQuantity(cart, product.id, quantity + 1))}
                            >
                              +
                            </Button>
                          </div>
                        )}
                      </li>
                    )
                  })}
              </ul>
            </Card>
          ))}
        </div>
        <div className="lg:col-span-2 lg:sticky lg:top-32">
          <Checkout store={store} cart={cart} />
        </div>
      </div>
      <StoreReviews businessId={business.id} />
    </>
  )
}

function Checkout({ store: { business, products }, cart }: { store: Store; cart: Cart }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const attempt = useRef<{ content: string; key: string }>()
  const form = useForm<z.infer<typeof checkoutSchema>>({
    resolver: zodResolver(checkoutSchema),
    defaultValues: {
      fulfillment: business.deliveryEnabled ? 'DELIVERY' : business.courierDeliveryEnabled ? 'COURIER' : 'PICKUP',
      deliveryAddress: '',
      notes: '',
    },
  })
  const fulfillment = form.watch('fulfillment')
  const [destination, setDestination] = useState<Point | null>(null)
  const courier = fulfillment === 'COURIER'
  // La tarifa del domiciliario la calcula el servidor según el punto marcado
  const quote = useQuery({
    queryKey: ['catalog', 'quote', business.id, destination],
    queryFn: () => catalogApi.quote(business.id, destination!),
    enabled: courier && destination !== null,
  })
  const feeCop = courier ? (quote.data?.feeCop ?? 0) : business.deliveryFeeCop
  const summary = summarize(products, cart, fulfillment, feeCop)
  const missingQuote = courier && !quote.data

  const { submit, error, sending } = useSubmit(form, async (values) => {
    const order: NewOrder = {
      ...values,
      businessId: business.id,
      items: summary.lines.map(({ item, quantity }) => ({ productId: item.id, quantity })),
      paymentMethod: 'CASH',
      expectedTotalCop: summary.totalCop,
      ...(values.fulfillment === 'COURIER' && destination
        ? { deliveryLatitude: destination.latitude, deliveryLongitude: destination.longitude }
        : {}),
    }
    // Reintentar el mismo pedido reutiliza la clave; si cambia algo, es otro pedido
    const content = JSON.stringify(order)
    if (attempt.current?.content !== content) attempt.current = { content, key: crypto.randomUUID() }
    try {
      const created = await orderApi.place(order, attempt.current.key)
      await queryClient.invalidateQueries({ queryKey: ['orders'] })
      navigate(`/pedidos/${created.id}`)
    } finally {
      // Precios y existencias pudieron cambiar, con o sin éxito
      void queryClient.invalidateQueries({ queryKey: ['catalog'] })
    }
  })

  return (
    <Card title="Tu pedido">
      {summary.lines.length === 0 ? (
        <p className="text-sm text-slate-400">Agrega productos para continuar.</p>
      ) : (
        <form onSubmit={submit} noValidate className="space-y-4">
          <ul className="space-y-1 text-sm">
            {summary.lines.map(({ item, quantity, totalCop }) => (
              <li key={item.id} className="flex justify-between gap-3">
                <span>
                  {quantity} × {item.name}
                </span>
                <span>{formatCop(totalCop)}</span>
              </li>
            ))}
          </ul>
          <SelectField label="Forma de entrega" {...form.register('fulfillment')}>
            {business.deliveryEnabled && <option value="DELIVERY">{FULFILLMENT_LABELS.DELIVERY}</option>}
            {business.courierDeliveryEnabled && <option value="COURIER">{FULFILLMENT_LABELS.COURIER}</option>}
            {business.pickupEnabled && <option value="PICKUP">{FULFILLMENT_LABELS.PICKUP}</option>}
          </SelectField>
          {courier && (
            <>
              <LocationPicker label="Punto de entrega" value={destination} onChange={setDestination} />
              <ErrorAlert error={quote.error} />
              {quote.data && (
                <p className="text-xs text-slate-400">
                  A {quote.data.distanceKm.toLocaleString('es-CO')} km del comercio, en línea recta.
                </p>
              )}
            </>
          )}
          {fulfillment !== 'PICKUP' && (
            <TextField
              label="Dirección de entrega"
              autoComplete="street-address"
              error={form.formState.errors.deliveryAddress?.message}
              {...form.register('deliveryAddress')}
            />
          )}
          <TextField label="Indicaciones (opcional)" maxLength={300} {...form.register('notes')} />
          <dl className="border-t border-slate-800 pt-3 text-sm space-y-1">
            <div className="flex justify-between text-slate-400">
              <dt>Subtotal</dt>
              <dd>{formatCop(summary.subtotalCop)}</dd>
            </div>
            <div className="flex justify-between text-slate-400">
              <dt>Domicilio</dt>
              <dd>{missingQuote ? 'Marca el punto' : formatCop(summary.feeCop)}</dd>
            </div>
            <div className="flex justify-between text-base font-bold">
              <dt>Total</dt>
              <dd>{formatCop(summary.totalCop)}</dd>
            </div>
          </dl>
          <p className="text-xs text-slate-500">
            {courier ? 'Pagas en efectivo al domiciliario.' : 'Pagas en efectivo al recibir.'} No hay cargos adicionales.
          </p>
          <ErrorAlert error={error} />
          <Button type="submit" className="w-full" loading={sending} disabled={!business.open || missingQuote}>
            {missingQuote ? 'Marca el punto de entrega' : `Confirmar pedido por ${formatCop(summary.totalCop)}`}
          </Button>
        </form>
      )}
    </Card>
  )
}
