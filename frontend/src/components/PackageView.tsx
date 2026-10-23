import React, { useState } from 'react'
import { 
  Package, MapPin, ShieldCheck, CheckCircle2, 
  ArrowRight, Clock, FileText, Box, AlertTriangle, UserCheck
} from 'lucide-react'
import { sound } from '../utils/sound'

interface PackageViewProps {
  onShowToast: (msg: string) => void
}

export const PackageView: React.FC<PackageViewProps> = ({ onShowToast }) => {
  const [pickup, setPickup] = useState('Notaría Única de Barbosa (Cra 8 # 7-22)')
  const [dropoff, setDropoff] = useState('Barrio La Floresta · Casa 14')
  const [packageType, setPackageType] = useState<'docs' | 'small' | 'medium' | 'fragile'>('docs')
  const [recipientName, setRecipientName] = useState('María Camila Santos')
  const [recipientPhone, setRecipientPhone] = useState('314 285 9912')
  const [dispatched, setDispatched] = useState(false)
  const [currentStep, setCurrentStep] = useState(1)

  const packageOptions = [
    { id: 'docs', label: 'Documentos', icon: FileText, desc: 'Papelería, trámites notariales', basePrice: 4000 },
    { id: 'small', label: 'Paquete Chico', icon: Box, desc: 'Hasta 2 kg (medicamentos, llaves)', basePrice: 5000 },
    { id: 'medium', label: 'Caja Mediana', icon: Package, desc: 'Hasta 8 kg (ropa, repuestos)', basePrice: 7000 },
  ]

  const selectedOption = packageOptions.find((p) => p.id === packageType) || packageOptions[0]

  const handleDispatch = () => {
    sound.success()
    setDispatched(true)
    onShowToast("📦 Mensajería solicitada. Repartidor asignado en Barbosa.")
    
    // Simulate courier pickup milestone
    setTimeout(() => {
      setCurrentStep(2)
      sound.tick()
      onShowToast("🛵 Mensajero llegó al punto de recogida y recibió el paquete.")
    }, 3000)
  }

  return (
    <div className="glass-surface p-5 rounded-2xl border border-slate-800 space-y-5">
      <div>
        <div className="flex items-center space-x-2">
          <h3 className="font-display font-bold text-xl text-white">BarboYa Envíos</h3>
          <span className="bg-cyan-500/10 text-cyan-300 border border-cyan-500/30 text-[10px] font-mono px-2 py-0.5 rounded-full uppercase">
            LOGÍSTICA SMART
          </span>
        </div>
        <p className="text-xs text-slate-400 mt-0.5">
          Mensajería express urbana en Barbosa. Entrega garantizada con código de confirmación PIN.
        </p>
      </div>

      {!dispatched ? (
        <div className="space-y-4">
          {/* Package Type Selector */}
          <div>
            <label className="block text-[11px] font-mono text-slate-400 mb-2">TIPO DE ENVÍO</label>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
              {packageOptions.map((opt) => {
                const Icon = opt.icon
                const isSelected = packageType === opt.id
                return (
                  <button
                    key={opt.id}
                    onClick={() => {
                      setPackageType(opt.id as any)
                      sound.tick()
                    }}
                    className={`p-3 rounded-xl border text-left transition-all ${
                      isSelected
                        ? 'bg-cyan-500/15 border-cyan-400 text-cyan-300 shadow-cyan-glow-sm luminous-border-cyan'
                        : 'bg-nexus-950/70 border-slate-800 text-slate-400 hover:border-slate-700'
                    }`}
                  >
                    <Icon className="w-5 h-5 mb-1.5 text-cyan-400" />
                    <div className="font-bold text-xs text-white">{opt.label}</div>
                    <div className="text-[10px] text-slate-400 mt-0.5">{opt.desc}</div>
                    <div className="text-[11px] font-mono font-bold text-neon-emerald mt-1.5">
                      $ {opt.basePrice.toLocaleString('es-CO')}
                    </div>
                  </button>
                )
              })}
            </div>
          </div>

          {/* Addresses */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
            <div>
              <label className="block text-[11px] font-mono text-slate-400 mb-1 flex items-center space-x-1">
                <span className="w-2 h-2 rounded-full bg-brand-500" />
                <span>PUNTO DE RECOGIDA</span>
              </label>
              <div className="relative">
                <MapPin className="absolute left-3 top-3 w-4 h-4 text-brand-400" />
                <input
                  type="text"
                  value={pickup}
                  onChange={(e) => setPickup(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 bg-nexus-950/80 border border-slate-800 focus:border-brand-500 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none"
                />
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-mono text-slate-400 mb-1 flex items-center space-x-1">
                <span className="w-2 h-2 rounded-full bg-cyan-glow" />
                <span>DIRECCIÓN DE DESTINO</span>
              </label>
              <div className="relative">
                <MapPin className="absolute left-3 top-3 w-4 h-4 text-cyan-400" />
                <input
                  type="text"
                  value={dropoff}
                  onChange={(e) => setDropoff(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 bg-nexus-950/80 border border-slate-800 focus:border-cyan-400 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none"
                />
              </div>
            </div>
          </div>

          {/* Recipient info */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
            <div>
              <label className="block text-[11px] font-mono text-slate-400 mb-1">NOMBRE DE QUIEN RECIBE</label>
              <input
                type="text"
                value={recipientName}
                onChange={(e) => setRecipientName(e.target.value)}
                className="w-full px-3 py-2.5 bg-nexus-950/80 border border-slate-800 rounded-xl text-xs text-white focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-[11px] font-mono text-slate-400 mb-1">TELÉFONO DE CONTACTO</label>
              <input
                type="text"
                value={recipientPhone}
                onChange={(e) => setRecipientPhone(e.target.value)}
                className="w-full px-3 py-2.5 bg-nexus-950/80 border border-slate-800 rounded-xl text-xs text-white focus:outline-none"
              />
            </div>
          </div>

          {/* Cost breakdown */}
          <div className="p-3.5 rounded-xl bg-nexus-950/90 border border-slate-800 flex items-center justify-between">
            <div>
              <div className="text-[10px] font-mono text-slate-400">TARIFA CALCULADA DE ENVÍO</div>
              <div className="text-xs text-slate-300">Incluye seguro de entrega y PIN de seguridad</div>
            </div>
            <div className="font-display font-black text-xl text-neon-emerald">
              $ {selectedOption.basePrice.toLocaleString('es-CO')} COP
            </div>
          </div>

          <button
            onClick={handleDispatch}
            className="w-full py-3.5 px-4 rounded-xl font-display font-bold text-sm bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-nexus-950 shadow-cyan-glow transition flex items-center justify-center space-x-2"
          >
            <span>Despachar Mensajería BarboYa</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      ) : (
        /* Dispatched Milestone Tracking View */
        <div className="glass-surface-elevated p-5 rounded-2xl border border-cyan-500/30 space-y-4 animate-fadeIn">
          <div className="flex items-center justify-between border-b border-slate-800 pb-3">
            <div>
              <span className="text-[10px] font-mono text-neon-emerald bg-neon-emerald/10 px-2 py-0.5 rounded border border-neon-emerald/30">
                ENVÍO #BY-8831 EN CURSO
              </span>
              <h4 className="font-bold text-sm text-white mt-1">Paquete en Ruta con Mensajero</h4>
            </div>
            <span className="font-mono text-xs text-cyan-300">Tiempo est: 12 min</span>
          </div>

          {/* Security PIN code */}
          <div className="p-3.5 bg-nexus-950 rounded-xl border border-slate-800 flex items-center justify-between">
            <div>
              <div className="text-[10px] font-mono text-slate-400">PIN DE ENTREGA REQUERIDO</div>
              <div className="text-xs text-slate-300">Compartir con {recipientName} al entregar.</div>
            </div>
            <div className="font-mono font-black text-2xl text-cyan-glow bg-cyan-950/40 px-3 py-1 rounded-lg border border-cyan-400/40">
              7194
            </div>
          </div>

          {/* Stepper tracker */}
          <div className="space-y-3 pt-2">
            {[
              { step: 1, title: 'Asignado a Repartidor', desc: 'Fabián Celis · Pulsar 180 (WXX-31D)' },
              { step: 2, title: 'Paquete Recogido', desc: 'Notaría Única de Barbosa' },
              { step: 3, title: 'En Tránsito hacia Destino', desc: 'Rumbo a Barrio La Floresta' },
              { step: 4, title: 'Entrega Verificada con PIN', desc: 'Garantía BarboYa Nexus' },
            ].map((st) => (
              <div key={st.step} className="flex items-start space-x-3">
                <div className={`w-6 h-6 rounded-full flex items-center justify-center shrink-0 text-xs font-mono font-bold ${
                  currentStep >= st.step 
                    ? 'bg-neon-emerald text-nexus-950 shadow-emerald-glow' 
                    : 'bg-slate-800 text-slate-500'
                }`}>
                  {currentStep >= st.step ? '✓' : st.step}
                </div>
                <div>
                  <div className={`text-xs font-bold ${currentStep >= st.step ? 'text-white' : 'text-slate-500'}`}>
                    {st.title}
                  </div>
                  <div className="text-[11px] text-slate-400">{st.desc}</div>
                </div>
              </div>
            ))}
          </div>

          <button
            onClick={() => {
              setDispatched(false)
              setCurrentStep(1)
            }}
            className="w-full mt-2 py-2.5 rounded-xl bg-nexus-950 border border-slate-700 hover:border-slate-500 text-xs font-semibold text-slate-300 transition"
          >
            Crear Otro Envío
          </button>
        </div>
      )}
    </div>
  )
}
