import type { InvoiceStatus } from '../invoices/invoiceTypes'
import type { Invoice } from '../invoices/invoiceTypes'
import type { PaymentMethod, PaymentStatus } from '../payments/paymentTypes'
import type { EnrollmentLearningProgress } from '../studentPackages/studentPackageTypes'

export type StudentPackageSourceType =
  | 'ENROLLMENT'
  | 'RENEWAL'
  | 'PACKAGE_CHANGE_REPLACEMENT'
  | 'PACKAGE_CHANGE_NEW_CYCLE'
  | 'LEGACY'

export type AttendanceReportStatus = 'PRESENT' | 'ABSENT' | 'EXCUSED'

export interface DebtReportItem {
  studentId: number
  studentCode: string
  studentName: string
  classroomName: string
  invoiceId: number
  invoiceCode: string
  invoiceType?: StudentPackageSourceType | null
  issueDate: string
  dueDate: string
  finalAmount: number
  paidAmount: number
  remainingAmount: number
  status: InvoiceStatus
  latestPackageName?: string | null
}

export interface DebtReportSummary {
  totalDebtAmount: number
  studentsWithDebtCount: number
  unpaidInvoiceCount: number
  partiallyPaidInvoiceCount: number
}

export interface DebtReportParams {
  classroomId?: number
  keyword?: string
  status?: InvoiceStatus
  fromDate?: string
  toDate?: string
  page?: number
  size?: number
}

export interface RevenueByDateItem {
  date: string
  amount: number
}

export interface PaymentReport {
  totalRevenue: number
  paymentCount: number
  cashAmount: number
  bankTransferAmount: number
  revenueByDate: RevenueByDateItem[]
  payments: import('../payments/paymentTypes').Payment[]
}

export interface PaymentReportParams {
  fromDate?: string
  toDate?: string
  classroomId?: number
  paymentMethod?: PaymentMethod
  paymentStatus?: PaymentStatus
  keyword?: string
  page?: number
  size?: number
}

export interface RevenueReport {
  totalRevenue: number
  revenueByDate: RevenueByDateItem[]
  revenueByPaymentMethod: { paymentMethod: PaymentMethod; amount: number }[]
  payments: import('../payments/paymentTypes').Payment[]
}

export interface RevenueReportParams {
  fromDate?: string
  toDate?: string
  classroomId?: number
  paymentMethod?: PaymentMethod
  page?: number
  size?: number
}

export interface InvoiceReportParams {
  classroomId?: number
  keyword?: string
  status?: InvoiceStatus
  fromDate?: string
  toDate?: string
  page?: number
  size?: number
}

export interface AttendanceReportItem {
  id: number
  sessionDate: string
  classroomName: string
  studentCode: string
  studentName: string
  status: AttendanceReportStatus
  note?: string | null
}

export interface AttendanceReportSummary {
  totalCount: number
  presentCount: number
  absentCount: number
  excusedCount: number
  presentRate: number
}

export interface AttendanceReport {
  summary: AttendanceReportSummary
  items: AttendanceReportItem[]
}

export interface AttendanceReportParams {
  classroomId?: number
  keyword?: string
  sessionDate?: string
  status?: AttendanceReportStatus
  page?: number
  size?: number
}

export type EnrollmentProgressReportItem = EnrollmentLearningProgress

export function debtItemToInvoice(item: DebtReportItem): Invoice {
  return {
    id: item.invoiceId,
    invoiceCode: item.invoiceCode,
    studentId: item.studentId,
    studentCode: item.studentCode,
    studentName: item.studentName,
    classroomId: 0,
    classroomName: item.classroomName,
    enrollmentId: 0,
    studentPackageId: 0,
    packageNameSnapshot: item.latestPackageName ?? '',
    totalSessionsSnapshot: 0,
    amount: item.finalAmount,
    discountAmount: 0,
    adjustmentAmount: 0,
    finalAmount: item.finalAmount,
    paidAmount: item.paidAmount,
    remainingAmount: item.remainingAmount,
    dueDate: item.dueDate,
    status: item.status,
    createdAt: item.issueDate,
    updatedAt: item.issueDate,
  }
}
