import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type { StudentDebtSummary, StudentSummarySearchParams } from '../financial/financialSummaryTypes'
import type { Invoice } from '../invoices/invoiceTypes'
import type { DebtSearchParams } from './debtTypes'

export async function getDebts(params: DebtSearchParams) {
  const response = await httpClient.get<ApiResponse<Invoice[]>>('/debts', {
    params,
  })

  return response.data
}

export async function getDebtStudentSummaries(params?: StudentSummarySearchParams) {
  const response = await httpClient.get<ApiResponse<StudentDebtSummary[]>>('/debts/student-summaries', {
    params,
  })

  return response.data
}
