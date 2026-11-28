import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '../../services/api/auth'
import { useAuth } from '../../contexts/AuthContext'
import { TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Card, ErrorAlert } from '../../components/ui/surface'

export function DeleteAccountCard() {
  const { signOut } = useAuth()
  const [open, setOpen] = useState(false)
  const [password, setPassword] = useState('')
  const [missing, setMissing] = useState(false)
  const remove = useMutation({
    mutationFn: authApi.deleteAccount,
    // La sesión ya no existe en el servidor: se limpia la de este dispositivo
    onSuccess: () => signOut(),
  })

  return (
    <Card
      title="Eliminar mi cuenta"
      description="Borra tus datos personales, direcciones y documentos y cierra todas tus sesiones. No se puede deshacer."
    >
      <p className="text-xs text-slate-400">
        La ley obliga a conservar la prueba de las operaciones: tus pedidos, comisiones, casos de soporte, registros de
        seguridad y las autorizaciones que diste. Antes debes terminar tus pedidos en curso y pagar comisiones pendientes.
      </p>
      {open ? (
        <form
          noValidate
          className="space-y-3"
          onSubmit={(event) => {
            event.preventDefault()
            setMissing(!password)
            if (password) remove.mutate(password)
          }}
        >
          <ErrorAlert error={remove.error} />
          <div className="max-w-xs">
            <TextField
              label="Tu contraseña para confirmar"
              type="password"
              autoComplete="current-password"
              autoFocus
              value={password}
              error={missing ? 'Escribe tu contraseña' : undefined}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <div className="flex gap-2">
            <Button type="submit" variant="destructive" size="sm" loading={remove.isPending}>
              Eliminar definitivamente
            </Button>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => {
                setOpen(false)
                setPassword('')
                remove.reset()
              }}
            >
              Conservar mi cuenta
            </Button>
          </div>
        </form>
      ) : (
        <Button variant="secondary" size="sm" onClick={() => setOpen(true)}>
          Quiero eliminar mi cuenta
        </Button>
      )}
    </Card>
  )
}
