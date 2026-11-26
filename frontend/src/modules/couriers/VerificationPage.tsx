import { useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import type { z } from 'zod'
import { CourierDocument, CourierView, Requirement, courierApi } from '../../services/api/courier'
import { useAuth, useCurrentUser } from '../../contexts/AuthContext'
import { useSubmit } from '../../hooks/useSubmit'
import { courierProfileSchema, uploadProblem } from '../../validations/courier'
import { SelectField, TextField } from '../../components/ui/field'
import { Button } from '../../components/ui/button'
import { Alert, Card, ErrorAlert } from '../../components/ui/surface'
import { DocumentPreview } from './DocumentPreview'
import { VEHICLE_LABELS, documentState, formatDay, today } from './labels'

/** Guarda la respuesta y actualiza el estado de la cuenta, que puede cambiar. */
function useApply() {
  const queryClient = useQueryClient()
  const { reloadUser } = useAuth()
  return async (view: CourierView) => {
    queryClient.setQueryData(['courier'], view)
    await reloadUser()
  }
}

export function VerificationPage() {
  const user = useCurrentUser()
  const courier = useQuery({ queryKey: ['courier'], queryFn: courierApi.view })

  return (
    <>
      <h1 className="text-2xl font-display font-black">Verificación</h1>
      <ErrorAlert error={courier.error} />
      {courier.isPending && <p className="text-sm text-slate-400">Cargando…</p>}
      {courier.data && (
        <>
          {user.status === 'ACTIVE' ? (
            <Alert variant="success">
              Tu cuenta está aprobada. Si cambias tus datos o un documento obligatorio, volverá a revisión.
            </Alert>
          ) : courier.data.pending.length > 0 ? (
            <Alert variant="warning">
              <p>Para que aprueben tu cuenta falta:</p>
              <ul className="list-disc pl-5">
                {courier.data.pending.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </Alert>
          ) : (
            <Alert>Tus documentos están completos. Falta que activen tu cuenta.</Alert>
          )}
          <ProfileForm view={courier.data} />
          {courier.data.requirements.map((requirement) => (
            <DocumentCard
              key={requirement.type}
              requirement={requirement}
              document={courier.data.documents.find((d) => d.type === requirement.type)}
            />
          ))}
        </>
      )}
    </>
  )
}

function ProfileForm({ view: { profile } }: { view: CourierView }) {
  const apply = useApply()
  const [saved, setSaved] = useState(false)
  const form = useForm<z.infer<typeof courierProfileSchema>>({
    resolver: zodResolver(courierProfileSchema),
    defaultValues: {
      documentNumber: profile?.documentNumber ?? '',
      vehicleType: profile?.vehicleType ?? 'MOTORCYCLE',
      plate: profile?.plate ?? '',
      acceptsDataProcessing: profile !== null,
    },
  })
  const { submit, error, sending } = useSubmit(form, async (values) => {
    setSaved(false)
    await apply(await courierApi.saveProfile(values))
    form.reset(values)
    setSaved(true)
  })
  const { errors, isDirty } = form.formState
  const vehicle = form.watch('vehicleType')

  return (
    <Card title="Tus datos">
      <form onSubmit={submit} noValidate className="space-y-4">
        <div className="grid gap-4 sm:grid-cols-3">
          <TextField
            label="Número de documento"
            inputMode="numeric"
            error={errors.documentNumber?.message}
            {...form.register('documentNumber')}
          />
          <SelectField label="Vehículo" {...form.register('vehicleType')}>
            {courierProfileSchema.shape.vehicleType.options.map((value) => (
              <option key={value} value={value}>
                {VEHICLE_LABELS[value]}
              </option>
            ))}
          </SelectField>
          {vehicle !== 'BICYCLE' && (
            <TextField label="Placa" error={errors.plate?.message} {...form.register('plate')} />
          )}
        </div>
        <label className="flex items-start gap-2 text-sm text-slate-300">
          <input type="checkbox" className="mt-1" {...form.register('acceptsDataProcessing')} />
          <span>
            Autorizo a BarboYa a tratar mis datos personales y mis documentos, incluida la foto de mi rostro (dato
            sensible), para verificar mi identidad y habilitar mi cuenta. Puedo eliminar mis documentos; sin ellos no
            podré operar.
          </span>
        </label>
        {errors.acceptsDataProcessing && (
          <p role="alert" className="text-xs text-rose-400">
            {errors.acceptsDataProcessing.message}
          </p>
        )}
        <ErrorAlert error={error} />
        {saved && !isDirty && <Alert variant="success">Datos guardados.</Alert>}
        <Button type="submit" loading={sending}>
          Guardar mis datos
        </Button>
      </form>
    </Card>
  )
}

function DocumentCard({ requirement, document }: { requirement: Requirement; document?: CourierDocument }) {
  const apply = useApply()
  const input = useRef<HTMLInputElement>(null)
  const [expiresOn, setExpiresOn] = useState('')
  const [problem, setProblem] = useState<string | null>(null)
  const [confirming, setConfirming] = useState(false)
  const upload = useMutation({
    mutationFn: (file: File) => courierApi.upload(requirement.type, file, expiresOn),
    onSuccess: async (view) => {
      if (input.current) input.current.value = ''
      setExpiresOn('')
      await apply(view)
    },
  })
  const remove = useMutation({
    mutationFn: () => courierApi.remove(document!.id),
    onSuccess: async (view) => {
      setConfirming(false)
      await apply(view)
    },
  })
  const face = requirement.type === 'FACE_PHOTO'

  const send = () => {
    const file = input.current?.files?.[0]
    const issue = uploadProblem(requirement, file, expiresOn, today())
    setProblem(issue)
    if (!issue) upload.mutate(file!)
  }

  return (
    <section aria-label={requirement.label} className="pro-surface-elevated rounded-3xl p-5 space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="font-bold">
          {requirement.label}
          {!requirement.required && <span className="text-xs font-normal text-slate-500"> (si aplica)</span>}
        </h2>
        <span className="text-xs font-semibold text-sky-300">{documentState(document)}</span>
      </div>
      {document?.expiresOn && <p className="text-xs text-slate-400">Vence el {formatDay(document.expiresOn)}</p>}
      {document?.reviewNote && <p className="text-sm text-amber-300">Observación: {document.reviewNote}</p>}
      {document && (
        <DocumentPreview
          key={document.id}
          label={requirement.label}
          contentType={document.contentType}
          load={() => courierApi.ownFile(document.id)}
        />
      )}
      <div className="flex flex-wrap items-end gap-3">
        <label className="text-xs text-slate-300 space-y-1">
          <span className="block">{face ? 'Foto (JPG o PNG)' : 'Archivo (JPG, PNG o PDF)'}</span>
          <input
            ref={input}
            type="file"
            accept={face ? 'image/jpeg,image/png' : 'image/jpeg,image/png,application/pdf'}
            capture={face ? 'user' : undefined}
            className="text-sm"
          />
        </label>
        {requirement.expires && (
          <div className="w-48">
            <TextField
              label="Fecha de vencimiento"
              type="date"
              min={today()}
              value={expiresOn}
              onChange={(e) => setExpiresOn(e.target.value)}
            />
          </div>
        )}
        <Button size="sm" loading={upload.isPending} onClick={send}>
          {document ? 'Reemplazar' : 'Subir'}
        </Button>
        {document &&
          (confirming ? (
            <>
              <Button variant="destructive" size="sm" loading={remove.isPending} onClick={() => remove.mutate()}>
                Confirmar eliminación
              </Button>
              <Button variant="secondary" size="sm" onClick={() => setConfirming(false)}>
                Volver
              </Button>
            </>
          ) : (
            <Button variant="secondary" size="sm" onClick={() => setConfirming(true)}>
              Eliminar
            </Button>
          ))}
      </div>
      {problem && (
        <p role="alert" className="text-xs text-rose-400">
          {problem}
        </p>
      )}
      <ErrorAlert error={upload.error ?? remove.error} />
    </section>
  )
}
