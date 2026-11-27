import { apiClient } from './client'

const { http } = apiClient

export interface AddressInput {
  label: string
  address: string
  notes: string
  latitude: number | null
  longitude: number | null
}

export type SavedAddress = Omit<AddressInput, 'notes'> & { id: string; notes: string | null }

export const addressApi = {
  list: async () => (await http.get<SavedAddress[]>('/addresses')).data,
  save: async (data: AddressInput, id?: string) =>
    (id ? await http.put<SavedAddress>(`/addresses/${id}`, data) : await http.post<SavedAddress>('/addresses', data))
      .data,
  remove: async (id: string) => void (await http.delete(`/addresses/${id}`)),
}
