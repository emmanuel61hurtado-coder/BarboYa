import React, { useState } from 'react'
import { Navigation2, Crosshair, Layers, Radio, Shield, Zap, Car, Bike, Sparkles } from 'lucide-react'
import { sound } from '../utils/sound'

interface InteractiveMapProps {
  origin?: string
  destination?: string
  tripActive?: boolean
  selectedVehicle?: 'moto' | 'car'
  onSelectLocation?: (name: string, type: 'origin' | 'dest') => void
}

const BARBOSA_POIS = [
  { id: 'parque', name: 'Parque Emilio Valenzuela', x: 50, y: 48, type: 'landmark', icon: '🏛️' },
  { id: 'terminal', name: 'Terminal de Transportes', x: 28, y: 72, type: 'transit', icon: '🚌' },
  { id: 'hospital', name: 'Hospital San Antonio', x: 74, y: 32, type: 'health', icon: '🏥' },
  { id: 'uis', name: 'Sede UIS Barbosa', x: 38, y: 22, type: 'education', icon: '🎓' },
  { id: 'mercado', name: 'Plaza de Mercado', x: 62, y: 58, type: 'commercial', icon: '🛒' },
  { id: 'pozonegro', name: 'Vereda Pozo Negro', x: 82, y: 78, type: 'vereda', icon: '🌲' },
]

const LIVE_DRIVERS = [
  { id: 'd1', name: 'Carlos M. (Moto FZ)', type: 'moto', x: 44, y: 42, eta: '2 min', plate: 'KLP-44E', rating: 4.98 },
  { id: 'd2', name: 'Javier R. (Taxi Spark)', type: 'car', x: 58, y: 52, eta: '4 min', plate: 'XMA-901', rating: 4.92 },
  { id: 'd3', name: 'Diana G. (Moto Gixxer)', type: 'moto', x: 33, y: 64, eta: '3 min', plate: 'BTX-12D', rating: 4.95 },
  { id: 'd4', name: 'Mauricio S. (Duster Confort)', type: 'car', x: 68, y: 38, eta: '5 min', plate: 'HJR-204', rating: 4.89 },
  { id: 'd5', name: 'Luis E. (Moto Pulsar)', type: 'moto', x: 76, y: 68, eta: '4 min', plate: 'MWZ-88F', rating: 4.96 },
]

export const InteractiveMap: React.FC<InteractiveMapProps> = ({
  origin = 'Parque Emilio Valenzuela',
  destination = 'Terminal de Transportes',
  tripActive = false,
  selectedVehicle = 'moto',
  onSelectLocation
}) => {
  const [zoom, setZoom] = useState<number>(1)
  const [showGeofence, setShowGeofence] = useState<boolean>(true)
  const [showDrivers, setShowDrivers] = useState<boolean>(true)
  const [showTraffic, setShowTraffic] = useState<boolean>(false)
  const [activePoi, setActivePoi] = useState<string | null>(null)
  const [hoveredDriver, setHoveredDriver] = useState<typeof LIVE_DRIVERS[0] | null>(null)

  return (
    <div className="relative w-full h-[380px] sm:h-[460px] lg:h-[540px] rounded-2xl overflow-hidden glass-surface-elevated border border-cyan-500/20 shadow-cyan-glow transition-all duration-500 group select-none">
      {/* Top Map Telemetry Bar */}
      <div className="absolute top-3 left-3 right-3 z-30 flex items-center justify-between pointer-events-none">
        <div className="flex items-center space-x-2 bg-nexus-950/80 backdrop-blur-md px-3 py-1.5 rounded-xl border border-cyan-500/30 text-xs shadow-lg pointer-events-auto">
          <div className="flex items-center space-x-1.5">
            <span className="w-2 h-2 rounded-full bg-cyan-glow animate-ping" />
            <span className="font-mono text-cyan-300 font-bold tracking-wider text-[11px]">NEXUS TELEMETRY</span>
          </div>
          <span className="text-slate-600">|</span>
          <span className="text-slate-300 font-mono text-[10px] hidden sm:inline">5.9333° N, 73.6208° W · 1,585m</span>
          <span className="text-slate-600 hidden sm:inline">|</span>
          <div className="flex items-center space-x-1 text-neon-emerald text-[11px] font-semibold">
            <Radio className="w-3 h-3 animate-pulse" />
            <span>PostGIS ST_DWithin 3km</span>
          </div>
        </div>

        {/* Floating Quick Map Controls */}
        <div className="flex items-center space-x-1.5 pointer-events-auto">
          <button
            onClick={() => {
              setShowGeofence(!showGeofence)
              sound.tick()
            }}
            className={`p-2 rounded-xl backdrop-blur-md border text-xs transition flex items-center space-x-1 ${
              showGeofence ? 'bg-cyan-500/20 border-cyan-500/40 text-cyan-300' : 'bg-nexus-950/70 border-slate-800 text-slate-400 hover:text-white'
            }`}
            title="Conmutar Radio Geocerca 3km"
          >
            <Shield className="w-3.5 h-3.5" />
            <span className="text-[10px] font-mono hidden md:inline">GEOCERCA</span>
          </button>

          <button
            onClick={() => {
              setShowDrivers(!showDrivers)
              sound.tick()
            }}
            className={`p-2 rounded-xl backdrop-blur-md border text-xs transition flex items-center space-x-1 ${
              showDrivers ? 'bg-neon-emerald/20 border-neon-emerald/40 text-neon-emerald' : 'bg-nexus-950/70 border-slate-800 text-slate-400 hover:text-white'
            }`}
            title="Conmutar Conductores Online"
          >
            <Zap className="w-3.5 h-3.5" />
            <span className="text-[10px] font-mono hidden md:inline">5 ONLINE</span>
          </button>

          <div className="flex items-center bg-nexus-950/80 backdrop-blur-md rounded-xl border border-slate-800 p-0.5">
            <button
              onClick={() => {
                setZoom(Math.min(zoom + 0.15, 1.4))
                sound.tick()
              }}
              className="px-2.5 py-1 text-slate-300 hover:text-cyan-300 text-xs font-mono font-bold transition"
            >
              +
            </button>
            <span className="text-[9px] font-mono text-slate-500 px-1">{Math.round(zoom * 100)}%</span>
            <button
              onClick={() => {
                setZoom(Math.max(zoom - 0.15, 0.85))
                sound.tick()
              }}
              className="px-2.5 py-1 text-slate-300 hover:text-cyan-300 text-xs font-mono font-bold transition"
            >
              -
            </button>
          </div>
        </div>
      </div>

      {/* Main Vector Hologram Canvas */}
      <div 
        className="w-full h-full relative overflow-hidden transition-transform duration-300 origin-center"
        style={{ transform: `scale(${zoom})` }}
      >
        {/* Ambient Dark Gradients */}
        <div className="absolute inset-0 bg-gradient-to-b from-[#0a0f1d] via-[#07090e] to-[#040609]" />
        
        {/* Subtle Cyber Grid Lines */}
        <div className="absolute inset-0 cyber-grid-dense opacity-40" />

        {/* Topographic & River Vector Simulation for Barbosa */}
        <svg className="absolute inset-0 w-full h-full pointer-events-none" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <linearGradient id="cyanLine" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#00F2FE" stopOpacity="0.8" />
              <stop offset="50%" stopColor="#00E5A3" stopOpacity="0.9" />
              <stop offset="100%" stopColor="#38BDF8" stopOpacity="0.8" />
            </linearGradient>
            <linearGradient id="riverGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#0284c7" stopOpacity="0.25" />
              <stop offset="100%" stopColor="#0369a1" stopOpacity="0.4" />
            </linearGradient>
            <radialGradient id="radarSweep" cx="50%" cy="50%" r="50%">
              <stop offset="0%" stopColor="#00F2FE" stopOpacity="0.25" />
              <stop offset="60%" stopColor="#00F2FE" stopOpacity="0.05" />
              <stop offset="100%" stopColor="#00F2FE" stopOpacity="0" />
            </radialGradient>
          </defs>

          {/* Río Suárez & Quebrada water paths */}
          <path
            d="M 10 120 Q 180 180 320 140 T 640 260 T 920 300"
            fill="none"
            stroke="url(#riverGrad)"
            strokeWidth="12"
            strokeLinecap="round"
          />
          <path
            d="M 500 20 Q 520 180 480 350 T 450 540"
            fill="none"
            stroke="url(#riverGrad)"
            strokeWidth="8"
            strokeLinecap="round"
          />

          {/* Street Mesh Grid in Barbosa */}
          <g stroke="rgba(255,255,255,0.06)" strokeWidth="1.5">
            <line x1="20%" y1="20%" x2="80%" y2="20%" />
            <line x1="15%" y1="35%" x2="85%" y2="35%" />
            <line x1="10%" y1="50%" x2="90%" y2="50%" />
            <line x1="15%" y1="65%" x2="85%" y2="65%" />
            <line x1="20%" y1="80%" x2="80%" y2="80%" />

            <line x1="25%" y1="15%" x2="25%" y2="85%" />
            <line x1="40%" y1="10%" x2="40%" y2="90%" />
            <line x1="55%" y1="10%" x2="55%" y2="90%" />
            <line x1="70%" y1="15%" x2="70%" y2="85%" />
          </g>

          {/* PostGIS 3km Geofence Ring */}
          {showGeofence && (
            <g className="transition-opacity duration-300">
              <circle
                cx="50%"
                cy="48%"
                r="180"
                fill="none"
                stroke="#00F2FE"
                strokeWidth="1"
                strokeDasharray="6 6"
                opacity="0.35"
              />
              <circle
                cx="50%"
                cy="48%"
                r="180"
                fill="rgba(0, 242, 254, 0.02)"
              />
              <text x="52%" y="22%" fill="#00F2FE" opacity="0.6" fontSize="10" fontFamily="JetBrains Mono">
                RADIO SEGURO 3,000m (BARBOSA CO)
              </text>
            </g>
          )}

          {/* Pulsing Central Radar Beam */}
          <g className="origin-center animate-radar-sweep" style={{ transformOrigin: '50% 48%' }}>
            <line x1="50%" y1="48%" x2="50%" y2="10%" stroke="#00F2FE" strokeWidth="1.5" opacity="0.6" />
            <path
              d="M 50% 48% L 50% 10% A 180 180 0 0 1 78% 30% Z"
              fill="url(#radarSweep)"
              opacity="0.3"
            />
          </g>

          {/* Dynamic Active Route Trajectory */}
          <path
            d="M 50% 48% Q 40% 60% 28% 72%"
            fill="none"
            stroke="url(#cyanLine)"
            strokeWidth="4"
            strokeLinecap="round"
            strokeDasharray="8 6"
            className="animate-pulse"
          />
        </svg>

        {/* POI Markers */}
        {BARBOSA_POIS.map((poi) => {
          const isSelected = activePoi === poi.id || (poi.id === 'parque' && !activePoi)
          return (
            <div
              key={poi.id}
              onClick={() => {
                setActivePoi(poi.id)
                sound.tick()
                if (onSelectLocation) onSelectLocation(poi.name, 'dest')
              }}
              style={{ left: `${poi.x}%`, top: `${poi.y}%` }}
              className={`absolute -translate-x-1/2 -translate-y-1/2 z-20 cursor-pointer group/poi transition-all duration-300 ${
                isSelected ? 'scale-110 z-30' : 'hover:scale-105'
              }`}
            >
              <div className="relative flex flex-col items-center">
                {/* Holographic Tooltip */}
                <div className={`px-2 py-0.5 rounded-md text-[10px] font-mono whitespace-nowrap mb-1 transition-all duration-200 border shadow-lg ${
                  isSelected 
                    ? 'bg-cyan-950/90 text-cyan-300 border-cyan-400 font-bold luminous-border-cyan' 
                    : 'bg-nexus-950/80 text-slate-300 border-slate-700/60 group-hover/poi:text-white'
                }`}>
                  <span className="mr-1">{poi.icon}</span>
                  {poi.name}
                </div>

                {/* Point Anchor Pin */}
                <div className={`w-3.5 h-3.5 rounded-full flex items-center justify-center transition-all ${
                  isSelected 
                    ? 'bg-cyan-glow ring-4 ring-cyan-500/30' 
                    : 'bg-slate-400 group-hover/poi:bg-cyan-400 ring-2 ring-nexus-900'
                }`}>
                  <div className="w-1.5 h-1.5 rounded-full bg-nexus-950" />
                </div>
              </div>
            </div>
          )
        })}

        {/* Real-time Moving Drivers */}
        {showDrivers && LIVE_DRIVERS.map((driver) => {
          return (
            <div
              key={driver.id}
              onMouseEnter={() => setHoveredDriver(driver)}
              onMouseLeave={() => setHoveredDriver(null)}
              style={{ left: `${driver.x}%`, top: `${driver.y}%` }}
              className="absolute -translate-x-1/2 -translate-y-1/2 z-25 cursor-pointer transition-all duration-500 hover:scale-125"
            >
              <div className="relative">
                {/* Ping aura */}
                <div className="absolute -inset-2 bg-neon-emerald/30 rounded-full animate-ping pointer-events-none" />
                
                {/* Vehicle Badge */}
                <div className={`p-1.5 rounded-full border shadow-lg flex items-center justify-center ${
                  driver.type === 'moto' 
                    ? 'bg-neon-emerald/20 border-neon-emerald text-neon-emerald shadow-emerald-glow' 
                    : 'bg-cyan-500/20 border-cyan-400 text-cyan-300 shadow-cyan-glow'
                }`}>
                  {driver.type === 'moto' ? <Bike className="w-3.5 h-3.5" /> : <Car className="w-3.5 h-3.5" />}
                </div>

                {/* Mini ETA tag */}
                <div className="absolute -bottom-4 left-1/2 -translate-x-1/2 bg-nexus-950/90 border border-slate-700 text-[9px] font-mono font-bold text-slate-200 px-1 py-0.2 rounded whitespace-nowrap">
                  {driver.eta}
                </div>
              </div>
            </div>
          )
        })}

        {/* Hovered Driver Floating Detail Box */}
        {hoveredDriver && (
          <div 
            style={{ left: `${hoveredDriver.x}%`, top: `${hoveredDriver.y - 14}%` }}
            className="absolute -translate-x-1/2 z-40 bg-nexus-950/95 backdrop-blur-xl border border-neon-emerald/50 rounded-xl p-3 shadow-2xl min-w-[190px] animate-fadeIn pointer-events-none"
          >
            <div className="flex items-center justify-between text-xs mb-1">
              <span className="font-bold text-white">{hoveredDriver.name}</span>
              <span className="text-neon-emerald font-mono font-bold">★ {hoveredDriver.rating}</span>
            </div>
            <div className="text-[11px] text-slate-400 flex items-center justify-between">
              <span>Placa: <strong className="text-cyan-300 font-mono">{hoveredDriver.plate}</strong></span>
              <span className="bg-neon-emerald/10 text-neon-emerald px-1.5 py-0.5 rounded text-[10px]">A 300m</span>
            </div>
            <div className="mt-1 text-[10px] text-slate-400 flex items-center space-x-1">
              <Sparkles className="w-3 h-3 text-cyan-400" />
              <span>Verificado por BarboYa Nexus</span>
            </div>
          </div>
        )}

        {/* Origin / Destination Waypoints on Active Route */}
        <div style={{ left: '50%', top: '48%' }} className="absolute -translate-x-1/2 -translate-y-1/2 z-30 pointer-events-none">
          <div className="flex flex-col items-center">
            <span className="bg-brand-500 text-white font-mono text-[9px] font-bold px-1.5 py-0.5 rounded shadow">A: ORIGEN</span>
            <div className="w-4 h-4 rounded-full bg-brand-500 ring-4 ring-brand-500/40 animate-pulse mt-0.5" />
          </div>
        </div>

        <div style={{ left: '28%', top: '72%' }} className="absolute -translate-x-1/2 -translate-y-1/2 z-30 pointer-events-none">
          <div className="flex flex-col items-center">
            <span className="bg-cyan-glow text-nexus-950 font-mono text-[9px] font-bold px-1.5 py-0.5 rounded shadow">B: DESTINO</span>
            <div className="w-4 h-4 rounded-full bg-cyan-glow ring-4 ring-cyan-500/40 animate-pulse mt-0.5" />
          </div>
        </div>
      </div>

      {/* Bottom Route Telemetry Banner */}
      <div className="absolute bottom-3 left-3 right-3 z-30 flex flex-col sm:flex-row items-center justify-between gap-2 bg-nexus-950/85 backdrop-blur-md p-2.5 rounded-xl border border-slate-800 text-xs">
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
