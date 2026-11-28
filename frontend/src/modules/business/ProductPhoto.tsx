import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Product, shopApi } from '../../services/api/commerce'
import { ErrorAlert } from '../../components/ui/surface'
import { shrinkPhoto } from '../../utils/image'

export function ProductPhoto({ product }: { product: Product }) {
  const queryClient = useQueryClient()
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['shop', 'products'] })
  const upload = useMutation({
    mutationFn: async (file: File) => shopApi.setPhoto(product.id, await shrinkPhoto(file)),
    onSuccess: refresh,
  })
  const remove = useMutation({ mutationFn: () => shopApi.removePhoto(product.id), onSuccess: refresh })

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-3">
        {product.photoUrl && (
          <img
            src={product.photoUrl}
            alt={`Foto de ${product.name}`}
            className="h-16 w-16 rounded-xl object-cover"
          />
        )}
        <label className="cursor-pointer text-xs text-sky-400 hover:underline focus-within:underline">
          {upload.isPending ? 'Subiendo…' : product.photoUrl ? 'Cambiar foto' : 'Agregar foto'}
          <input
            type="file"
            accept="image/jpeg,image/png"
            aria-label={`Foto para ${product.name}`}
            className="sr-only"
            disabled={upload.isPending}
            onChange={(event) => {
              const file = event.target.files?.[0]
              // Permite volver a elegir el mismo archivo
              event.target.value = ''
              if (file) upload.mutate(file)
            }}
          />
        </label>
        {product.photoUrl && (
          <button
            type="button"
            className="text-xs text-slate-400 hover:underline disabled:opacity-40"
            disabled={remove.isPending}
            onClick={() => remove.mutate()}
          >
            Quitar foto
          </button>
        )}
      </div>
      <ErrorAlert error={upload.error ?? remove.error} />
    </div>
  )
}
