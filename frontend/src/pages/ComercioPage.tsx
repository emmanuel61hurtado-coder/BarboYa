import React from 'react';
import { Comercio } from '../types';
import { CartDrawer } from '../components/CartDrawer';

interface ComercioPageProps {
  comercio: Comercio;
  onBack: () => void;
  onAddToCart: (producto: any) => void;
  cart: { producto: any; cantidad: number }[];
  totalCart: number;
  onCheckout: () => void;
}

export const ComercioPage: React.FC<ComercioPageProps> = ({
  comercio,
  onBack,
  onAddToCart,
  cart,
  totalCart,
  onCheckout,
}) => {
  return (
    <div>
      <button 
        onClick={onBack} 
        className="mb-4 text-sm font-semibold text-[#FF6B00] hover:underline"
      >
        ← Volver a comercios
      </button>

      <div className="bg-white rounded-2xl p-6 shadow-sm mb-8 flex items-center space-x-6">
        <img 
          src={comercio.logo_url} 
          alt={comercio.nombre} 
          className="w-24 h-24 rounded-full object-cover shadow" 
        />
        <div>
          <h1 className="text-3xl font-bold text-gray-900">{comercio.nombre}</h1>
          <p className="text-gray-600">{comercio.descripcion}</p>
          <p className="text-sm text-gray-500 mt-1">📍 {comercio.direccion} • ★ {comercio.calificacion_promedio}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2">
          <h2 className="text-2xl font-bold mb-4">Menú</h2>
          <div className="space-y-4">
            {comercio.productos.map(producto => (
              <div 
                key={producto.id} 
                className="bg-white p-4 rounded-xl shadow-sm flex items-center justify-between border border-gray-100"
              >
                <div className="flex space-x-4 items-center">
                  <img 
                    src={producto.imagen_url} 
                    alt={producto.nombre} 
                    className="w-20 h-20 rounded-lg object-cover" 
                  />
                  <div>
                    <h4 className="font-bold text-gray-900">{producto.nombre}</h4>
                    <p className="text-sm text-gray-600 mb-2">{producto.descripcion}</p>
                    <span className="font-semibold text-[#FF6B00]">
                      ${producto.precio.toLocaleString()}
                    </span>
                  </div>
                </div>
                <button 
                  onClick={() => onAddToCart(producto)}
                  className="bg-[#FF6B00] text-white px-4 py-2 rounded-xl font-semibold shadow-sm hover:bg-orange-600 transition"
                >
                  Agregar
                </button>
              </div>
            ))}
          </div>
        </div>

        <CartDrawer 
          cart={cart} 
          totalCart={totalCart} 
          onCheckout={onCheckout} 
        />
      </div>
    </div>
  );
};
