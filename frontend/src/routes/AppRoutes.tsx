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
import { ExplorePage } from '../modules/client/ExplorePage'
import { StorePage } from '../modules/client/StorePage'
import { MyOrdersPage } from '../modules/client/MyOrdersPage'
import { OrderPage } from '../modules/client/OrderPage'
import { AddressesPage } from '../modules/client/AddressesPage'
import { OrderBoardPage } from '../modules/business/OrderBoardPage'
import { ProductsPage } from '../modules/business/ProductsPage'
import { BusinessPage } from '../modules/business/BusinessPage'
import { VerificationPage } from '../modules/couriers/VerificationPage'
import { DeliveriesPage } from '../modules/couriers/DeliveriesPage'
import { MyAccountPage } from '../modules/finance/MyAccountPage'
import { AdminFinancePage } from '../modules/finance/AdminFinancePage'
import { SupportPage } from '../modules/support/SupportPage'
import { TicketPage } from '../modules/support/TicketPage'
import { OrdersPage } from '../modules/admin/OrdersPage'
import { SupportQueuePage } from '../modules/admin/SupportQueuePage'
import { SupportTicketPage } from '../modules/admin/SupportTicketPage'
import { LegalPage } from '../modules/legal/LegalPage'
import { MyRatingsPage } from '../modules/ratings/MyRatingsPage'
import { RatingsModerationPage } from '../modules/admin/RatingsModerationPage'

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
      <Route path="/terminos" element={<LegalPage slug="terminos" />} />
      <Route path="/privacidad" element={<LegalPage slug="privacidad" />} />
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
          <Route path="/soporte" element={<SupportPage />} />
          <Route path="/soporte/:id" element={<TicketPage />} />
          <Route element={<Allow roles={['CUSTOMER']} />}>
            <Route path="/comercios" element={<ExplorePage />} />
            <Route path="/comercios/:id" element={<StorePage />} />
            <Route path="/pedidos" element={<MyOrdersPage />} />
            <Route path="/pedidos/:id" element={<OrderPage />} />
            <Route path="/direcciones" element={<AddressesPage />} />
          </Route>
          <Route element={<Allow roles={['RESTAURANT', 'STORE']} />}>
            <Route path="/recibidos" element={<OrderBoardPage />} />
            <Route path="/productos" element={<ProductsPage />} />
            <Route path="/mi-comercio" element={<BusinessPage />} />
          </Route>
          <Route element={<Allow roles={['COURIER']} />}>
            <Route path="/verificacion" element={<VerificationPage />} />
            <Route path="/entregas" element={<DeliveriesPage />} />
          </Route>
          <Route element={<Allow roles={['RESTAURANT', 'STORE', 'COURIER']} />}>
            <Route path="/comisiones" element={<MyAccountPage />} />
            <Route path="/calificaciones" element={<MyRatingsPage />} />
          </Route>
          <Route element={<Allow roles={['ADMIN', 'SUPERADMIN']} />}>
            <Route path="/admin/finanzas" element={<AdminFinancePage />} />
            <Route path="/admin/pedidos" element={<OrdersPage />} />
            <Route path="/admin/soporte" element={<SupportQueuePage />} />
            <Route path="/admin/soporte/:id" element={<SupportTicketPage />} />
            <Route path="/admin/calificaciones" element={<RatingsModerationPage />} />
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
