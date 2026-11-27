import { apiClient } from './client'
import type { Page } from './types'

const { http } = apiClient

export type RatingTarget = 'BUSINESS' | 'COURIER'

export interface RatingSummary {
  /** Nulo cuando todavía no hay calificaciones. */
  average: number | null
  count: number
}

export interface OrderRatings {
  openUntil: string | null
  targets: { target: RatingTarget; summary: RatingSummary; mine: { stars: number; comment: string | null } | null }[]
}

export type Reviews = Page<{ stars: number; comment: string | null; at: string }> & { summary: RatingSummary }

export interface ModeratedRating {
  id: string
  orderId: string
  authorName: string
  target: RatingTarget
  targetName: string
  stars: number
  comment: string | null
  hidden: boolean
  hiddenReason: string | null
  at: string
}

export const ratingApi = {
  forOrder: async (orderId: string) => (await http.get<OrderRatings>(`/orders/${orderId}/ratings`)).data,
  rate: async (orderId: string, data: { target: RatingTarget; stars: number; comment: string }) =>
    (await http.post<OrderRatings>(`/orders/${orderId}/ratings`, data)).data,
  summaries: async (ids: string[]) =>
    (await http.get<Record<string, RatingSummary>>('/ratings/businesses', { params: { ids: ids.join(',') } })).data,
  reviews: async (businessId: string, page: number) =>
    (await http.get<Reviews>(`/ratings/businesses/${businessId}`, { params: { page } })).data,
  mine: async (page: number) => (await http.get<Reviews>('/ratings/me', { params: { page } })).data,
  moderation: async (hidden: boolean, page: number) =>
    (await http.get<Page<ModeratedRating>>('/admin/ratings', { params: { hidden, page } })).data,
  moderate: async (id: string, action: 'hide' | 'restore', reason: string) =>
    (await http.post<ModeratedRating>(`/admin/ratings/${id}/${action}`, { reason })).data,
}
