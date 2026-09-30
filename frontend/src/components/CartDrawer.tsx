import React from 'react';

interface CartItem {
  producto: any;
  cantidad: number;
}

interface CartDrawerProps {
  cart: CartItem[];
  totalCart: number;
  onCheckout: () => void;
}

export const CartDrawer: React.FC<CartDrawerProps> = ({ cart, totalCart, onCheckout }) => {
  return (
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
            onClick={onCheckout}
            className="w-full bg-[#FF6B00] text-white py-3 rounded-xl font-bold shadow-sm hover:bg-orange-600 transition"
          >
            Confirmar Pedido
          </button>
        </div>
      )}
    </div>
  );
};
