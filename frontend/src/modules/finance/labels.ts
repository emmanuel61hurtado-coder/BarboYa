import type { MovementKind, PaymentChannel } from '../../services/api/finance'

export const MOVEMENT_LABELS: Record<MovementKind, string> = {
  COMMISSION: 'Comisión',
  PAYMENT: 'Pago recibido',
  ADJUSTMENT: 'Deuda perdonada',
  REVERSAL: 'Reversión',
}

export const METHOD_LABELS: Record<PaymentChannel, string> = {
  BANK_TRANSFER: 'Transferencia',
  NEQUI: 'Nequi',
  CASH: 'Efectivo',
}
