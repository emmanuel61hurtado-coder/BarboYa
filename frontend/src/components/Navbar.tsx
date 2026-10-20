import React from 'react'
import { ShieldAlert, Volume2, VolumeX, Sparkles, User, Car, Store, ShieldCheck, MapPin, LogIn, LogOut } from 'lucide-react'
import { AppTab, UserRole, UserSession } from '../types'
import { sound } from '../utils/sound'

interface NavbarProps {
  activeTab: AppTab
  setActiveTab: (tab: AppTab) => void
  onTriggerSOS: () => void
  audioEnabled: boolean
  setAudioEnabled: (val: boolean) => void
  session: UserSession | null
  onOpenAuth: (role?: UserRole) => void
  onLogout: () => void
  onShowToast: (msg: string) => void
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  setActiveTab,
  onTriggerSOS,
  audioEnabled,
  setAudioEnabled,
  session,
  onOpenAuth,
  onLogout,
  onShowToast,
}) => {
  const handleTabChange = (tab: AppTab) => {
    // Protección estricta de Admin en Frontend y Backend
    if (tab === 'admin') {
      if (!session || (session.role !== 'admin' && session.role !== 'superadmin')) {
        sound.alert()
        onShowToast('🔒 Acceso Denegado: Esta área está restringida exclusivamente a administradores autorizados.')
        onOpenAuth('admin')
        return
      }
    }
    setActiveTab(tab)
    sound.tick()
  }

  return (
    <header className="sticky top-0 z-50 glass-surface border-b border-slate-800/80 transition-all duration-300">
      <div className="max-w-7xl mx-auto px-3 sm:px-6 py-2.5 flex items-center justify-between gap-2">
        {/* Brand Logo & Telemetry */}
        <div className="flex items-center space-x-3">
          <div 
            onClick={() => handleTabChange('client')}
            className="flex items-center space-x-2 cursor-pointer group"
          >
            <div className="relative">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-brand-500 via-brand-600 to-amber-600 flex items-center justify-center font-display font-black text-white text-xl shadow-brand-glow transform group-hover:scale-105 transition-transform">
                B
              </div>
              <div className="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-cyan-glow rounded-full ring-2 ring-nexus-950 animate-ping" />
            </div>

            <div className="flex flex-col">
              <div className="flex items-center space-x-1.5">
                <span className="font-display font-black text-xl tracking-wider text-white">
                  BARBO<span className="text-cyan-glow">YA</span>
                </span>
                <span className="bg-cyan-500/10 border border-cyan-400/40 text-cyan-300 font-mono text-[9px] font-bold px-1.5 py-0.2 rounded-full uppercase tracking-widest hidden sm:inline-block">
                  NEXUS
                </span>
              </div>
              <div className="flex items-center space-x-1 text-[10px] text-slate-400 font-mono">
                <MapPin className="w-2.5 h-2.5 text-cyan-400" />
                <span>Barbosa, Santander</span>
              </div>
            </div>
          </div>
        </div>

        {/* Central Futuristic Tab Switcher */}
        <nav className="flex items-center bg-nexus-950/90 p-1 rounded-2xl border border-slate-800/90 shadow-inner">
          <button
            onClick={() => handleTabChange('client')}
            className={`px-3 sm:px-4 py-1.5 rounded-xl font-medium text-xs sm:text-sm flex items-center space-x-1.5 transition-all duration-300 ${
              activeTab === 'client'
                ? 'bg-gradient-to-r from-cyan-500/20 to-cyan-500/10 text-cyan-300 border border-cyan-400/50 shadow-cyan-glow-sm font-semibold'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
            }`}
          >
            <User className="w-3.5 h-3.5" />
            <span>Cliente</span>
          </button>

          <button
            onClick={() => handleTabChange('driver')}
            className={`px-3 sm:px-4 py-1.5 rounded-xl font-medium text-xs sm:text-sm flex items-center space-x-1.5 transition-all duration-300 ${
              activeTab === 'driver'
                ? 'bg-gradient-to-r from-neon-emerald/20 to-neon-emerald/10 text-neon-emerald border border-neon-emerald/50 shadow-emerald-glow font-semibold'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
            }`}
          >
            <Car className="w-3.5 h-3.5" />
            <span>Conductor</span>
          </button>

          <button
            onClick={() => handleTabChange('merchant')}
            className={`px-3 sm:px-4 py-1.5 rounded-xl font-medium text-xs sm:text-sm flex items-center space-x-1.5 transition-all duration-300 ${
              activeTab === 'merchant'
                ? 'bg-gradient-to-r from-brand-500/20 to-brand-500/10 text-brand-400 border border-brand-500/50 shadow-brand-glow font-semibold'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
            }`}
          >
            <Store className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Comercio</span>
            <span className="sm:hidden">Tienda</span>
          </button>

          <button
            onClick={() => handleTabChange('admin')}
            className={`px-3 sm:px-4 py-1.5 rounded-xl font-medium text-xs sm:text-sm flex items-center space-x-1.5 transition-all duration-300 ${
              activeTab === 'admin'
                ? 'bg-gradient-to-r from-neon-violet/20 to-neon-violet/10 text-neon-violet border border-neon-violet/50 shadow-lg font-semibold'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
            }`}
          >
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Admin</span>
          </button>
        </nav>

        {/* Right Action Icons: Session, Audio Toggle & Tactical SOS */}
        <div className="flex items-center space-x-2">
          {/* User Session Info or Login CTA */}
          {session ? (
            <div className="flex items-center space-x-1.5 bg-nexus-900/80 border border-slate-800 rounded-xl px-2.5 py-1 text-xs">
              <div className="flex items-center space-x-1.5">
                <span className="w-2 h-2 rounded-full bg-cyan-glow animate-pulse" />
                <span className="font-bold text-white text-xs max-w-[90px] truncate">
                  {session.fullName.split(' ')[0]}
                </span>
                <span className={`text-[9px] font-mono px-1.5 py-0.2 rounded font-bold uppercase ${
                  session.role === 'admin' || session.role === 'superadmin'
                    ? 'bg-purple-500/20 text-purple-300 border border-purple-500/40'
                    : session.role === 'driver' || session.role === 'courier'
                    ? 'bg-neon-emerald/20 text-neon-emerald border border-neon-emerald/40'
                    : session.role === 'merchant_owner'
                    ? 'bg-brand-500/20 text-brand-300 border border-brand-500/40'
                    : 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40'
                }`}>
                  {session.role === 'merchant_owner' ? 'Tienda' : session.role}
                </span>
              </div>
              <button
                onClick={() => {
                  sound.tick()
                  onLogout()
                }}
                className="p-1 text-slate-400 hover:text-rose-400 transition ml-1"
                title="Cerrar Sesión"
              >
                <LogOut className="w-3.5 h-3.5" />
              </button>
            </div>
          ) : (
            <button
              onClick={() => {
                sound.tick()
                onOpenAuth('customer')
              }}
              className="py-1.5 px-3 rounded-xl bg-cyan-500/15 hover:bg-cyan-500/25 border border-cyan-500/30 text-cyan-300 text-xs font-semibold flex items-center space-x-1.5 transition active:scale-95"
            >
              <LogIn className="w-3.5 h-3.5" />
              <span>Ingresar</span>
            </button>
          )}

          {/* Sound Synthesizer Toggle */}
          <button
            onClick={() => {
              const next = !audioEnabled
              setAudioEnabled(next)
              sound.setEnabled(next)
              if (next) sound.success()
            }}
            className={`p-2 rounded-xl border text-xs transition duration-200 hidden sm:flex items-center justify-center ${
              audioEnabled
                ? 'bg-nexus-900 border-cyan-500/30 text-cyan-300'
                : 'bg-nexus-900 border-slate-800 text-slate-500'
            }`}
            title={audioEnabled ? "Efectos de audio activos" : "Audio silenciado"}
          >
            {audioEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
          </button>

          {/* Tactical SOS Button */}
          <button
            onClick={() => {
              sound.alert()
              onTriggerSOS()
            }}
            className="group relative bg-gradient-to-r from-red-600 via-rose-600 to-red-700 hover:from-red-500 hover:to-rose-600 text-white px-3 sm:px-3.5 py-1.5 sm:py-2 rounded-xl font-display font-bold text-xs flex items-center space-x-1.5 shadow-rose-glow border border-red-400/40 active:scale-95 transition-all cursor-pointer"
          >
            <span className="relative flex h-2.5 w-2.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-white opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-white"></span>
            </span>
            <ShieldAlert className="w-4 h-4 text-white" />
            <span className="tracking-wider">SOS</span>
          </button>
        </div>
      </div>
    </header>
  )
}
