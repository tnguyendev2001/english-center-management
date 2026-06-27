export type InvoiceStatus = 'UNPAID' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELED'

export interface Invoice {
  id: number
  invoiceCode: string
  studentId: number
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
  page: number
  size: number
}
