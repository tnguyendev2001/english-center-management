import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type {
  CancelPaymentPayload,
  CreatePaymentPayload,
  Payment,
  PaymentSearchParams,
} from './paymentTypes'

export async function getPayments(params: PaymentSearchParams) {
  const response = await httpClient.get<ApiResponse<Payment[]>>('/payments', {
    params,
  })

  return response.data
}

export async function createPayment(invoiceId: number, payload: CreatePaymentPayload) {
  const response = await httpClient.post<ApiResponse<Payment>>(`/invoices/${invoiceId}/payments`, payload)

  return response.data.data
}

export async function cancelPayment(paymentId: number, payload: CancelPaymentPayload) {
  const response = await httpClient.post<ApiResponse<Payment>>(`/payments/${paymentId}/cancel`, payload)

  return response.data.data
}
