import { apiClient } from './client'

const { http } = apiClient

export type VehicleType = 'BICYCLE' | 'MOTORCYCLE' | 'CAR'
export type DocumentType = 'IDENTITY' | 'FACE_PHOTO' | 'DRIVER_LICENSE' | 'SOAT' | 'TECHNICAL_REVIEW'
export type DocumentStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface CourierProfileInput {
  documentNumber: string
  vehicleType: VehicleType
  plate: string
  acceptsDataProcessing: boolean
}

export interface Requirement {
  type: DocumentType
  label: string
  required: boolean
  expires: boolean
}

export interface CourierDocument {
  id: string
  type: DocumentType
  status: DocumentStatus
  contentType: string
  expiresOn: string | null
  expired: boolean
  reviewNote: string | null
  uploadedAt: string
}

export interface CourierView {
  profile: { documentNumber: string; vehicleType: VehicleType; plate: string | null; consentAt: string } | null
  requirements: Requirement[]
  documents: CourierDocument[]
  pending: string[]
}

const file = async (url: string) => (await http.get<Blob>(url, { responseType: 'blob' })).data

export const courierApi = {
  view: async () => (await http.get<CourierView>('/courier/profile')).data,
  saveProfile: async (data: CourierProfileInput) => (await http.put<CourierView>('/courier/profile', data)).data,
  upload: async (type: DocumentType, document: File, expiresOn?: string) => {
    const form = new FormData()
    form.append('file', document)
    return (
      await http.post<CourierView>(`/courier/documents/${type}`, form, {
        params: { expiresOn: expiresOn || undefined },
        // Una foto puede tardar en subir con mala señal
        timeout: 60_000,
      })
    ).data
  },
  remove: async (id: string) => (await http.delete<CourierView>(`/courier/documents/${id}`)).data,
  ownFile: (id: string) => file(`/courier/documents/${id}/file`),

  review: async (userId: string) => (await http.get<CourierView>(`/admin/users/${userId}/courier`)).data,
  reviewFile: (id: string) => file(`/admin/documents/${id}/file`),
  decide: async (id: string, decision: 'APPROVED' | 'REJECTED', note?: string) =>
    (await http.post<CourierView>(`/admin/documents/${id}/review`, { decision, note })).data,
}
