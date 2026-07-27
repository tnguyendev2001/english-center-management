import type { CenterProfile } from '../center/centerProfileTypes'

export type PaymentStatus = 'VALID' | 'CANCELED'
export type PaymentMethod = 'CASH' | 'BANK_TRANSFER' | 'OTHER'

export interface Payment {
  id: number
  paymentCode: string
  invoiceId: number
  invoiceCode: string
  cycleNo?: number | null
  billingLabel?: string | null
  packageNameSnapshot?: string | null
  effectiveFrom?: string | null
  studentId: number
  studentCode: string
  studentName: string
  classroomId: number
  classroomCode?: string | null
  classroomName: string
  amount: number
  paymentDate: string
  method: PaymentMethod
  financialAccountId?: number | null
  financialAccountCode?: string | null
  financialAccountName?: string | null
  status: PaymentStatus
  note?: string | null
  cancelReason?: string | null
  createdBy?: string | null
  canceledBy?: string | null
  createdAt: string
  canceledAt?: string | null
}

export interface PaymentReceiptDocument {
  center: CenterProfile
  documentTitle: string
  paymentId: number
  paymentCode: string
  paymentDate: string
  status: PaymentStatus
  canceled: boolean
  payerName?: string | null
  studentCode: string
  studentName: string
  classroomCode?: string | null
  classroomName?: string | null
  invoiceCode: string
  billingLabel?: string | null
  packageName?: string | null
  effectiveFrom?: string | null
  invoiceTotalAmount?: number | null
  invoiceRemainingAfterPayment?: number | null
  amount: number
  amountInWords?: string | null
  method: PaymentMethod
  financialAccountName?: string | null
  referenceNumber?: string | null
  note?: string | null
  recordedBy?: string | null
  createdAt?: string | null
  createdBy?: string | null
  canceledAt?: string | null
  canceledBy?: string | null
  cancelReason?: string | null
  footerNote?: string | null
}

export interface CreatePaymentPayload {
  amount: number
  paymentDate: string
  method: PaymentMethod
  financialAccountId?: number | null
  note?: string | null
}

export interface CancelPaymentPayload {
  reason: string
}

export interface PaymentSearchParams {
  page: number
  size: number
}

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  VALID: 'Hợp lệ',
  CANCELED: 'Đã hủy',
}

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Tiền mặt',
  BANK_TRANSFER: 'Chuyển khoản',
  OTHER: 'Khác',
}
