import React, { useState, useEffect } from 'react';
import { mockApi } from './api/client';
import { Comercio, Pedido } from './types';
import { Navbar } from './components/Navbar';
import { HomePage } from './pages/HomePage';
import { ComercioPage } from './pages/ComercioPage';
import { PedidosPage } from './pages/PedidosPage';

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
      detalles: cart.map(i => ({
        producto_id: i.producto.id,
        cantidad: i.cantidad,
        nombre_producto: i.producto.nombre,
        precio_unitario: i.producto.precio,
        subtotal: i.producto.precio * i.cantidad
      }))
    });
    const updated = await mockApi.getPedidos();
    setPedidos(updated);
    setCart([]);
    setView('pedidos');
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Navbar view={view} setView={setView} pedidosCount={pedidos.length} />

      <main className="flex-1 max-w-7xl mx-auto px-4 py-8 w-full">
        {view === 'home' && (
          <HomePage 
            comercios={comercios} 
            onSelectComercio={(comercio) => {
              setSelectedComercio(comercio);
              setView('comercio');
            }} 
          />
        )}

        {view === 'comercio' && selectedComercio && (
          <ComercioPage 
            comercio={selectedComercio}
            onBack={() => setView('home')}
            onAddToCart={addToCart}
            cart={cart}
            totalCart={totalCart}
            onCheckout={checkout}
          />
        )}

        {view === 'pedidos' && (
          <PedidosPage pedidos={pedidos} />
        )}
      </main>
    </div>
  );
}

export default App;
