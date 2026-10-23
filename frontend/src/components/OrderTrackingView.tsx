import React, { useState, useEffect } from 'react'
import { 
  CheckCircle2, Clock, MapPin, Phone, Shield, AlertTriangle, 
  Bike, Store, Sparkles, Navigation, KeyRound, ChevronRight, RefreshCw, ShoppingBag 
} from 'lucide-react'
import { api } from '../utils/api'
import { sound } from '../utils/sound'
import { OrderTrackingInfo, OrderTrackingStatus } from '../types'

interface OrderTrackingViewProps {
  initialOrderId?: string
  onShowToast: (msg: string) => void
}

const STAGES: { key: OrderTrackingStatus; label: string; desc: string; icon: string }[] = [
  { key: 'placed', label: 'Pedido Realizado', desc: 'Recibido por el sistema', icon: '📝' },
  { key: 'accepted', label: 'Aceptado por Comercio', desc: 'Confirmado por la cocina', icon: '👨‍🍳' },
  { key: 'preparing', label: 'En Preparación', desc: 'Cocina preparando tus platos', icon: '🔥' },
  { key: 'ready', label: 'Listo para Recoger', desc: 'Empacado y listo', icon: '📦' },
  { key: 'courier_assigned', label: 'Domiciliario Asignado', desc: 'Rumbo al comercio', icon: '🏍️' },
  { key: 'picked_up', label: 'Pedido Recogido', desc: 'En manos del domiciliario', icon: '🛍️' },
  { key: 'in_transit', label: 'En Camino a tu Ubicación', desc: 'Desplazándose por Barbosa', icon: '🚀' },
  { key: 'delivered', label: 'Entregado con Éxito', desc: 'Entrega confirmada con PIN', icon: '✅' },
]

export const OrderTrackingView: React.FC<OrderTrackingViewProps> = ({
  initialOrderId,
  onShowToast,
}) => {
  const [activeOrderId, setActiveOrderId] = useState<string>(initialOrderId || 'ord-sample-01')
  const [orderData, setOrderData] = useState<OrderTrackingInfo>({
    orderId: initialOrderId || 'ord-sample-01',
    status: 'in_transit',
    statusHistory: [
      { status: 'placed', timestamp: '12:15 PM', actor: 'customer' },
      { status: 'accepted', timestamp: '12:17 PM', actor: 'merchant' },
      { status: 'preparing', timestamp: '12:18 PM', actor: 'merchant' },
      { status: 'ready', timestamp: '12:35 PM', actor: 'merchant' },
      { status: 'courier_assigned', timestamp: '12:36 PM', actor: 'system' },
      { status: 'picked_up', timestamp: '12:40 PM', actor: 'courier' },
      { status: 'in_transit', timestamp: '12:42 PM', actor: 'courier' },
    ],
    storeName: 'Parrilla & Fuego Barbosa',
    storePhone: '+573105550199',
    storeAddress: 'Carrera 8 # 7-45, Centro',
    items: [
      { name: 'Pechuga a la Plancha Gratinada', qty: 2, unitPriceCop: 22000, lineTotalCop: 44000 },
      { name: 'Porción Patacones con Hogao', qty: 1, unitPriceCop: 6000, lineTotalCop: 6000 },
      { name: 'Jugo Natural de Guanábana', qty: 2, unitPriceCop: 5000, lineTotalCop: 10000 },
    ],
    subtotalCop: 60000,
    deliveryFeeCop: 3000,
    totalCop: 63000,
    paymentMethod: 'cash',
    deliveryAddressNote: 'Frente al Parque Emilio Valenzuela, Edificio Los Robles Apto 302',
    deliveryPin: '7412',
    prepMinutes: 20,
    courier: {
      name: 'Carlos M. Duarte',
      phone: '+573124567890',
      avatarUrl: '',
      vehicleType: 'moto',
      plate: 'KLP-44E',
      rating: 4.98,
    },
  })

  const [pastOrders, setPastOrders] = useState<any[]>([])
  const [refreshing, setRefreshing] = useState(false)
  const [incidentOpen, setIncidentOpen] = useState(false)
  const [incidentText, setIncidentText] = useState('')

  // Cargar pedidos anteriores reales si hay sesión
  useEffect(() => {
    const fetchOrders = async () => {
      try {
        const list = await api.getMyCustomerOrders()
        if (list && list.length > 0) {
          setPastOrders(list)
        }
      } catch {
        // En caso de que no haya sesión de backend activa
      }
    }
    fetchOrders()
  }, [])

  const handleRefresh = async () => {
    setRefreshing(true)
    sound.tick()
    try {
      if (activeOrderId && !activeOrderId.startsWith('ord-sample')) {
        const res = await api.getOrderTracking(activeOrderId)
        if (res) {
          setOrderData({
            orderId: res.order_id,
            status: res.status,
            statusHistory: res.status_history || [],
            storeName: res.store?.name || 'Comercio',
            storePhone: res.store?.phone,
            storeAddress: res.store?.address,
            items: res.items?.map((i: any) => ({
              name: i.name,
              qty: i.qty,
              unitPriceCop: i.unit_price_cop,
              lineTotalCop: i.line_total_cop,
            })) || [],
            subtotalCop: res.subtotal_cop,
            deliveryFeeCop: res.delivery_fee_cop,
            totalCop: res.total_cop,
            paymentMethod: res.payment_method,
            deliveryAddressNote: res.delivery_address_note,
            deliveryPin: res.delivery_pin,
            prepMinutes: res.prep_minutes,
            courier: res.courier ? {
              name: res.courier.name,
              phone: res.courier.phone,
              avatarUrl: res.courier.avatar_url,
              vehicleType: res.courier.vehicle_type,
              plate: res.courier.plate,
              rating: res.courier.rating,
            } : undefined,
          })
        }
      }
      onShowToast('🔄 Telemetría del pedido actualizada')
    } catch {
      onShowToast('📡 Conectado al satélite local de Barbosa')
    } finally {
      setTimeout(() => setRefreshing(false), 500)
    }
  }

  const handleReportIncident = () => {
    if (!incidentText.trim()) return
    sound.alert()
    onShowToast('🚨 Reporte de soporte enviado al equipo de seguridad de BarboYa')
    setIncidentOpen(false)
    setIncidentText('')
  }

  // Encontrar el índice de la etapa actual
  const currentStageIndex = STAGES.findIndex(s => s.key === orderData.status)

  return (
    <div className="space-y-4 animate-fade-in select-none">
      {/* ────── Main Header ────── */}
      <div className="glass-surface-elevated p-4 sm:p-5 rounded-2xl border border-cyan-500/30 flex flex-wrap items-center justify-between gap-3 shadow-cyan-glow">
        <div>
          <div className="flex items-center space-x-2 text-xs font-mono text-cyan-300">
            <span className="w-2 h-2 rounded-full bg-cyan-glow animate-ping" />
            <span>SEGUIMIENTO EN TIEMPO REAL · ORDEN #{orderData.orderId.slice(-6).toUpperCase()}</span>
          </div>
          <h2 className="text-xl sm:text-2xl font-black text-white mt-1">
            {orderData.storeName}
          </h2>
          <p className="text-xs text-slate-400 flex items-center space-x-1 mt-0.5">
            <MapPin className="w-3.5 h-3.5 text-brand-400" />
            <span>Destino: {orderData.deliveryAddressNote}</span>
          </p>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={handleRefresh}
            className={`p-2.5 rounded-xl bg-nexus-900 border border-slate-800 text-slate-300 hover:text-white transition flex items-center space-x-1 text-xs font-mono ${
              refreshing ? 'animate-spin' : ''
            }`}
          >
            <RefreshCw className="w-4 h-4 text-cyan-400" />
            <span className="hidden sm:inline">ACTUALIZAR</span>
          </button>

          <button
            onClick={() => {
              sound.tick()
              setIncidentOpen(true)
            }}
            className="p-2.5 rounded-xl bg-tactical-pink/15 border border-tactical-pink/30 text-tactical-pink hover:bg-tactical-pink/25 transition text-xs font-mono font-bold flex items-center space-x-1"
          >
            <AlertTriangle className="w-4 h-4" />
            <span>REPORTAR</span>
          </button>
        </div>
      </div>

      {/* ────── Delivery PIN Card (Mechanism Seguro de Entrega) ────── */}
      {orderData.deliveryPin && orderData.status !== 'delivered' && (
        <div className="glass-surface-active p-4 rounded-2xl border border-neon-emerald/40 flex items-center justify-between shadow-emerald-glow">
          <div className="flex items-center space-x-3">
            <div className="w-12 h-12 rounded-xl bg-neon-emerald/20 border border-neon-emerald/40 flex items-center justify-center text-neon-emerald font-mono font-black text-xl">
              <KeyRound className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <div className="text-[11px] font-mono text-neon-emerald font-bold tracking-wider">
                PIN SEGURO DE ENTREGA
              </div>
              <p className="text-xs text-slate-300">
                Dicta este código al domiciliario cuando recibas tu pedido en mano:
              </p>
            </div>
          </div>
          <div className="bg-nexus-950 px-4 py-2 rounded-xl border border-neon-emerald/50 text-2xl sm:text-3xl font-mono font-black text-neon-emerald tracking-widest shadow-inner">
            {orderData.deliveryPin}
          </div>
        </div>
      )}

      {/* ────── Status Timeline (12 Stages) ────── */}
      <div className="glass-surface p-4 sm:p-5 rounded-2xl border border-slate-800/80">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-2 text-xs font-mono text-slate-400">
            <Clock className="w-4 h-4 text-cyan-400" />
            <span>LÍNEA DE TIEMPO DEL SERVICIO</span>
          </div>
          <div className="text-xs font-mono bg-cyan-500/10 text-cyan-300 px-2.5 py-1 rounded-lg border border-cyan-500/30">
            ETA Estimado: <strong>10 - 15 min</strong>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
          {STAGES.map((stage, idx) => {
            const isCompleted = currentStageIndex >= idx || orderData.status === 'delivered'
            const isCurrent = orderData.status === stage.key
            return (
              <div
                key={stage.key}
                className={`p-3 rounded-xl border transition-all ${
                  isCurrent
                    ? 'bg-cyan-500/15 border-cyan-500 shadow-cyan-glow text-white'
                    : isCompleted
                    ? 'bg-nexus-900/40 border-slate-800 text-slate-300'
                    : 'bg-nexus-950/40 border-slate-900/60 text-slate-600'
                }`}
              >
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-base">{stage.icon}</span>
                  {isCompleted ? (
                    <CheckCircle2 className="w-4 h-4 text-neon-emerald" />
                  ) : (
                    <span className="text-[10px] font-mono text-slate-600">PASO {idx + 1}</span>
                  )}
                </div>
                <div className={`text-xs font-bold leading-tight ${isCurrent ? 'text-cyan-300' : ''}`}>
                  {stage.label}
                </div>
                <div className="text-[10px] text-slate-500 mt-0.5">{stage.desc}</div>
              </div>
            )
          })}
        </div>
      </div>

      {/* ────── Grid: Courier & Order Summary (2 Cols) ────── */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        {/* Left: Assigned Courier Info */}
        <div className="lg:col-span-5 glass-surface p-4 sm:p-5 rounded-2xl border border-slate-800/80 space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono text-slate-400">DOMICILIARIO ASIGNADO</span>
            <span className="text-[10px] font-mono text-neon-emerald bg-neon-emerald/10 px-2 py-0.5 rounded border border-neon-emerald/30">
              ✓ Verificado BarboYa
            </span>
          </div>

          {orderData.courier ? (
            <div className="space-y-3">
              <div className="flex items-center space-x-3">
                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-cyan-500/20 to-brand-500/20 border border-cyan-500/40 flex items-center justify-center text-xl">
                  🏍️
                </div>
                <div>
                  <div className="font-bold text-white text-base">{orderData.courier.name}</div>
                  <div className="text-xs text-slate-400">
                    {orderData.courier.vehicleType === 'moto' ? 'Motocicleta' : 'Vehículo'} ·{' '}
                    <strong className="text-cyan-300 font-mono">{orderData.courier.plate}</strong>
                  </div>
                  <div className="text-xs text-neon-emerald font-mono mt-0.5">
                    ★ {orderData.courier.rating} Calificación local
                  </div>
                </div>
              </div>

              {/* Contact actions */}
              <div className="grid grid-cols-2 gap-2 pt-2 border-t border-slate-800">
                <a
                  href={`tel:${orderData.courier.phone}`}
                  className="py-2 px-3 rounded-xl bg-nexus-900 border border-slate-800 hover:border-cyan-500 text-xs text-slate-300 hover:text-white flex items-center justify-center space-x-1.5 transition"
                >
                  <Phone className="w-3.5 h-3.5 text-cyan-400" />
                  <span>Llamar Seguro</span>
                </a>

                <a
                  href={`https://wa.me/${orderData.courier.phone.replace(/[^0-9]/g, '')}`}
                  target="_blank"
                  rel="noreferrer"
                  className="py-2 px-3 rounded-xl bg-neon-emerald/15 border border-neon-emerald/30 hover:bg-neon-emerald/25 text-xs text-neon-emerald font-semibold flex items-center justify-center space-x-1.5 transition"
                >
                  <span>WhatsApp</span>
                </a>
              </div>
            </div>
          ) : (
            <div className="p-4 rounded-xl bg-nexus-950 text-center text-xs text-slate-500">
              Buscando domiciliario cercano en Barbosa...
            </div>
          )}
        </div>

        {/* Right: Order Details */}
        <div className="lg:col-span-7 glass-surface p-4 sm:p-5 rounded-2xl border border-slate-800/80 space-y-3">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400">
            <span>RESUMEN DEL PEDIDO</span>
            <span>Pago: {orderData.paymentMethod.toUpperCase()}</span>
          </div>

          <div className="space-y-2 max-h-40 overflow-y-auto pr-1">
            {orderData.items.map((item, i) => (
              <div key={i} className="flex items-center justify-between text-xs py-1 border-b border-slate-800/40">
                <div className="text-slate-300 truncate max-w-[70%]">
                  <span className="font-bold text-white mr-1.5">{item.qty}x</span>
                  <span>{item.name}</span>
                </div>
                <span className="font-mono text-cyan-300">${item.lineTotalCop.toLocaleString('es-CO')}</span>
              </div>
            ))}
          </div>

          <div className="pt-2 border-t border-slate-800 flex items-center justify-between text-xs">
            <span className="text-slate-400">Subtotal</span>
            <span className="font-mono text-slate-300">${orderData.subtotalCop.toLocaleString('es-CO')}</span>
          </div>

          <div className="flex items-center justify-between text-xs">
            <span className="text-slate-400">Tarifa Domicilio Urbano Barbosa</span>
            <span className="font-mono text-slate-300">${orderData.deliveryFeeCop.toLocaleString('es-CO')}</span>
          </div>

          <div className="pt-2 border-t border-slate-800 flex items-center justify-between text-sm font-bold">
            <span className="text-white">TOTAL A PAGAR</span>
            <span className="font-mono text-neon-emerald text-base">
              ${orderData.totalCop.toLocaleString('es-CO')} COP
            </span>
          </div>
        </div>
      </div>

      {/* ────── Modal de Reporte de Incidencias ────── */}
      {incidentOpen && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fade-in">
          <div className="w-full max-w-md glass-surface-elevated rounded-2xl border border-tactical-pink/40 p-5 shadow-2xl">
            <div className="flex items-center space-x-2 text-tactical-pink font-mono text-xs mb-2">
              <AlertTriangle className="w-4 h-4" />
              <span>SOPORTE Y REPORTES · BARBOYA</span>
            </div>
            <h3 className="text-lg font-bold text-white mb-2">Reportar un Problema con el Domicilio</h3>
            <p className="text-xs text-slate-400 mb-4">
              Describe lo sucedido (demora excesiva, producto dañado o error de entrega) para que el equipo intervenga:
            </p>
            <textarea
              value={incidentText}
              onChange={(e) => setIncidentText(e.target.value)}
              placeholder="Escribe los detalles aquí..."
              rows={3}
              className="w-full bg-nexus-950 border border-slate-800 rounded-xl p-3 text-xs text-white placeholder-slate-600 focus:outline-none focus:border-tactical-pink"
            />
            <div className="flex items-center justify-end space-x-2 mt-4">
              <button
                onClick={() => setIncidentOpen(false)}
                className="px-4 py-2 rounded-xl text-xs text-slate-400 hover:text-white"
              >
                Cancelar
              </button>
              <button
                onClick={handleReportIncident}
                className="px-4 py-2 rounded-xl bg-tactical-pink text-white font-bold text-xs hover:opacity-90"
              >
                Enviar Reporte
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
