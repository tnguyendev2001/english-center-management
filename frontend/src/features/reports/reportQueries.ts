import { useQuery } from '@tanstack/react-query'
import {
  getAttendanceReport,
  getDebtReport,
  getDebtReportSummary,
  getEnrollmentProgressReport,
  getInvoiceReport,
  getPaymentReport,
} from './reportApi'
import type {
  AttendanceReportParams,
  DebtReportParams,
  InvoiceReportParams,
  PaymentReportParams,
} from './reportTypes'

export const reportKeys = {
  all: ['reports'] as const,
  debtSummary: ['reports', 'debts', 'summary'] as const,
  debts: (params: DebtReportParams) => ['reports', 'debts', params] as const,
  payments: (params: PaymentReportParams) => ['reports', 'payments', params] as const,
  invoices: (params: InvoiceReportParams) => ['reports', 'invoices', params] as const,
  attendance: (params: AttendanceReportParams) => ['reports', 'attendance', params] as const,
  enrollmentProgress: ['reports', 'enrollment-progress'] as const,
}

export function useDebtReportSummary() {
  return useQuery({
    queryKey: reportKeys.debtSummary,
    queryFn: getDebtReportSummary,
  })
}

export function useDebtReport(params: DebtReportParams) {
  return useQuery({
    queryKey: reportKeys.debts(params),
    queryFn: () => getDebtReport(params),
  })
}

export function usePaymentReport(params: PaymentReportParams) {
  return useQuery({
    queryKey: reportKeys.payments(params),
    queryFn: () => getPaymentReport(params),
  })
}

export function useInvoiceReport(params: InvoiceReportParams) {
  return useQuery({
    queryKey: reportKeys.invoices(params),
    queryFn: () => getInvoiceReport(params),
  })
}

export function useAttendanceReport(params: AttendanceReportParams) {
  return useQuery({
    queryKey: reportKeys.attendance(params),
    queryFn: () => getAttendanceReport(params),
  })
}

export function useEnrollmentProgressReport() {
  return useQuery({
    queryKey: reportKeys.enrollmentProgress,
    queryFn: getEnrollmentProgressReport,
  })
}
