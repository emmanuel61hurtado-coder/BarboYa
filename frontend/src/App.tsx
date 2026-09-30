import React, { useState, useEffect } from 'react';
import { mockApi } from './api/client';
import { Comercio, Pedido } from './types';

export function App() {
  const [comercios, setComercios] = useState<Comercio[]>([]);
  const [selectedComercio, setSelectedComercio] = useState<Comercio | null>(null);
  const [cart, setCart] = useState<{ producto: any; cantidad: number }[]>([]);
  const [pedidos, setPedidos] = useState<Pedido[]>([]);
  const [view, setView] = useState<'home' | 'comercio' | 'pedidos'>('home');

  useEffect(() => {
    mockApi.getComercios().then(setComercios);
    mockApi.getPedidos().then(setPedidos);
  }, []);

  const addToCart = (producto: any) => {
    setCart(prev => {
      const existing = prev.find(i => i.producto.id === producto.id);
      if (existing) {
        return prev.map(i => i.producto.id === producto.id ? { ...i, cantidad: i.cantidad + 1 } : i);
      }
      return [...prev, { producto, cantidad: 1 }];
    });
  };

  const totalCart = cart.reduce((acc, item) => acc + (item.producto.precio * item.cantidad), 0);

  const checkout = async () => {
    if (!selectedComercio) return;
    await mockApi.createPedido({
      cliente_id: 'u1',
      comercio_id: selectedComercio.id,
      direccion_id: 'dir-1',
      subtotal: totalCart,
      costo_envio: 5000,
      total: totalCart + 5000,
      metodo_pago: 'TARJETA',
      detalles: cart.map(i => ({ producto_id: i.producto.id, cantidad: i.cantidad, nombre_producto: i.producto.nombre, precio_unitario: i.producto.precio, subtotal: i.producto.precio * i.cantidad }))
    });
    const updated = await mockApi.getPedidos();
    setPedidos(updated);
    setCart([]);
    setView('pedidos');
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* Navbar */}
      <header className="bg-white shadow-sm sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-2 cursor-pointer" onClick={() => setView('home')}>
            <span className="text-2xl font-bold text-[#FF6B00]">BarboYa</span>
            <span className="text-xs bg-orange-100 text-[#FF6B00] px-2 py-0.5 rounded-full font-semibold">Delivery</span>
          </div>
          <nav className="flex space-x-6">
            <button onClick={() => setView('home')} className={`font-medium ${view === 'home' ? 'text-[#FF6B00]' : 'text-gray-600'}`}>Inicio</button>
            <button onClick={() => setView('pedidos')} className={`font-medium ${view === 'pedidos' ? 'text-[#FF6B00]' : 'text-gray-600'}`}>Mis Pedidos ({pedidos.length})</button>
          </nav>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-7xl mx-auto px-4 py-8 w-full">
        {view === 'home' && (
          <div>
            <div className="mb-8 text-center">
              <h1 className="text-4xl font-bold text-gray-900 mb-2">¿Qué se te antoja hoy?</h1>
              <p className="text-gray-600">Comida rápida, restaurantes y más en la puerta de tu casa.</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {comercios.map(comercio => (
                <div 
                  key={comercio.id} 
                  onClick={() => { setSelectedComercio(comercio); setView('comercio'); }}
                  className="bg-white rounded-2xl shadow-sm hover:shadow-md transition cursor-pointer overflow-hidden border border-gray-100"
                >
                  <img src={comercio.banner_url} alt={comercio.nombre} className="h-48 w-full object-cover" />
                  <div className="p-5">
                    <div className="flex justify-between items-start mb-2">
                      <h3 className="text-lg font-bold text-gray-900">{comercio.nombre}</h3>
                      <span className="bg-green-100 text-green-700 text-xs px-2.5 py-1 rounded-full font-bold">★ {comercio.calificacion_promedio}</span>
                    </div>
                    <p className="text-gray-600 text-sm mb-4">{comercio.descripcion}</p>
                    <div className="flex items-center text-xs text-gray-500 justify-between">
                      <span>📍 {comercio.direccion}</span>
                      <span className="text-[#FF6B00] font-semibold">Ver menú →</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {view === 'comercio' && selectedComercio && (
          <div>
            <button onClick={() => setView('home')} className="mb-4 text-sm font-semibold text-[#FF6B00] hover:underline">← Volver a comercios</button>
            <div className="bg-white rounded-2xl p-6 shadow-sm mb-8 flex items-center space-x-6">
              <img src={selectedComercio.logo_url} alt={selectedComercio.nombre} className="w-24 h-24 rounded-full object-cover shadow" />
              <div>
                <h1 className="text-3xl font-bold text-gray-900">{selectedComercio.nombre}</h1>
                <p className="text-gray-600">{selectedComercio.descripcion}</p>
                <p className="text-sm text-gray-500 mt-1">📍 {selectedComercio.direccion} • ★ {selectedComercio.calificacion_promedio}</p>
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              <div className="lg:col-span-2">
                <h2 className="text-2xl font-bold mb-4">Menú</h2>
                <div className="space-y-4">
                  {selectedComercio.productos.map(producto => (
                    <div key={producto.id} className="bg-white p-4 rounded-xl shadow-sm flex items-center justify-between border border-gray-100">
                      <div className="flex space-x-4 items-center">
                        <img src={producto.imagen_url} alt={producto.nombre} className="w-20 h-20 rounded-lg object-cover" />
                        <div>
                          <h4 className="font-bold text-gray-900">{producto.nombre}</h4>
                          <p className="text-sm text-gray-600 mb-2">{producto.descripcion}</p>
                          <span className="font-semibold text-[#FF6B00]">${producto.precio.toLocaleString()}</span>
                        </div>
                      </div>
                      <button 
                        onClick={() => addToCart(producto)}
                        className="bg-[#FF6B00] text-white px-4 py-2 rounded-xl font-semibold shadow-sm hover:bg-orange-600 transition"
                      >
                        Agregar
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              {/* Cart Drawer Sidebar */}
              <div className="bg-white p-6 rounded-2xl shadow-sm h-fit border border-gray-100">
                <h3 className="text-xl font-bold mb-4">Tu Pedido</h3>
                {cart.length === 0 ? (
                  <p className="text-gray-500 text-sm">Tu carrito está vacío.</p>
                ) : (
                  <div>
                    <div className="space-y-3 mb-6">
                      {cart.map((item, idx) => (
                        <div key={idx} className="flex justify-between text-sm">
                          <span>{item.cantidad}x {item.producto.nombre}</span>
                          <span className="font-semibold">${(item.producto.precio * item.cantidad).toLocaleString()}</span>
                        </div>
                      ))}
                    </div>
                    <div className="border-t pt-4 space-y-2 mb-6">
                      <div className="flex justify-between text-sm text-gray-600">
                        <span>Subtotal</span>
                        <span>${totalCart.toLocaleString()}</span>
                      </div>
                      <div className="flex justify-between text-sm text-gray-600">
                        <span>Domicilio</span>
                        <span>$5,000</span>
                      </div>
                      <div className="flex justify-between font-bold text-lg text-gray-900 border-t pt-2">
                        <span>Total</span>
                        <span>${(totalCart + 5000).toLocaleString()}</span>
                      </div>
                    </div>
                    <button 
                      onClick={checkout}
                      className="w-full bg-[#FF6B00] text-white py-3 rounded-xl font-bold shadow-sm hover:bg-orange-600 transition"
                    >
                      Confirmar Pedido
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {view === 'pedidos' && (
          <div>
            <h1 className="text-3xl font-bold mb-6">Mis Pedidos</h1>
            <div className="space-y-4">
              {pedidos.map(pedido => (
                <div key={pedido.id} className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col md:flex-row justify-between items-start md:items-center">
                  <div>
                    <div className="flex items-center space-x-3 mb-2">
                      <span className="font-bold text-lg">Pedido #{pedido.id}</span>
                      <span className="bg-orange-100 text-[#FF6B00] text-xs px-3 py-1 rounded-full font-bold">{pedido.estado}</span>
                    </div>
                    <p className="text-sm text-gray-500">Método de pago: {pedido.metodo_pago} • Total: ${pedido.total.toLocaleString()}</p>
                  </div>
                  <div className="mt-4 md:mt-0">
                    <button className="bg-gray-100 text-gray-700 px-4 py-2 rounded-xl text-sm font-semibold hover:bg-gray-200">Ver Seguimiento</button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
export default App;
