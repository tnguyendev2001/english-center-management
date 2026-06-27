import { useQuery } from '@tanstack/react-query'
import { getInvoice, getInvoices } from './invoiceApi'
import type { InvoiceSearchParams } from './invoiceTypes'

export const invoiceKeys = {
  all: ['invoices'] as const,
  list: (params: InvoiceSearchParams) => ['invoices', 'list', params] as const,
  detail: (id: number) => ['invoices', 'detail', id] as const,
}

export function useInvoices(params: InvoiceSearchParams) {
  return useQuery({
    queryKey: invoiceKeys.list(params),
    queryFn: () => getInvoices(params),
  })
}

export function useInvoiceDetail(id: number) {
  return useQuery({
    queryKey: invoiceKeys.detail(id),
    queryFn: () => getInvoice(id),
    enabled: Number.isFinite(id),
  })
}
