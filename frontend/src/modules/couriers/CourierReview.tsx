import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CourierDocument, CourierView, courierApi } from '../../services/api/courier'
import { TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Alert, ErrorAlert } from '../../components/ui/surface'
import { DocumentPreview } from './DocumentPreview'
import { VEHICLE_LABELS, documentState, formatDay } from './labels'

/** Revisión humana de los documentos de un domiciliario. */
export function CourierReview({ userId }: { userId: string }) {
  // Siempre lo último: el domiciliario puede haber reemplazado un archivo
  const courier = useQuery({
    queryKey: ['admin', 'courier', userId],
    queryFn: () => courierApi.review(userId),
    staleTime: 0,
    refetchOnMount: 'always',
  })

  if (courier.error) return <ErrorAlert error={courier.error} />
  if (!courier.data) return <p className="text-sm text-slate-400">Cargando…</p>
  const { profile, requirements, documents, pending } = courier.data
  if (!profile) return <p className="text-sm text-amber-300">Todavía no ha registrado sus datos.</p>

  return (
    <div className="space-y-3 rounded-2xl border border-slate-700 p-3">
      <p className="text-sm">
        Documento {profile.documentNumber} · {VEHICLE_LABELS[profile.vehicleType]}
        {profile.plate && ` · Placa ${profile.plate}`}
      </p>
      {pending.length > 0 ? (
        <Alert variant="warning">Pendiente: {pending.join('; ')}</Alert>
      ) : (
        <Alert variant="success">Documentación completa. Ya puedes aprobar la cuenta.</Alert>
      )}
      <ul className="divide-y divide-slate-800">
        {documents.map((document) => (
          <ReviewRow
            key={document.id}
            userId={userId}
            document={document}
            label={requirements.find((r) => r.type === document.type)?.label ?? document.type}
          />
        ))}
      </ul>
    </div>
  )
}

function ReviewRow({ userId, document, label }: { userId: string; document: CourierDocument; label: string }) {
  const queryClient = useQueryClient()
  const [rejecting, setRejecting] = useState(false)
  const [note, setNote] = useState('')
  const decide = useMutation({
    mutationFn: (decision: 'APPROVED' | 'REJECTED') =>
      courierApi.decide(document.id, decision, decision === 'REJECTED' ? note : undefined),
    onSuccess: (view: CourierView) => {
      queryClient.setQueryData(['admin', 'courier', userId], view)
      setRejecting(false)
    },
  })

  return (
    <li aria-label={label} className="py-3 space-y-2">
      <p className="text-sm font-semibold">
        {label} · <span className="text-sky-300">{documentState(document)}</span>
        {document.expiresOn && <span className="text-slate-400"> · vence {formatDay(document.expiresOn)}</span>}
      </p>
      {document.reviewNote && <p className="text-xs text-amber-300">Observación: {document.reviewNote}</p>}
      <DocumentPreview label={label} contentType={document.contentType} load={() => courierApi.reviewFile(document.id)} />
      <div className="flex flex-wrap items-end gap-2">
        {document.status !== 'APPROVED' && (
          <Button size="sm" loading={decide.isPending} onClick={() => decide.mutate('APPROVED')}>
            Aprobar documento
          </Button>
        )}
        {rejecting ? (
          <>
            <div className="w-64">
              <TextField label="Motivo del rechazo" autoFocus value={note} onChange={(e) => setNote(e.target.value)} />
            </div>
            <Button variant="destructive" size="sm" onClick={() => decide.mutate('REJECTED')}>
              Confirmar rechazo
            </Button>
            <Button variant="secondary" size="sm" onClick={() => setRejecting(false)}>
              Volver
            </Button>
          </>
        ) : (
          <Button variant="secondary" size="sm" onClick={() => setRejecting(true)}>
            Rechazar
          </Button>
        )}
      </div>
      <ErrorAlert error={decide.error} />
    </li>
  )
}
