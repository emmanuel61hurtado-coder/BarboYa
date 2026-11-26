import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../services/api/client'
import { connectOrderEvents } from '../services/realtime'

/** Mantiene al día las listas de pedidos mientras haya sesión. */
export function useOrderEvents(enabled: boolean) {
  const queryClient = useQueryClient()

  useEffect(() => {
    if (!enabled) return
    const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws'
    return connectOrderEvents({
      url: `${scheme}://${window.location.host}/ws/v1/events`,
      getToken: apiClient.getAccessToken,
      renew: apiClient.refresh,
      onChange: () => {
        void queryClient.invalidateQueries({ queryKey: ['orders'] })
        // Cada pedido reserva o devuelve existencias
        void queryClient.invalidateQueries({ queryKey: ['shop', 'products'] })
      },
    })
  }, [enabled, queryClient])
}
