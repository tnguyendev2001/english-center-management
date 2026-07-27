import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type { StudentTuitionSummary, StudentSummarySearchParams } from '../financial/financialSummaryTypes'
import type {
  Invoice,
  InvoiceDocument,
  InvoiceListSummary,
  InvoiceSearchParams,
} from './invoiceTypes'

export async function getInvoices(params: InvoiceSearchParams) {
  const response = await httpClient.get<ApiResponse<Invoice[]>>('/invoices', {
    params,
  })

  return response.data
}

export async function getInvoiceSummary(classroomId?: number) {
  const response = await httpClient.get<ApiResponse<InvoiceListSummary>>('/invoices/summary', {
    params: classroomId != null ? { classroomId } : undefined,
  })

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

export async function getInvoiceDocument(id: number) {
  const response = await httpClient.get<ApiResponse<InvoiceDocument>>(`/invoices/${id}/document`)

  return response.data.data
}
