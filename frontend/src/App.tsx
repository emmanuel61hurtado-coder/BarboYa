import React, { useState, useEffect } from 'react'
import { 
  Car, Utensils, Package, ShieldAlert, Sparkles, 
  MapPin, AlertTriangle, Radio, Navigation2, CheckCircle2, 
  User, Store, ShieldCheck, Heart, Lock, KeyRound
} from 'lucide-react'
import { AppTab, ServiceMode, UserRole, UserSession } from './types'
import { Navbar } from './components/Navbar'
import { InteractiveMap } from './components/InteractiveMap'
import { RealMap } from './components/RealMap'
import { MoveView } from './components/MoveView'
import { FoodView } from './components/FoodView'
import { PackageView } from './components/PackageView'
import { OrderTrackingView } from './components/OrderTrackingView'
import { DriverPanel } from './components/DriverPanel'
import { MerchantPanel } from './components/MerchantPanel'
import { AdminPanel } from './components/AdminPanel'
import { SosModal } from './components/SosModal'
import { AuthModal } from './components/AuthModal'
import { sound } from './utils/sound'
import { api } from './utils/api'

export default function App() {
  const [activeTab, setActiveTab] = useState<AppTab>('client')
  const [serviceMode, setServiceMode] = useState<ServiceMode>('move')
  const [sosOpen, setSosOpen] = useState<boolean>(false)
  const [sosBannerActive, setSosBannerActive] = useState<boolean>(false)
  const [toast, setToast] = useState<string | null>(null)
  const [audioEnabled, setAudioEnabled] = useState<boolean>(true)
  const [mapType, setMapType] = useState<'real' | 'hologram'>('real')

  // Auth Session State
  const [session, setSession] = useState<UserSession | null>(null)
  const [authModalOpen, setAuthModalOpen] = useState<boolean>(false)
  const [authInitialRole, setAuthInitialRole] = useState<UserRole>('customer')

  // Map route state shared between MoveView and Maps
  const [origin, setOrigin] = useState<string>('Parque Emilio Valenzuela')
  const [destination, setDestination] = useState<string>('Terminal de Transportes de Barbosa')

  // Cargar sesión persistida al inicio
  useEffect(() => {
    try {
      const stored = localStorage.getItem('barboya_user_session')
      if (stored) {
        setSession(JSON.parse(stored))
      }
    } catch {
      // Ignore parse errors
    }
  }, [])

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

  const handleOpenAuth = (role: UserRole = 'customer') => {
    setAuthInitialRole(role)
    setAuthModalOpen(true)
  }

  const handleLogout = () => {
    api.logout()
    setSession(null)
    if (activeTab === 'admin') {
      setActiveTab('client')
    }
    showToast('👋 Sesión cerrada exitosamente')
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
        session={session}
        onOpenAuth={handleOpenAuth}
        onLogout={handleLogout}
        onShowToast={showToast}
      />

      {/* Critical SOS Active Banner */}
      {sosBannerActive && (
        <div className="bg-gradient-to-r from-red-600 via-rose-600 to-red-700 text-white px-4 py-2 flex items-center justify-between text-xs font-mono font-bold shadow-rose-glow animate-pulse z-40">
          <div className="flex items-center space-x-2">
            <ShieldAlert className="w-4 h-4 animate-bounce" />
            <span>ALERTA TÁCTICA ACTIVA: Red de emergencia de Barbosa enlazada. Coordenadas transmitidas.</span>
          </div>
          <button
            onClick={() => setSosBannerActive(false)}
            className="underline hover:text-slate-200 text-[11px] cursor-pointer"
          >
            Desactivar Banner
          </button>
        </div>
      )}

      {/* Toast Notification Floating Pill */}
      {toast && (
        <div className="fixed bottom-6 right-6 z-50 glass-surface-elevated border border-cyan-400/50 text-white px-4 py-3 rounded-2xl shadow-cyan-glow flex items-center space-x-3 text-xs animate-slide-up">
          <Sparkles className="w-4 h-4 text-cyan-300 animate-spin" />
          <span className="font-medium">{toast}</span>
        </div>
      )}

      {/* Main Content Arena */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-3 sm:px-6 py-5 sm:py-6 space-y-6">
        {/* ======================= CLIENT VIEW ======================= */}
        {activeTab === 'client' && (
          <div className="space-y-6">
            {/* Top Submenu: Service Switcher (Move vs Food vs Package vs Tracking) */}
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h1 className="text-2xl sm:text-3xl font-display font-black text-white tracking-wide flex items-center space-x-2">
                  <span>Hola, {session ? session.fullName.split(' ')[0] : 'Barbosa'}</span>
                  <span className="text-xs bg-cyan-500/20 text-cyan-300 font-mono px-2 py-0.5 rounded-full border border-cyan-500/30">
                    SANTANDER
                  </span>
                </h1>
                <p className="text-xs text-slate-400 font-medium">
                  Movilidad sin comisiones abusivas, restaurantes locales y mensajería en tiempo real.
                </p>
              </div>

              {/* Sub-modes Toggle */}
              <div className="flex items-center space-x-1.5 bg-nexus-900/90 p-1 rounded-2xl border border-slate-800 shadow-inner">
                <button
                  onClick={() => {
                    setServiceMode('move')
                    sound.tick()
                  }}
                  className={`px-3 py-2 rounded-xl border flex items-center space-x-1.5 transition-all ${
                    serviceMode === 'move'
                      ? 'bg-cyan-500/15 border-cyan-500 text-cyan-300 shadow-cyan-glow font-bold'
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
                  className={`px-3 py-2 rounded-xl border flex items-center space-x-1.5 transition-all ${
                    serviceMode === 'food'
                      ? 'bg-brand-500/15 border-brand-500 text-brand-400 shadow-brand-glow font-bold'
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
                  className={`px-3 py-2 rounded-xl border flex items-center space-x-1.5 transition-all ${
                    serviceMode === 'package'
                      ? 'bg-neon-emerald/15 border-neon-emerald text-neon-emerald shadow-emerald-glow font-bold'
                      : 'bg-nexus-950/70 border-slate-800 text-slate-400 hover:text-white hover:border-slate-700'
                  }`}
                >
                  <Package className="w-4 h-4" />
                  <span className="text-[11px] font-mono">Envíos</span>
                </button>

                <button
                  onClick={() => {
                    setServiceMode('tracking')
                    sound.tick()
                  }}
                  className={`px-3 py-2 rounded-xl border flex items-center space-x-1.5 transition-all ${
                    serviceMode === 'tracking'
                      ? 'bg-purple-500/15 border-purple-500 text-purple-300 shadow-purple-500/20 font-bold'
                      : 'bg-nexus-950/70 border-slate-800 text-slate-400 hover:text-white hover:border-slate-700'
                  }`}
                >
                  <Navigation2 className="w-4 h-4" />
                  <span className="text-[11px] font-mono">Seguimiento</span>
                </button>
              </div>
            </div>

            {/* Dynamic Grid: Service Action + Interactive Live Map */}
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
                {serviceMode === 'tracking' && <OrderTrackingView onShowToast={showToast} />}
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

        {/* ======================= ADMIN VIEW (PROTECTED) ======================= */}
        {activeTab === 'admin' && (
          session?.role === 'admin' || session?.role === 'superadmin' ? (
            <AdminPanel onShowToast={showToast} />
          ) : (
            <div className="glass-surface-elevated p-8 rounded-3xl border border-purple-500/40 text-center max-w-lg mx-auto space-y-4 shadow-2xl animate-fade-in my-12">
              <div className="w-16 h-16 rounded-2xl bg-purple-500/20 border border-purple-500/40 text-purple-300 flex items-center justify-center mx-auto text-2xl font-black">
                <Lock className="w-8 h-8" />
              </div>
              <h2 className="text-2xl font-black text-white tracking-wide">ÁREA ADMINISTRATIVA RESTRINGIDA</h2>
              <p className="text-xs text-slate-400 leading-relaxed">
                El centro de control de BarboYa requiere verificación de privilegios en el servidor.
                Ningún usuario puede obtener acceso administrativo simplemente modificando el cliente web.
              </p>
              <button
                onClick={() => handleOpenAuth('admin')}
                className="py-2.5 px-6 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold text-xs uppercase tracking-wider transition shadow-lg flex items-center justify-center space-x-2 mx-auto"
              >
                <KeyRound className="w-4 h-4" />
                <span>Ingresar con Cuenta de Administrador</span>
              </button>
            </div>
          )
        )}
      </main>

      {/* Authentication Modal */}
      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
        initialRole={authInitialRole}
        onSuccess={(newSession) => {
          setSession(newSession)
          showToast(`✨ Bienvenido a BarboYa, ${newSession.fullName}`)
          // Redirección inteligente según el rol autenticado
          if (newSession.role === 'admin' || newSession.role === 'superadmin') {
            setActiveTab('admin')
          } else if (newSession.role === 'driver' || newSession.role === 'courier') {
            setActiveTab('driver')
          } else if (newSession.role === 'merchant_owner' || newSession.role === 'merchant_employee') {
            setActiveTab('merchant')
          } else {
            setActiveTab('client')
          }
        }}
      />

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
