export type AppTab = 'client' | 'driver' | 'merchant' | 'admin'
export type ServiceMode = 'move' | 'food' | 'package'
export type VehicleType = 'moto' | 'car'
export type TripState = 'idle' | 'searching' | 'offers_received' | 'trip_active' | 'completed'

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
