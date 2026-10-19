import React, { useState, useEffect, useMemo, useRef } from 'react'
import { MapContainer, TileLayer, Marker, Popup, Polyline, Circle, useMap, ZoomControl } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { 
  Navigation2, Radio, Shield, Zap, Sparkles, Bike, Car 
} from 'lucide-react'
import { sound } from '../utils/sound'

/* ───────────────────── BARBOSA, SANTANDER CONSTANTS ───────────────────── */
const BARBOSA_CENTER: [number, number] = [5.9328, -73.6204]
const DEFAULT_ZOOM = 15

/* ───────────────────── CUSTOM MARKER ICONS ───────────────────── */
// Origin marker — warm orange pin
const originIcon = L.divIcon({
  className: '',
  html: `<div style="
    width:22px;height:22px;border-radius:50%;
    background:linear-gradient(135deg,#f97316,#ea580c);
    border:3px solid #07090e;
    box-shadow:0 0 12px rgba(249,115,22,0.6);
    display:flex;align-items:center;justify-content:center;
  "><div style="width:6px;height:6px;border-radius:50%;background:white;"></div></div>`,
  iconSize: [22, 22],
  iconAnchor: [11, 11],
  popupAnchor: [0, -14],
})

// Destination marker — cyan glow pin
const destIcon = L.divIcon({
  className: '',
  html: `<div style="
    width:22px;height:22px;border-radius:50%;
    background:linear-gradient(135deg,#00F2FE,#00D2FF);
    border:3px solid #07090e;
    box-shadow:0 0 12px rgba(0,242,254,0.6);
    display:flex;align-items:center;justify-content:center;
  "><div style="width:6px;height:6px;border-radius:50%;background:#07090e;"></div></div>`,
  iconSize: [22, 22],
  iconAnchor: [11, 11],
  popupAnchor: [0, -14],
})

// POI marker — small subtle white dot
const poiIcon = L.divIcon({
  className: '',
  html: `<div style="
    width:12px;height:12px;border-radius:50%;
    background:rgba(148,163,184,0.7);
    border:2px solid #07090e;
  "></div>`,
  iconSize: [12, 12],
  iconAnchor: [6, 6],
  popupAnchor: [0, -8],
})

// Driver marker factory
const driverIcon = (type: 'moto' | 'car') =>
  L.divIcon({
    className: '',
    html: `<div style="
      width:28px;height:28px;border-radius:50%;
      background:${type === 'moto' ? 'rgba(0,229,163,0.25)' : 'rgba(0,210,255,0.25)'};
      border:2px solid ${type === 'moto' ? '#00E5A3' : '#00D2FF'};
      box-shadow:0 0 10px ${type === 'moto' ? 'rgba(0,229,163,0.5)' : 'rgba(0,210,255,0.5)'};
      display:flex;align-items:center;justify-content:center;
      font-size:14px;
    ">${type === 'moto' ? '🏍️' : '🚗'}</div>`,
    iconSize: [28, 28],
    iconAnchor: [14, 14],
    popupAnchor: [0, -16],
  })

/* ───────────────────── BARBOSA DATA ───────────────────── */
const BARBOSA_POIS: { name: string; coords: [number, number]; emoji: string }[] = [
  { name: 'Parque Emilio Valenzuela', coords: [5.9328, -73.6204], emoji: '🏛️' },
  { name: 'Iglesia San Antonio de Padua', coords: [5.9331, -73.6199], emoji: '⛪' },
  { name: 'Terminal de Transportes', coords: [5.9290, -73.6180], emoji: '🚌' },
  { name: 'Hospital San Antonio', coords: [5.9355, -73.6230], emoji: '🏥' },
  { name: 'Plaza de Mercado', coords: [5.9318, -73.6190], emoji: '🛒' },
  { name: 'Alcaldía Municipal', coords: [5.9333, -73.6208], emoji: '🏛️' },
  { name: 'Colegio Pio XII', coords: [5.9310, -73.6220], emoji: '🎓' },
]

const LIVE_DRIVERS: {
  id: string; name: string; vehicle: string; type: 'moto' | 'car'
  plate: string; rating: number; coords: [number, number]; eta: string
}[] = [
  { id: 'd1', name: 'Carlos M. Duarte', vehicle: 'Yamaha FZ 2.0', type: 'moto', plate: 'KLP-44E', rating: 4.98, coords: [5.9340, -73.6215], eta: '2 min' },
  { id: 'd2', name: 'Javier Rodríguez P.', vehicle: 'Chevrolet Spark GT', type: 'car', plate: 'XMA-901', rating: 4.92, coords: [5.9308, -73.6175], eta: '4 min' },
  { id: 'd3', name: 'Diana M. Gómez', vehicle: 'Suzuki Gixxer 150', type: 'moto', plate: 'BTX-12D', rating: 4.95, coords: [5.9300, -73.6240], eta: '3 min' },
  { id: 'd4', name: 'Mauricio Suárez', vehicle: 'Renault Duster', type: 'car', plate: 'HJR-204', rating: 4.89, coords: [5.9365, -73.6190], eta: '5 min' },
  { id: 'd5', name: 'Luis E. Pineda', vehicle: 'Bajaj Pulsar 180', type: 'moto', plate: 'MWZ-88F', rating: 4.96, coords: [5.9278, -73.6210], eta: '4 min' },
]

// Simulated route polyline from Parque to Terminal
const ROUTE_POINTS: [number, number][] = [
  [5.9328, -73.6204],
  [5.9325, -73.6198],
  [5.9320, -73.6193],
  [5.9315, -73.6190],
  [5.9308, -73.6186],
  [5.9300, -73.6183],
  [5.9295, -73.6181],
  [5.9290, -73.6180],
]

/* ───────────────────── COMPONENT TO FLY MAP TO A POINT ───────────────────── */
function FlyTo({ center, zoom }: { center: [number, number]; zoom?: number }) {
  const map = useMap()
  useEffect(() => {
    map.flyTo(center, zoom || map.getZoom(), { duration: 1 })
  }, [center, zoom, map])
  return null
}

/* ───────────────────── PROPS ───────────────────── */
interface RealMapProps {
  origin?: string
  destination?: string
  originCoords?: [number, number]
  destCoords?: [number, number]
  showRoute?: boolean
  showDrivers?: boolean
  showGeofence?: boolean
  selectedVehicle?: 'moto' | 'car'
  onSelectLocation?: (name: string, type: 'origin' | 'dest', coords?: [number, number]) => void
}

/* ───────────────────── MAIN COMPONENT ───────────────────── */
export const RealMap: React.FC<RealMapProps> = ({
  origin = 'Parque Emilio Valenzuela',
  destination = 'Terminal de Transportes',
  originCoords = BARBOSA_POIS[0].coords,
  destCoords = BARBOSA_POIS[2].coords,
  showRoute: showRouteProp = true,
  showDrivers: showDriversProp = true,
  showGeofence: showGeofenceProp = true,
  selectedVehicle = 'moto',
  onSelectLocation,
}) => {
  const [mapStyle, setMapStyle] = useState<'dark' | 'satellite' | 'streets'>('dark')
  const [driversVisible, setDriversVisible] = useState(showDriversProp)
  const [geofenceVisible, setGeofenceVisible] = useState(showGeofenceProp)
  const [routeVisible, setRouteVisible] = useState(showRouteProp)
  const [mapCenter, setMapCenter] = useState<[number, number]>(BARBOSA_CENTER)

  return (
    <div className="relative w-full h-[380px] sm:h-[460px] lg:h-[540px] rounded-2xl overflow-hidden glass-surface-elevated border border-cyan-500/20 shadow-cyan-glow transition-all duration-500 group select-none">
      {/* ────── Top Telemetry Bar ────── */}
      <div className="absolute top-3 left-3 right-3 z-[1000] flex items-center justify-between pointer-events-none">
        <div className="flex items-center space-x-2 bg-nexus-950/85 backdrop-blur-md px-3 py-1.5 rounded-xl border border-cyan-500/30 text-xs shadow-lg pointer-events-auto">
          <div className="flex items-center space-x-1.5">
            <span className="w-2 h-2 rounded-full bg-cyan-glow animate-ping" />
            <span className="font-mono text-cyan-300 font-bold tracking-wider text-[11px]">MAPA EN VIVO</span>
          </div>
          <span className="text-slate-600">|</span>
          <span className="text-slate-300 font-mono text-[10px] hidden sm:inline">5.9328° N, 73.6204° W · Barbosa</span>
          <span className="text-slate-600 hidden sm:inline">|</span>
          <div className="flex items-center space-x-1 text-neon-emerald text-[11px] font-semibold hidden sm:flex">
            <Radio className="w-3 h-3 animate-pulse" />
            <span>OpenStreetMap · PostGIS 3km</span>
          </div>
        </div>

        {/* Map Controls */}
        <div className="flex items-center space-x-1.5 pointer-events-auto">
          {/* Layer switcher */}
          <div className="flex items-center bg-nexus-950/80 backdrop-blur-md rounded-xl p-0.5 border border-slate-800">
            <button
              onClick={() => {
                setMapStyle('dark')
                sound.tick()
              }}
              className={`px-2 py-1 rounded-lg text-[10px] font-mono transition ${
                mapStyle === 'dark' ? 'bg-cyan-500/20 text-cyan-300 font-bold border border-cyan-500/40' : 'text-slate-400 hover:text-white'
              }`}
              title="Estilo Cyber Dark"
            >
              DARK
            </button>
            <button
              onClick={() => {
                setMapStyle('satellite')
                sound.tick()
              }}
              className={`px-2 py-1 rounded-lg text-[10px] font-mono transition ${
                mapStyle === 'satellite' ? 'bg-brand-500/20 text-brand-300 font-bold border border-brand-500/40' : 'text-slate-400 hover:text-white'
              }`}
              title="Vista Satelital"
            >
              SAT
            </button>
            <button
              onClick={() => {
                setMapStyle('streets')
                sound.tick()
              }}
              className={`px-2 py-1 rounded-lg text-[10px] font-mono transition ${
                mapStyle === 'streets' ? 'bg-emerald-500/20 text-emerald-300 font-bold border border-emerald-500/40' : 'text-slate-400 hover:text-white'
              }`}
              title="Calles OpenStreetMap"
            >
              CALLES
            </button>
          </div>

          <button
            onClick={() => {
              setGeofenceVisible(!geofenceVisible)
              sound.tick()
            }}
            className={`p-2 rounded-xl backdrop-blur-md border text-xs transition flex items-center space-x-1 ${
              geofenceVisible ? 'bg-cyan-500/20 border-cyan-500/40 text-cyan-300' : 'bg-nexus-950/70 border-slate-800 text-slate-400 hover:text-white'
            }`}
          >
            <Shield className="w-3.5 h-3.5" />
            <span className="text-[10px] font-mono hidden md:inline">GEOCERCA</span>
          </button>

          <button
            onClick={() => {
              setDriversVisible(!driversVisible)
              sound.tick()
            }}
            className={`p-2 rounded-xl backdrop-blur-md border text-xs transition flex items-center space-x-1 ${
              driversVisible ? 'bg-neon-emerald/20 border-neon-emerald/40 text-neon-emerald' : 'bg-nexus-950/70 border-slate-800 text-slate-400 hover:text-white'
            }`}
          >
            <Zap className="w-3.5 h-3.5" />
            <span className="text-[10px] font-mono hidden md:inline">{LIVE_DRIVERS.length} ONLINE</span>
          </button>

          <button
            onClick={() => {
              setRouteVisible(!routeVisible)
              sound.tick()
            }}
            className={`p-2 rounded-xl backdrop-blur-md border text-xs transition flex items-center space-x-1 ${
              routeVisible ? 'bg-brand-500/20 border-brand-500/40 text-brand-400' : 'bg-nexus-950/70 border-slate-800 text-slate-400 hover:text-white'
            }`}
          >
            <Navigation2 className="w-3.5 h-3.5" />
            <span className="text-[10px] font-mono hidden md:inline">RUTA</span>
          </button>
        </div>
      </div>

      {/* ────── Leaflet Map ────── */}
      <MapContainer
        center={BARBOSA_CENTER}
        zoom={DEFAULT_ZOOM}
        zoomControl={false}
        className="w-full h-full z-0"
        style={{ background: '#07090e' }}
        attributionControl={false}
      >
        <FlyTo center={mapCenter} />

        {/* Dynamic Tile Layer based on active map style */}
        {mapStyle === 'dark' && (
          <TileLayer
            key="dark-tiles"
            url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
            attribution='&copy; CARTO &copy; OpenStreetMap'
            maxZoom={19}
          />
        )}
        {mapStyle === 'satellite' && (
          <TileLayer
            key="sat-tiles"
            url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
            attribution='&copy; Esri, Maxar, Earthstar Geographics'
            maxZoom={18}
          />
        )}
        {mapStyle === 'streets' && (
          <TileLayer
            key="street-tiles"
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution='&copy; OpenStreetMap contributors'
            maxZoom={19}
          />
        )}

        <ZoomControl position="bottomright" />

        {/* 3km Geofence Circle */}
        {geofenceVisible && (
          <Circle
            center={BARBOSA_CENTER}
            radius={3000}
            pathOptions={{
              color: '#00F2FE',
              weight: 1.5,
              dashArray: '8 6',
              fillColor: '#00F2FE',
              fillOpacity: 0.03,
            }}
          />
        )}

        {/* Route Polyline */}
        {routeVisible && (
          <Polyline
            positions={ROUTE_POINTS}
            pathOptions={{
              color: '#00F2FE',
              weight: 4,
              opacity: 0.85,
              dashArray: '10 8',
              lineCap: 'round',
              lineJoin: 'round',
            }}
          />
        )}

        {/* Origin Marker */}
        <Marker position={originCoords} icon={originIcon}>
          <Popup className="nexus-popup">
            <div style={{ fontFamily: 'Outfit, sans-serif', fontSize: '12px' }}>
              <strong style={{ color: '#f97316' }}>📍 ORIGEN</strong><br />
              {origin}
            </div>
          </Popup>
        </Marker>

        {/* Destination Marker */}
        <Marker position={destCoords} icon={destIcon}>
          <Popup className="nexus-popup">
            <div style={{ fontFamily: 'Outfit, sans-serif', fontSize: '12px' }}>
              <strong style={{ color: '#00F2FE' }}>🎯 DESTINO</strong><br />
              {destination}
            </div>
          </Popup>
        </Marker>

        {/* POI Markers */}
        {BARBOSA_POIS.map((poi) => (
          <Marker
            key={poi.name}
            position={poi.coords}
            icon={poiIcon}
            eventHandlers={{
              click: () => {
                sound.tick()
                if (onSelectLocation) {
                  onSelectLocation(poi.name, 'dest', poi.coords)
                }
              },
            }}
          >
            <Popup>
              <div style={{ fontFamily: 'Outfit, sans-serif', fontSize: '12px' }}>
                <strong>{poi.emoji} {poi.name}</strong><br />
                <span style={{ fontSize: '10px', color: '#94a3b8' }}>
                  {poi.coords[0].toFixed(4)}° N, {Math.abs(poi.coords[1]).toFixed(4)}° W
                </span>
              </div>
            </Popup>
          </Marker>
        ))}

        {/* Live Driver Markers */}
        {driversVisible &&
          LIVE_DRIVERS.map((drv) => (
            <Marker key={drv.id} position={drv.coords} icon={driverIcon(drv.type)}>
              <Popup>
                <div style={{ fontFamily: 'Outfit, sans-serif', fontSize: '12px', minWidth: '180px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <strong>{drv.name}</strong>
                    <span style={{ color: '#00E5A3', fontFamily: 'JetBrains Mono', fontWeight: 'bold', fontSize: '11px' }}>★ {drv.rating}</span>
                  </div>
                  <div style={{ fontSize: '11px', color: '#94a3b8', marginTop: '4px' }}>
                    {drv.vehicle}<br />
                    Placa: <strong style={{ color: '#00D2FF', fontFamily: 'JetBrains Mono' }}>{drv.plate}</strong>
                  </div>
                  <div style={{ marginTop: '6px', fontSize: '11px', display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: '#00E5A3' }}>ETA: {drv.eta}</span>
                    <span style={{ color: '#94a3b8', fontSize: '10px' }}>✓ Verificado BarboYa</span>
                  </div>
                </div>
              </Popup>
            </Marker>
          ))}
      </MapContainer>

      {/* ────── Bottom Route Info Bar ────── */}
      <div className="absolute bottom-3 left-3 right-3 z-[1000] flex flex-col sm:flex-row items-center justify-between gap-2 bg-nexus-950/85 backdrop-blur-md p-2.5 rounded-xl border border-slate-800 text-xs">
        <div className="flex items-center space-x-2 text-slate-300 text-xs truncate max-w-full">
          <Navigation2 className="w-4 h-4 text-cyan-400 shrink-0" />
          <span className="font-semibold text-white truncate">{origin}</span>
          <span className="text-cyan-400 font-mono">➔</span>
          <span className="font-semibold text-cyan-300 truncate">{destination}</span>
        </div>

        <div className="flex items-center space-x-3 shrink-0 text-slate-400 font-mono text-[11px]">
          <div>Distancia: <span className="text-white font-bold">2.4 km</span></div>
          <div>ETA: <span className="text-neon-emerald font-bold">5 - 7 min</span></div>
          <div className="hidden md:inline bg-cyan-500/10 text-cyan-300 px-2 py-0.5 rounded border border-cyan-500/30">
            {selectedVehicle === 'moto' ? 'Moto Rápida' : 'Auto Confort'}
          </div>
        </div>
      </div>
    </div>
  )
}
