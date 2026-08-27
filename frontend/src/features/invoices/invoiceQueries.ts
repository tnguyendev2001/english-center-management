import { useQuery } from '@tanstack/react-query'
import {
  getInvoice,
  getInvoices,
  getTuitionNotice,
  getTuitionOverview,
  getTuitionStudentSummaries,
} from './invoiceApi'
import type { StudentSummarySearchParams } from '../financial/financialSummaryTypes'
import type { InvoiceSearchParams } from './invoiceTypes'

export const invoiceKeys = {
  all: ['invoices'] as const,
  list: (params: InvoiceSearchParams) => ['invoices', 'list', params] as const,
  studentSummaries: (params?: StudentSummarySearchParams) => ['invoices', 'student-summaries', params] as const,
  overview: ['invoices', 'overview'] as const,
  detail: (id: number) => ['invoices', 'detail', id] as const,
  tuitionNotice: (id: number) => ['invoices', 'tuition-notice', id] as const,
}

export function useInvoices(params: InvoiceSearchParams, enabled = true) {
  return useQuery({
    queryKey: invoiceKeys.list(params),
    queryFn: () => getInvoices(params),
    enabled,
  })
}

export function useTuitionStudentSummaries(params: StudentSummarySearchParams, enabled = true) {
  return useQuery({
    queryKey: invoiceKeys.studentSummaries(params),
    queryFn: () => getTuitionStudentSummaries(params),
    enabled,
  })
}

export function useTuitionOverview() {
  return useQuery({
    queryKey: invoiceKeys.overview,
    queryFn: getTuitionOverview,
  })
}

export function useInvoiceDetail(id: number) {
  return useQuery({
    queryKey: invoiceKeys.detail(id),
    queryFn: () => getInvoice(id),
    enabled: Number.isFinite(id),
  })
}

export function useTuitionNotice(invoiceId: number, enabled = true) {
  return useQuery({
    queryKey: invoiceKeys.tuitionNotice(invoiceId),
    queryFn: () => getTuitionNotice(invoiceId),
    enabled: enabled && Number.isFinite(invoiceId),
  })
}
