import React, { useState } from 'react'
import { 
  Store, Clock, DollarSign, CheckCircle2, 
  AlertCircle, ChefHat, Bike, TrendingUp, Sparkles, X, Check
} from 'lucide-react'
import { sound } from '../utils/sound'

interface MerchantPanelProps {
  onShowToast: (msg: string) => void
}

interface OrderQueueItem {
  id: string
  orderNumber: string
  customerName: string
  address: string
  items: string
  totalCop: number
  paymentMethod: string
  status: 'new' | 'kitchen' | 'ready' | 'dispatched'
  createdAt: string
  etaMinutes: number
}

export const MerchantPanel: React.FC<MerchantPanelProps> = ({ onShowToast }) => {
  const [isOpen, setIsOpen] = useState<boolean>(true)
  const [orders, setOrders] = useState<OrderQueueItem[]>([
    {
      id: 'ord-108',
      orderNumber: '#108',
      customerName: 'Santiago Vargas',
      address: 'Barrio La Floresta · Calle 10 # 5-40',
      items: '1x Picada Criolla Familiar ($38.000) + 1x Porción Chorizo ($14.000)',
      totalCop: 52000,
      paymentMethod: 'Nequi / Transferencia',
      status: 'new',
      createdAt: 'Hace 2 min',
      etaMinutes: 25,
    },
    {
      id: 'ord-107',
      orderNumber: '#107',
      customerName: 'Adriana Ortiz',
      address: 'Cra 7 # 8-15 (Frente al Parque)',
      items: '2x Carne Oreada Tradicional (300g)',
      totalCop: 52000,
      paymentMethod: 'Efectivo',
      status: 'kitchen',
      createdAt: 'Hace 14 min',
      etaMinutes: 10,
    }
  ])

  const [inventory, setInventory] = useState([
    { id: 'inv-1', name: 'Picada Criolla Familiar', inStock: true, priceCop: 38000 },
    { id: 'inv-2', name: 'Carne Oreada Tradicional (300g)', inStock: true, priceCop: 26000 },
    { id: 'inv-3', name: 'Chorizo Santandereano Artesanal', inStock: false, priceCop: 14000 },
  ])

  const handleUpdateOrderStatus = (orderId: string, nextStatus: OrderQueueItem['status']) => {
    sound.success()
    setOrders((prev) =>
      prev.map((ord) => (ord.id === orderId ? { ...ord, status: nextStatus } : ord))
    )
    if (nextStatus === 'kitchen') {
      onShowToast("👨‍🍳 Pedido pasado a cocina. Tiempo iniciado: 25 min.")
    } else if (nextStatus === 'ready') {
      onShowToast("🛵 Pedido listo para repartidor. Notificando a repartidores en Barbosa...")
    }
  }

  const handleToggleStock = (invId: string) => {
    sound.tick()
    setInventory((prev) =>
      prev.map((item) => (item.id === invId ? { ...item, inStock: !item.inStock } : item))
    )
  }

  return (
    <div className="space-y-6">
      {/* Merchant Header */}
      <div className="glass-surface-elevated p-5 rounded-2xl border border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <h2 className="font-display font-bold text-xl text-white">Asados Doña Rosa Barbosa</h2>
            <span className="bg-brand-500/10 text-brand-400 border border-brand-500/30 text-[10px] font-mono px-2 py-0.5 rounded-full font-bold">
              COMERCIO AFILIADO
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Barbosa, Santander · Comisión de plataforma: <strong className="text-white">10%</strong>
          </p>
        </div>

        {/* Store Open/Close Toggle */}
        <button
          onClick={() => {
            setIsOpen(!isOpen)
            sound.tick()
          }}
          className={`px-4 py-2 rounded-xl text-xs font-bold font-display flex items-center space-x-2 transition ${
            isOpen
              ? 'bg-neon-emerald/20 text-neon-emerald border border-neon-emerald/40 shadow-emerald-glow'
              : 'bg-rose-500/20 text-rose-300 border border-rose-500/40'
          }`}
        >
          <span className={`w-2 h-2 rounded-full ${isOpen ? 'bg-neon-emerald animate-ping' : 'bg-rose-500'}`} />
          <span>{isOpen ? 'LOCAL ABIERTO (RECIBIENDO)' : 'LOCAL CERRADO'}</span>
        </button>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="glass-surface p-4 rounded-xl border border-slate-800">
          <div className="text-[11px] font-mono text-slate-400">VENTAS DEL DÍA</div>
          <div className="font-display font-black text-2xl text-neon-emerald mt-1">$ 245.000 COP</div>
          <div className="text-[10px] text-slate-500 mt-0.5">7 pedidos procesados</div>
        </div>

        <div className="glass-surface p-4 rounded-xl border border-slate-800">
          <div className="text-[11px] font-mono text-slate-400">TIEMPO PROMEDIO PREPARACIÓN</div>
          <div className="font-display font-black text-2xl text-cyan-glow mt-1">18 min</div>
          <div className="text-[10px] text-neon-emerald mt-0.5">Dentro del rango óptimo (&lt; 25m)</div>
        </div>

        <div className="glass-surface p-4 rounded-xl border border-slate-800">
          <div className="text-[11px] font-mono text-slate-400">LIQUIDACIÓN DISPONIBLE</div>
          <div className="font-display font-black text-2xl text-white mt-1">$ 220.500 COP</div>
          <div className="text-[10px] text-slate-400 mt-0.5">Descontando 10% de fee BarboYa</div>
        </div>
      </div>

      {/* Live Orders Pipeline */}
      <div className="glass-surface p-5 rounded-2xl border border-slate-800 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center space-x-2">
            <ChefHat className="w-5 h-5 text-brand-400" />
            <h3 className="font-display font-bold text-base text-white">Cola de Pedidos en Vivo</h3>
          </div>
          <span className="text-xs font-mono text-cyan-300">
            {orders.length} pedidos activos
          </span>
        </div>

        <div className="space-y-3">
          {orders.map((ord) => (
            <div
              key={ord.id}
              className={`p-4 rounded-xl border transition-all ${
                ord.status === 'new'
                  ? 'bg-brand-500/10 border-brand-500/40 luminous-border-brand'
                  : 'bg-nexus-950/80 border-slate-800'
              }`}
            >
              <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-2">
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="font-display font-bold text-sm text-white">{ord.orderNumber}</span>
                    <span className="text-xs text-slate-300">· {ord.customerName}</span>
                    <span className="text-[10px] font-mono bg-slate-800 text-slate-400 px-1.5 py-0.2 rounded">
                      {ord.createdAt}
                    </span>
                  </div>
                  <div className="text-xs text-brand-300 font-semibold mt-1">{ord.items}</div>
                  <div className="text-[11px] text-slate-400 mt-0.5">{ord.address} · Pago: {ord.paymentMethod}</div>
                </div>

                <div className="text-right">
                  <div className="font-display font-black text-lg text-neon-emerald">
                    $ {ord.totalCop.toLocaleString('es-CO')} COP
                  </div>

                  {ord.status === 'new' && (
                    <div className="flex space-x-2 mt-2">
                      <button
                        onClick={() => handleUpdateOrderStatus(ord.id, 'kitchen')}
                        className="px-3 py-1.5 rounded-lg bg-neon-emerald hover:bg-emerald-500 text-nexus-950 font-bold text-xs transition shadow-emerald-glow"
                      >
                        Aceptar y Cocinar (25m)
                      </button>
                    </div>
                  )}

                  {ord.status === 'kitchen' && (
                    <button
                      onClick={() => handleUpdateOrderStatus(ord.id, 'ready')}
                      className="px-3 py-1.5 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-nexus-950 font-bold text-xs transition shadow-cyan-glow mt-2"
                    >
                      Listo para Repartidor
                    </button>
                  )}

                  {ord.status === 'ready' && (
                    <span className="text-xs font-mono text-cyan-300 bg-cyan-950/60 px-2 py-1 rounded inline-block mt-2">
                      Esperando recogida de repartidor
                    </span>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Quick Stock / Menu Management */}
      <div className="glass-surface p-5 rounded-2xl border border-slate-800 space-y-3">
        <h4 className="font-display font-bold text-sm text-white">Disponibilidad de Productos en Barbosa</h4>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {inventory.map((item) => (
            <div
              key={item.id}
              className="p-3 rounded-xl bg-nexus-950/80 border border-slate-800 flex items-center justify-between"
            >
              <div>
                <div className="font-bold text-xs text-white">{item.name}</div>
                <div className="text-[11px] font-mono text-slate-400">${item.priceCop.toLocaleString('es-CO')}</div>
              </div>
              <button
                onClick={() => handleToggleStock(item.id)}
                className={`text-[10px] font-mono px-2 py-1 rounded font-bold transition ${
                  item.inStock
                    ? 'bg-neon-emerald/20 text-neon-emerald border border-neon-emerald/40'
                    : 'bg-rose-500/20 text-rose-300 border border-rose-500/40'
                }`}
              >
                {item.inStock ? 'ACTIVO' : 'AGOTADO'}
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
