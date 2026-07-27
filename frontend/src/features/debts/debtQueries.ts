import { useQuery } from '@tanstack/react-query'
import type { StudentSummarySearchParams } from '../financial/financialSummaryTypes'
import type { DebtSearchParams } from './debtTypes'
import { getDebts, getDebtStudentSummaries } from './debtApi'

export const debtKeys = {
  all: ['debts'] as const,
  list: (params: DebtSearchParams) => ['debts', 'list', params] as const,
  studentSummaries: (params?: StudentSummarySearchParams) => ['debts', 'student-summaries', params] as const,
}

export function useDebts(params: DebtSearchParams, enabled = true) {
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
