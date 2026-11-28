import { useEffect, useRef, useState } from 'react'
import { deliveryApi } from '../../services/api/delivery'
import { Point, toPoint } from '../../utils/geo'

const MIN_INTERVAL_MS = 10_000
const HEARTBEAT_MS = 30_000

/**
 * Envía la posición mientras la pantalla está abierta. El navegador no la
 * comparte en segundo plano: sin envíos durante 2 minutos deja de contar.
 */
export function useLocationSharing(active: boolean, online: boolean) {
  const [position, setPosition] = useState<Point | null>(null)
  const [problem, setProblem] = useState<string | null>(null)
  const latest = useRef<Point | null>(null)
  const sentAt = useRef(0)

  useEffect(() => {
    if (!active) return
    if (!navigator.geolocation) {
      setProblem('Este dispositivo no permite compartir la ubicación.')
      return
    }
    const send = (force = false) => {
      if (!latest.current || (!force && Date.now() - sentAt.current < MIN_INTERVAL_MS)) return
      sentAt.current = Date.now()
      deliveryApi.setPresence(online, latest.current).then(
        () => setProblem(null),
        (error: Error) => setProblem(`No se pudo enviar tu ubicación: ${error.message}`),
      )
    }
    const watch = navigator.geolocation.watchPosition(
      (value) => {
        latest.current = toPoint(value)
        setPosition(latest.current)
        send()
      },
      () => setProblem('Activa el permiso de ubicación para trabajar.'),
      { enableHighAccuracy: true, maximumAge: 5_000 },
    )
    const heartbeat = setInterval(() => send(true), HEARTBEAT_MS)
    return () => {
      navigator.geolocation.clearWatch(watch)
      clearInterval(heartbeat)
    }
  }, [active, online])

  return { position, problem }
}
