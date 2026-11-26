import { useState } from 'react'
import type { FieldPath, FieldValues, UseFormReturn } from 'react-hook-form'
import { ApiError } from '../services/api/client'

/**
 * Envía un formulario a la API: los errores por campo del servidor se muestran
 * junto al campo y los demás como mensaje general.
 */
export function useSubmit<T extends FieldValues>(
  form: UseFormReturn<T>,
  action: (values: T) => Promise<void>,
) {
  const [error, setError] = useState<unknown>(null)

  const submit = form.handleSubmit(async (values) => {
    setError(null)
    try {
      await action(values)
    } catch (failure) {
      if (failure instanceof ApiError) {
        for (const [field, message] of Object.entries(failure.fieldErrors)) {
          if (field in values) form.setError(field as FieldPath<T>, { message })
        }
      }
      setError(failure)
    }
  })

  return { submit, error, sending: form.formState.isSubmitting }
}
