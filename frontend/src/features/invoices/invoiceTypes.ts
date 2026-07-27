import type { CenterProfile } from '../center/centerProfileTypes'
import type { Payment } from '../payments/paymentTypes'

export type InvoiceStatus = 'UNPAID' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELED' | 'REPLACED'

export type DebtStatus = 'NOT_DUE' | 'DUE_TODAY' | 'OVERDUE' | 'PARTIALLY_PAID'

export interface Invoice {
  id: number
  invoiceCode: string
  studentId: number
  studentCode: string
  studentName: string
  classroomId: number
  classroomCode?: string | null
  classroomName: string
  teacherName?: string | null
  enrollmentId: number
  studentPackageId: number
  packageId?: number | null
  packageCode?: string | null
  packageNameSnapshot: string
  totalSessionsSnapshot: number
  packagePriceSnapshot?: number | null
  cycleNo?: number | null
  billingLabel?: string | null
  effectiveFrom?: string | null
  estimatedEffectiveTo?: string | null
  amount: number
  discountAmount: number
  adjustmentAmount: number
  finalAmount: number
  paidAmount: number
  remainingAmount: number
  issueDate?: string | null
  dueDate: string
  overdueDays?: number | null
  debtStatus?: DebtStatus | string | null
  status: InvoiceStatus
  note?: string | null
  source?: string | null
  createdAt: string
  updatedAt: string
}

export interface InvoiceListSummary {
  totalRemainingCollectible: number
  unpaidCount: number
  partiallyPaidCount: number
  collectedThisMonth: number
}

export interface InvoiceDocumentNextCycle {
  invoiceId: number
  invoiceCode: string
  billingLabel?: string | null
  effectiveFrom?: string | null
  dueDate?: string | null
  remainingAmount?: number | null
}

export interface InvoiceDocument {
  center: CenterProfile
  documentTitle: string
  invoiceId: number
  invoiceCode: string
  issueDate?: string | null
  dueDate?: string | null
  status: InvoiceStatus
  studentCode: string
  studentName: string
  parentName?: string | null
  parentPhone?: string | null
  classroomCode?: string | null
  classroomName?: string | null
  teacherName?: string | null
  packageName?: string | null
  packageSessionCount?: number | null
  packagePriceSnapshot?: number | null
  cycleNo?: number | null
  billingLabel?: string | null
  effectiveFrom?: string | null
  estimatedEffectiveTo?: string | null
  estimatedEffectiveToDisplay?: string | null
  totalAmount: number
  paidAmount: number
  remainingAmount: number
  transferContentSuggestion?: string | null
  note?: string | null
  nextCycleMessage?: string | null
  nextCycle?: InvoiceDocumentNextCycle | null
  remainingSessionsBeforeRenewal?: number | null
  payments: Payment[]
}

export interface InvoiceSearchParams {
  status?: InvoiceStatus
  studentId?: number
  classroomId?: number
  packageId?: number
  cycleNo?: number
  overdue?: boolean
  hasRemainingDebt?: boolean
  effectiveFrom?: string
  effectiveTo?: string
  dueFrom?: string
  dueTo?: string
  keyword?: string
  page: number
  size: number
}

export const INVOICE_STATUS_LABELS: Record<InvoiceStatus, string> = {
  UNPAID: 'Chưa thanh toán',
  PARTIALLY_PAID: 'Thanh toán một phần',
  PAID: 'Đã thanh toán',
  REPLACED: 'Đã thay thế',
  CANCELED: 'Đã hủy',
}

export const DEBT_STATUS_LABELS: Record<string, string> = {
  NOT_DUE: 'Chưa đến hạn',
  DUE_TODAY: 'Đến hạn hôm nay',
  OVERDUE: 'Quá hạn',
  PARTIALLY_PAID: 'Thanh toán một phần',
}

export function formatEstimatedEffectiveTo(
  estimatedEffectiveTo?: string | null,
  totalSessions?: number | null,
): string {
  if (estimatedEffectiveTo) {
    const [year, month, day] = estimatedEffectiveTo.split('-')
    if (year && month && day) {
      return `${day}/${month}/${year}`
    }
    return estimatedEffectiveTo
  }
  if (totalSessions != null) {
    return `Đến khi sử dụng hết ${totalSessions} buổi`
  }
  return '-'
}
