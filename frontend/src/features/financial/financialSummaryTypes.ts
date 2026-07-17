import type { PaymentMethod } from '../payments/paymentTypes'

export interface StudentTuitionSummary {
  studentId: number
  studentCode: string
  studentName: string
  classroomId: number
  classroomName: string
  totalTuitionAmount: number
  totalPaidAmount: number
  remainingDebt: number
  totalInvoiceCount: number
  unpaidCount: number
  partialCount: number
  paidCount: number
  hasReplacedInvoices: boolean
}

export interface StudentDebtSummary {
  studentId: number
  studentCode: string
  studentName: string
  classroomId: number
  classroomName: string
  totalRemainingDebt: number
  debtInvoiceCount: number
  unpaidCount: number
  partialCount: number
  nearestDueDate?: string | null
}

export interface StudentPaymentSummary {
  studentId: number
  studentCode: string
  studentName: string
  classroomId: number
  classroomName: string
  totalPaidAmount: number
  paymentCount: number
  lastPaymentDate?: string | null
  lastPaymentMethod?: PaymentMethod | null
}

export interface StudentSummarySearchParams {
  classroomId?: number
  fromDate?: string
  toDate?: string
}
