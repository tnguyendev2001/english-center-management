import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type {
  StudentSummarySearchParams,
  StudentTuitionSummary,
  TuitionOverview,
} from '../financial/financialSummaryTypes'
import type { Invoice, InvoiceSearchParams, TuitionNotice } from './invoiceTypes'

export async function getInvoices(params: InvoiceSearchParams) {
  const response = await httpClient.get<ApiResponse<Invoice[]>>('/invoices', {
    params,
  })

  return response.data
}

export async function getTuitionOverview() {
  const response = await httpClient.get<ApiResponse<TuitionOverview>>('/invoices/overview')

  return response.data.data
}

export async function getTuitionStudentSummaries(params?: StudentSummarySearchParams) {
  const response = await httpClient.get<ApiResponse<StudentTuitionSummary[]>>('/invoices/student-summaries', {
    params,
  })

  return response.data
}

export async function getInvoice(id: number) {
  const response = await httpClient.get<ApiResponse<Invoice>>(`/invoices/${id}`)

  return response.data.data
}

export async function getTuitionNotice(invoiceId: number) {
  const response = await httpClient.get<ApiResponse<TuitionNotice>>(`/invoices/${invoiceId}/tuition-notice`)

  return response.data.data
}
