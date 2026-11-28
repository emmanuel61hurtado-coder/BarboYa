import { useEffect, useState } from 'react'
import { api } from './api'

/** Apagado mientras no se sepa: no se ofrece un servicio suspendido por no poder consultar. */
const OFF = { trips: false, crypto: false }

export function useFeatures() {
  const [features, setFeatures] = useState(OFF)
  useEffect(() => {
    api.getFeatures().then(setFeatures, () => setFeatures(OFF))
  }, [])
  return features
}
