/** Un turno. day: 1 lunes … 7 domingo. Si closes es menor que opens, cierra después de medianoche. */
export interface Shift {
  day: number
  opens: string
  closes: string
}

export const DAY_LABELS = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']

/** Los turnos de un día tal como se editan: ninguno, uno o dos. */
export type DayShifts = { opens: string; closes: string }[]

export function toWeek(hours: Shift[]): DayShifts[] {
  return DAY_LABELS.map((_, index) =>
    hours
      .filter((shift) => shift.day === index + 1)
      .sort((a, b) => a.opens.localeCompare(b.opens))
      .map(({ opens, closes }) => ({ opens, closes })),
  )
}

export const fromWeek = (week: DayShifts[]): Shift[] =>
  week.flatMap((shifts, index) => shifts.map((shift) => ({ day: index + 1, ...shift })))

const TIME = /^([01]\d|2[0-3]):[0-5]\d$/

/** Mismas reglas que aplica el servidor; devuelve el problema del día o null. */
export function dayProblem(shifts: DayShifts): string | null {
  if (shifts.some((s) => !TIME.test(s.opens) || !TIME.test(s.closes))) return 'Completa las horas'
  if (shifts.some((s) => s.opens === s.closes)) return 'La hora de cierre debe ser distinta a la de apertura'
  const [first, second] = [...shifts].sort((a, b) => a.opens.localeCompare(b.opens))
  if (second && (first.closes < first.opens || second.opens < first.closes)) {
    return 'Los turnos no se pueden cruzar'
  }
  return null
}

export const describeDay = (shifts: DayShifts) =>
  shifts.length === 0 ? 'Cerrado' : shifts.map((s) => `${s.opens} a ${s.closes}`).join(' y ')
