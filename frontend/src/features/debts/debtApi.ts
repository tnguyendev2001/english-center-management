import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type { Invoice, InvoiceSearchParams } from '../invoices/invoiceTypes'

export async function getDebts(params: InvoiceSearchParams) {
  const response = await httpClient.get<ApiResponse<Invoice[]>>('/debts', {
    params,
  })

  return response.data
}
