import React, { useState } from 'react'
import { 
  Car, Bike, MapPin, DollarSign, ArrowRight, ShieldCheck, Shield,
  Sparkles, Clock, Check, X, Phone, Share2, AlertCircle, RefreshCw, ChevronRight, UserCheck
} from 'lucide-react'
import { VehicleType, DriverOffer, TripState } from '../types'
import { sound } from '../utils/sound'

interface MoveViewProps {
  onShowToast: (msg: string) => void
  onUpdateOriginDest?: (origin: string, dest: string) => void
}

const SAMPLE_OFFERS: DriverOffer[] = [
  {
    id: 'off-1',
    driverName: 'Carlos Mario Duarte',
    vehicle: 'Yamaha FZ 2.0 (Negra)',
    plate: 'KLP-44E',
    rating: 4.98,
    tripsCount: 420,
    distanceKm: 0.8,
    etaMinutes: 2,
    proposedPriceCop: 6000,
    photoUrl: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=120&auto=format&fit=crop&q=80',
    verified: true,
  },
  {
    id: 'off-2',
    driverName: 'Diana Marcela Gómez',
    vehicle: 'Suzuki Gixxer 150',
    plate: 'BTX-12D',
    rating: 4.95,
    tripsCount: 285,
    distanceKm: 1.2,
    etaMinutes: 4,
    proposedPriceCop: 5500,
    photoUrl: 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=120&auto=format&fit=crop&q=80',
    verified: true,
    isFemale: true,
  },
  {
    id: 'off-3',
    driverName: 'Javier Rodríguez P.',
    vehicle: 'Chevrolet Spark GT (Gris)',
    plate: 'XMA-901',
    rating: 4.91,
    tripsCount: 610,
    distanceKm: 1.5,
    etaMinutes: 5,
    proposedPriceCop: 7000,
    photoUrl: 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=120&auto=format&fit=crop&q=80',
    verified: true,
  }
]

export const MoveView: React.FC<MoveViewProps> = ({ onShowToast, onUpdateOriginDest }) => {
  const [vehicle, setVehicle] = useState<VehicleType>('moto')
  const [origin, setOrigin] = useState<string>('Parque Emilio Valenzuela')
  const [destination, setDestination] = useState<string>('Terminal de Transportes de Barbosa')
  const [priceCop, setPriceCop] = useState<number>(6000)
  const [preferFemaleDriver, setPreferFemaleDriver] = useState<boolean>(false)
  const [tripState, setTripState] = useState<TripState>('idle')
  const [acceptedDriver, setAcceptedDriver] = useState<DriverOffer | null>(null)
  const [counterInputId, setCounterInputId] = useState<string | null>(null)
  const [counterPrice, setCounterPrice] = useState<number>(5500)
  const [tripProgress, setTripProgress] = useState<number>(35)

  // Filtrado de ofertas según preferencia
  const availableOffers = SAMPLE_OFFERS.filter((offer: DriverOffer) => {
    if (preferFemaleDriver) {
      return offer.isFemale === true
    }
    return true
  })

  const handleAdjustPrice = (delta: number) => {
    const next = Math.max(3000, priceCop + delta)
    setPriceCop(next)
    sound.tick()
  }

  const handleRequestTrip = () => {
    sound.tick()
    setTripState('searching')
    onShowToast(
      preferFemaleDriver
        ? "⚡ Buscando conductoras verificadas disponibles en Barbosa..."
        : "⚡ Transmitiendo solicitud vía WebSockets a conductores cercanos..."
    )
    
    // Simulate real-time responses from PostGIS radius
    setTimeout(() => {
      setTripState('offers_received')
      sound.success()
      if (preferFemaleDriver) {
        onShowToast("🔔 Conductora verificada encontrada en tu sector.")
      } else {
        onShowToast("🔔 3 conductores han respondido a tu solicitud.")
      }
    }, 1800)
  }

  const handleAcceptOffer = (driver: DriverOffer) => {
    sound.success()
    setAcceptedDriver(driver)
    setTripState('trip_active')
    onShowToast(`🎉 ¡Oferta de ${driver.driverName} aceptada! Conductor en camino.`)
  }

  const handleSendCounter = (driverId: string) => {
    sound.tick()
    onShowToast(`💬 Contraoferta de $${counterPrice.toLocaleString('es-CO')} enviada al conductor.`)
    setCounterInputId(null)
  }

  const handleCompleteTrip = () => {
    sound.success()
    setTripState('completed')
    onShowToast("✅ Viaje completado satisfactoriamente. Tarifa liquidada.")
    setTimeout(() => {
      setTripState('idle')
      setAcceptedDriver(null)
    }, 2500)
  }

  return (
    <div className="space-y-4">
      {/* State: IDLE OR SEARCHING OR OFFERS */}
      {tripState !== 'trip_active' && tripState !== 'completed' && (
        <div className="glass-surface p-5 rounded-2xl border border-slate-800 space-y-5">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-display font-bold text-lg text-white flex items-center space-x-2">
                <span>BarboYa Move</span>
                <span className="text-[10px] bg-cyan-500/10 text-cyan-300 border border-cyan-500/30 px-2 py-0.5 rounded-full font-mono">
                  PON TU PRECIO
                </span>
              </h3>
              <p className="text-xs text-slate-400">Negocia tu tarifa directamente con conductores verificados de Barbosa.</p>
            </div>
          </div>

          {/* Vehicle Selector: Moto vs Car */}
          <div className="grid grid-cols-2 gap-2.5">
            <button
              onClick={() => {
                setVehicle('moto')
                sound.tick()
              }}
              className={`p-3 rounded-xl border flex items-center space-x-3 transition-all duration-300 text-left ${
                vehicle === 'moto'
                  ? 'bg-gradient-to-r from-cyan-500/15 to-cyan-500/5 border-cyan-400 text-cyan-300 shadow-cyan-glow-sm luminous-border-cyan'
                  : 'bg-nexus-900/60 border-slate-800 text-slate-400 hover:border-slate-700'
              }`}
            >
              <div className={`p-2 rounded-lg ${vehicle === 'moto' ? 'bg-cyan-500/20 text-cyan-300' : 'bg-slate-800 text-slate-400'}`}>
                <Bike className="w-5 h-5" />
              </div>
              <div>
                <div className="font-bold text-sm text-white flex items-center space-x-1.5">
                  <span>Moto Ágil</span>
                  <span className="text-[10px] font-mono bg-neon-emerald/20 text-neon-emerald px-1.5 py-0.2 rounded font-normal">Más rápida</span>
                </div>
                <div className="text-[11px] text-slate-400">1 pasajero · 2-3 min llegada</div>
              </div>
            </button>

            <button
              onClick={() => {
                setVehicle('car')
                sound.tick()
              }}
              className={`p-3 rounded-xl border flex items-center space-x-3 transition-all duration-300 text-left ${
                vehicle === 'car'
                  ? 'bg-gradient-to-r from-brand-500/15 to-brand-500/5 border-brand-500 text-brand-400 shadow-brand-glow luminous-border-brand'
                  : 'bg-nexus-900/60 border-slate-800 text-slate-400 hover:border-slate-700'
              }`}
            >
              <div className={`p-2 rounded-lg ${vehicle === 'car' ? 'bg-brand-500/20 text-brand-400' : 'bg-slate-800 text-slate-400'}`}>
                <Car className="w-5 h-5" />
              </div>
              <div>
                <div className="font-bold text-sm text-white flex items-center space-x-1.5">
                  <span>Auto Confort</span>
                </div>
                <div className="text-[11px] text-slate-400">Hasta 4 pax · Equipaje</div>
              </div>
            </button>
          </div>

          {/* Route Inputs */}
          <div className="space-y-3">
            <div>
              <label className="block text-[11px] font-mono text-slate-400 mb-1 flex items-center space-x-1">
                <span className="w-2 h-2 rounded-full bg-brand-500" />
                <span>PUNTO DE RECOGIDA (ORIGEN)</span>
              </label>
              <div className="relative">
                <MapPin className="absolute left-3 top-3 w-4 h-4 text-brand-400" />
                <input
                  type="text"
                  value={origin}
                  onChange={(e) => {
                    setOrigin(e.target.value)
                    if (onUpdateOriginDest) onUpdateOriginDest(e.target.value, destination)
                  }}
                  className="w-full pl-9 pr-3 py-2.5 bg-nexus-950/80 border border-slate-800 focus:border-brand-500 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-brand-500 transition font-medium"
                />
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-mono text-slate-400 mb-1 flex items-center space-x-1">
                <span className="w-2 h-2 rounded-full bg-cyan-glow" />
                <span>PUNTO DE LLEGADA (DESTINO)</span>
              </label>
              <div className="relative">
                <MapPin className="absolute left-3 top-3 w-4 h-4 text-cyan-400" />
                <input
                  type="text"
                  value={destination}
                  onChange={(e) => {
                    setDestination(e.target.value)
                    if (onUpdateOriginDest) onUpdateOriginDest(origin, e.target.value)
                  }}
                  className="w-full pl-9 pr-3 py-2.5 bg-nexus-950/80 border border-slate-800 focus:border-cyan-400 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-cyan-400 transition font-medium"
                />
              </div>
            </div>

            {/* Quick destination chips for Barbosa */}
            <div className="flex flex-wrap gap-1.5 pt-1">
              {['Hospital San Antonio', 'Sede UIS', 'Plaza de Mercado', 'Pozo Negro'].map((fav) => (
                <button
                  key={fav}
                  onClick={() => {
                    setDestination(fav)
                    sound.tick()
                    if (onUpdateOriginDest) onUpdateOriginDest(origin, fav)
                  }}
                  className="text-[11px] font-mono bg-nexus-900 hover:bg-slate-800 border border-slate-800 text-slate-300 px-2.5 py-1 rounded-lg transition"
                >
                  + {fav}
                </button>
              ))}
            </div>
          </div>

          {/* "Pon Tu Precio" Interactive Fare Stepper */}
          <div className="p-4 rounded-xl bg-nexus-950/90 border border-slate-800/90 space-y-3">
            <div className="flex items-center justify-between text-xs">
              <span className="font-mono text-slate-400">TARIFA PROPUESTA (COP)</span>
              <span className="text-[11px] text-cyan-400 font-mono">Sugerida: $5.500 COP</span>
            </div>

            <div className="flex items-center justify-between gap-3">
              <button
                onClick={() => handleAdjustPrice(-500)}
                className="w-12 h-12 rounded-xl bg-slate-800/80 hover:bg-slate-700 text-white font-mono text-xl font-bold flex items-center justify-center border border-slate-700 transition active:scale-95"
              >
                -
              </button>

              <div className="text-center flex-1">
                <div className="font-display font-black text-2xl sm:text-3xl text-cyan-glow tracking-wide">
                  $ {priceCop.toLocaleString('es-CO')}
                </div>
                <div className="text-[10px] text-slate-400 font-mono">Pasos de ± $500 COP</div>
              </div>

              <button
                onClick={() => handleAdjustPrice(500)}
                className="w-12 h-12 rounded-xl bg-slate-800/80 hover:bg-slate-700 text-white font-mono text-xl font-bold flex items-center justify-center border border-slate-700 transition active:scale-95"
              >
                +
              </button>
            </div>
          </div>

          {/* Preferencia: Conductora Mujer (Modulo 4) */}
          <div className="p-3.5 rounded-xl bg-nexus-950/80 border border-slate-800 flex items-center justify-between transition hover:border-pink-500/40">
            <div className="flex items-center space-x-3">
              <div className={`p-2 rounded-xl transition ${preferFemaleDriver ? 'bg-pink-500/20 text-pink-400 border border-pink-500/40' : 'bg-slate-800 text-slate-400'}`}>
                <Shield className="w-4 h-4" />
              </div>
              <div>
                <div className="text-xs font-bold text-white flex items-center space-x-1.5">
                  <span>Preferir conductora mujer</span>
                  <span className="text-[9px] font-mono bg-pink-500/20 text-pink-300 px-1.5 py-0.2 rounded font-normal">
                    Voluntario
                  </span>
                </div>
                <div className="text-[10px] text-slate-400">
                  Prioriza conductoras verificadas por el sistema de seguridad
                </div>
              </div>
            </div>

            <button
              type="button"
              onClick={() => {
                const next = !preferFemaleDriver
                setPreferFemaleDriver(next)
                sound.tick()
                onShowToast(next ? "🌸 Preferencia de conductora mujer activada" : "Filtro de género desactivado")
              }}
              className={`w-11 h-6 rounded-full transition-colors relative p-0.5 ${
                preferFemaleDriver ? 'bg-pink-500' : 'bg-slate-700'
              }`}
            >
              <div
                className={`w-5 h-5 rounded-full bg-white shadow-md transform transition-transform ${
                  preferFemaleDriver ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          {/* Action Button: Solicitud */}
          {tripState === 'idle' && (
            <button
              onClick={handleRequestTrip}
              className="w-full py-3.5 px-4 rounded-xl font-display font-bold text-base bg-gradient-to-r from-brand-500 via-orange-500 to-amber-500 hover:from-brand-600 hover:to-orange-600 text-white shadow-brand-glow transform hover:-translate-y-0.5 active:translate-y-0 transition-all flex items-center justify-center space-x-2"
            >
              <span>Solicitar Viaje a Mi Precio</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          )}

          {tripState === 'searching' && (
            <div className="p-4 rounded-xl bg-nexus-950 border border-cyan-500/30 flex items-center justify-center space-x-3 text-cyan-300">
              <RefreshCw className="w-5 h-5 animate-spin" />
              <span className="text-sm font-semibold font-mono">
                {preferFemaleDriver ? 'Buscando conductoras verificadas en Barbosa...' : 'Buscando conductores en Barbosa (Radio 3km)...'}
              </span>
            </div>
          )}

          {/* State: OFFERS RECEIVED */}
          {tripState === 'offers_received' && (
            <div className="space-y-3 pt-2">
              <div className="flex items-center justify-between text-xs">
                <span className="font-bold text-white flex items-center space-x-1.5">
                  <span className="w-2 h-2 rounded-full bg-neon-emerald animate-pulse" />
                  <span>Ofertas de Conductores ({availableOffers.length})</span>
                </span>
                <button
                  onClick={() => {
                    setTripState('idle')
                    sound.tick()
                  }}
                  className="text-slate-400 hover:text-rose-400 text-[11px] underline"
                >
                  Cancelar búsqueda
                </button>
              </div>

              {/* Si no hay ofertas con el filtro activo */}
              {availableOffers.length === 0 ? (
                <div className="p-4 rounded-xl bg-nexus-950/90 border border-amber-500/30 text-center space-y-2.5">
                  <p className="text-xs text-amber-200">
                    En este momento no hay conductoras mujeres disponibles cerca de tu ubicación.
                  </p>
                  <div className="flex items-center justify-center space-x-2">
                    <button
                      onClick={() => {
                        setPreferFemaleDriver(false)
                        sound.tick()
                      }}
                      className="px-3 py-1.5 rounded-lg bg-slate-800 text-xs text-cyan-300 hover:bg-slate-700"
                    >
                      Ver todos los conductores
                    </button>
                    <button
                      onClick={() => setTripState('idle')}
                      className="px-3 py-1.5 rounded-lg bg-slate-900 text-xs text-slate-400 hover:text-white"
                    >
                      Cancelar
                    </button>
                  </div>
                </div>
              ) : (
                <div className="space-y-2.5">
                  {availableOffers.map((offer: DriverOffer) => (
                    <div
                      key={offer.id}
                      className="p-3.5 rounded-xl bg-nexus-950/90 border border-slate-800 hover:border-cyan-500/40 transition-all duration-200 space-y-2.5"
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-2.5">
                          <img
                            src={offer.photoUrl}
                            alt={offer.driverName}
                            className="w-10 h-10 rounded-full object-cover ring-2 ring-cyan-500/40"
                          />
                          <div>
                            <div className="font-bold text-sm text-white flex items-center space-x-1">
                              <span>{offer.driverName}</span>
                              {offer.verified && (
                                <span title="Conductor Verificado"><UserCheck className="w-3.5 h-3.5 text-cyan-400" /></span>
                              )}
                              {offer.isFemale && (
                                <span className="text-[10px] bg-pink-500/20 text-pink-300 px-1 rounded font-mono">Mujer</span>
                              )}
                            </div>
                            <div className="text-[11px] text-slate-400">
                              {offer.vehicle} · <span className="font-mono text-cyan-300">{offer.plate}</span>
                            </div>
                          </div>
                        </div>

                        <div className="text-right">
                          <div className="font-display font-black text-lg text-neon-emerald">
                            $ {offer.proposedPriceCop.toLocaleString('es-CO')}
                          </div>
                          <div className="text-[10px] text-slate-400 font-mono">
                            ★ {offer.rating} ({offer.tripsCount} v.)
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center justify-between text-[11px] text-slate-400 border-t border-slate-900 pt-2 font-mono">
                        <span>Llegada estimada: <strong className="text-white">{offer.etaMinutes} min</strong></span>
                        <span>Distancia: <strong className="text-white">{offer.distanceKm} km</strong></span>
                      </div>

                    {/* Actions: Accept or Counter-Offer */}
                    {counterInputId === offer.id ? (
                      <div className="flex items-center gap-2 pt-1">
                        <input
                          type="number"
                          value={counterPrice}
                          onChange={(e) => setCounterPrice(Number(e.target.value))}
                          step={500}
                          className="w-32 px-2.5 py-1.5 bg-slate-900 border border-cyan-400 rounded-lg text-xs font-bold text-cyan-300"
                        />
                        <button
                          onClick={() => handleSendCounter(offer.id)}
                          className="px-3 py-1.5 bg-cyan-500 hover:bg-cyan-600 text-nexus-950 font-bold rounded-lg text-xs transition"
                        >
                          Enviar
                        </button>
                        <button
                          onClick={() => setCounterInputId(null)}
                          className="px-2 py-1.5 bg-slate-800 text-slate-400 rounded-lg text-xs"
                        >
                          X
                        </button>
                      </div>
                    ) : (
                      <div className="grid grid-cols-2 gap-2 pt-1">
                        <button
                          onClick={() => {
                            setCounterInputId(offer.id)
                            setCounterPrice(offer.proposedPriceCop - 500)
                            sound.tick()
                          }}
                          className="py-1.5 px-3 rounded-lg border border-slate-700 hover:border-slate-500 text-xs font-semibold text-slate-300 transition"
                        >
                          Contraofertar
                        </button>
                        <button
                          onClick={() => handleAcceptOffer(offer)}
                          className="py-1.5 px-3 rounded-lg bg-neon-emerald hover:bg-emerald-500 text-nexus-950 text-xs font-bold transition shadow-emerald-glow flex items-center justify-center space-x-1"
                        >
                          <Check className="w-3.5 h-3.5" />
                          <span>Aceptar</span>
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
        </div>
      )}

      {/* State: TRIP ACTIVE */}
      {tripState === 'trip_active' && acceptedDriver && (
        <div className="glass-surface-active p-5 rounded-2xl border border-neon-emerald/40 space-y-4 animate-fadeIn">
          {/* Active Trip Header */}
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <span className="w-2.5 h-2.5 rounded-full bg-neon-emerald animate-ping" />
              <span className="font-display font-bold text-white text-base">VIAJE EN CURSO</span>
            </div>
            <span className="font-mono text-xs text-neon-emerald bg-neon-emerald/10 border border-neon-emerald/30 px-2.5 py-1 rounded-full">
              ETA: 4 min
            </span>
          </div>

          {/* Security PIN Display */}
          <div className="p-3 bg-nexus-950/90 rounded-xl border border-slate-800 flex items-center justify-between">
            <div>
              <div className="text-[10px] font-mono text-slate-400">PIN DE SEGURIDAD (ENTRÉGALO AL SUBIR)</div>
              <div className="text-xs text-slate-300">El conductor lo validará en su consola.</div>
            </div>
            <div className="font-mono font-black text-2xl tracking-widest text-cyan-glow bg-cyan-950/40 px-3 py-1 rounded-lg border border-cyan-400/40">
              4829
            </div>
          </div>

          {/* Assigned Driver Card */}
          <div className="flex items-center justify-between p-3.5 rounded-xl bg-nexus-950/60 border border-slate-800">
            <div className="flex items-center space-x-3">
              <img
                src={acceptedDriver.photoUrl}
                alt={acceptedDriver.driverName}
                className="w-12 h-12 rounded-full object-cover ring-2 ring-neon-emerald"
              />
              <div>
                <div className="font-bold text-sm text-white flex items-center space-x-1.5">
                  <span>{acceptedDriver.driverName}</span>
                  <ShieldCheck className="w-4 h-4 text-cyan-400" />
                </div>
                <div className="text-xs text-slate-400">{acceptedDriver.vehicle}</div>
                <div className="text-[11px] font-mono text-cyan-300 font-bold">{acceptedDriver.plate}</div>
              </div>
            </div>

            <div className="text-right">
              <div className="font-display font-bold text-lg text-neon-emerald">
                $ {acceptedDriver.proposedPriceCop.toLocaleString('es-CO')}
              </div>
              <div className="text-xs text-slate-400 font-mono">Pago Efectivo / Nequi</div>
            </div>
          </div>

          {/* Dynamic Progress Bar */}
          <div className="space-y-1.5">
            <div className="flex justify-between text-[11px] font-mono text-slate-400">
              <span>Recogida en {origin}</span>
              <span>Rumbo a {destination}</span>
            </div>
            <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
              <div
                className="h-full bg-gradient-to-r from-cyan-glow to-neon-emerald rounded-full transition-all duration-700"
                style={{ width: `${tripProgress}%` }}
              />
            </div>
          </div>

          {/* Action Row */}
          <div className="grid grid-cols-2 gap-2 pt-2">
            <button
              onClick={() => onShowToast("🔗 Enlace público de seguimiento copiado al portapapeles.")}
              className="p-2.5 rounded-xl bg-nexus-950 border border-slate-700 text-xs font-semibold text-slate-300 hover:text-white flex items-center justify-center space-x-1.5 transition"
            >
              <Share2 className="w-3.5 h-3.5 text-cyan-400" />
              <span>Compartir Ruta</span>
            </button>

            <button
              onClick={handleCompleteTrip}
              className="p-2.5 rounded-xl bg-neon-emerald hover:bg-emerald-500 text-nexus-950 text-xs font-bold flex items-center justify-center space-x-1.5 transition shadow-emerald-glow"
            >
              <Check className="w-4 h-4" />
              <span>Simular Fin de Viaje</span>
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
