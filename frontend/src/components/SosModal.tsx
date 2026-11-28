import React, { useState } from 'react'
import { ShieldAlert, Phone, MapPin, Radio, X, AlertCircle } from 'lucide-react'
import { api } from '../utils/api'
import { sound } from '../utils/sound'

interface SosModalProps {
  isOpen: boolean
  onClose: () => void
  onShowToast: (msg: string) => void
  onAlertRecorded?: () => void
}

// Líneas nacionales. Agrega números locales solo después de confirmarlos con cada entidad.
const EMERGENCY_LINES = [
  { number: '123', title: 'Emergencias', detail: 'Policía y atención de emergencias' },
  { number: '119', title: 'Bomberos', detail: 'Incendios y rescate' },
]

const getPosition = (): Promise<{ lat: number; lng: number } | undefined> =>
  new Promise((resolve) => {
    if (!navigator.geolocation) return resolve(undefined)
    // Respaldo por si el navegador deja la solicitud de permiso sin responder
    const fallback = setTimeout(() => resolve(undefined), 8000)
    const done = (value?: { lat: number; lng: number }) => {
      clearTimeout(fallback)
      resolve(value)
    }
    navigator.geolocation.getCurrentPosition(
      (p) => done({ lat: p.coords.latitude, lng: p.coords.longitude }),
      () => done(),
      { enableHighAccuracy: true, timeout: 6000 }
    )
  })

export const SosModal: React.FC<SosModalProps> = ({ isOpen, onClose, onShowToast, onAlertRecorded }) => {
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!isOpen) return null

  const handleClose = () => {
    sound.tick()
    setError(null)
    onClose()
  }

  const handleRegister = async () => {
    sound.alert()
    setError(null)
    if (!api.getToken()) {
      setError('Inicia sesión para registrar la alerta. Si estás en peligro, llama al 123.')
      return
    }
    setSending(true)
    try {
      const position = await getPosition()
      const res = await api.triggerSos(position)
      onShowToast(`${res.message}${position ? '' : ' La alerta se guardó sin tu ubicación.'}`)
      onAlertRecorded?.()
      onClose()
    } catch (err: any) {
      setError(`No se pudo registrar la alerta (${err?.message || 'sin conexión'}). Llama al 123.`)
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 bg-black/85 backdrop-blur-md flex items-center justify-center p-4 animate-fadeIn">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="sos-title"
        className="w-full max-w-lg glass-surface-elevated rounded-2xl border-2 border-red-500/80 p-6 space-y-5 shadow-rose-glow max-h-[90vh] overflow-y-auto"
      >
        <div className="flex items-center justify-between border-b border-red-900/60 pb-3">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 rounded-xl bg-red-600/20 text-red-400 border border-red-500">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <div>
              <h3 id="sos-title" className="font-display font-black text-lg text-white tracking-wide">
                EMERGENCIA SOS
              </h3>
              <p className="text-xs text-red-300 font-mono">Si estás en peligro, llama primero.</p>
            </div>
          </div>
          <button onClick={handleClose} aria-label="Cerrar" className="text-slate-400 hover:text-white p-1 rounded-lg">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="space-y-2">
          {EMERGENCY_LINES.map((line) => (
            <a
              key={line.number}
              href={`tel:${line.number}`}
              className="p-3 rounded-xl bg-nexus-950/90 border border-slate-800 hover:border-red-500 flex items-center justify-between transition group"
            >
              <div className="flex items-center space-x-3">
                <div className="p-2 rounded-lg bg-red-500/10 text-red-400 group-hover:bg-red-500 group-hover:text-white transition">
                  <Phone className="w-4 h-4" />
                </div>
                <div>
                  <div className="font-bold text-xs text-white">
                    {line.title} · {line.number}
                  </div>
                  <div className="text-[11px] text-slate-400 font-mono">{line.detail}</div>
                </div>
              </div>
              <span className="text-xs font-mono text-red-400 font-bold group-hover:underline">Llamar ➔</span>
            </a>
          ))}
        </div>

        <div className="p-3 bg-nexus-950 rounded-xl border border-red-900/40 flex items-start space-x-2 text-xs text-slate-300">
          <MapPin className="w-4 h-4 text-red-400 shrink-0" />
          <span>
            Puedes dejar la alerta registrada en BarboYa, con tu ubicación si lo permites. El registro no avisa
            automáticamente a la policía ni a tus contactos.
          </span>
        </div>

        {error && (
          <div role="alert" className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/40 flex items-start space-x-2 text-xs text-rose-200">
            <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <div className="grid grid-cols-2 gap-3 pt-2">
          <button
            onClick={handleClose}
            className="py-2.5 px-4 rounded-xl border border-slate-700 hover:border-slate-500 text-xs font-bold text-slate-300 transition"
          >
            Cancelar
          </button>
          <button
            onClick={handleRegister}
            disabled={sending}
            className="py-2.5 px-4 rounded-xl bg-red-600 hover:bg-red-500 text-white font-display font-bold text-xs shadow-rose-glow transition flex items-center justify-center space-x-1.5 disabled:opacity-50"
          >
            <Radio className="w-4 h-4" />
            <span>{sending ? 'Registrando…' : 'Registrar alerta'}</span>
          </button>
        </div>
      </div>
    </div>
  )
}
