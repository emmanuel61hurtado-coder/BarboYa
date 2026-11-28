import { describe, expect, it } from 'vitest'
import { dayProblem, describeDay, fromWeek, toWeek } from './hours'

describe('horario', () => {
  const hours = [
    { day: 5, opens: '17:00', closes: '01:00' },
    { day: 1, opens: '14:00', closes: '18:00' },
    { day: 1, opens: '08:00', closes: '12:00' },
  ]

  it('agrupa por día en orden y se puede volver a enviar', () => {
    const week = toWeek(hours)
    expect(week).toHaveLength(7)
    expect(describeDay(week[0])).toBe('08:00 a 12:00 y 14:00 a 18:00')
    expect(describeDay(week[1])).toBe('Cerrado')
    expect(fromWeek(week)).toEqual([hours[2], hours[1], hours[0]])
  })

  it('acepta cerrar después de medianoche', () => {
    expect(dayProblem([{ opens: '18:00', closes: '02:00' }])).toBeNull()
  })

  it('rechaza horas vacías, iguales o turnos cruzados', () => {
    expect(dayProblem([{ opens: '', closes: '12:00' }])).toBe('Completa las horas')
    expect(dayProblem([{ opens: '08:00', closes: '08:00' }])).toContain('distinta')
    const crossed = [
      { opens: '08:00', closes: '14:00' },
      { opens: '13:00', closes: '18:00' },
    ]
    expect(dayProblem(crossed)).toBe('Los turnos no se pueden cruzar')
    const afterMidnight = [
      { opens: '18:00', closes: '02:00' },
      { opens: '20:00', closes: '23:00' },
    ]
    expect(dayProblem(afterMidnight)).toBe('Los turnos no se pueden cruzar')
  })
})
