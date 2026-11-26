import { useEffect, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Button } from '../../components/ui/button'
import { ErrorAlert } from '../../components/ui/surface'

/** Muestra un archivo privado: se descarga con la sesión y vive solo en memoria. */
export function DocumentPreview({ label, contentType, load }: {
  label: string
  contentType: string
  load: () => Promise<Blob>
}) {
  const [url, setUrl] = useState<string | null>(null)
  const open = useMutation({
    mutationFn: load,
    onSuccess: (blob) => setUrl(URL.createObjectURL(blob)),
  })

  useEffect(() => () => void (url && URL.revokeObjectURL(url)), [url])

  return (
    <div className="space-y-2">
      <Button
        variant="link"
        size="inline"
        loading={open.isPending}
        aria-label={`${url ? 'Ocultar' : 'Ver'} ${label}`}
        onClick={() => (url ? setUrl(null) : open.mutate())}
      >
        {url ? 'Ocultar archivo' : 'Ver archivo'}
      </Button>
      <ErrorAlert error={open.error} />
      {url &&
        (contentType === 'application/pdf' ? (
          <a href={url} download={`${label}.pdf`} className="block text-sm text-sky-400 underline">
            Descargar PDF
          </a>
        ) : (
          <img src={url} alt={label} className="max-h-80 max-w-full rounded-xl border border-slate-700" />
        ))}
    </div>
  )
}
