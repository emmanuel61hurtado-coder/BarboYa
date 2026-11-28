import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../services/api/client'
import { connectOrderEvents } from '../services/realtime'

/** Mantiene al día pedidos y casos de soporte mientras haya sesión. */
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
        // Pedidos disponibles y entrega en curso del domiciliario
        void queryClient.invalidateQueries({ queryKey: ['work'] })
        void queryClient.invalidateQueries({ queryKey: ['support'] })
      },
    })
  }, [enabled, queryClient])
}
