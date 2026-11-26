import { Link } from 'react-router-dom'
import { useCurrentUser } from '../../contexts/AuthContext'
import { Alert, Card } from '../../components/ui/surface'
import { Button } from '../../components/ui/button'
import { ROLE_LABELS, STATUS_LABELS } from '../../utils/labels'

export function HomePage() {
  const user = useCurrentUser()
  const details = [
    ['Tipo de cuenta', ROLE_LABELS[user.role]],
    ['Estado', STATUS_LABELS[user.status]],
    ['Correo', `${user.email}${user.emailVerified ? ' (verificado)' : ' (sin verificar)'}`],
    ['Celular', user.phone],
  ]

  return (
    <>
      <h1 className="text-2xl font-display font-black">Hola, {user.fullName.split(' ')[0]}</h1>
      {user.status === 'PENDING_VERIFICATION' && (
        <Alert variant="warning">
          Tu cuenta está en revisión. Podrás operar cuando una persona del equipo la apruebe.
        </Alert>
      )}
      {!user.emailVerified && (
        <Alert>
          Verifica tu correo para proteger tu cuenta.{' '}
          <Button asChild variant="link" size="inline">
            <Link to="/seguridad">Verificar ahora</Link>
          </Button>
        </Alert>
      )}
      <Card title="Tu cuenta">
        <dl className="grid gap-3 sm:grid-cols-2">
          {details.map(([term, value]) => (
            <div key={term}>
              <dt className="text-xs text-slate-500">{term}</dt>
              <dd className="text-sm break-words">{value}</dd>
            </div>
          ))}
        </dl>
      </Card>
      <Card title="Servicios">
        {user.role === 'CUSTOMER' && (
          <Button asChild>
            <Link to="/comercios">Pedir en un comercio</Link>
          </Button>
        )}
        {(user.role === 'RESTAURANT' || user.role === 'STORE') && (
          <Button asChild>
            <Link to="/mi-comercio">Administrar mi comercio</Link>
          </Button>
        )}
        <p className="text-sm text-slate-400">
          Los domiciliarios de BarboYa, los viajes y los pagos en línea todavía no están disponibles.
        </p>
      </Card>
    </>
  )
}
