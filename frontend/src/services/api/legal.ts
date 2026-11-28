import { apiClient } from './client'

const { http } = apiClient

export type DocumentSlug = 'terminos' | 'privacidad'

export interface LegalInfo {
  termsVersion: string
  privacyVersion: string
  /** Falso mientras falten datos del responsable o la aprobación de un abogado. */
  reviewed: boolean
  pharmacy: boolean
  driverRegistration: boolean
}

export const legalApi = {
  info: async () => (await http.get<LegalInfo>('/legal')).data,
  document: async (slug: DocumentSlug) =>
    (await http.get<{ version: string; reviewed: boolean; body: string }>(`/legal/documents/${slug}`)).data,
  pending: async () => (await http.get<string[]>('/legal/pending')).data,
  accept: async (termsVersion: string, privacyVersion: string) =>
    void (await http.post('/legal/acceptances', { termsVersion, privacyVersion })),
}
