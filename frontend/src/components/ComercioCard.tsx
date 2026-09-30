import React from 'react';
import { Comercio } from '../types';

interface ComercioCardProps {
  comercio: Comercio;
  onSelect: (comercio: Comercio) => void;
}

export const ComercioCard: React.FC<ComercioCardProps> = ({ comercio, onSelect }) => {
  return (
    <div 
      onClick={() => onSelect(comercio)}
      className="bg-white rounded-2xl shadow-sm hover:shadow-md transition cursor-pointer overflow-hidden border border-gray-100"
    >
      <img src={comercio.banner_url} alt={comercio.nombre} className="h-48 w-full object-cover" />
      <div className="p-5">
        <div className="flex justify-between items-start mb-2">
          <h3 className="text-lg font-bold text-gray-900">{comercio.nombre}</h3>
          <span className="bg-green-100 text-green-700 text-xs px-2.5 py-1 rounded-full font-bold">
            ★ {comercio.calificacion_promedio}
          </span>
        </div>
        <p className="text-gray-600 text-sm mb-4">{comercio.descripcion}</p>
        <div className="flex items-center text-xs text-gray-500 justify-between">
          <span>📍 {comercio.direccion}</span>
          <span className="text-[#FF6B00] font-semibold">Ver menú →</span>
        </div>
      </div>
    </div>
  );
};
