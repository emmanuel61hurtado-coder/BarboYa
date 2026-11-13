import React, { useState } from 'react'
import { 
  Car, Bike, DollarSign, Power, Navigation, 
  Check, X, AlertCircle, Clock, ShieldCheck, TrendingUp, Wallet, ArrowUpRight
} from 'lucide-react'
import { sound } from '../utils/sound'

interface DriverPanelProps {
  onShowToast: (msg: string) => void
}

export const DriverPanel: React.FC<DriverPanelProps> = ({ onShowToast }) => {
  const [isOnline, setIsOnline] = useState<boolean>(true)
  const [walletBalance, setWalletBalance] = useState<number>(18500)
  const [todayEarnings, setTodayEarnings] = useState<number>(74000)
  const [tripsCompleted, setTripsCompleted] = useState<number>(6)
  const [hasIncomingRequest, setHasIncomingRequest] = useState<boolean>(true)
  const [showTopupModal, setShowTopupModal] = useState<boolean>(false)
  const [topupAmount, setTopupAmount] = useState<number>(10000)

  const handleToggleOnline = () => {
    const next = !isOnline
    setIsOnline(next)
    if (next) {
      sound.success()
      onShowToast("🟢 Estás EN LÍNEA. Recibiendo solicitudes en Barbosa.")
    } else {
      sound.tick()
      onShowToast("⚪ Modo FUERA DE SERVICIO activado.")
    }
  }

  const handleAcceptRequest = () => {
    sound.success()
    setHasIncomingRequest(false)
    setTodayEarnings((prev) => prev + 6000)
    setTripsCompleted((prev) => prev + 1)
    setWalletBalance((prev) => prev - 300) // Descuenta el fee de $300 COP
    onShowToast("✅ ¡Carrera aceptada! Navega al punto de recogida en Parque Emilio Valenzuela.")
  }

  const handleRejectRequest = () => {
    sound.tick()
    setHasIncomingRequest(false)
    onShowToast("❌ Solicitud descartada.")
  }

  const handleTopup = () => {
    sound.success()
    setWalletBalance((prev) => prev + topupAmount)
    setShowTopupModal(false)
    onShowToast(`💰 Recarga aprobada. +$${topupAmount.toLocaleString('es-CO')} acreditados a tu bolsa.`)
  }

  return (
    <div className="space-y-6">
      {/* Top Cockpit Header */}
      <div className="glass-surface-elevated p-5 rounded-2xl border border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <h2 className="font-display font-bold text-xl text-white">Consola del Conductor</h2>
            <span className="bg-neon-emerald/10 text-neon-emerald border border-neon-emerald/30 text-[10px] font-mono px-2 py-0.5 rounded-full font-bold">
              BARBOYA MOVE PRO
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Carlos Mario Duarte · Yamaha FZ 2.0 (<span className="text-cyan-300 font-mono">KLP-44E</span>)
          </p>
        </div>

        {/* Big Online/Offline Tactile Toggle */}
        <button
          onClick={handleToggleOnline}
          className={`px-5 py-2.5 rounded-xl font-display font-bold text-xs sm:text-sm flex items-center space-x-2 transition-all duration-300 shadow-lg ${
            isOnline
              ? 'bg-gradient-to-r from-neon-emerald to-emerald-600 text-nexus-950 shadow-emerald-glow luminous-border-emerald'
              : 'bg-slate-800 text-slate-400 border border-slate-700 hover:text-white'
          }`}
        >
          <Power className={`w-4 h-4 ${isOnline ? 'animate-pulse' : ''}`} />
          <span>{isOnline ? 'DISPONIBLE (EN LÍNEA)' : 'FUERA DE SERVICIO'}</span>
        </button>
      </div>

      {/* Telemetry Stats: Wallet, Earnings, Rating */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Wallet & Hybrid Billing Scheme */}
        <div className="glass-surface p-5 rounded-2xl border border-slate-800 space-y-2 relative overflow-hidden">
          <div className="flex justify-between items-start">
            <span className="text-[11px] font-mono text-slate-400">BOLSA PREPAGO</span>
            <span className="text-[10px] font-mono bg-cyan-500/10 text-cyan-300 border border-cyan-500/30 px-1.5 py-0.2 rounded">
              FEE $300/VIAJE
            </span>
          </div>
          <div className="font-display font-black text-2xl text-white">
            $ {walletBalance.toLocaleString('es-CO')} COP
          </div>
          <div className="flex items-center justify-between text-[11px] pt-1">
            <span className="text-neon-emerald font-medium">✓ Apto para aceptar</span>
            <button
              onClick={() => {
                setShowTopupModal(true)
                sound.tick()
              }}
              className="text-cyan-400 hover:text-cyan-300 font-semibold underline flex items-center space-x-0.5"
            >
              <span>Recargar</span>
              <ArrowUpRight className="w-3 h-3" />
            </button>
          </div>
        </div>

        {/* Today's Earnings */}
        <div className="glass-surface p-5 rounded-2xl border border-slate-800 space-y-2">
          <div className="flex justify-between items-start">
            <span className="text-[11px] font-mono text-slate-400">GANANCIAS DE HOY</span>
            <span className="text-[10px] font-mono text-neon-emerald flex items-center space-x-0.5">
              <TrendingUp className="w-3 h-3" />
              <span>+18% vs ayer</span>
            </span>
          </div>
          <div className="font-display font-black text-2xl text-neon-emerald">
            $ {todayEarnings.toLocaleString('es-CO')} COP
          </div>
          <div className="text-[11px] text-slate-400 font-mono">
            {tripsCompleted} carreras completadas · 100% de la tarifa cobrada
          </div>
        </div>

        {/* Rating and Tier */}
        <div className="glass-surface p-5 rounded-2xl border border-slate-800 space-y-2">
          <div className="flex justify-between items-start">
            <span className="text-[11px] font-mono text-slate-400">DESEMPEÑO Y CONFIANZA</span>
            <span className="text-[10px] font-mono bg-amber-500/10 text-amber-300 px-1.5 py-0.2 rounded border border-amber-500/30">
              NEXUS ELITE
            </span>
          </div>
          <div className="font-display font-black text-2xl text-amber-400 flex items-center space-x-2">
            <span>4.98 ★</span>
            <span className="text-xs text-slate-400 font-normal font-mono">(420 viajes)</span>
          </div>
          <div className="text-[11px] text-slate-400 font-mono">
            Tasa de aceptación: <strong className="text-white">98.2%</strong>
          </div>
        </div>
      </div>

      {/* Incoming Request Radar Simulator */}
      {isOnline && hasIncomingRequest && (
        <div className="glass-surface-active p-5 rounded-2xl border border-cyan-400/50 space-y-4 shadow-cyan-glow animate-fadeIn">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <span className="w-3 h-3 rounded-full bg-cyan-glow animate-ping" />
              <h3 className="font-display font-bold text-base text-white">NUEVA SOLICITUD EN TU RADIO (0.8 KM)</h3>
            </div>
            <span className="text-xs font-mono text-cyan-300 bg-cyan-950/70 border border-cyan-400/30 px-2 py-0.5 rounded">
              Expira en: 18s
            </span>
          </div>

          <div className="p-4 rounded-xl bg-nexus-950/80 border border-slate-800 space-y-2.5">
            <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-2">
              <div>
                <div className="font-bold text-sm text-white flex items-center space-x-2">
                  <span>Cliente: Laura Sofía V. (4.9★)</span>
                  <span className="text-[10px] font-mono bg-slate-800 text-slate-300 px-1.5 py-0.2 rounded">1 Pasajero</span>
                </div>
                <div className="text-xs text-slate-400 mt-1">
                  Recogida: <strong className="text-slate-200">Parque Emilio Valenzuela</strong> ➔ Destino: <strong className="text-slate-200">Hospital San Antonio</strong>
                </div>
              </div>

              <div className="text-right">
                <div className="font-display font-black text-2xl text-cyan-glow">
                  $ 6.000 COP
                </div>
                <div className="text-[10px] font-mono text-slate-400">
                  Fee a descontar: $300 COP
                </div>
              </div>
            </div>

            <div className="flex items-center justify-between text-[11px] font-mono text-slate-400 border-t border-slate-900 pt-2">
              <span>Distancia viaje: <strong className="text-white">1.8 km</strong></span>
              <span>Ganancia neta estimada: <strong className="text-neon-emerald font-bold">$ 5.700 COP</strong></span>
            </div>
          </div>

          {/* Action Buttons: Accept / Counter / Ignore */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 pt-1">
            <button
              onClick={handleRejectRequest}
              className="py-2.5 px-4 rounded-xl border border-slate-700 hover:border-rose-500/50 text-slate-400 hover:text-rose-300 text-xs font-bold transition flex items-center justify-center space-x-1"
            >
              <X className="w-3.5 h-3.5" />
              <span>Ignorar</span>
            </button>

            <button
              onClick={() => {
                sound.tick()
                onShowToast("💬 Contraoferta de $7.000 enviada al cliente.")
                setHasIncomingRequest(false)
              }}
              className="py-2.5 px-4 rounded-xl bg-nexus-900 border border-cyan-400 text-cyan-300 hover:bg-cyan-950 text-xs font-bold transition flex items-center justify-center space-x-1"
            >
              <span>Contraofertar $7.000</span>
            </button>

            <button
              onClick={handleAcceptRequest}
              className="py-2.5 px-4 rounded-xl bg-neon-emerald hover:bg-emerald-500 text-nexus-950 text-xs font-bold transition shadow-emerald-glow flex items-center justify-center space-x-1"
            >
              <Check className="w-4 h-4" />
              <span>Aceptar Viaje ($6.000)</span>
            </button>
          </div>
        </div>
      )}

      {/* Quick Topup Modal */}
      {showTopupModal && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-sm glass-surface-elevated p-6 rounded-2xl border border-cyan-500/40 space-y-4 shadow-2xl">
            <div className="flex justify-between items-center border-b border-slate-800 pb-2">
              <h4 className="font-display font-bold text-base text-white">Recargar Bolsa Prepago</h4>
              <button onClick={() => setShowTopupModal(false)} className="text-slate-400 hover:text-white">✕</button>
            </div>

            <p className="text-xs text-slate-400">
              Recarga tu saldo para continuar aceptando viajes bajo el fee de $300 COP por carrera.
            </p>

            <div className="grid grid-cols-3 gap-2">
              {[5000, 10000, 25000].map((amt) => (
                <button
                  key={amt}
                  onClick={() => setTopupAmount(amt)}
                  className={`p-2.5 rounded-xl border text-center transition ${
                    topupAmount === amt
                      ? 'bg-cyan-500/20 border-cyan-400 text-cyan-300 font-bold'
                      : 'bg-nexus-950 border-slate-800 text-slate-400'
                  }`}
                >
                  <div className="text-xs font-mono">${amt.toLocaleString('es-CO')}</div>
                </button>
              ))}
            </div>

            <div className="space-y-2 pt-2">
              <button
                onClick={handleTopup}
                className="w-full py-2.5 rounded-xl bg-gradient-to-r from-neon-emerald to-emerald-600 text-nexus-950 font-display font-bold text-xs shadow-emerald-glow transition"
              >
                Pagar con Nequi / Daviplata
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
