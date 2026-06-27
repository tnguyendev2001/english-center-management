import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type { Invoice, InvoiceSearchParams } from './invoiceTypes'

export async function getInvoices(params: InvoiceSearchParams) {
  const response = await httpClient.get<ApiResponse<Invoice[]>>('/invoices', {
    params,
  })

  return response.data
}

export async function getInvoice(id: number) {
  const response = await httpClient.get<ApiResponse<Invoice>>(`/invoices/${id}`)

  return response.data.data
}
