import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Business, shopApi } from '../../services/api/commerce'
import { Button } from '../../components/ui/button'
import { Alert, Card, ErrorAlert } from '../../components/ui/surface'
import { DAY_LABELS, DayShifts, dayProblem, fromWeek, toWeek } from '../../utils/hours'

const NEW_SHIFT = { opens: '08:00', closes: '18:00' }
const input = 'rounded-lg pro-input px-2 py-1.5 text-sm'

export function HoursCard({ business }: { business: Business }) {
  const queryClient = useQueryClient()
  const [week, setWeek] = useState(() => toWeek(business.hours))
  const [checked, setChecked] = useState(false)
  const [changed, setChanged] = useState(false)
  const save = useMutation({
    mutationFn: () => shopApi.setHours(fromWeek(week)),
    onSuccess: (updated) => {
      queryClient.setQueryData(['shop', 'business'], updated)
      setChanged(false)
    },
  })

  const setDay = (index: number, shifts: DayShifts) => {
    setWeek(week.map((current, i) => (i === index ? shifts : current)))
    setChanged(true)
    save.reset()
  }

  const problems = week.map(dayProblem)

  return (
    <Card
      title="Horario de atención"
      description="Hora de Colombia. Sin ningún día marcado no hay horario: solo cuenta el botón de recibir pedidos."
    >
      <form
        noValidate
        className="space-y-3"
        onSubmit={(event) => {
          event.preventDefault()
          setChecked(true)
          if (problems.every((problem) => problem === null)) save.mutate()
        }}
      >
        {week.map((shifts, index) => {
          const day = DAY_LABELS[index]
          return (
            <fieldset key={day} aria-label={day} className="space-y-1 border-b border-slate-800 pb-3">
              <label className="flex items-center gap-2 text-sm font-semibold">
                <input
                  type="checkbox"
                  checked={shifts.length > 0}
                  onChange={(e) => setDay(index, e.target.checked ? [NEW_SHIFT] : [])}
                />
                {day}
              </label>
              {shifts.map((shift, position) => (
                <div key={position} className="flex flex-wrap items-center gap-2 text-sm">
                  {(['opens', 'closes'] as const).map((side) => (
                    <label key={side} className="flex items-center gap-1 text-xs text-slate-400">
                      {side === 'opens' ? 'Abre' : 'Cierra'}
                      <input
                        type="time"
                        className={input}
                        aria-label={`${side === 'opens' ? 'Abre' : 'Cierra'} ${day} turno ${position + 1}`}
                        value={shift[side]}
                        onChange={(e) =>
                          setDay(
                            index,
                            shifts.map((s, i) => (i === position ? { ...s, [side]: e.target.value } : s)),
                          )
                        }
                      />
                    </label>
                  ))}
                  {position === 1 && (
                    <Button variant="secondary" size="sm" onClick={() => setDay(index, [shifts[0]])}>
                      Quitar turno
                    </Button>
                  )}
                </div>
              ))}
              {shifts.length === 1 && (
                <button
                  type="button"
                  className="text-xs text-sky-400 hover:underline"
                  onClick={() => setDay(index, [shifts[0], { opens: '', closes: '' }])}
                >
                  Agregar segundo turno
                </button>
              )}
              {checked && problems[index] && (
                <p role="alert" className="text-xs text-rose-400">
                  {problems[index]}
                </p>
              )}
            </fieldset>
          )
        })}
        <ErrorAlert error={save.error} />
        {save.isSuccess && !changed && <Alert variant="success">Horario guardado.</Alert>}
        <Button type="submit" loading={save.isPending}>
          Guardar horario
        </Button>
      </form>
    </Card>
  )
}
