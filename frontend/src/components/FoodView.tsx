import React, { useState } from 'react'
import { 
  Utensils, Clock, Star, Plus, Minus, ShoppingBag, 
  Check, ArrowRight, Sparkles, X, ChevronRight, MapPin
} from 'lucide-react'
import { Restaurant, MenuItem, CartItem } from '../types'
import { sound } from '../utils/sound'

interface FoodViewProps {
  onShowToast: (msg: string) => void
}

const SAMPLE_RESTAURANTS: Restaurant[] = [
  {
    id: 'rest-1',
    name: 'Asados Doña Rosa Barbosa',
    tagline: 'Carnes a la brasa al estilo santandereano, arepas de maíz pelado y picadas criollas',
    category: 'Carnes & Brasa',
    rating: 4.94,
    reviewCount: 312,
    deliveryTime: '20-30 min',
    deliveryFeeCop: 3000,
    minOrderCop: 15000,
    imageUrl: 'https://images.unsplash.com/photo-1544025162-d76694265947?w=600&auto=format&fit=crop&q=80',
    isOpen: true,
    featuredDish: 'Picada Criolla Familiar',
    menu: [
      {
        id: 'm1',
        name: 'Picada Criolla Familiar',
        description: 'Carne oreada, costilla de cerdo ahumada, papa criolla, plátano maduro y arepa de maíz pelado.',
        priceCop: 38000,
        category: 'Platos Fuertes',
        imageUrl: 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=400&auto=format&fit=crop&q=80',
        popular: true,
        prepTimeMinutes: 25,
      },
      {
        id: 'm2',
        name: 'Carne Oreada Tradicional (300g)',
        description: 'Corte magro curado al sol y a la leña, acompañada de yuca al vapor y ají casero.',
        priceCop: 26000,
        category: 'Platos Fuertes',
        imageUrl: 'https://images.unsplash.com/photo-1544025162-d76694265947?w=400&auto=format&fit=crop&q=80',
        popular: true,
        prepTimeMinutes: 20,
      },
      {
        id: 'm3',
        name: 'Chorizo Santandereano Artesanal',
        description: 'Porción de 3 chorizos campesinos con arepa de choclo caliente.',
        priceCop: 14000,
        category: 'Entradas',
        imageUrl: 'https://images.unsplash.com/photo-1587314168485-3236d6710814?w=400&auto=format&fit=crop&q=80',
        prepTimeMinutes: 15,
      }
    ]
  },
  {
    id: 'rest-2',
    name: 'Pizzería El Parque Emilio',
    tagline: 'Masa madre horneada en piedra, ingredientes frescos y quesos madurados',
    category: 'Pizza Artesanal',
    rating: 4.88,
    reviewCount: 198,
    deliveryTime: '15-25 min',
    deliveryFeeCop: 2500,
    minOrderCop: 18000,
    imageUrl: 'https://images.unsplash.com/photo-1513104890138-7c749659a591?w=600&auto=format&fit=crop&q=80',
    isOpen: true,
    featuredDish: 'Pizza Barbosa Especial',
    menu: [
      {
        id: 'p1',
        name: 'Pizza Barbosa Especial (Grande 8 porciones)',
        description: 'Mozzarella de búfala, jamón serrano, tomates secos marinados y reducción balsámica.',
        priceCop: 34000,
        category: 'Pizzas Grandes',
        imageUrl: 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=400&auto=format&fit=crop&q=80',
        popular: true,
        prepTimeMinutes: 20,
      },
      {
        id: 'p2',
        name: 'Pizza Pepperoni Clásica',
        description: 'Doble porción de pepperoni crujiente, orégano campesino y salsa pomodoro.',
        priceCop: 29000,
        category: 'Pizzas Grandes',
        imageUrl: 'https://images.unsplash.com/photo-1628840042765-356cda07504e?w=400&auto=format&fit=crop&q=80',
        prepTimeMinutes: 18,
      }
    ]
  },
  {
    id: 'rest-3',
    name: 'Café & Dulces de la Provincia',
    tagline: 'Café de origen de la hoya del Río Suárez, bocadillo veleño con queso y postres locales',
    category: 'Café & Postres',
    rating: 4.96,
    reviewCount: 145,
    deliveryTime: '10-20 min',
    deliveryFeeCop: 2000,
    minOrderCop: 10000,
    imageUrl: 'https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=600&auto=format&fit=crop&q=80',
    isOpen: true,
    featuredDish: 'Copa Dulce Veleña',
    menu: [
      {
        id: 'c1',
        name: 'Combo Degustación Dulce Barbosa',
        description: 'Bocadillo veleño en lonja con queso campesino asado y taza de café especial.',
        priceCop: 12000,
        category: 'Cafetería',
        imageUrl: 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=400&auto=format&fit=crop&q=80',
        popular: true,
        prepTimeMinutes: 10,
      }
    ]
  }
]

export const FoodView: React.FC<FoodViewProps> = ({ onShowToast }) => {
  const [selectedRest, setSelectedRest] = useState<Restaurant>(SAMPLE_RESTAURANTS[0])
  const [activeCategory, setActiveCategory] = useState<string>('all')
  const [cart, setCart] = useState<CartItem[]>([])
  const [showCartDrawer, setShowCartDrawer] = useState<boolean>(false)
  const [orderPlaced, setOrderPlaced] = useState<boolean>(false)

  const categories = ['all', 'Carnes & Brasa', 'Pizza Artesanal', 'Café & Postres']

  const handleAddToCart = (item: MenuItem) => {
    sound.tick()
    setCart((prev) => {
      const idx = prev.findIndex((ci) => ci.item.id === item.id)
      if (idx >= 0) {
        const copy = [...prev]
        copy[idx].quantity += 1
        return copy
      }
      return [...prev, { item, quantity: 1 }]
    })
    onShowToast(`🛒 "${item.name}" agregado a tu pedido.`)
  }

  const handleUpdateQty = (itemId: string, delta: number) => {
    sound.tick()
    setCart((prev) => {
      return prev
        .map((ci) => {
          if (ci.item.id === itemId) {
            const nextQty = ci.quantity + delta
            return nextQty > 0 ? { ...ci, quantity: nextQty } : null
          }
          return ci
        })
        .filter(Boolean) as CartItem[]
    })
  }

  const subtotalCop = cart.reduce((acc, ci) => acc + ci.item.priceCop * ci.quantity, 0)
  const totalCop = subtotalCop > 0 ? subtotalCop + selectedRest.deliveryFeeCop : 0

  const handleCheckout = () => {
    sound.success()
    setOrderPlaced(true)
    onShowToast("🚀 Pedido confirmado. Notificando a la cocina del restaurante...")
  }

  return (
    <div className="space-y-6">
      {/* Editorial Header */}
      <div className="glass-surface p-5 rounded-2xl border border-slate-800 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <h3 className="font-display font-bold text-xl text-white">BarboYa Food</h3>
            <span className="bg-brand-500/10 text-brand-400 border border-brand-500/30 text-[10px] font-mono px-2 py-0.5 rounded-full uppercase">
              GASTRO NEXUS
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Comercios gastronómicos de Barbosa con despacho rápido y seguimiento en vivo.
          </p>
        </div>

        {/* View Cart Button */}
        <button
          onClick={() => {
            setShowCartDrawer(true)
            sound.tick()
          }}
          className="relative px-4 py-2 rounded-xl bg-nexus-900 border border-slate-700 hover:border-brand-500 text-xs font-bold text-white flex items-center space-x-2 transition"
        >
          <ShoppingBag className="w-4 h-4 text-brand-400" />
          <span>Mi Pedido ({cart.reduce((a, b) => a + b.quantity, 0)})</span>
          {cart.length > 0 && (
            <span className="font-mono text-brand-400 font-bold ml-1">
              $ {subtotalCop.toLocaleString('es-CO')}
            </span>
          )}
        </button>
      </div>

      {/* Category Pills */}
      <div className="flex items-center space-x-2 overflow-x-auto pb-1 scrollbar-none">
        {categories.map((cat) => (
          <button
            key={cat}
            onClick={() => {
              setActiveCategory(cat)
              sound.tick()
            }}
            className={`px-3.5 py-1.5 rounded-xl text-xs font-medium whitespace-nowrap transition-all ${
              activeCategory === cat
                ? 'bg-brand-500 text-white font-bold shadow-brand-glow'
                : 'bg-nexus-900/80 text-slate-400 border border-slate-800 hover:text-white'
            }`}
          >
            {cat === 'all' ? 'Todos los Comercios' : cat}
          </button>
        ))}
      </div>

      {/* Restaurant Carousel / Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {SAMPLE_RESTAURANTS.filter((r) => activeCategory === 'all' || r.category === activeCategory).map((rest) => (
          <div
            key={rest.id}
            onClick={() => {
              setSelectedRest(rest)
              sound.tick()
            }}
            className={`rounded-2xl overflow-hidden glass-surface border cursor-pointer transition-all duration-300 group ${
              selectedRest.id === rest.id
                ? 'border-brand-500 shadow-brand-glow ring-1 ring-brand-500/50'
                : 'border-slate-800/80 hover:border-slate-700'
            }`}
          >
            {/* Restaurant Cover Photo */}
            <div className="relative h-36 w-full overflow-hidden">
              <img
                src={rest.imageUrl}
                alt={rest.name}
                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-nexus-950 via-transparent to-transparent" />
              
              <div className="absolute top-2.5 right-2.5 bg-nexus-950/80 backdrop-blur-md px-2 py-0.5 rounded-lg border border-slate-700 flex items-center space-x-1 text-[11px] font-mono text-neon-emerald font-bold">
                <Star className="w-3 h-3 fill-neon-emerald" />
                <span>{rest.rating}</span>
              </div>

              <div className="absolute bottom-2.5 left-2.5 text-[10px] font-mono bg-nexus-950/80 backdrop-blur-md px-2 py-0.5 rounded text-slate-300 flex items-center space-x-1 border border-slate-800">
                <Clock className="w-3 h-3 text-cyan-400" />
                <span>{rest.deliveryTime}</span>
              </div>
            </div>

            {/* Restaurant Details */}
            <div className="p-4 space-y-1.5">
              <h4 className="font-bold text-sm text-white group-hover:text-brand-400 transition-colors">
                {rest.name}
              </h4>
              <p className="text-[11px] text-slate-400 line-clamp-2 leading-relaxed">
                {rest.tagline}
              </p>
              <div className="flex items-center justify-between pt-2 border-t border-slate-800/80 text-[11px] font-mono text-slate-400">
                <span>Envío: <strong className="text-white">${rest.deliveryFeeCop.toLocaleString('es-CO')}</strong></span>
                <span className="text-brand-400 font-semibold">{rest.menu.length} platos disponibles</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Selected Restaurant Menu Showcase */}
      <div className="glass-surface p-5 rounded-2xl border border-slate-800 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div>
            <h4 className="font-display font-bold text-lg text-white flex items-center space-x-2">
              <span>Menú de {selectedRest.name}</span>
            </h4>
            <p className="text-xs text-slate-400">{selectedRest.tagline}</p>
          </div>
          <span className="text-xs bg-neon-emerald/10 text-neon-emerald border border-neon-emerald/30 px-2.5 py-1 rounded-full font-mono">
            COCINA ACTIVA
          </span>
        </div>

        {/* Dishes Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
          {selectedRest.menu.map((dish) => (
            <div
              key={dish.id}
              className="p-3.5 rounded-xl bg-nexus-950/70 border border-slate-800/90 hover:border-slate-700 flex gap-3 transition"
            >
              <img
                src={dish.imageUrl}
                alt={dish.name}
                className="w-20 h-20 rounded-xl object-cover shrink-0 ring-1 ring-slate-800"
              />
              <div className="flex-1 flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between">
                    <h5 className="font-bold text-sm text-white">{dish.name}</h5>
                    {dish.popular && (
                      <span className="text-[9px] bg-brand-500/20 text-brand-400 border border-brand-500/30 px-1.5 py-0.2 rounded font-mono">
                        TOP
                      </span>
                    )}
                  </div>
                  <p className="text-[11px] text-slate-400 line-clamp-2 mt-0.5 leading-relaxed">
                    {dish.description}
                  </p>
                </div>

                <div className="flex items-center justify-between mt-2 pt-1 border-t border-slate-900">
                  <div className="font-display font-bold text-sm text-cyan-glow">
                    $ {dish.priceCop.toLocaleString('es-CO')}
                  </div>

                  <button
                    onClick={() => handleAddToCart(dish)}
                    className="px-2.5 py-1 rounded-lg bg-brand-500 hover:bg-brand-600 text-white text-xs font-bold flex items-center space-x-1 shadow-brand-glow transition active:scale-95"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Agregar</span>
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Cart Drawer / Modal */}
      {showCartDrawer && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex justify-end animate-fadeIn">
          <div className="w-full max-w-md bg-nexus-900 border-l border-slate-800 h-full flex flex-col justify-between p-5 shadow-2xl">
            {/* Header */}
            <div>
              <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                <div className="flex items-center space-x-2">
                  <ShoppingBag className="w-5 h-5 text-brand-500" />
                  <h4 className="font-display font-bold text-lg text-white">Tu Pedido Actual</h4>
                </div>
                <button
                  onClick={() => setShowCartDrawer(false)}
                  className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Items List */}
              <div className="mt-4 space-y-3 max-h-[60vh] overflow-y-auto pr-1">
                {cart.length === 0 ? (
                  <div className="text-center py-12 text-slate-400 space-y-2">
                    <p className="text-sm">No has agregado platos a tu pedido aún.</p>
                    <p className="text-xs text-slate-500">Explora los restaurantes de Barbosa y añade tus favoritos.</p>
                  </div>
                ) : (
                  cart.map((ci) => (
                    <div
                      key={ci.item.id}
                      className="p-3 rounded-xl bg-nexus-950/80 border border-slate-800 flex items-center justify-between"
                    >
                      <div className="flex-1 pr-2">
                        <div className="font-bold text-xs text-white">{ci.item.name}</div>
                        <div className="text-[11px] font-mono text-cyan-300">
                          $ {(ci.item.priceCop * ci.quantity).toLocaleString('es-CO')}
                        </div>
                      </div>

                      <div className="flex items-center space-x-2 bg-nexus-900 px-2 py-1 rounded-lg border border-slate-700">
                        <button
                          onClick={() => handleUpdateQty(ci.item.id, -1)}
                          className="text-slate-400 hover:text-white"
                        >
                          <Minus className="w-3 h-3" />
                        </button>
                        <span className="font-mono text-xs font-bold text-white px-1">
                          {ci.quantity}
                        </span>
                        <button
                          onClick={() => handleUpdateQty(ci.item.id, 1)}
                          className="text-slate-400 hover:text-white"
                        >
                          <Plus className="w-3 h-3" />
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Cart Footer & Checkout */}
            {cart.length > 0 && (
              <div className="border-t border-slate-800 pt-4 space-y-3">
                <div className="space-y-1.5 text-xs text-slate-400 font-mono">
                  <div className="flex justify-between">
                    <span>Subtotal alimentos:</span>
                    <span className="text-white">$ {subtotalCop.toLocaleString('es-CO')}</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Tarifa de despacho local:</span>
                    <span className="text-white">$ {selectedRest.deliveryFeeCop.toLocaleString('es-CO')}</span>
                  </div>
                  <div className="flex justify-between text-sm font-bold text-white pt-2 border-t border-slate-800">
                    <span>TOTAL A PAGAR:</span>
                    <span className="text-neon-emerald font-display text-base">
                      $ {totalCop.toLocaleString('es-CO')} COP
                    </span>
                  </div>
                </div>

                <button
                  onClick={handleCheckout}
                  className="w-full py-3 px-4 rounded-xl font-display font-bold text-sm bg-gradient-to-r from-brand-500 to-orange-500 text-white shadow-brand-glow hover:from-brand-600 hover:to-orange-600 transition flex items-center justify-center space-x-2"
                >
                  <span>Confirmar Pedido y Despacho</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
