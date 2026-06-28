import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type { Invoice } from '../invoices/invoiceTypes'
import type {
  AttendanceReport,
  AttendanceReportParams,
  DebtReportItem,
  DebtReportParams,
  DebtReportSummary,
  EnrollmentProgressReportItem,
  InvoiceReportParams,
  PaymentReport,
  PaymentReportParams,
  RevenueReport,
  RevenueReportParams,
} from './reportTypes'

export async function getDebtReportSummary() {
  const response = await httpClient.get<ApiResponse<DebtReportSummary>>('/reports/debts/summary')
  return response.data.data
}

export async function getDebtReport(params: DebtReportParams) {
  const response = await httpClient.get<ApiResponse<DebtReportItem[]>>('/reports/debts', { params })
  return response.data
}

export async function getPaymentReport(params: PaymentReportParams) {
  const response = await httpClient.get<ApiResponse<PaymentReport>>('/reports/payments', { params })
  return response.data.data
}

export async function getRevenueReport(params: RevenueReportParams) {
  const response = await httpClient.get<ApiResponse<RevenueReport>>('/reports/revenue', { params })
  return response.data.data
}

export async function getInvoiceReport(params: InvoiceReportParams) {
  const response = await httpClient.get<ApiResponse<Invoice[]>>('/reports/invoices', { params })
  return response.data
}

export async function getAttendanceReport(params: AttendanceReportParams) {
  const response = await httpClient.get<ApiResponse<AttendanceReport>>('/reports/attendance', { params })
  return response.data.data
}

export async function getEnrollmentProgressReport() {
  const response = await httpClient.get<ApiResponse<EnrollmentProgressReportItem[]>>(
    '/reports/enrollment-progress',
  )
  return response.data.data
}
