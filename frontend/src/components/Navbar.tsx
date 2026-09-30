import React from 'react';

interface NavbarProps {
  view: 'home' | 'comercio' | 'pedidos';
  setView: (view: 'home' | 'comercio' | 'pedidos') => void;
  pedidosCount: number;
}

export const Navbar: React.FC<NavbarProps> = ({ view, setView, pedidosCount }) => {
  return (
    <header className="bg-white shadow-sm sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
        <div className="flex items-center space-x-2 cursor-pointer" onClick={() => setView('home')}>
          <span className="text-2xl font-bold text-[#FF6B00]">BarboYa</span>
          <span className="text-xs bg-orange-100 text-[#FF6B00] px-2 py-0.5 rounded-full font-semibold">Delivery</span>
        </div>
        <nav className="flex space-x-6">
          <button 
            onClick={() => setView('home')} 
            className={`font-medium transition-colors ${view === 'home' ? 'text-[#FF6B00]' : 'text-gray-600 hover:text-gray-900'}`}
          >
            Inicio
          </button>
          <button 
            onClick={() => setView('pedidos')} 
            className={`font-medium transition-colors ${view === 'pedidos' ? 'text-[#FF6B00]' : 'text-gray-600 hover:text-gray-900'}`}
          >
            Mis Pedidos ({pedidosCount})
          </button>
        </nav>
      </div>
    </header>
  );
};
