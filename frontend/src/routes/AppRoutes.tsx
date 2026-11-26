import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth, useCurrentUser } from '../contexts/AuthContext'
import type { Role } from '../services/api/types'
import { AuthLayout } from '../layouts/AuthLayout'
import { AppLayout } from '../layouts/AppLayout'
import { LoginPage } from '../modules/auth/LoginPage'
import { RegisterPage } from '../modules/auth/RegisterPage'
import { ForgotPasswordPage } from '../modules/auth/ForgotPasswordPage'
import { HomePage } from '../modules/account/HomePage'
import { SecurityPage } from '../modules/account/SecurityPage'
import { UsersPage } from '../modules/admin/UsersPage'
import { AuditPage } from '../modules/admin/AuditPage'
import { NewAdminPage } from '../modules/admin/NewAdminPage'

// Estas guardas solo ordenan la navegación: quien decide el acceso es el backend

function SignedIn() {
  const { state } = useAuth()
  const location = useLocation()
  if (state.status !== 'authenticated') {
    return <Navigate to="/ingresar" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}

function SignedOut() {
  const { state } = useAuth()
  const from = (useLocation().state as { from?: string } | null)?.from
  return state.status === 'authenticated' ? <Navigate to={from ?? '/'} replace /> : <Outlet />
}

function Allow({ roles }: { roles: Role[] }) {
  const user = useCurrentUser()
  return roles.includes(user.role) ? <Outlet /> : <Navigate to="/" replace />
}

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<SignedOut />}>
        <Route element={<AuthLayout />}>
          <Route path="/ingresar" element={<LoginPage />} />
          <Route path="/registro" element={<RegisterPage />} />
          <Route path="/recuperar" element={<ForgotPasswordPage />} />
        </Route>
      </Route>
      <Route element={<SignedIn />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/seguridad" element={<SecurityPage />} />
          <Route element={<Allow roles={['ADMIN', 'SUPERADMIN']} />}>
            <Route path="/admin/usuarios" element={<UsersPage />} />
            <Route path="/admin/auditoria" element={<AuditPage />} />
          </Route>
          <Route element={<Allow roles={['SUPERADMIN']} />}>
            <Route path="/admin/equipo" element={<NewAdminPage />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
