import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { LogOut } from 'lucide-react'
import { useAuth, useCurrentUser } from '../contexts/AuthContext'
import { Button } from '../components/ui/button'
import { ErrorAlert } from '../components/ui/surface'
import { useOrderEvents } from '../hooks/useOrderEvents'
import { ROLE_LABELS } from '../utils/labels'
import { cn } from '../utils/cn'

export function AppLayout() {
  const user = useCurrentUser()
  const { signOut } = useAuth()
  const [error, setError] = useState<unknown>(null)
  const [leaving, setLeaving] = useState(false)

  const staff = user.role === 'ADMIN' || user.role === 'SUPERADMIN'
  const customer = user.role === 'CUSTOMER'
  const merchant = user.role === 'RESTAURANT' || user.role === 'STORE'
  useOrderEvents(!staff)

  const links = [
    { to: '/', label: 'Inicio' },
    ...(customer
      ? [
          { to: '/comercios', label: 'Comercios' },
          { to: '/pedidos', label: 'Mis pedidos' },
          { to: '/direcciones', label: 'Direcciones' },
        ]
      : []),
    ...(merchant
      ? [
          { to: '/recibidos', label: 'Pedidos' },
          { to: '/productos', label: 'Productos' },
          { to: '/mi-comercio', label: 'Mi comercio' },
        ]
      : []),
    ...(user.role === 'COURIER'
      ? [
          { to: '/entregas', label: 'Entregas' },
          { to: '/verificacion', label: 'Verificación' },
        ]
      : []),
    ...(merchant || user.role === 'COURIER'
      ? [
          { to: '/comisiones', label: 'Comisiones' },
          { to: '/calificaciones', label: 'Calificaciones' },
        ]
      : []),
    ...(staff ? [] : [{ to: '/soporte', label: 'Soporte' }]),
    { to: '/seguridad', label: 'Seguridad' },
    ...(staff
      ? [
          { to: '/admin/usuarios', label: 'Usuarios' },
          { to: '/admin/pedidos', label: 'Pedidos' },
          { to: '/admin/soporte', label: 'Casos' },
          { to: '/admin/calificaciones', label: 'Opiniones' },
          { to: '/admin/finanzas', label: 'Finanzas' },
          { to: '/admin/auditoria', label: 'Auditoría' },
        ]
      : []),
    ...(user.role === 'SUPERADMIN' ? [{ to: '/admin/equipo', label: 'Nuevo administrador' }] : []),
  ]

  const leave = async () => {
    setLeaving(true)
    setError(null)
    try {
      await signOut()
    } catch (failure) {
      setError(failure)
      setLeaving(false)
    }
  }

  return (
    <div className="min-h-screen bg-[#090d16] text-slate-100">
      <header className="pro-surface sticky top-0 z-40">
        <div className="mx-auto max-w-5xl px-4 py-3 flex flex-wrap items-center justify-between gap-3">
          <span className="font-display font-black text-xl tracking-wider">
            BARBO<span className="text-sky-400">YA</span>
          </span>
          <div className="flex items-center gap-3">
            <div className="text-right leading-tight">
              <p className="text-sm font-semibold">{user.fullName}</p>
              <p className="text-[11px] text-slate-400">{ROLE_LABELS[user.role]}</p>
            </div>
            <Button variant="secondary" size="sm" loading={leaving} onClick={leave}>
              <LogOut className="w-3.5 h-3.5" /> Cerrar sesión
            </Button>
          </div>
        </div>
        <nav aria-label="Principal" className="mx-auto max-w-5xl px-4 pb-2 flex gap-1 overflow-x-auto">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.to === '/'}
              className={({ isActive }) =>
                cn(
                  'whitespace-nowrap rounded-lg px-3 py-1.5 text-xs font-semibold',
                  isActive ? 'bg-sky-600 text-white' : 'text-slate-400 hover:text-white',
                )
              }
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-6 space-y-5">
        <ErrorAlert error={error} />
        <Outlet />
      </main>
    </div>
  )
}
