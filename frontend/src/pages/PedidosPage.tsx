import React from 'react';
import { Pedido } from '../types';

interface PedidosPageProps {
  pedidos: Pedido[];
}

export const PedidosPage: React.FC<PedidosPageProps> = ({ pedidos }) => {
  return (
    <div>
      <h1 className="text-3xl font-bold mb-6">Mis Pedidos</h1>
      <div className="space-y-4">
        {pedidos.map(pedido => (
          <div 
            key={pedido.id} 
            className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col md:flex-row justify-between items-start md:items-center"
          >
            <div>
              <div className="flex items-center space-x-3 mb-2">
                <span className="font-bold text-lg">Pedido #{pedido.id}</span>
                <span className="bg-orange-100 text-[#FF6B00] text-xs px-3 py-1 rounded-full font-bold">
                  {pedido.estado}
                </span>
              </div>
              <p className="text-sm text-gray-500">
                Método de pago: {pedido.metodo_pago} • Total: ${pedido.total.toLocaleString()}
              </p>
            </div>
            <div className="mt-4 md:mt-0">
              <button className="bg-gray-100 text-gray-700 px-4 py-2 rounded-xl text-sm font-semibold hover:bg-gray-200">
                Ver Seguimiento
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
