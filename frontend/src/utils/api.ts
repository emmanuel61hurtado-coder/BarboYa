/**
 * BarboYa API Client
 * Centralized HTTP service for connecting Frontend with FastAPI backend
 * Handles JWT tokens, authentication state, and full platform endpoints
 */

const API_BASE_URL = 'http://localhost:8000'

export interface AuthSession {
  token: string
  role: 'customer' | 'driver' | 'courier' | 'merchant_owner' | 'merchant_employee' | 'admin' | 'superadmin'
  userId: string
  fullName: string
  phone: string
  avatarUrl?: string
  gender?: string
  driverInfo?: any
  merchantInfo?: any
}

class ApiService {
  private token: string | null = null

  constructor() {
    this.token = localStorage.getItem('barboya_jwt_token')
  }

  setToken(token: string | null) {
    this.token = token
    if (token) {
      localStorage.setItem('barboya_jwt_token', token)
    } else {
      localStorage.removeItem('barboya_jwt_token')
      localStorage.removeItem('barboya_user_session')
    }
  }

  getToken(): string | null {
    if (!this.token) {
      this.token = localStorage.getItem('barboya_jwt_token')
    }
    return this.token
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const token = this.getToken()
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...(options.headers as Record<string, string> || {}),
    }

    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    try {
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers,
      })

      if (!response.ok) {
        let errorData: any
        try {
          errorData = await response.json()
        } catch {
          errorData = { detail: `Error HTTP ${response.status}: ${response.statusText}` }
        }
        throw new Error(errorData.detail || errorData.message || 'Error en la petición')
      }

      return (await response.json()) as T
    } catch (err: any) {
      console.warn(`[BarboYa API] ${endpoint}:`, err.message)
      throw err
    }
  }

  // ──────────────────────── AUTH ────────────────────────
  async register(data: {
    fullName: string
    phone: string
    password: string
    role: 'customer' | 'driver' | 'merchant_owner'
    vehicleType?: 'moto' | 'car'
    vehiclePlate?: string
    documentId?: string
  }): Promise<{ access_token: string; role: string; user_id: string }> {
    const payload: any = {
      full_name: data.fullName,
      phone: data.phone,
      password: data.password,
      role: data.role,
    }
    if (data.role === 'driver') {
      payload.vehicle_type = data.vehicleType
      payload.vehicle_plate = data.vehiclePlate
      payload.document_id = data.documentId
    }

    const res = await this.request<{ access_token: string; role: string; user_id: string }>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    this.setToken(res.access_token)
    return res
  }

  async login(phone: string, password: string): Promise<{ access_token: string; role: string; user_id: string }> {
    const res = await this.request<{ access_token: string; role: string; user_id: string }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ phone, password }),
    })
    this.setToken(res.access_token)
    return res
  }

  async getMe(): Promise<any> {
    return this.request<any>('/auth/me')
  }

  async recoverPassword(phone: string, newPassword: string): Promise<any> {
    return this.request('/auth/recover-password', {
      method: 'POST',
      body: JSON.stringify({ phone, new_password: newPassword, security_pin: '1234' }),
    })
  }

  logout() {
    this.setToken(null)
  }

  // ──────────────────────── VIAJES (TRANSPORTE) ────────────────────────
  async requestTrip(data: {
    serviceType: 'passenger' | 'delivery'
    pickup: { lat: number; lng: number }
    pickupNote?: string
    dropoff: { lat: number; lng: number }
    dropoffNote?: string
    offeredFareCop: number
    paymentMethod?: string
    preferFemaleDriver?: boolean
  }): Promise<any> {
    return this.request('/trips', {
      method: 'POST',
      body: JSON.stringify({
        service_type: data.serviceType,
        pickup: data.pickup,
        pickup_note: data.pickupNote,
        dropoff: data.dropoff,
        dropoff_note: data.dropoffNote,
        offered_fare_cop: data.offeredFareCop,
        payment_method: data.paymentMethod || 'cash',
        prefer_female_driver: !!data.preferFemaleDriver,
      }),
    })
  }

  // ──────────────────────── SEGUIMIENTO DE DOMICILIOS ────────────────────────
  async createOrder(data: {
    merchantId: string
    items: { productId: string; qty: number }[]
    paymentMethod?: string
    deliveryAddressNote?: string
  }): Promise<any> {
    return this.request('/orders', {
      method: 'POST',
      body: JSON.stringify({
        merchant_id: data.merchantId,
        items: data.items.map(i => ({ product_id: i.productId, qty: i.qty })),
        payment_method: data.paymentMethod || 'cash',
        delivery_address_note: data.deliveryAddressNote,
      }),
    })
  }

  async getOrderTracking(orderId: string): Promise<any> {
    return this.request(`/orders/${orderId}/tracking`)
  }

  async verifyDeliveryPin(orderId: string, pin: string): Promise<any> {
    return this.request(`/orders/${orderId}/verify-delivery`, {
      method: 'POST',
      body: JSON.stringify({ pin }),
    })
  }

  async getMyCustomerOrders(): Promise<any[]> {
    return this.request('/orders/my-orders')
  }

  // ──────────────────────── CONDUCTOR ────────────────────────
  async getDriverProfile(): Promise<any> {
    return this.request('/drivers/me')
  }

  async updateDriverProfile(data: any): Promise<any> {
    return this.request('/drivers/me', {
      method: 'PUT',
      body: JSON.stringify(data),
    })
  }

  async submitDriverDocuments(data: {
    licenseDocUrl: string
    soatDocUrl: string
    soatExpiresAt?: string
    techDocUrl?: string
    techExpiresAt?: string
    selfieReferenceUrl: string
  }): Promise<any> {
    return this.request('/drivers/me/documents', {
      method: 'POST',
      body: JSON.stringify({
        license_doc_url: data.licenseDocUrl,
        soat_doc_url: data.soatDocUrl,
        soat_expires_at: data.soatExpiresAt,
        tech_doc_url: data.techDocUrl,
        tech_expires_at: data.techExpiresAt,
        selfie_reference_url: data.selfieReferenceUrl,
      }),
    })
  }

  async getDriverHistory(): Promise<any[]> {
    return this.request('/drivers/me/history')
  }

  async updateDriverLocation(lat: number, lng: number, isOnline: boolean): Promise<any> {
    return this.request('/drivers/location', {
      method: 'POST',
      body: JSON.stringify({ lat, lng, is_online: isOnline }),
    })
  }

  // ──────────────────────── COMERCIO ────────────────────────
  async registerMerchant(data: {
    name: string
    category: string
    description?: string
    addressNote: string
    location: { lat: number; lng: number }
    phone: string
    whatsapp?: string
    openingHours?: string
    logoUrl?: string
    coverImageUrl?: string
  }): Promise<any> {
    return this.request('/merchants/register', {
      method: 'POST',
      body: JSON.stringify({
        name: data.name,
        category: data.category,
        description: data.description,
        address_note: data.addressNote,
        location: data.location,
        phone: data.phone,
        whatsapp: data.whatsapp,
        opening_hours: data.openingHours,
        logo_url: data.logoUrl,
        cover_image_url: data.coverImageUrl,
      }),
    })
  }

  async getMyStoreProfile(): Promise<any> {
    return this.request('/merchants/my-store/profile')
  }

  async updateMyStoreProfile(data: any): Promise<any> {
    return this.request('/merchants/my-store/profile', {
      method: 'PUT',
      body: JSON.stringify(data),
    })
  }

  async getMyStoreProducts(): Promise<any[]> {
    return this.request('/merchants/my-store/products')
  }

  async addStoreProduct(data: {
    name: string
    description?: string
    priceCop: number
    imageUrl?: string
    isAvailable?: boolean
    stock?: number
  }): Promise<any> {
    return this.request('/merchants/my-store/products', {
      method: 'POST',
      body: JSON.stringify({
        name: data.name,
        description: data.description,
        price_cop: data.priceCop,
        image_url: data.imageUrl,
        is_available: data.isAvailable ?? true,
        stock: data.stock,
      }),
    })
  }

  async updateStoreProduct(productId: string, data: any): Promise<any> {
    return this.request(`/merchants/my-store/products/${productId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    })
  }

  async deleteStoreProduct(productId: string): Promise<any> {
    return this.request(`/merchants/my-store/products/${productId}`, {
      method: 'DELETE',
    })
  }

  async getMyStoreOrders(): Promise<any[]> {
    return this.request('/merchants/my-store/orders')
  }

  async acceptStoreOrder(orderId: string, prepMinutes: number): Promise<any> {
    return this.request(`/merchants/orders/${orderId}/accept`, {
      method: 'POST',
      body: JSON.stringify({ prep_minutes: prepMinutes }),
    })
  }

  async rejectStoreOrder(orderId: string, reason: string): Promise<any> {
    return this.request(`/merchants/orders/${orderId}/reject`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    })
  }

  async markStoreOrderReady(orderId: string): Promise<any> {
    return this.request(`/merchants/orders/${orderId}/ready`, {
      method: 'POST',
    })
  }

  async requestNearbyCourier(orderId: string): Promise<any> {
    return this.request(`/merchants/orders/${orderId}/request-courier`, {
      method: 'POST',
    })
  }

  // ──────────────────────── ADMINISTRACIÓN ────────────────────────
  async getAdminStats(): Promise<any> {
    return this.request('/admin/dashboard/stats')
  }

  async getAdminUsers(): Promise<any[]> {
    return this.request('/admin/users')
  }

  async setUserStatus(userId: string, isActive: boolean): Promise<any> {
    return this.request(`/admin/users/${userId}/status`, {
      method: 'POST',
      body: JSON.stringify({ is_active: isActive }),
    })
  }

  async getDriversForVerification(): Promise<any[]> {
    return this.request('/admin/drivers/verification')
  }

  async verifyDriver(driverId: string, action: 'approve' | 'reject' | 'suspend', reason?: string, isFemaleDriver = false): Promise<any> {
    return this.request(`/admin/drivers/${driverId}/verify`, {
      method: 'POST',
      body: JSON.stringify({ action, reason, is_female_driver: isFemaleDriver }),
    })
  }

  async getMerchantsForApproval(): Promise<any[]> {
    return this.request('/admin/merchants/approval')
  }

  async approveMerchant(merchantId: string, action: 'approve' | 'reject' | 'suspend', reason?: string): Promise<any> {
    return this.request(`/admin/merchants/${merchantId}/approval`, {
      method: 'POST',
      body: JSON.stringify({ action, reason }),
    })
  }

  async getAuditLogs(): Promise<any[]> {
    return this.request('/admin/audit-logs')
  }
}

export const api = new ApiService()
