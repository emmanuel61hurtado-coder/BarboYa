import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { z } from 'zod'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider, useAuth } from '../contexts/AuthContext'
import { ApiError } from '../services/api/client'
import { Alert } from '../components/ui/surface'
import { Button } from '../components/ui/button'
import { AppRoutes } from './AppRoutes'

// La politica de contenido del servidor no permite generar codigo en el navegador
z.config({ jitless: true })

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 15_000,
      // Repetir una petición rechazada no cambia la respuesta
      retry: (count, error) => !(error instanceof ApiError && error.status >= 400 && error.status < 500) && count < 2,
    },
  },
})

function SessionGate() {
  const { state, retry } = useAuth()
  if (state.status === 'loading' || state.status === 'unreachable') {
    return (
      <div className="min-h-screen bg-[#090d16] flex items-center justify-center px-4">
        {state.status === 'loading' ? (
          <p role="status" className="text-sm text-slate-400">
            Comprobando tu sesión…
          </p>
        ) : (
          <Alert variant="error" className="max-w-sm space-y-3">
            <p>No pudimos conectar con el servidor.</p>
            <Button variant="secondary" size="sm" onClick={retry}>
              Reintentar
            </Button>
          </Alert>
        )}
      </div>
    )
  }
  return <AppRoutes />
}

/** Interfaz conectada a la API /api/v1 (modo `v1`). */
export default function PlatformApp() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <SessionGate />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
