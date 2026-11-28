import { useEffect } from 'react'
import { CircleMarker, MapContainer, TileLayer, Tooltip, useMap, useMapEvents } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import type { Point } from '../../utils/geo'

export interface Marker {
  position: Point
  label: string
  color: string
}

export interface MapProps {
  markers: Marker[]
  /** Permite elegir un punto tocando el mapa. */
  onPick?: (point: Point) => void
}

// Parque principal de Barbosa, Santander
const BARBOSA: [number, number] = [5.9333, -73.6153]

function Behaviour({ markers, onPick }: MapProps) {
  const map = useMap()
  useMapEvents({
    click: (event) => onPick?.({ latitude: event.latlng.lat, longitude: event.latlng.lng }),
  })
  const signature = markers.map((m) => `${m.position.latitude},${m.position.longitude}`).join('|')

  useEffect(() => {
    if (markers.length === 0) return
    map.fitBounds(
      markers.map((m) => [m.position.latitude, m.position.longitude] as [number, number]),
      { maxZoom: 16, padding: [30, 30], animate: false },
    )
    // Solo se reencuadra cuando cambian las posiciones
  }, [signature]) // eslint-disable-line react-hooks/exhaustive-deps

  return null
}

/**
 * Mapa con OpenStreetMap. Su servidor público de teselas sirve para desarrollo y
 * poco tráfico; producción necesita un proveedor propio.
 */
export default function LeafletMap({ markers, onPick }: MapProps) {
  return (
    <MapContainer center={BARBOSA} zoom={15} className="h-64 w-full" scrollWheelZoom={false}>
      <TileLayer
        url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
      />
      <Behaviour markers={markers} onPick={onPick} />
      {markers.map((marker) => (
        <CircleMarker
          key={marker.label}
          center={[marker.position.latitude, marker.position.longitude]}
          radius={9}
          pathOptions={{ color: '#ffffff', weight: 2, fillColor: marker.color, fillOpacity: 1 }}
        >
          <Tooltip permanent direction="top" offset={[0, -8]}>
            {marker.label}
          </Tooltip>
        </CircleMarker>
      ))}
    </MapContainer>
  )
}
