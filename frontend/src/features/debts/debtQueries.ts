import { useQuery } from '@tanstack/react-query'
import type { InvoiceSearchParams } from '../invoices/invoiceTypes'
import { getDebts } from './debtApi'

export const debtKeys = {
  all: ['debts'] as const,
  list: (params: InvoiceSearchParams) => ['debts', 'list', params] as const,
}

export function useDebts(params: InvoiceSearchParams) {
  return useQuery({
    queryKey: debtKeys.list(params),
    queryFn: () => getDebts(params),
  })
}
