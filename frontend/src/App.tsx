import React, { useState } from 'react'
import { Car, Utensils, Package, ShieldAlert, User, DollarSign, Store, Settings, Phone, MapPin, CheckCircle, AlertTriangle } from 'lucide-react'

export default function App() {
  const [activeTab, setActiveTab] = useState<'client' | 'driver' | 'merchant' | 'admin'>('client')
  const [serviceMode, setServiceMode] = useState<'move' | 'food' | 'package'>('move')
  const [sosActive, setSosActive] = useState(false)
  const [toast, setToast] = useState<string | null>(null)

  const showToast = (msg: string) => {
    setToast(msg)
    setTimeout(() => setToast(null), 4000)
  }

  const triggerSOS = () => {
    setSosActive(true)
    showToast("⚠️ Alerta SOS activada. Notificando a soporte y contactos de confianza en Barbosa.")
  }

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col font-sans">
      {/* Top Navbar */}
      <header className="bg-slate-900 text-white shadow-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 py-3 flex justify-between items-center">
          <div className="flex items-center space-x-2">
            <span className="bg-brand-500 text-white font-black px-3 py-1 rounded-lg text-lg tracking-wider">BARBOYA</span>
            <span className="text-xs text-slate-400 hidden sm:inline">Barbosa, Santander (CO)</span>
          </div>

          <nav className="flex space-x-1 sm:space-x-3 text-sm">
            <button
              onClick={() => setActiveTab('client')}
              className={`px-3 py-1.5 rounded-md font-medium transition ${activeTab === 'client' ? 'bg-brand-500 text-white' : 'text-slate-300 hover:bg-slate-800'}`}
            >
              Cliente
            </button>
            <button
              onClick={() => setActiveTab('driver')}
              className={`px-3 py-1.5 rounded-md font-medium transition ${activeTab === 'driver' ? 'bg-brand-500 text-white' : 'text-slate-300 hover:bg-slate-800'}`}
            >
              Conductor
            </button>
            <button
              onClick={() => setActiveTab('merchant')}
              className={`px-3 py-1.5 rounded-md font-medium transition ${activeTab === 'merchant' ? 'bg-brand-500 text-white' : 'text-slate-300 hover:bg-slate-800'}`}
            >
              Comercio
            </button>
            <button
              onClick={() => setActiveTab('admin')}
              className={`px-3 py-1.5 rounded-md font-medium transition ${activeTab === 'admin' ? 'bg-brand-500 text-white' : 'text-slate-300 hover:bg-slate-800'}`}
            >
              Admin
            </button>
          </nav>

          <button
            onClick={triggerSOS}
            className="bg-red-600 hover:bg-red-700 text-white px-3 py-1.5 rounded-lg font-bold text-xs flex items-center space-x-1 shadow animate-pulse"
          >
            <ShieldAlert className="w-4 h-4" />
            <span>SOS</span>
          </button>
        </div>
      </header>

      {/* Toast Notification */}
      {toast && (
        <div className="fixed bottom-5 right-5 z-50 bg-slate-900 text-white px-4 py-3 rounded-xl shadow-2xl border border-slate-700 flex items-center space-x-3">
          <AlertTriangle className="w-5 h-5 text-brand-500" />
          <p className="text-sm">{toast}</p>
        </div>
      )}

      {/* SOS Banner */}
      {sosActive && (
        <div className="bg-red-600 text-white px-4 py-2 text-center text-sm font-bold flex justify-center items-center space-x-4">
          <span>🚨 CENTRO DE OPERACIONES BARBOYA: Alerta SOS activa. Un operador se comunicará de inmediato.</span>
          <button onClick={() => setSosActive(false)} className="underline text-xs bg-red-800 px-2 py-1 rounded">Resolver / Desactivar</button>
        </div>
      )}

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6">
        {activeTab === 'client' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Left Column: Service Selection & Forms */}
            <div className="lg:col-span-1 bg-white p-6 rounded-2xl shadow-sm border border-slate-200 space-y-6">
              <h2 className="text-xl font-bold text-slate-800">¿Qué necesitas hoy?</h2>
              
              <div className="grid grid-cols-3 gap-2">
                <button
                  onClick={() => setServiceMode('move')}
                  className={`p-3 rounded-xl border flex flex-col items-center justify-center space-y-1 transition ${serviceMode === 'move' ? 'border-brand-500 bg-brand-50 text-brand-700 font-bold' : 'border-slate-200 hover:bg-slate-50 text-slate-600'}`}
                >
                  <Car className="w-6 h-6" />
                  <span className="text-xs">Viajar</span>
                </button>
                <button
                  onClick={() => setServiceMode('food')}
                  className={`p-3 rounded-xl border flex flex-col items-center justify-center space-y-1 transition ${serviceMode === 'food' ? 'border-brand-500 bg-brand-50 text-brand-700 font-bold' : 'border-slate-200 hover:bg-slate-50 text-slate-600'}`}
                >
                  <Utensils className="w-6 h-6" />
                  <span className="text-xs">Comida</span>
                </button>
                <button
                  onClick={() => setServiceMode('package')}
                  className={`p-3 rounded-xl border flex flex-col items-center justify-center space-y-1 transition ${serviceMode === 'package' ? 'border-brand-500 bg-brand-50 text-brand-700 font-bold' : 'border-slate-200 hover:bg-slate-50 text-slate-600'}`}
                >
                  <Package className="w-6 h-6" />
                  <span className="text-xs">Envíos</span>
                </button>
              </div>

              {serviceMode === 'move' && (
                <div className="space-y-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Origen (Recogida)</label>
                    <div className="relative">
                      <MapPin className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                      <input type="text" defaultValue="Parque Principal de Barbosa" className="w-full pl-9 pr-3 py-2 border rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500" />
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Destino</label>
                    <div className="relative">
                      <MapPin className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                      <input type="text" placeholder="¿A dónde vas?" className="w-full pl-9 pr-3 py-2 border rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500" />
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Pon tu precio (COP)</label>
                    <div className="relative">
                      <DollarSign className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                      <input type="number" defaultValue={6000} className="w-full pl-9 pr-3 py-2 border rounded-xl text-sm font-bold text-brand-600 focus:outline-none focus:ring-2 focus:ring-brand-500" />
                    </div>
                  </div>
                  <button onClick={() => showToast("🚗 Viaje solicitado. Esperando ofertas de conductores en Barbosa...")} className="w-full bg-brand-500 hover:bg-brand-600 text-white font-bold py-3 rounded-xl shadow transition">
                    Solicitar Viaje
                  </button>
                </div>
              )}

              {serviceMode === 'food' && (
                <div className="space-y-4">
                  <p className="text-xs text-slate-500">Pide tus platos favoritos de los mejores restaurantes de Barbosa.</p>
                  <div className="space-y-2">
                    <div className="p-3 border rounded-xl hover:border-brand-500 cursor-pointer flex justify-between items-center">
                      <div>
                        <h4 className="font-bold text-sm">Asados Doña Rosa</h4>
                        <p className="text-xs text-slate-500">Carnes a la brasa · 20-30 min</p>
                      </div>
                      <span className="text-xs bg-emerald-100 text-emerald-800 font-semibold px-2 py-1 rounded">Abierto</span>
                    </div>
                    <div className="p-3 border rounded-xl hover:border-brand-500 cursor-pointer flex justify-between items-center">
                      <div>
                        <h4 className="font-bold text-sm">Pizzería El Parque</h4>
                        <p className="text-xs text-slate-500">Pizza artesanal · 15-25 min</p>
                      </div>
                      <span className="text-xs bg-emerald-100 text-emerald-800 font-semibold px-2 py-1 rounded">Abierto</span>
                    </div>
                  </div>
                  <button onClick={() => showToast("🍔 Pedido de comida creado con éxito.")} className="w-full bg-brand-500 hover:bg-brand-600 text-white font-bold py-3 rounded-xl shadow transition">
                    Ver Menú y Comprar
                  </button>
                </div>
              )}

              {serviceMode === 'package' && (
                <div className="space-y-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Descripción del paquete</label>
                    <input type="text" placeholder="Ej. Documentos notaría, caja pequeña" className="w-full px-3 py-2 border rounded-xl text-sm" />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Dirección de entrega</label>
                    <input type="text" placeholder="Barrio / Dirección" className="w-full px-3 py-2 border rounded-xl text-sm" />
                  </div>
                  <button onClick={() => showToast("📦 Mensajería solicitada. Un repartidor se dirigirá al punto.")} className="w-full bg-brand-500 hover:bg-brand-600 text-white font-bold py-3 rounded-xl shadow transition">
                    Solicitar Mensajería
                  </button>
                </div>
              )}
            </div>

            {/* Right Column: Live Map Simulation */}
            <div className="lg:col-span-2 bg-white p-6 rounded-2xl shadow-sm border border-slate-200 flex flex-col justify-between">
              <div className="flex justify-between items-center mb-4">
                <h3 className="font-bold text-slate-800">Mapa en Tiempo Real · Barbosa, Santander</h3>
                <span className="text-xs bg-emerald-100 text-emerald-800 font-semibold px-2.5 py-1 rounded-full flex items-center space-x-1">
                  <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
                  <span>PostGIS Activo</span>
                </span>
              </div>

              {/* Simulated Map View */}
              <div className="relative w-full h-96 bg-slate-200 rounded-xl overflow-hidden flex items-center justify-center border border-slate-300">
                <div className="absolute inset-0 opacity-20 bg-[radial-gradient(#cbd5e1_1px,transparent_1px)] [background-size:16px_16px]"></div>
                <div className="text-center z-10 space-y-2">
                  <div className="inline-block p-3 bg-white rounded-full shadow-lg">
                    <MapPin className="w-8 h-8 text-brand-500 animate-bounce" />
                  </div>
                  <p className="text-sm font-bold text-slate-700">Centro de Barbosa, Santander</p>
                  <p className="text-xs text-slate-500">Lat: 5.9333, Lng: -73.6208 (SRID: 4326)</p>
                </div>

                {/* Floating Driver marker simulation */}
                <div className="absolute bottom-10 left-12 bg-white px-3 py-1.5 rounded-xl shadow-md border flex items-center space-x-2 text-xs font-semibold text-slate-700">
                  <div className="w-3 h-3 bg-emerald-500 rounded-full"></div>
                  <span>Moto Conductor Cerca (1.2 km)</span>
                </div>
              </div>

              <div className="mt-4 pt-4 border-t flex justify-between items-center text-xs text-slate-500">
                <span>PIN de Seguridad: <strong className="text-slate-800 font-mono">4829</strong></span>
                <span className="text-brand-600 font-semibold">Garantía BarboYa Move & Delivery</span>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'driver' && (
          <div className="space-y-6">
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 flex flex-col sm:flex-row justify-between items-center">
              <div>
                <h2 className="text-xl font-bold text-slate-800">Panel del Conductor / Repartidor</h2>
                <p className="text-xs text-slate-500">Conectado en Barbosa, Santander · Bolsa Prepago Activa</p>
              </div>
              <div className="mt-4 sm:mt-0 flex items-center space-x-3">
                <span className="text-sm font-semibold text-emerald-600">En Línea</span>
                <button onClick={() => showToast("Estado de disponibilidad actualizado.")} className="bg-emerald-500 hover:bg-emerald-600 text-white font-bold px-4 py-2 rounded-xl text-sm shadow">
                  Desconectar
                </button>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                <h4 className="text-xs font-bold text-slate-400 uppercase">Saldo Bolsa Prepago</h4>
                <p className="text-2xl font-black text-slate-800 mt-1">$ 18.500 COP</p>
                <p className="text-xs text-emerald-600 mt-2">✓ Suficiente para aceptar carreras</p>
              </div>
              <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                <h4 className="text-xs font-bold text-slate-400 uppercase">Ganancias de Hoy</h4>
                <p className="text-2xl font-black text-slate-800 mt-1">$ 74.000 COP</p>
                <p className="text-xs text-slate-500 mt-2">6 servicios completados</p>
              </div>
              <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                <h4 className="text-xs font-bold text-slate-400 uppercase">Calificación</h4>
                <p className="text-2xl font-black text-slate-800 mt-1">4.95 ★</p>
                <p className="text-xs text-slate-500 mt-2">Basado en 142 calificaciones</p>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'merchant' && (
          <div className="space-y-6">
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 flex justify-between items-center">
              <div>
                <h2 className="text-xl font-bold text-slate-800">Panel de Comercio · Asados Doña Rosa</h2>
                <p className="text-xs text-slate-500">Barbosa, Santander · Comisión 10%</p>
              </div>
              <span className="bg-emerald-100 text-emerald-800 text-xs font-bold px-3 py-1.5 rounded-xl">Local Abierto</span>
            </div>

            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 space-y-4">
              <h3 className="font-bold text-slate-800">Pedidos Entrantes en Tiempo Real</h3>
              <div className="border rounded-xl p-4 flex justify-between items-center bg-brand-50 border-brand-200">
                <div>
                  <span className="text-xs font-bold text-brand-600 bg-brand-100 px-2 py-0.5 rounded">Nuevo Pedido #108</span>
                  <h4 className="font-bold text-slate-800 mt-1">1x Picada Criolla Familiar ($ 35.000)</h4>
                  <p className="text-xs text-slate-500">Entrega: Barrio La Floresta · Pago: Efectivo</p>
                </div>
                <div className="flex space-x-2">
                  <button onClick={() => showToast("Pedido aceptado. Preparación iniciada.")} className="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold px-3 py-2 rounded-xl">
                    Aceptar (25 min)
                  </button>
                  <button onClick={() => showToast("Pedido rechazado.")} className="bg-red-600 hover:bg-red-700 text-white text-xs font-bold px-3 py-2 rounded-xl">
                    Rechazar
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'admin' && (
          <div className="space-y-6">
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
              <h2 className="text-xl font-bold text-slate-800">Panel Administrativo Central</h2>
              <p className="text-xs text-slate-500">Supervisión de operaciones en Barbosa, Santander</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="bg-white p-5 rounded-2xl border">
                <span className="text-xs text-slate-400 uppercase font-bold">Usuarios Totales</span>
                <p className="text-2xl font-black mt-1">1,248</p>
              </div>
              <div className="bg-white p-5 rounded-2xl border">
                <span className="text-xs text-slate-400 uppercase font-bold">Conductores Activos</span>
                <p className="text-2xl font-black mt-1">34</p>
              </div>
              <div className="bg-white p-5 rounded-2xl border">
                <span className="text-xs text-slate-400 uppercase font-bold">Comercios Asociados</span>
                <p className="text-2xl font-black mt-1">18</p>
              </div>
              <div className="bg-white p-5 rounded-2xl border">
                <span className="text-xs text-slate-400 uppercase font-bold">Alertas SOS Abiertas</span>
                <p className="text-2xl font-black mt-1 text-red-600">0</p>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="bg-slate-900 text-slate-400 text-xs py-4 text-center border-t border-slate-800">
        <p>© 2026 BarboYa · Movilidad, Domicilios y Mensajería en Barbosa, Santander (Col). Todos los derechos reservados.</p>
      </footer>
    </div>
  )
}
