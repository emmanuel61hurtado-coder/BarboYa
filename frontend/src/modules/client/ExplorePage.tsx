import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Category, catalogApi } from '../../services/api/commerce'
import { SelectField, TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Card, ErrorAlert, Pager } from '../../components/ui/surface'
import { CATEGORY_LABELS, FULFILLMENT_LABELS, formatCop } from '../../utils/commerce'
import { ratingApi } from '../../services/api/ratings'
import { RatingBadge } from '../ratings/Stars'

export function ExplorePage() {
  const [text, setText] = useState('')
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState<Category | ''>('')
  const [page, setPage] = useState(0)
  const stores = useQuery({
    queryKey: ['catalog', search, category, page],
    queryFn: () => catalogApi.search(search, category, page),
    placeholderData: keepPreviousData,
  })

  const ids = stores.data?.items.map((store) => store.id) ?? []
  const ratings = useQuery({
    queryKey: ['ratings', 'stores', ids],
    queryFn: () => ratingApi.summaries(ids),
    enabled: ids.length > 0,
  })

  return (
    <>
      <h1 className="text-2xl font-display font-black">Comercios</h1>
      <Card>
        <form
          role="search"
          className="flex flex-wrap items-end gap-3"
          onSubmit={(event) => {
            event.preventDefault()
            setSearch(text.trim())
            setPage(0)
          }}
        >
          <div className="flex-1 min-w-[12rem]">
            <TextField label="Buscar comercio o producto" value={text} onChange={(e) => setText(e.target.value)} />
          </div>
          <div className="w-52">
            <SelectField
              label="Categoría"
              value={category}
              onChange={(e) => {
                setCategory(e.target.value as Category | '')
                setPage(0)
              }}
            >
              <option value="">Todas</option>
              {(Object.keys(CATEGORY_LABELS) as Category[]).map((value) => (
                <option key={value} value={value}>
                  {CATEGORY_LABELS[value]}
                </option>
              ))}
            </SelectField>
          </div>
          <Button type="submit">Buscar</Button>
        </form>
      </Card>
      <ErrorAlert error={stores.error} />
      {stores.isPending && <p className="text-sm text-slate-400">Cargando…</p>}
      {stores.data?.items.length === 0 && (
        <p className="text-sm text-slate-400">No encontramos comercios con esa búsqueda.</p>
      )}
      <ul className="grid gap-3 sm:grid-cols-2">
        {stores.data?.items.map((store) => (
          <li key={store.id}>
            <Link to={`/comercios/${store.id}`} className="block h-full pro-card rounded-2xl p-4 space-y-1">
              <div className="flex items-start justify-between gap-2">
                <h2 className="font-bold">{store.name}</h2>
                <span className={store.open ? 'text-xs text-emerald-400' : 'text-xs text-slate-500'}>
                  {store.open ? 'Abierto' : 'Cerrado'}
                </span>
              </div>
              <p className="text-xs text-slate-400">
                {CATEGORY_LABELS[store.category]} · {store.address}
              </p>
              {ratings.data?.[store.id] && <RatingBadge summary={ratings.data[store.id]} />}
              <p className="text-xs text-slate-500">
                {[
                  store.deliveryEnabled && `${FULFILLMENT_LABELS.DELIVERY} ${formatCop(store.deliveryFeeCop)}`,
                  store.courierDeliveryEnabled && FULFILLMENT_LABELS.COURIER,
                  store.pickupEnabled && FULFILLMENT_LABELS.PICKUP,
                ]
                  .filter(Boolean)
                  .join(' · ')}
              </p>
            </Link>
          </li>
        ))}
      </ul>
      {stores.data && stores.data.total > stores.data.size && <Pager {...stores.data} onChange={setPage} />}
    </>
  )
}
