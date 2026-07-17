export type PaymentStatus = 'VALID' | 'CANCELED'
export type PaymentMethod = 'CASH' | 'BANK_TRANSFER' | 'OTHER'

export interface Payment {
  id: number
  paymentCode: string
  invoiceId: number
  invoiceCode: string
  studentId: number
  studentCode: string
  studentName: string
  classroomId: number
  classroomName: string
  amount: number
  paymentDate: string
  method: PaymentMethod
  status: PaymentStatus
  note?: string | null
  cancelReason?: string | null
  createdAt: string
  canceledAt?: string | null
}

export interface CreatePaymentPayload {
  amount: number
  paymentDate: string
  method: PaymentMethod
  note?: string | null
}

export interface CancelPaymentPayload {
  reason: string
}

export interface PaymentSearchParams {
  page: number
  size: number
}
