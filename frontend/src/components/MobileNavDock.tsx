import React from 'react'
import { Car, Utensils, Package, Bike, User, ShieldAlert, Store, ShieldCheck } from 'lucide-react'
import { AppTab, ServiceMode, UserSession } from '../types'
import { sound } from '../utils/sound'

interface MobileNavDockProps {
  activeTab: AppTab
  setActiveTab: (tab: AppTab) => void
  serviceMode: ServiceMode
  setServiceMode: (mode: ServiceMode) => void
  session: UserSession | null
  onOpenAuth: () => void
  onTriggerSOS: () => void
}

export const MobileNavDock: React.FC<MobileNavDockProps> = ({
  activeTab,
  setActiveTab,
  serviceMode,
  setServiceMode,
  session,
  onOpenAuth,
  onTriggerSOS,
}) => {
  return (
    <div className="sm:hidden fixed bottom-0 left-0 right-0 z-40 bg-slate-950/95 border-t border-slate-800/90 backdrop-blur-2xl px-2 py-1.5 shadow-2xl safe-area-pb">
      <div className="grid grid-cols-5 items-center justify-around gap-1 max-w-md mx-auto">
        {/* 1. Viajes */}
        <button
          type="button"
          onClick={() => {
            setActiveTab('client')
            setServiceMode('move')
            sound.tick()
          }}
          className={`flex flex-col items-center justify-center py-1.5 px-1 rounded-xl transition-all ${
            activeTab === 'client' && serviceMode === 'move'
              ? 'text-sky-400 bg-sky-500/10 font-bold'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <Car className="w-5 h-5 mb-0.5" />
          <span className="text-[10px] tracking-tight">Viajes</span>
        </button>

        {/* 2. Comida */}
        <button
          type="button"
          onClick={() => {
            setActiveTab('client')
            setServiceMode('food')
            sound.tick()
          }}
          className={`flex flex-col items-center justify-center py-1.5 px-1 rounded-xl transition-all ${
            activeTab === 'client' && serviceMode === 'food'
              ? 'text-orange-400 bg-orange-500/10 font-bold'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <Utensils className="w-5 h-5 mb-0.5" />
          <span className="text-[10px] tracking-tight">Comida</span>
        </button>

        {/* 3. Envíos */}
        <button
          type="button"
          onClick={() => {
            setActiveTab('client')
            setServiceMode('package')
            sound.tick()
          }}
          className={`flex flex-col items-center justify-center py-1.5 px-1 rounded-xl transition-all ${
            activeTab === 'client' && serviceMode === 'package'
              ? 'text-emerald-400 bg-emerald-500/10 font-bold'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <Package className="w-5 h-5 mb-0.5" />
          <span className="text-[10px] tracking-tight">Envíos</span>
        </button>

        {/* 4. Conductor / Negocio */}
        <button
          type="button"
          onClick={() => {
            setActiveTab('driver')
            sound.tick()
          }}
          className={`flex flex-col items-center justify-center py-1.5 px-1 rounded-xl transition-all ${
            activeTab === 'driver'
              ? 'text-emerald-400 bg-emerald-500/10 font-bold'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <Bike className="w-5 h-5 mb-0.5" />
          <span className="text-[10px] tracking-tight">Conductor</span>
        </button>

        {/* 5. Cuenta / Portal Dashboard */}
        <button
          type="button"
          onClick={() => {
            setActiveTab('auth')
            sound.tick()
          }}
          className={`flex flex-col items-center justify-center py-1.5 px-1 rounded-xl transition-all ${
            activeTab === 'auth'
              ? 'text-indigo-400 bg-indigo-500/10 font-bold'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <User className="w-5 h-5 mb-0.5" />
          <span className="text-[10px] tracking-tight">
            {session ? session.fullName.split(' ')[0] : 'Cuenta'}
          </span>
        </button>
      </div>
    </div>
  )
}
