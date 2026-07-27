import { useQuery } from '@tanstack/react-query'
import {
  getInvoice,
  getInvoiceDocument,
  getInvoiceSummary,
  getInvoices,
  getTuitionStudentSummaries,
} from './invoiceApi'
import type { StudentSummarySearchParams } from '../financial/financialSummaryTypes'
import type { InvoiceSearchParams } from './invoiceTypes'

export const invoiceKeys = {
  all: ['invoices'] as const,
  list: (params: InvoiceSearchParams) => ['invoices', 'list', params] as const,
  summary: (classroomId?: number) => ['invoices', 'summary', classroomId] as const,
  studentSummaries: (params?: StudentSummarySearchParams) => ['invoices', 'student-summaries', params] as const,
  detail: (id: number) => ['invoices', 'detail', id] as const,
  document: (id: number) => ['invoices', 'document', id] as const,
}

export function useInvoices(params: InvoiceSearchParams, enabled = true) {
  return useQuery({
    queryKey: invoiceKeys.list(params),
    queryFn: () => getInvoices(params),
    enabled,
  })
}

export function useInvoiceSummary(classroomId?: number, enabled = true) {
  return useQuery({
    queryKey: invoiceKeys.summary(classroomId),
    queryFn: () => getInvoiceSummary(classroomId),
    enabled,
  })
}

export function useTuitionStudentSummaries(params?: StudentSummarySearchParams, enabled = true) {
  return useQuery({
    queryKey: invoiceKeys.studentSummaries(params),
    queryFn: () => getTuitionStudentSummaries(params),
    enabled,
  })
}

export function useInvoiceDetail(id?: number, enabled = true) {
  return useQuery({
    queryKey: invoiceKeys.detail(id ?? 0),
    queryFn: () => getInvoice(id!),
    enabled: enabled && id != null && Number.isFinite(id),
  })
}

export function useInvoiceDocument(id?: number, enabled = true) {
  return useQuery({
    queryKey: invoiceKeys.document(id ?? 0),
    queryFn: () => getInvoiceDocument(id!),
    enabled: enabled && id != null && Number.isFinite(id),
  })
}
