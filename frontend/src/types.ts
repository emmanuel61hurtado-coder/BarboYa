export type AppTab = 'client' | 'driver' | 'merchant' | 'admin'
export type ServiceMode = 'move' | 'food' | 'package' | 'tracking'
export type VehicleType = 'moto' | 'car'
export type TripState = 'idle' | 'searching' | 'offers_received' | 'trip_active' | 'completed'

export type UserRole =
  | 'customer'
  | 'driver'
  | 'courier'
  | 'merchant_owner'
  | 'merchant_employee'
  | 'admin'
  | 'superadmin'

export interface UserSession {
  token: string
  role: UserRole
  userId: string
  fullName: string
  phone: string
  avatarUrl?: string
  gender?: string
  driverInfo?: any
  merchantInfo?: any
}

export interface DriverOffer {
  id: string
  driverName: string
  vehicle: string
  plate: string
  rating: number
  tripsCount: number
  distanceKm: number
  etaMinutes: number
  proposedPriceCop: number
  photoUrl: string
  verified: boolean
  isFemale?: boolean
}

export interface MenuItem {
  id: string
  name: string
  description: string
  priceCop: number
  category: string
  imageUrl: string
  popular?: boolean
  prepTimeMinutes: number
}

export interface Restaurant {
  id: string
  name: string
  tagline: string
  category: string
  rating: number
  reviewCount: number
  deliveryTime: string
  deliveryFeeCop: number
  minOrderCop: number
  imageUrl: string
  isOpen: boolean
  featuredDish: string
  menu: MenuItem[]
}

export interface CartItem {
  item: MenuItem
  quantity: number
  notes?: string
}

export interface PackageOrder {
  pickupAddress: string
  dropoffAddress: string
  recipientName: string
  recipientPhone: string
  packageType: 'docs' | 'small' | 'medium' | 'fragile'
  estimatedCostCop: number
  securityPin: string
  status: 'draft' | 'dispatched' | 'in_transit' | 'delivered'
}

export type OrderTrackingStatus =
  | 'placed'
  | 'pending_confirmation'
  | 'accepted'
  | 'rejected'
  | 'preparing'
  | 'ready'
  | 'courier_search'
  | 'courier_assigned'
  | 'courier_heading_to_merchant'
  | 'picked_up'
  | 'in_transit'
  | 'delivered'
  | 'cancelled'

export interface OrderTrackingInfo {
  orderId: string
  status: OrderTrackingStatus
  statusHistory: { status: string; timestamp: string; actor?: string; reason?: string }[]
  storeName: string
  storePhone?: string
  storeAddress?: string
  items: { name: string; qty: number; unitPriceCop: number; lineTotalCop: number }[]
  subtotalCop: number
  deliveryFeeCop: number
  totalCop: number
  paymentMethod: string
  deliveryAddressNote?: string
  deliveryPin?: string
  prepMinutes?: number
  courier?: {
    name: string
    phone: string
    avatarUrl?: string
    vehicleType: string
    plate: string
    rating: number
  }
}
