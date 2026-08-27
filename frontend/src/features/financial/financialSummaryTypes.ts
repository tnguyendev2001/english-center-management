import type { PaymentMethod } from '../payments/paymentTypes'

export interface StudentTuitionSummary {
  studentId: number
  studentCode: string
  studentName: string
  currentClassroomId?: number | null
  currentClassroomName?: string | null
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
  currentClassroomId?: number | null
  currentClassroomName?: string | null
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
  keyword?: string
  classroomId?: number
  fromDate?: string
  toDate?: string
  page?: number
  size?: number
}

export interface TuitionOverview {
  totalOutstanding: number
  studentsWithDebt: number
  unpaidInvoiceCount: number
  partialInvoiceCount: number
}
