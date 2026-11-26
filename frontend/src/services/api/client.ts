import axios, { AxiosAdapter, AxiosError, InternalAxiosRequestConfig } from 'axios'
import type { TokenResponse } from './types'

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly fieldErrors: Record<string, string> = {},
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

interface Problem {
  code?: string
  detail?: string
  errors?: Record<string, string>
}

type RetriableConfig = InternalAxiosRequestConfig & { retried?: boolean }

// Un 401 aquí significa credenciales o cookie inválidas: renovar no ayuda
const NO_RENEWAL = new Set([
  '/auth/login',
  '/auth/register',
  '/auth/refresh',
  '/auth/logout',
  '/auth/password/forgot',
  '/auth/password/reset',
])

function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error
  const response = (error as AxiosError<Problem>).response
  if (!response) {
    return new ApiError(0, 'NETWORK_ERROR', 'No pudimos conectar con el servidor. Revisa tu conexión.')
  }
  const problem: Problem = typeof response.data === 'object' && response.data ? response.data : {}
  return new ApiError(
    response.status,
    problem.code ?? 'UNEXPECTED_ERROR',
    problem.detail ?? 'Ocurrió un error inesperado. Intenta de nuevo.',
    problem.errors,
  )
}

/** Una sola renovación a la vez entre pestañas: cada token de refresco sirve una vez. */
function acrossTabs<T>(task: () => Promise<T>): Promise<T> {
  if (typeof navigator === 'undefined' || !navigator.locks) return task()
  return navigator.locks.request('barboya-refresh', task) as Promise<T>
}

export function createApiClient(adapter?: AxiosAdapter) {
  const http = axios.create({ baseURL: '/api/v1', timeout: 15_000, ...(adapter ? { adapter } : {}) })

  // El token de acceso vive solo en memoria; la sesión se recupera con la cookie HttpOnly
  let accessToken: string | null = null
  let renewal: Promise<TokenResponse> | null = null
  const renewedListeners = new Set<(session: TokenResponse) => void>()
  const expiredListeners = new Set<() => void>()

  function refresh(): Promise<TokenResponse> {
    renewal ??= acrossTabs(async () => {
      const { data } = await http.post<TokenResponse>('/auth/refresh')
      accessToken = data.accessToken
      renewedListeners.forEach((listener) => listener(data))
      return data
    }).finally(() => {
      renewal = null
    })
    return renewal
  }

  http.interceptors.request.use((config) => {
    if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
    return config
  })

  http.interceptors.response.use(undefined, async (error: unknown) => {
    const { config, response } = error as AxiosError
    const request = config as RetriableConfig | undefined
    if (response?.status !== 401 || !request || request.retried || NO_RENEWAL.has(request.url ?? '')) {
      throw toApiError(error)
    }
    request.retried = true
    // Si otra petición ya renovó el token mientras esta viajaba, basta con repetirla
    const alreadyRenewed = accessToken !== null && request.headers.Authorization !== `Bearer ${accessToken}`
    if (!alreadyRenewed) {
      try {
        await refresh()
      } catch (renewalError) {
        // Solo un rechazo del servidor cierra la sesión; un corte de red no
        if (renewalError instanceof ApiError && renewalError.status === 401) {
          accessToken = null
          expiredListeners.forEach((listener) => listener())
        }
        throw renewalError
      }
    }
    return http(request)
  })

  return {
    http,
    refresh,
    setAccessToken(token: string | null) {
      accessToken = token
    },
    onSessionRenewed(listener: (session: TokenResponse) => void) {
      renewedListeners.add(listener)
      return () => void renewedListeners.delete(listener)
    },
    onSessionExpired(listener: () => void) {
      expiredListeners.add(listener)
      return () => void expiredListeners.delete(listener)
    },
  }
}

export const apiClient = createApiClient()
