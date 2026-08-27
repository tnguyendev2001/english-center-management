export type InvoiceStatus = 'UNPAID' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELED' | 'REPLACED'

export interface Invoice {
  id: number
  invoiceCode: string
  studentId: number
  studentCode: string
  studentName: string
  classroomId: number
  classroomName: string
  enrollmentId: number
  studentPackageId: number
  packageNameSnapshot: string
  totalSessionsSnapshot: number
  amount: number
  discountAmount: number
  adjustmentAmount: number
  finalAmount: number
  paidAmount: number
  remainingAmount: number
  dueDate: string
  status: InvoiceStatus
  note?: string | null
  createdAt: string
  updatedAt: string
}

export interface InvoiceSearchParams {
  status?: InvoiceStatus
  studentId?: number
  classroomId?: number
  keyword?: string
  packageName?: string
  dueFrom?: string
  dueTo?: string
  page: number
  size: number
}

export interface TuitionNotice {
  invoiceId: number
  invoiceCode: string
  centerName: string
  centerAddress?: string | null
  centerPhone?: string | null
  noticeDate: string
  studentCode: string
  studentName: string
  classroomName: string
  packageName: string
  packageSessionCount: number
  periodStart?: string | null
  periodEnd?: string | null
  invoiceAmount: number
  paidAmount: number
  remainingAmount: number
  amountInWords: string
  dueDate?: string | null
  invoiceStatus: InvoiceStatus
  noticeText: string
  footerText: string
}
