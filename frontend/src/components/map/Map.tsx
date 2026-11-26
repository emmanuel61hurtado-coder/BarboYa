import { Suspense, lazy, useState } from 'react'
import type { MapProps } from './LeafletMap'
import { Button } from '../ui/button'
import { Point, currentPosition } from '../../utils/geo'

// El mapa se descarga solo en las pantallas que lo usan
const LeafletMap = lazy(() => import('./LeafletMap'))

export function Map(props: MapProps) {
  return (
    <div className="relative z-0 isolate overflow-hidden rounded-2xl border border-slate-700 text-slate-900">
      <Suspense fallback={<div className="h-64 grid place-items-center text-sm text-slate-400">Cargando mapa…</div>}>
        <LeafletMap {...props} />
      </Suspense>
    </div>
  )
}

/** Elegir un punto: tocando el mapa o con la ubicación del dispositivo. */
export function LocationPicker({ label, value, onChange }: {
  label: string
  value: Point | null
  onChange: (point: Point) => void
}) {
  const [problem, setProblem] = useState<string | null>(null)
  const [locating, setLocating] = useState(false)

  const useMine = async () => {
    setProblem(null)
    setLocating(true)
    try {
      onChange(await currentPosition())
    } catch {
      setProblem('No pudimos obtener tu ubicación. Tócala en el mapa.')
    } finally {
      setLocating(false)
    }
  }

  return (
    <fieldset className="space-y-2">
      <legend className="text-xs font-medium text-slate-300 mb-1">{label}</legend>
      <Map
        markers={value ? [{ position: value, label: 'Aquí', color: '#0ea5e9' }] : []}
        onPick={onChange}
      />
      <div className="flex flex-wrap items-center gap-3">
        <Button variant="secondary" size="sm" loading={locating} onClick={useMine}>
          Usar mi ubicación
        </Button>
        <span className="text-xs text-slate-500">
          {value ? `Punto marcado: ${value.latitude.toFixed(5)}, ${value.longitude.toFixed(5)}` : 'Sin punto marcado'}
        </span>
      </div>
      {problem && (
        <p role="alert" className="text-xs text-rose-400">
          {problem}
        </p>
      )}
    </fieldset>
  )
}
