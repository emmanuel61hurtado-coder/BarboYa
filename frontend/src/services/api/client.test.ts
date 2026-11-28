import { describe, expect, it, vi } from 'vitest'
import { AxiosAdapter, AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ApiError, createApiClient } from './client'

type Reply = { status: number; data?: unknown } | 'offline'

/** Servidor simulado: solo para comprobar cómo reacciona el cliente. */
function server(handler: (config: InternalAxiosRequestConfig) => Reply) {
  const calls: string[] = []
  const adapter: AxiosAdapter = async (config) => {
    calls.push(config.url!)
    await Promise.resolve()
    const reply = handler(config)
    if (reply === 'offline') throw new AxiosError('Network Error', 'ERR_NETWORK', config)
    const response = { ...reply, statusText: '', headers: {}, config } as AxiosResponse
    if (reply.status >= 400) throw new AxiosError('fallo', 'ERR_BAD_REQUEST', config, null, response)
    return response
  }
  return { client: createApiClient(adapter), calls }
}

const session = { accessToken: 'nuevo', user: { id: '1' } }

/** Rechaza el token viejo y acepta el renovado. */
const expiredToken = (refresh: Reply) => (config: InternalAxiosRequestConfig): Reply => {
  if (config.url === '/auth/refresh') return refresh
  return config.headers.Authorization === 'Bearer nuevo' ? { status: 200, data: 'ok' } : { status: 401 }
}

describe('cliente de la API', () => {
  it('envía el token de acceso', async () => {
    const seen = vi.fn()
    const { client } = server((config) => {
      seen(config.headers.Authorization)
      return { status: 200 }
    })
    client.setAccessToken('abc')

    await client.http.get('/users/me')

    expect(seen).toHaveBeenCalledWith('Bearer abc')
  })

  it('renueva una sola vez aunque fallen varias peticiones a la vez', async () => {
    const { client, calls } = server(expiredToken({ status: 200, data: session }))
    client.setAccessToken('viejo')

    const replies = await Promise.all([client.http.get('/a'), client.http.get('/b'), client.http.get('/c')])

    expect(replies.map((r) => r.data)).toEqual(['ok', 'ok', 'ok'])
    expect(calls.filter((url) => url === '/auth/refresh')).toHaveLength(1)
  })

  it('avisa que la sesión terminó cuando el servidor rechaza la renovación', async () => {
    const { client } = server(expiredToken({ status: 401 }))
    const expired = vi.fn()
    client.onSessionExpired(expired)
    client.setAccessToken('viejo')

    await expect(client.http.get('/a')).rejects.toMatchObject({ status: 401 })

    expect(expired).toHaveBeenCalledTimes(1)
  })

  it('no cierra la sesión por un corte de red', async () => {
    const { client } = server(expiredToken('offline'))
    const expired = vi.fn()
    client.onSessionExpired(expired)
    client.setAccessToken('viejo')

    await expect(client.http.get('/a')).rejects.toMatchObject({ code: 'NETWORK_ERROR' })

    expect(expired).not.toHaveBeenCalled()
  })

  it('no intenta renovar cuando las credenciales son incorrectas', async () => {
    const { client, calls } = server(() => ({ status: 401, data: { code: 'INVALID_CREDENTIALS', detail: 'No' } }))

    await expect(client.http.post('/auth/login')).rejects.toMatchObject({ code: 'INVALID_CREDENTIALS' })

    expect(calls).toEqual(['/auth/login'])
  })

  it('no repite sin fin una petición que sigue rechazada', async () => {
    const { client, calls } = server((config) =>
      config.url === '/auth/refresh' ? { status: 200, data: session } : { status: 401 },
    )

    await expect(client.http.get('/a')).rejects.toBeInstanceOf(ApiError)

    expect(calls).toEqual(['/a', '/auth/refresh', '/a'])
  })

  it('conserva los errores por campo del servidor', async () => {
    const { client } = server(() => ({
      status: 400,
      data: { code: 'VALIDATION_ERROR', detail: 'Revisa', errors: { email: 'Correo no válido' } },
    }))

    await expect(client.http.post('/auth/register')).rejects.toMatchObject({
      status: 400,
      message: 'Revisa',
      fieldErrors: { email: 'Correo no válido' },
    })
  })
})
