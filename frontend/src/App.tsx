import React, { useState } from 'react'
import { 
  Car, Utensils, Package, ShieldAlert, Sparkles, 
  MapPin, AlertTriangle, Radio, Navigation2, CheckCircle2, 
  User, Store, ShieldCheck, Heart
} from 'lucide-react'
import { AppTab, ServiceMode } from './types'
import { Navbar } from './components/Navbar'
import { InteractiveMap } from './components/InteractiveMap'
import { RealMap } from './components/RealMap'
import { MoveView } from './components/MoveView'
import { FoodView } from './components/FoodView'
import { PackageView } from './components/PackageView'
import { DriverPanel } from './components/DriverPanel'
import { MerchantPanel } from './components/MerchantPanel'
import { AdminPanel } from './components/AdminPanel'
import { SosModal } from './components/SosModal'
import { sound } from './utils/sound'

export default function App() {
  const [activeTab, setActiveTab] = useState<AppTab>('client')
  const [serviceMode, setServiceMode] = useState<ServiceMode>('move')
  const [sosOpen, setSosOpen] = useState<boolean>(false)
  const [sosBannerActive, setSosBannerActive] = useState<boolean>(false)
  const [toast, setToast] = useState<string | null>(null)
  const [audioEnabled, setAudioEnabled] = useState<boolean>(true)
  const [mapType, setMapType] = useState<'real' | 'hologram'>('real')

  // Map route state shared between MoveView and Maps
  const [origin, setOrigin] = useState<string>('Parque Emilio Valenzuela')
  const [destination, setDestination] = useState<string>('Terminal de Transportes de Barbosa')

  const showToast = (msg: string) => {
    setToast(msg)
    setTimeout(() => {
      setToast(null)
    }, 4500)
  }

  const handleTriggerSOS = () => {
    setSosOpen(true)
    setSosBannerActive(true)
  }

  return (
    <div className="min-h-screen bg-[#07090e] text-slate-100 flex flex-col font-sans selection:bg-cyan-500/30 selection:text-cyan-200 relative overflow-x-hidden cyber-grid">
      {/* Ambient Radial Lighting Effects */}
      <div className="fixed top-0 left-1/4 w-[600px] h-[600px] bg-cyan-500/5 blur-[160px] rounded-full pointer-events-none -z-10" />
      <div className="fixed bottom-0 right-1/4 w-[500px] h-[500px] bg-brand-500/5 blur-[140px] rounded-full pointer-events-none -z-10" />

      {/* Futuristic Navbar */}
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onTriggerSOS={handleTriggerSOS}
        audioEnabled={audioEnabled}
        setAudioEnabled={setAudioEnabled}
      />

      {/* Critical SOS Active Banner */}
      {sosBannerActive && (
        <div className="bg-gradient-to-r from-red-600 via-rose-700 to-red-600 text-white px-4 py-2.5 text-xs font-mono font-bold flex flex-wrap justify-between items-center gap-2 border-b border-red-400 shadow-rose-glow animate-pulse">
          <div className="flex items-center space-x-2">
            <ShieldAlert className="w-4 h-4 text-white" />
            <span>🚨 ALERTA SOS ACTIVA EN BARBOSA: Operador enlazado y monitoreando coordenadas GPS.</span>
          </div>
          <button
            onClick={() => {
              setSosBannerActive(false)
              sound.tick()
            }}
            className="underline bg-red-950/80 px-2.5 py-1 rounded border border-red-400 text-slate-200 hover:text-white"
          >
            Desactivar / Cerrar Alerta
          </button>
        </div>
      )}

      {/* Floating Tactical Toast */}
      {toast && (
        <div className="fixed bottom-5 right-5 z-50 glass-surface-elevated text-white px-4 py-3 rounded-2xl shadow-cyan-glow border border-cyan-400/40 flex items-center space-x-3 max-w-sm animate-float-slow">
          <div className="p-1.5 rounded-lg bg-cyan-500/20 text-cyan-300">
            <Radio className="w-4 h-4 animate-ping" />
          </div>
          <p className="text-xs font-medium leading-snug">{toast}</p>
        </div>
      )}

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-3 sm:px-6 py-4 sm:py-6 space-y-6">
        {/* ======================= CLIENT VIEW ======================= */}
        {activeTab === 'client' && (
          <div className="space-y-6">
            {/* Hero Welcome Banner */}
            <div className="glass-surface p-4 sm:p-5 rounded-2xl border border-slate-800/80 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
              <div>
                <div className="flex items-center space-x-2">
                  <h1 className="font-display font-black text-xl sm:text-2xl text-white tracking-wide">
                    BARBO<span className="text-cyan-glow">YA</span> NEXUS
                  </h1>
                  <span className="text-[10px] font-mono bg-cyan-500/10 text-cyan-300 border border-cyan-400/30 px-2 py-0.5 rounded-full">
                    SISTEMA ACTIVO
                  </span>
                </div>
                <p className="text-xs text-slate-400 mt-1 max-w-xl">
                  Plataforma integral de movilidad inteligente, gastronomía local y logística urbana en Barbosa, Santander.
                </p>
              </div>

              {/* Service Modes Selector */}
              <div className="grid grid-cols-3 gap-2 w-full md:w-auto">
                <button
                  onClick={() => {
                    setServiceMode('move')
                    sound.tick()
                  }}
                  className={`px-3 py-2.5 rounded-xl border flex flex-col items-center justify-center space-y-1 transition-all ${
                    serviceMode === 'move'
                      ? 'bg-cyan-500/15 border-cyan-400 text-cyan-300 shadow-cyan-glow-sm luminous-border-cyan font-bold'
                      : 'bg-nexus-950/70 border-slate-800 text-slate-400 hover:text-white hover:border-slate-700'
                  }`}
                >
                  <Car className="w-4 h-4" />
                  <span className="text-[11px] font-mono">Viajar</span>
                </button>

                <button
                  onClick={() => {
                    setServiceMode('food')
                    sound.tick()
                  }}
                  className={`px-3 py-2.5 rounded-xl border flex flex-col items-center justify-center space-y-1 transition-all ${
                    serviceMode === 'food'
                      ? 'bg-brand-500/15 border-brand-500 text-brand-400 shadow-brand-glow luminous-border-brand font-bold'
                      : 'bg-nexus-950/70 border-slate-800 text-slate-400 hover:text-white hover:border-slate-700'
                  }`}
                >
                  <Utensils className="w-4 h-4" />
                  <span className="text-[11px] font-mono">Comida</span>
                </button>

                <button
                  onClick={() => {
                    setServiceMode('package')
                    sound.tick()
                  }}
                  className={`px-3 py-2.5 rounded-xl border flex flex-col items-center justify-center space-y-1 transition-all ${
                    serviceMode === 'package'
                      ? 'bg-neon-emerald/15 border-neon-emerald text-neon-emerald shadow-emerald-glow luminous-border-emerald font-bold'
                      : 'bg-nexus-950/70 border-slate-800 text-slate-400 hover:text-white hover:border-slate-700'
                  }`}
                >
                  <Package className="w-4 h-4" />
                  <span className="text-[11px] font-mono">Envíos</span>
                </button>
              </div>
            </div>

            {/* Dynamic Grid: Service Action + Interactive Hologram Map */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
              {/* Left Column: Active Service Interaction (5 cols) */}
              <div className="lg:col-span-5 w-full">
                {serviceMode === 'move' && (
                  <MoveView
                    onShowToast={showToast}
                    onUpdateOriginDest={(o, d) => {
                      setOrigin(o)
                      setDestination(d)
                    }}
                  />
                )}
                {serviceMode === 'food' && <FoodView onShowToast={showToast} />}
                {serviceMode === 'package' && <PackageView onShowToast={showToast} />}
              </div>

              {/* Right Column: Live Map (Real or Hologram) (7 cols) */}
              <div className="lg:col-span-7 w-full space-y-3">
                {/* Map Type Switcher */}
                <div className="flex items-center justify-between px-1">
                  <div className="flex items-center space-x-2">
                    <span className="text-xs font-mono text-slate-400">MODO MAPA:</span>
                    <div className="flex items-center bg-nexus-900/90 rounded-xl p-1 border border-slate-800">
                      <button
                        onClick={() => {
                          setMapType('real')
                          sound.tick()
                        }}
                        className={`px-3 py-1 rounded-lg text-xs font-bold transition flex items-center space-x-1.5 ${
                          mapType === 'real'
                            ? 'bg-gradient-to-r from-cyan-500/20 to-brand-500/20 text-cyan-300 border border-cyan-500/40 shadow-sm'
                            : 'text-slate-400 hover:text-white'
                        }`}
                      >
                        <span>🗺️</span>
                        <span>Mapa Real Barbosa</span>
                        <span className="text-[10px] bg-cyan-500/20 text-cyan-300 px-1.5 py-0.2 rounded font-mono">EN VIVO</span>
                      </button>

                      <button
                        onClick={() => {
                          setMapType('hologram')
                          sound.tick()
                        }}
                        className={`px-3 py-1 rounded-lg text-xs font-bold transition flex items-center space-x-1.5 ${
                          mapType === 'hologram'
                            ? 'bg-gradient-to-r from-cyan-500/20 to-brand-500/20 text-cyan-300 border border-cyan-500/40 shadow-sm'
                            : 'text-slate-400 hover:text-white'
                        }`}
                      >
                        <span>📡</span>
                        <span>Radar Táctico</span>
                      </button>
                    </div>
                  </div>

                  <span className="text-[11px] font-mono text-neon-emerald hidden sm:inline">
                    ● Geocerca 3.0 km activa
                  </span>
                </div>

                {/* Render active map */}
                {mapType === 'real' ? (
                  <RealMap
                    origin={origin}
                    destination={destination}
                    selectedVehicle={serviceMode === 'move' ? 'moto' : 'car'}
                    onSelectLocation={(loc, type) => {
                      if (type === 'dest') {
                        setDestination(loc)
                        showToast(`🎯 Destino fijado: ${loc}`)
                      } else {
                        setOrigin(loc)
                        showToast(`📍 Origen fijado: ${loc}`)
                      }
                    }}
                  />
                ) : (
                  <InteractiveMap
                    origin={origin}
                    destination={destination}
                    selectedVehicle={serviceMode === 'move' ? 'moto' : 'car'}
                    onSelectLocation={(loc, type) => {
                      if (type === 'dest') {
                        setDestination(loc)
                        showToast(`🎯 Destino fijado: ${loc}`)
                      } else {
                        setOrigin(loc)
                        showToast(`📍 Origen fijado: ${loc}`)
                      }
                    }}
                  />
                )}

                {/* Quick Info Bar below map */}
                <div className="glass-surface p-3.5 rounded-xl border border-slate-800/80 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-400">
                  <div className="flex items-center space-x-2">
                    <span className="w-2 h-2 rounded-full bg-neon-emerald animate-pulse" />
                    <span className="text-slate-300 font-mono text-[11px]">
                      Conductores activos en Barbosa: <strong className="text-white">34 verificados</strong>
                    </span>
                  </div>
                  <div className="flex items-center space-x-4 text-[11px] font-mono">
                    <span>Soporte 24/7 Barbosa</span>
                    <span className="text-slate-600">·</span>
                    <span className="text-cyan-300">Garantía de Cobro Justo</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ======================= DRIVER VIEW ======================= */}
        {activeTab === 'driver' && <DriverPanel onShowToast={showToast} />}

        {/* ======================= MERCHANT VIEW ======================= */}
        {activeTab === 'merchant' && <MerchantPanel onShowToast={showToast} />}

        {/* ======================= ADMIN VIEW ======================= */}
        {activeTab === 'admin' && <AdminPanel onShowToast={showToast} />}
      </main>

      {/* SOS Protocol Emergency Modal */}
      <SosModal
        isOpen={sosOpen}
        onClose={() => setSosOpen(false)}
        onShowToast={showToast}
      />

      {/* Futuristic Footer */}
      <footer className="glass-surface border-t border-slate-800/80 py-6 mt-12 text-slate-400 text-xs">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center space-x-2">
            <span className="font-display font-bold text-white tracking-wider">BARBOYA NEXUS</span>
            <span className="text-slate-600">|</span>
            <span className="text-[11px] font-mono text-slate-400">
              Barbosa, Santander · Colombia (Lat: 5.9333, Lng: -73.6208)
            </span>
          </div>

          <div className="text-[11px] font-mono text-slate-500 text-center sm:text-right">
            © 2026 BarboYa. Movilidad, Gastronomía y Logística Urbana. Todos los derechos reservados.
          </div>
        </div>
      </footer>
    </div>
  )
}
