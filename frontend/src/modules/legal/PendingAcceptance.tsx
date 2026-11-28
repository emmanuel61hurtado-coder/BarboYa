import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { legalApi } from '../../services/api/legal'
import { Button } from '../../components/ui/button'
import { Alert, ErrorAlert } from '../../components/ui/surface'

/** Cuando cambian los documentos, o la cuenta es anterior, se pide aceptarlos. */
export function PendingAcceptance() {
  const queryClient = useQueryClient()
  const pending = useQuery({ queryKey: ['legal', 'pending'], queryFn: legalApi.pending })
  const accept = useMutation({
    mutationFn: async () => {
      const info = await legalApi.info()
      await legalApi.accept(info.termsVersion, info.privacyVersion)
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['legal', 'pending'] }),
  })
  if (!pending.data?.length) return null

  return (
    <Alert variant="warning" className="space-y-2">
      <p>
        Revisa los{' '}
        <a href="/terminos" target="_blank" rel="noreferrer" className="underline">
          términos
        </a>{' '}
        y la{' '}
        <a href="/privacidad" target="_blank" rel="noreferrer" className="underline">
          política de datos
        </a>
        . Al aceptar autorizas el tratamiento de tus datos como allí se describe.
      </p>
      <ErrorAlert error={accept.error} />
      <Button size="sm" loading={accept.isPending} onClick={() => accept.mutate()}>
        Acepto y autorizo
      </Button>
    </Alert>
  )
}
