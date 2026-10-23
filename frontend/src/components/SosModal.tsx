import React from 'react'
import { ShieldAlert, Phone, MapPin, Radio, AlertTriangle, X, Check } from 'lucide-react'
import { sound } from '../utils/sound'

interface SosModalProps {
  isOpen: boolean
  onClose: () => void
  onShowToast: (msg: string) => void
}

export const SosModal: React.FC<SosModalProps> = ({ isOpen, onClose, onShowToast }) => {
  if (!isOpen) return null

  const handleBroadcast = () => {
    sound.alert()
    onShowToast("🚨 TELEMETRÍA ENVIADA: Coordenadas transmitidas a Policía Barbosa y contactos de emergencia.")
    onClose()
  }

  return (
    <div className="fixed inset-0 z-50 bg-black/85 backdrop-blur-md flex items-center justify-center p-4 animate-fadeIn">
      <div className="w-full max-w-lg glass-surface-elevated rounded-2xl border-2 border-red-500/80 p-6 space-y-5 shadow-rose-glow">
        {/* Header Alert */}
        <div className="flex items-center justify-between border-b border-red-900/60 pb-3">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 rounded-xl bg-red-600/20 text-red-400 border border-red-500 animate-pulse">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <div>
              <h3 className="font-display font-black text-lg text-white tracking-wide">
                PROTOCOLO DE EMERGENCIA SOS · BARBOYA NEXUS
              </h3>
              <p className="text-xs text-red-300 font-mono">
                Activación de Seguridad Inmediata · Barbosa, Santander
              </p>
            </div>
          </div>
          <button
            onClick={() => {
              sound.tick()
              onClose()
            }}
            className="text-slate-400 hover:text-white p-1 rounded-lg"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Current Location Telemetry */}
        <div className="p-3 bg-nexus-950 rounded-xl border border-red-900/40 flex items-center justify-between text-xs font-mono">
          <div className="flex items-center space-x-2 text-slate-300">
            <MapPin className="w-4 h-4 text-red-400" />
            <span>Coordenadas: 5.9333° N, 73.6208° W</span>
          </div>
          <span className="text-neon-emerald">Precisión GPS: 4m</span>
        </div>

        {/* Emergency Contacts in Barbosa */}
        <div className="space-y-2.5">
          <h4 className="text-xs font-mono text-slate-400 uppercase">Líneas de Atención Inmediata en Barbosa</h4>

          <div className="space-y-2">
            <a
              href="tel:123"
              className="p-3 rounded-xl bg-nexus-950/90 border border-slate-800 hover:border-red-500 flex items-center justify-between transition group"
            >
              <div className="flex items-center space-x-3">
                <div className="p-2 rounded-lg bg-red-500/10 text-red-400 group-hover:bg-red-500 group-hover:text-white transition">
                  <Phone className="w-4 h-4" />
                </div>
                <div>
                  <div className="font-bold text-xs text-white">Policía Nacional · Estación Barbosa</div>
                  <div className="text-[11px] text-slate-400 font-mono">Línea directa: 123 / (607) 748-5111</div>
                </div>
              </div>
              <span className="text-xs font-mono text-red-400 font-bold group-hover:underline">Llamar ➔</span>
            </a>

            <a
              href="tel:6077485222"
              className="p-3 rounded-xl bg-nexus-950/90 border border-slate-800 hover:border-red-500 flex items-center justify-between transition group"
            >
              <div className="flex items-center space-x-3">
                <div className="p-2 rounded-lg bg-red-500/10 text-red-400 group-hover:bg-red-500 group-hover:text-white transition">
                  <Phone className="w-4 h-4" />
                </div>
                <div>
                  <div className="font-bold text-xs text-white">Urgencias Hospital San Antonio</div>
                  <div className="text-[11px] text-slate-400 font-mono">Atención médica 24 Horas: (607) 748-5222</div>
                </div>
              </div>
              <span className="text-xs font-mono text-red-400 font-bold group-hover:underline">Llamar ➔</span>
            </a>

            <a
              href="tel:6077485300"
              className="p-3 rounded-xl bg-nexus-950/90 border border-slate-800 hover:border-red-500 flex items-center justify-between transition group"
            >
              <div className="flex items-center space-x-3">
                <div className="p-2 rounded-lg bg-red-500/10 text-red-400 group-hover:bg-red-500 group-hover:text-white transition">
                  <Phone className="w-4 h-4" />
                </div>
                <div>
                  <div className="font-bold text-xs text-white">Cuerpo de Bomberos de Barbosa</div>
                  <div className="text-[11px] text-slate-400 font-mono">Rescate y Emergencias: (607) 748-5300</div>
                </div>
              </div>
              <span className="text-xs font-mono text-red-400 font-bold group-hover:underline">Llamar ➔</span>
            </a>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="grid grid-cols-2 gap-3 pt-2">
          <button
            onClick={() => {
              sound.tick()
              onClose()
            }}
            className="py-2.5 px-4 rounded-xl border border-slate-700 hover:border-slate-500 text-xs font-bold text-slate-300 transition"
          >
            Falsa Alarma / Cancelar
          </button>

          <button
            onClick={handleBroadcast}
            className="py-2.5 px-4 rounded-xl bg-red-600 hover:bg-red-500 text-white font-display font-bold text-xs shadow-rose-glow transition flex items-center justify-center space-x-1.5"
          >
            <Radio className="w-4 h-4 animate-ping" />
            <span>Emitir Alerta Táctica</span>
          </button>
        </div>
      </div>
    </div>
  )
}
