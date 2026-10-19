import React, { useState } from 'react'
import { 
  ShieldCheck, Users, Car, Store, DollarSign, 
  Settings, CheckCircle, XCircle, AlertTriangle, Activity, Sliders
} from 'lucide-react'
import { sound } from '../utils/sound'

interface AdminPanelProps {
  onShowToast: (msg: string) => void
}

export const AdminPanel: React.FC<AdminPanelProps> = ({ onShowToast }) => {
  const [feePerTrip, setFeePerTrip] = useState<number>(300)
  const [dailyPass, setDailyPass] = useState<number>(3000)
  const [nightSurcharge, setNightSurcharge] = useState<number>(1000)

  const [pendingDrivers, setPendingDrivers] = useState([
    {
      id: 'drv-app-1',
      name: 'Nelson Darío Flórez',
      vehicle: 'Bajaj Pulsar NS 200 (2024)',
      plate: 'QWE-89G',
      phone: '312 890 1144',
      docs: ['Licencia A2 Vigente', 'SOAT 2027', 'Tecnomecánica'],
      submittedAt: 'Hoy 14:20',
    },
    {
      id: 'drv-app-2',
      name: 'Yurani Becerra P.',
      vehicle: 'Renault Sandero Stepway',
      plate: 'TYU-402',
      phone: '315 442 8891',
      docs: ['Licencia B1 Vigente', 'SOAT Vigente', 'Antecedentes Policía'],
      submittedAt: 'Hoy 11:45',
    }
  ])

  const handleApproveDriver = (id: string, name: string) => {
    sound.success()
    setPendingDrivers((prev) => prev.filter((d) => d.id !== id))
    onShowToast(`🛡️ Conductor ${name} verificado y habilitado en el ecosistema BarboYa.`)
  }

  const handleRejectDriver = (id: string, name: string) => {
    sound.tick()
    setPendingDrivers((prev) => prev.filter((d) => d.id !== id))
    onShowToast(`⚠️ Solicitud de ${name} devuelta para subsanar documentos.`)
  }

  const handleSaveTariff = () => {
    sound.success()
    onShowToast("⚙️ Parámetros de cobro híbrido actualizados en base de datos PostgreSQL.")
  }

  return (
    <div className="space-y-6">
      {/* Admin Central Title */}
      <div className="glass-surface-elevated p-5 rounded-2xl border border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <h2 className="font-display font-bold text-xl text-white">Centro de Control BarboYa Nexus</h2>
            <span className="bg-cyan-500/10 text-cyan-300 border border-cyan-500/30 text-[10px] font-mono px-2 py-0.5 rounded-full font-bold">
              SUPERADMIN BARBOSA
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Supervisión integral de movilidad, restaurantes, mensajería y seguridad territorial.
          </p>
        </div>

        <div className="flex items-center space-x-2 bg-nexus-950 px-3 py-1.5 rounded-xl border border-slate-800 text-xs font-mono">
          <Activity className="w-4 h-4 text-neon-emerald animate-pulse" />
          <span className="text-slate-300">Cluster PostGIS / Redis: <strong className="text-neon-emerald">100% OPERACIONAL</strong></span>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="glass-surface p-4 rounded-xl border border-slate-800">
          <div className="text-[11px] font-mono text-slate-400">GMV HOY (VENTAS TOTALES)</div>
          <div className="font-display font-black text-2xl text-white mt-1">$ 1.420.000</div>
          <div className="text-[10px] text-neon-emerald mt-0.5">68 transacciones en Barbosa</div>
        </div>

        <div className="glass-surface p-4 rounded-xl border border-slate-800">
          <div className="text-[11px] font-mono text-slate-400">COMISIONES & FEES HOY</div>
          <div className="font-display font-black text-2xl text-cyan-glow mt-1">$ 142.800</div>
          <div className="text-[10px] text-slate-400 mt-0.5">Cobro híbrido transparente</div>
        </div>

        <div className="glass-surface p-4 rounded-xl border border-slate-800">
          <div className="text-[11px] font-mono text-slate-400">CONDUCTORES ACTIVOS</div>
          <div className="font-display font-black text-2xl text-neon-emerald mt-1">34</div>
          <div className="text-[10px] text-slate-400 mt-0.5">26 motos · 8 automóviles</div>
        </div>

        <div className="glass-surface p-4 rounded-xl border border-slate-800">
          <div className="text-[11px] font-mono text-slate-400">ALERTAS SOS ABIERTAS</div>
          <div className="font-display font-black text-2xl text-neon-emerald mt-1">0</div>
          <div className="text-[10px] text-neon-emerald mt-0.5">Sin incidentes críticos reportados</div>
        </div>
      </div>

      {/* Verification Queue & Tariff Settings */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Verification Queue */}
        <div className="glass-surface p-5 rounded-2xl border border-slate-800 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-800 pb-3">
            <div className="flex items-center space-x-2">
              <ShieldCheck className="w-5 h-5 text-cyan-400" />
              <h3 className="font-display font-bold text-base text-white">Solicitudes de Verificación de Conductores</h3>
            </div>
            <span className="text-xs font-mono text-slate-400">{pendingDrivers.length} pendientes</span>
          </div>

          <div className="space-y-3">
            {pendingDrivers.length === 0 ? (
              <div className="text-center py-8 text-slate-500 text-xs font-mono">
                No hay solicitudes pendientes por revisar.
              </div>
            ) : (
              pendingDrivers.map((drv) => (
                <div
                  key={drv.id}
                  className="p-3.5 rounded-xl bg-nexus-950/80 border border-slate-800 space-y-2.5"
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <h4 className="font-bold text-sm text-white">{drv.name}</h4>
                      <p className="text-xs text-slate-400">{drv.vehicle} · <span className="font-mono text-cyan-300 font-bold">{drv.plate}</span></p>
                      <p className="text-[11px] text-slate-500">Tel: {drv.phone} · {drv.submittedAt}</p>
                    </div>

                    <div className="flex space-x-2">
                      <button
                        onClick={() => handleRejectDriver(drv.id, drv.name)}
                        className="p-1.5 rounded-lg border border-slate-700 hover:border-rose-500 text-slate-400 hover:text-rose-400 transition"
                        title="Rechazar"
                      >
                        <XCircle className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleApproveDriver(drv.id, drv.name)}
                        className="px-2.5 py-1.5 rounded-lg bg-neon-emerald hover:bg-emerald-500 text-nexus-950 font-bold text-xs flex items-center space-x-1 transition shadow-emerald-glow"
                      >
                        <CheckCircle className="w-3.5 h-3.5" />
                        <span>Aprobar</span>
                      </button>
                    </div>
                  </div>

                  <div className="flex flex-wrap gap-1 pt-1 border-t border-slate-900">
                    {drv.docs.map((doc, idx) => (
                      <span key={idx} className="text-[10px] font-mono bg-slate-900 text-slate-300 px-2 py-0.5 rounded border border-slate-800">
                        ✓ {doc}
                      </span>
                    ))}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Tariff Matrix & Hybrid Billing Config */}
        <div className="glass-surface p-5 rounded-2xl border border-slate-800 space-y-4">
          <div className="flex items-center space-x-2 border-b border-slate-800 pb-3">
            <Sliders className="w-5 h-5 text-brand-400" />
            <h3 className="font-display font-bold text-base text-white">Configuración del Esquema Híbrido</h3>
          </div>

          <div className="space-y-3.5">
            <div>
              <label className="block text-xs font-mono text-slate-400 mb-1">
                FEE FIJO POR CARRERA (DEFAULT_TRIP_FEE_COP)
              </label>
              <div className="flex items-center space-x-3">
                <input
                  type="number"
                  value={feePerTrip}
                  onChange={(e) => setFeePerTrip(Number(e.target.value))}
                  className="w-full px-3 py-2 bg-nexus-950 border border-slate-800 rounded-xl text-xs font-bold text-cyan-300 focus:outline-none"
                />
                <span className="text-xs font-mono text-slate-400 shrink-0">COP / viaje</span>
              </div>
            </div>

            <div>
              <label className="block text-xs font-mono text-slate-400 mb-1">
                PASE ILIMITADO 24H (PRICE_DAILY_COP)
              </label>
              <div className="flex items-center space-x-3">
                <input
                  type="number"
                  value={dailyPass}
                  onChange={(e) => setDailyPass(Number(e.target.value))}
                  className="w-full px-3 py-2 bg-nexus-950 border border-slate-800 rounded-xl text-xs font-bold text-cyan-300 focus:outline-none"
                />
                <span className="text-xs font-mono text-slate-400 shrink-0">COP / día</span>
              </div>
            </div>

            <div>
              <label className="block text-xs font-mono text-slate-400 mb-1">
                RECARGO NOCTURNO (8:00 PM - 5:00 AM)
              </label>
              <div className="flex items-center space-x-3">
                <input
                  type="number"
                  value={nightSurcharge}
                  onChange={(e) => setNightSurcharge(Number(e.target.value))}
                  className="w-full px-3 py-2 bg-nexus-950 border border-slate-800 rounded-xl text-xs font-bold text-cyan-300 focus:outline-none"
                />
                <span className="text-xs font-mono text-slate-400 shrink-0">COP</span>
              </div>
            </div>

            <button
              onClick={handleSaveTariff}
              className="w-full mt-2 py-3 px-4 rounded-xl font-display font-bold text-xs bg-brand-500 hover:bg-brand-600 text-white shadow-brand-glow transition"
            >
              Guardar y Sincronizar Parámetros
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
