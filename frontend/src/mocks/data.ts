import { Comercio, Pedido } from '../types';

export const MOCK_COMERCIOS: Comercio[] = [
  {
    id: 'c1',
    user_id: 'u2',
    nombre: 'Burger Master Gourmet',
    descripcion: 'Las mejores hamburguesas artesanales de la ciudad',
    logo_url: 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd',
    banner_url: 'https://images.unsplash.com/photo-1550547660-d9450f859349',
    direccion: 'Calle 100 # 15-20',
    lat: 4.671234,
    lng: -74.053123,
    abierto: true,
    calificacion_promedio: 4.8,
    created_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
    productos: [
      {
        id: 'p1',
        comercio_id: 'c1',
        nombre: 'Hamburguesa Doble Carne',
        descripcion: 'Carne angus, queso cheddar fundido y tocino',
        precio: 28000,
        imagen_url: 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd',
        disponible: true,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      },
      {
        id: 'p2',
        comercio_id: 'c1',
        nombre: 'Papas Rústicas',
        descripcion: 'Papas doradas con especias y salsa de la casa',
        precio: 10000,
        imagen_url: 'https://images.unsplash.com/photo-1573080496219-bb080dd4f877',
        disponible: true,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      }
    ]
  },
  {
    id: 'c2',
    user_id: 'u3',
    nombre: 'Pizza Napoli Express',
    descripcion: 'Auténtica pizza italiana al horno de leña',
    logo_url: 'https://images.unsplash.com/photo-1513104890138-7c749659a591',
    banner_url: 'https://images.unsplash.com/photo-1534308983496-4fabb1a015ee',
    direccion: 'Carrera 7 # 72-40',
    lat: 4.651234,
    lng: -74.063123,
    abierto: true,
    calificacion_promedio: 4.6,
    created_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
    productos: [
      {
        id: 'p3',
        comercio_id: 'c2',
        nombre: 'Pizza Margherita',
        descripcion: 'Salsa de tomate pomodoro, mozzarella fior di latte y albahaca fresca',
        precio: 32000,
        imagen_url: 'https://images.unsplash.com/photo-1513104890138-7c749659a591',
        disponible: true,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      }
    ]
  }
];

export const MOCK_PEDIDOS: Pedido[] = [
  {
    id: 'ped-1',
    cliente_id: 'u1',
    comercio_id: 'c1',
    direccion_id: 'dir-1',
    estado: 'PREPARANDO',
    subtotal: 28000,
    costo_envio: 5000,
    descuento: 0,
    total: 33000,
    metodo_pago: 'TARJETA',
    created_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
    detalles: [
      {
        id: 'det-1',
        producto_id: 'p1',
        nombre_producto: 'Hamburguesa Doble Carne',
        precio_unitario: 28000,
        cantidad: 1,
        subtotal: 28000
      }
    ]
  }
];
