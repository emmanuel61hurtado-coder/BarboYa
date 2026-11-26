import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { authApi, Registration } from '../services/api/auth'
import { ApiError, apiClient } from '../services/api/client'
import type { User } from '../services/api/types'

type State =
  | { status: 'loading' }
  | { status: 'unreachable' }
  | { status: 'anonymous' }
  | { status: 'authenticated'; user: User }

interface AuthContextValue {
  state: State
  retry: () => void
  signIn: (identifier: string, password: string) => Promise<void>
  signUp: (data: Registration) => Promise<void>
  signOut: () => Promise<void>
  reloadUser: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<State>({ status: 'loading' })
  const queryClient = useQueryClient()
  const currentUserId = useRef<string | null>(null)

  const show = useCallback(
    (user: User | null) => {
      // Al cambiar de cuenta no puede quedar nada en caché de la anterior
      if (currentUserId.current !== (user?.id ?? null)) queryClient.clear()
      currentUserId.current = user?.id ?? null
      setState(user ? { status: 'authenticated', user } : { status: 'anonymous' })
    },
    [queryClient],
  )

  const restore = useCallback(async () => {
    setState({ status: 'loading' })
    try {
      show(await authApi.restore())
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) show(null)
      else setState({ status: 'unreachable' })
    }
  }, [show])

  useEffect(() => {
    const stopRenewed = apiClient.onSessionRenewed((session) => show(session.user))
    const stopExpired = apiClient.onSessionExpired(() => show(null))
    void restore()
    return () => {
      stopRenewed()
      stopExpired()
    }
  }, [restore, show])

  const value = useMemo<AuthContextValue>(
    () => ({
      state,
      retry: () => void restore(),
      signIn: async (identifier, password) => show(await authApi.login(identifier, password)),
      signUp: async (data) => show(await authApi.register(data)),
      signOut: async () => {
        await authApi.logout()
        show(null)
      },
      reloadUser: async () => show(await authApi.me()),
    }),
    [state, restore, show],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth requiere AuthProvider')
  return context
}

/** Para las pantallas que solo se muestran con sesión iniciada. */
export function useCurrentUser(): User {
  const { state } = useAuth()
  if (state.status !== 'authenticated') throw new Error('No hay sesión')
  return state.user
}
