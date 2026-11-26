import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { connectOrderEvents } from './realtime'

class FakeSocket {
  sent: string[] = []
  closed = false
  onopen: (() => void) | null = null
  onmessage: ((event: { data: string }) => void) | null = null
  onclose: ((event: { code: number }) => void) | null = null
  send(data: string) {
    this.sent.push(data)
  }
  close() {
    this.closed = true
  }
}

function setup() {
  const sockets: FakeSocket[] = []
  const onChange = vi.fn()
  const renew = vi.fn(async () => undefined)
  const stop = connectOrderEvents({
    url: 'ws://prueba',
    getToken: () => 'abc',
    renew,
    onChange,
    createSocket: () => {
      const socket = new FakeSocket()
      sockets.push(socket)
      return socket as unknown as WebSocket
    },
  })
  return { sockets, onChange, renew, stop }
}

describe('avisos en vivo', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => vi.useRealTimers())

  it('se identifica con el primer mensaje y avisa de los cambios', () => {
    const { sockets, onChange } = setup()

    sockets[0].onopen!()
    sockets[0].onmessage!({ data: '{"type":"ready"}' })
    sockets[0].onmessage!({ data: '{"type":"order.updated","orderId":"1"}' })

    expect(JSON.parse(sockets[0].sent[0])).toEqual({ type: 'auth', token: 'abc' })
    expect(onChange).toHaveBeenCalledTimes(2)
  })

  it('reconecta esperando cada vez más', async () => {
    const { sockets, renew } = setup()

    sockets[0].onclose!({ code: 1006 })
    await vi.advanceTimersByTimeAsync(1000)
    expect(sockets).toHaveLength(2)

    sockets[1].onclose!({ code: 1006 })
    await vi.advanceTimersByTimeAsync(1999)
    expect(sockets).toHaveLength(2)
    await vi.advanceTimersByTimeAsync(1)
    expect(sockets).toHaveLength(3)
    expect(renew).not.toHaveBeenCalled()
  })

  it('renueva la sesión si el servidor rechaza el token', async () => {
    const { sockets, renew } = setup()

    sockets[0].onclose!({ code: 4401 })
    await vi.advanceTimersByTimeAsync(1000)

    expect(renew).toHaveBeenCalledTimes(1)
    expect(sockets).toHaveLength(2)
  })

  it('al detenerse no vuelve a conectar', async () => {
    const { sockets, stop } = setup()

    stop()
    sockets[0].onclose!({ code: 1000 })
    await vi.advanceTimersByTimeAsync(60_000)

    expect(sockets[0].closed).toBe(true)
    expect(sockets).toHaveLength(1)
  })
})
