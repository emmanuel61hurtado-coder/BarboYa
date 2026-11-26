/** Aviso en vivo de cambios en pedidos. Los datos se piden después por la API. */

interface Options {
  url: string
  getToken: () => string | null
  /** Renueva la sesión cuando el servidor rechaza el token. */
  renew: () => Promise<unknown>
  /** Algo pudo cambiar: al recibir un aviso y también al reconectar. */
  onChange: () => void
  createSocket?: (url: string) => WebSocket
}

const UNAUTHORIZED = 4401
const MAX_DELAY_MS = 30_000

export function connectOrderEvents({ url, getToken, renew, onChange, createSocket }: Options) {
  let socket: WebSocket | null = null
  let timer: ReturnType<typeof setTimeout> | undefined
  let failures = 0
  let stopped = false

  const open = () => {
    const current = createSocket ? createSocket(url) : new WebSocket(url)
    socket = current
    // El token va en el primer mensaje, no en la URL
    current.onopen = () => current.send(JSON.stringify({ type: 'auth', token: getToken() }))
    current.onmessage = (event) => {
      const type = (JSON.parse(String(event.data)) as { type?: string }).type
      if (type === 'ready') failures = 0
      if (type === 'ready' || type === 'order.updated' || type === 'offers.updated') onChange()
    }
    current.onclose = (event) => {
      if (stopped) return
      const delay = Math.min(MAX_DELAY_MS, 1000 * 2 ** failures++)
      timer = setTimeout(async () => {
        if (event.code === UNAUTHORIZED) await renew().catch(() => undefined)
        if (!stopped) open()
      }, delay)
    }
  }

  open()
  return () => {
    stopped = true
    clearTimeout(timer)
    socket?.close()
  }
}
