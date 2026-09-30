import React from 'react';
import { Comercio } from '../types';
import { ComercioCard } from '../components/ComercioCard';

interface HomePageProps {
  comercios: Comercio[];
  onSelectComercio: (comercio: Comercio) => void;
}

export const HomePage: React.FC<HomePageProps> = ({ comercios, onSelectComercio }) => {
  return (
    <div>
      <div className="mb-8 text-center">
        <h1 className="text-4xl font-bold text-gray-900 mb-2">¿Qué se te antoja hoy?</h1>
        <p className="text-gray-600">Comida rápida, restaurantes y más en la puerta de tu casa.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {comercios.map(comercio => (
          <ComercioCard 
            key={comercio.id} 
            comercio={comercio} 
            onSelect={onSelectComercio} 
          />
        ))}
      </div>
    </div>
  );
};
