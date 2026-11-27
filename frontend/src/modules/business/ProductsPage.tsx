import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from 'react-router-dom'
import type { z } from 'zod'
import { Product, shopApi } from '../../services/api/commerce'
import { useSubmit } from '../../hooks/useSubmit'
import { productSchema } from '../../validations/commerce'
import { TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Card, ErrorAlert } from '../../components/ui/surface'
import { formatCop } from '../../utils/commerce'
import { ProductPhoto } from './ProductPhoto'

export function ProductsPage() {
  const business = useQuery({ queryKey: ['shop', 'business'], queryFn: shopApi.business })
  const products = useQuery({
    queryKey: ['shop', 'products'],
    queryFn: shopApi.products,
    enabled: Boolean(business.data),
  })
  const [editing, setEditing] = useState<Product | 'new' | null>(null)

  if (business.data === null) {
    return (
      <Card title="Primero registra tu comercio">
        <Button asChild>
          <Link to="/mi-comercio">Completar los datos</Link>
        </Button>
      </Card>
    )
  }

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-display font-black">Productos</h1>
        {!editing && <Button onClick={() => setEditing('new')}>Nuevo producto</Button>}
      </div>
      <ErrorAlert error={business.error ?? products.error} />
      {editing && (
        <ProductForm
          key={editing === 'new' ? 'new' : editing.id}
          product={editing === 'new' ? undefined : editing}
          onClose={() => setEditing(null)}
        />
      )}
      <Card>
        {products.data?.length === 0 && <p className="text-sm text-slate-400">Aún no tienes productos.</p>}
        <ul className="divide-y divide-slate-800">
          {products.data?.map((product) => (
            <ProductRow key={product.id} product={product} onEdit={() => setEditing(product)} />
          ))}
        </ul>
      </Card>
    </>
  )
}

function ProductRow({ product, onEdit }: { product: Product; onEdit: () => void }) {
  const queryClient = useQueryClient()
  const [confirming, setConfirming] = useState(false)
  const archive = useMutation({
    mutationFn: () => shopApi.archiveProduct(product.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['shop', 'products'] }),
  })
  const stock = product.stock === null ? 'Sin control de existencias' : `Existencias: ${product.stock}`

  return (
    <li aria-label={product.name} className="py-3 space-y-2">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-semibold">{product.name}</p>
          <p className="text-xs text-slate-400">
            {product.category} · {formatCop(product.priceCop)} · {stock}
            {!product.available && ' · No disponible'}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" onClick={onEdit}>
            Editar
          </Button>
          {confirming ? (
            <>
              <Button variant="destructive" size="sm" loading={archive.isPending} onClick={() => archive.mutate()}>
                Confirmar retiro
              </Button>
              <Button variant="secondary" size="sm" onClick={() => setConfirming(false)}>
                Volver
              </Button>
            </>
          ) : (
            <Button variant="secondary" size="sm" onClick={() => setConfirming(true)}>
              Retirar
            </Button>
          )}
        </div>
      </div>
      <ProductPhoto product={product} />
      <ErrorAlert error={archive.error} />
    </li>
  )
}

function ProductForm({ product, onClose }: { product?: Product; onClose: () => void }) {
  const queryClient = useQueryClient()
  const form = useForm<z.infer<typeof productSchema>>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      name: product?.name ?? '',
      description: product?.description ?? '',
      category: product?.category ?? '',
      priceCop: product?.priceCop,
      stock: product?.stock ?? null,
      available: product?.available ?? true,
    },
  })
  const { submit, error, sending } = useSubmit(form, async (values) => {
    await shopApi.saveProduct(values, product?.id)
    await queryClient.invalidateQueries({ queryKey: ['shop', 'products'] })
    onClose()
  })
  const { errors } = form.formState

  return (
    <Card title={product ? 'Editar producto' : 'Nuevo producto'}>
      <form onSubmit={submit} noValidate className="space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField label="Nombre" autoFocus error={errors.name?.message} {...form.register('name')} />
          <TextField label="Categoría del menú" error={errors.category?.message} {...form.register('category')} />
          <TextField
            label="Precio (COP)"
            type="number"
            min={100}
            step={100}
            error={errors.priceCop?.message}
            {...form.register('priceCop', { valueAsNumber: true })}
          />
          <TextField
            label="Existencias (opcional)"
            type="number"
            min={0}
            hint="Vacío: no se controlan"
            error={errors.stock?.message}
            {...form.register('stock', {
              setValueAs: (value) => (value === '' || value === null ? null : Number(value)),
            })}
          />
        </div>
        <TextField label="Descripción (opcional)" maxLength={500} {...form.register('description')} />
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" {...form.register('available')} /> Disponible para la venta
        </label>
        <ErrorAlert error={error} />
        <div className="flex gap-2">
          <Button type="submit" loading={sending}>
            Guardar producto
          </Button>
          <Button variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
        </div>
      </form>
    </Card>
  )
}
