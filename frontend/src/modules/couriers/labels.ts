import type { CourierDocument, VehicleType } from '../../services/api/courier'

export const VEHICLE_LABELS: Record<VehicleType, string> = {
  BICYCLE: 'Bicicleta',
  MOTORCYCLE: 'Motocicleta',
  CAR: 'Automóvil',
}

export function documentState(document: CourierDocument | undefined) {
  if (!document) return 'Sin subir'
  if (document.expired) return 'Vencido'
  return { PENDING: 'Por revisar', APPROVED: 'Aprobado', REJECTED: 'Rechazado' }[document.status]
}

/** Las fechas sin hora se muestran tal cual, sin cambiar de día por la zona horaria. */
export const formatDay = (day: string) => day.split('-').reverse().join('/')

export function today() {
  const now = new Date()
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}
