import { apiClient } from './client'
import type { Role } from './types'

const { http } = apiClient

export type MovementKind = 'COMMISSION' | 'PAYMENT' | 'ADJUSTMENT' | 'REVERSAL'
export type PaymentChannel = 'BANK_TRANSFER' | 'NEQUI' | 'CASH'

export interface Movement {
  id: string
  kind: MovementKind
  /** Positivo aumenta la deuda; negativo la reduce. */
  effectCop: number
  orderId: string | null
  method: PaymentChannel | null
  reference: string | null
  note: string | null
  reversed: boolean
  at: string
}

export interface Statement {
  balanceCop: number
  debtLimitCop: number
  blocked: boolean
  commissionBps: number
  movements: Movement[]
  page: number
  size: number
  total: number
}

export interface Period {
  orders: number
  salesCop: number
  commissionsCop: number
}

export interface Summary {
  today: Period
  last7Days: Period
  last30Days: Period
  outstandingCop: number
  collectedCop: number
  waivedCop: number
  debtors: { partyId: string; name: string; role: Role; balanceCop: number; blocked: boolean }[]
}

export interface Payment {
  amountCop: number
  method: PaymentChannel
  reference: string
  note: string
}

const account = (id: string) => `/admin/finance/accounts/${id}`

export const financeApi = {
  mine: async (page: number) => (await http.get<Statement>('/finance/statement', { params: { page } })).data,
  summary: async () => (await http.get<Summary>('/admin/finance/summary')).data,
  account: async (id: string, page: number) =>
    (await http.get<Statement>(account(id), { params: { page } })).data,
  registerPayment: async (id: string, payment: Payment) =>
    (await http.post<Statement>(`${account(id)}/payments`, payment)).data,
  waive: async (id: string, amountCop: number, reason: string) =>
    (await http.post<Statement>(`${account(id)}/adjustments`, { amountCop, reason })).data,
  reverse: async (entryId: string, reason: string) =>
    (await http.post<Statement>(`/admin/finance/entries/${entryId}/reversal`, { reason })).data,
}
