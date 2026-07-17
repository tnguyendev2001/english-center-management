import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { StudentSummarySearchParams } from '../financial/financialSummaryTypes'
import { dashboardKeys } from '../dashboard/dashboardQueries'
import { debtKeys } from '../debts/debtQueries'
import { invoiceKeys } from '../invoices/invoiceQueries'
import { reportKeys } from '../reports/reportQueries'
import { revenueKeys } from '../revenue/revenueQueries'
import { cancelPayment, createPayment, getPayments, getPaymentStudentSummaries } from './paymentApi'
import type { CancelPaymentPayload, CreatePaymentPayload, PaymentSearchParams } from './paymentTypes'

export const paymentKeys = {
  all: ['payments'] as const,
  list: (params: PaymentSearchParams) => ['payments', 'list', params] as const,
  studentSummaries: (params?: StudentSummarySearchParams) => ['payments', 'student-summaries', params] as const,
}

export function usePayments(params: PaymentSearchParams) {
  return useQuery({
    queryKey: paymentKeys.list(params),
    queryFn: () => getPayments(params),
  })
}

export function usePaymentStudentSummaries(params?: StudentSummarySearchParams, enabled = true) {
  return useQuery({
    queryKey: paymentKeys.studentSummaries(params),
    queryFn: () => getPaymentStudentSummaries(params),
    enabled,
  })
}

export function useCreatePayment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ invoiceId, payload }: { invoiceId: number; payload: CreatePaymentPayload }) =>
      createPayment(invoiceId, payload),
    onSuccess: () => {
      invalidateMoneyQueries(queryClient)
    },
  })
}

export function useCancelPayment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ paymentId, payload }: { paymentId: number; payload: CancelPaymentPayload }) =>
      cancelPayment(paymentId, payload),
    onSuccess: () => {
      invalidateMoneyQueries(queryClient)
    },
  })
}

function invalidateMoneyQueries(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: invoiceKeys.all })
  queryClient.invalidateQueries({ queryKey: paymentKeys.all })
  queryClient.invalidateQueries({ queryKey: debtKeys.all })
  queryClient.invalidateQueries({ queryKey: revenueKeys.all })
  queryClient.invalidateQueries({ queryKey: dashboardKeys.all })
  queryClient.invalidateQueries({ queryKey: reportKeys.all })
}
