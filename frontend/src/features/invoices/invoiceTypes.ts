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
  page: number
  size: number
}
