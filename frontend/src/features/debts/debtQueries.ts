import { useQuery } from '@tanstack/react-query'
import type { StudentSummarySearchParams } from '../financial/financialSummaryTypes'
import type { InvoiceSearchParams } from '../invoices/invoiceTypes'
import { getDebts, getDebtStudentSummaries } from './debtApi'

export const debtKeys = {
  all: ['debts'] as const,
  list: (params: InvoiceSearchParams) => ['debts', 'list', params] as const,
  studentSummaries: (params?: StudentSummarySearchParams) => ['debts', 'student-summaries', params] as const,
}

export function useDebts(params: InvoiceSearchParams, enabled = true) {
  return useQuery({
    queryKey: debtKeys.list(params),
    queryFn: () => getDebts(params),
    enabled,
  })
}

export function useDebtStudentSummaries(params?: StudentSummarySearchParams, enabled = true) {
  return useQuery({
    queryKey: debtKeys.studentSummaries(params),
    queryFn: () => getDebtStudentSummaries(params),
    enabled,
  })
}
