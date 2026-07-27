import type { InvoiceSearchParams } from '../invoices/invoiceTypes'

export interface DebtSearchParams extends Omit<InvoiceSearchParams, 'status' | 'hasRemainingDebt' | 'effectiveFrom' | 'effectiveTo'> {
  status?: string
  overdue?: boolean
  multipleInvoices?: boolean
  remainingFrom?: number
  remainingTo?: number
}
