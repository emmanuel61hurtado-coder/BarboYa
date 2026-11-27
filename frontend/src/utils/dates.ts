/** Hoy en Colombia como AAAA-MM-DD, sin importar la zona del dispositivo. */
export const todayInColombia = (now = new Date()) =>
  new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Bogota',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(now)

const dayFormat = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'long', timeZone: 'UTC' })

/** Muestra una fecha sin hora. Convertirla a la zona local la correría un día. */
export const formatDay = (isoDate: string) => dayFormat.format(new Date(`${isoDate}T00:00:00Z`))
