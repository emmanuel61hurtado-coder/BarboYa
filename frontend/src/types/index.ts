export type UserRole = 'CLIENTE' | 'REPARTIDOR' | 'COMERCIO' | 'ADMIN';
export type UserStatus = 'PENDIENTE' | 'ACTIVO' | 'BLOQUEADO';

export interface User {
  id: string;
  email: string;
  nombre: string;
  telefono: string;
  rol: UserRole;
  estado: UserStatus;
  created_at: string;
  updated_at: string;
}

export interface Producto {
  id: string;
  comercio_id: string;
  categoria_id?: string;
  nombre: string;
  descripcion?: string;
  precio: number;
  imagen_url?: string;
  disponible: boolean;
  created_at: string;
  updated_at: string;
}

export interface Comercio {
  id: string;
  user_id: string;
  nombre: string;
  descripcion?: string;
  logo_url?: string;
  banner_url?: string;
  direccion: string;
  lat: number;
  lng: number;
  abierto: boolean;
  calificacion_promedio: number;
  created_at: string;
  updated_at: string;
  productos: Producto[];
}

export interface Direccion {
  id: string;
  user_id: string;
  nombre: string;
  direccion: string;
  detalles?: string;
  lat: number;
  lng: number;
}

export type EstadoPedido = 'CREADO' | 'ACEPTADO' | 'PREPARANDO' | 'LISTO' | 'EN_CAMINO' | 'ENTREGADO' | 'CANCELADO';

export interface DetallePedido {
  id: string;
  producto_id: string;
  nombre_producto: string;
  precio_unitario: number;
  cantidad: number;
  subtotal: number;
}

export interface Pedido {
  id: string;
  cliente_id: string;
  comercio_id: string;
  repartidor_id?: string;
  direccion_id: string;
  estado: EstadoPedido;
  subtotal: number;
  costo_envio: number;
  descuento: number;
  total: number;
  metodo_pago: string;
  cupon_codigo?: string;
  created_at: string;
  updated_at: string;
  detalles: DetallePedido[];
}
