import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { dashboardKeys } from '../dashboard/dashboardQueries'
import { debtKeys } from '../debts/debtQueries'
import { invoiceKeys } from '../invoices/invoiceQueries'
import { paymentKeys } from '../payments/paymentQueries'
import {
  activateFinanceAccount,
  activateFinanceCategory,
  closeFinancialPeriod,
  createFinanceAccount,
  createFinanceCategory,
  deactivateFinanceAccount,
  deactivateFinanceCategory,
  exportFinanceReport,
  getAccountBalances,
  getCompareDefaults,
  getComparePeriods,
  getFinanceAccounts,
  getFinanceCategories,
  getFinanceConfig,
  getFinanceOverview,
  getFinanceTransaction,
  getFinanceTransactions,
  getFinancialPeriods,
  getMonthlyReport,
  getPaymentReconciliation,
  getStudentDebtDetails,
  getYearlyReport,
  postManualExpense,
  postManualIncome,
  postTransfer,
  reopenFinancialPeriod,
  repairPaymentLedger,
  cancelTransaction,
  updateFinanceAccount,
  updateFinanceCategory,
} from './financeApi'
import type {
  AccountBalanceParams,
  AccountPayload,
  CategoryPayload,
  ClosePeriodPayload,
  ComparePeriodsParams,
  ExportReportParams,
  ManualExpensePayload,
  ManualIncomePayload,
  MonthlyReportParams,
  OverviewParams,
  ReconciliationParams,
  ReopenPeriodPayload,
  CancelPayload,
  TransactionSearchParams,
  TransferPayload,
  YearlyReportParams,
} from './financeTypes'

export const financeKeys = {
  all: ['finance'] as const,
  accounts: () => ['finance', 'accounts'] as const,
  categories: () => ['finance', 'categories'] as const,
  transactions: (params: TransactionSearchParams) =>
    ['finance', 'transactions', params] as const,
  transaction: (id: number) => ['finance', 'transaction', id] as const,
  config: () => ['finance', 'config'] as const,
  overview: (params?: OverviewParams) => ['finance', 'overview', params] as const,
  monthlyReport: (params: MonthlyReportParams) =>
    ['finance', 'reports', 'monthly', params] as const,
  yearlyReport: (params: YearlyReportParams) =>
    ['finance', 'reports', 'yearly', params] as const,
  compareDefaults: () => ['finance', 'reports', 'compare', 'defaults'] as const,
  compare: (params: ComparePeriodsParams) =>
    [
      'finance-comparison',
      params.mode,
      params.currentYear,
      params.currentMonth,
      params.comparisonYear,
      params.comparisonMonth,
    ] as const,
  accountBalances: (params?: AccountBalanceParams) =>
    ['finance', 'reports', 'account-balances', params] as const,
  studentDebt: () => ['finance', 'reports', 'student-debt'] as const,
  reconciliation: (params?: ReconciliationParams) =>
    ['finance', 'reconciliation', params] as const,
  periods: () => ['finance', 'periods'] as const,
}

function invalidateFinanceQueries(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: financeKeys.all })
}

function invalidateRelatedMoneyQueries(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: paymentKeys.all })
  queryClient.invalidateQueries({ queryKey: invoiceKeys.all })
  queryClient.invalidateQueries({ queryKey: debtKeys.all })
  queryClient.invalidateQueries({ queryKey: dashboardKeys.all })
}

export function useFinanceAccounts(enabled = true) {
  return useQuery({
    queryKey: financeKeys.accounts(),
    queryFn: getFinanceAccounts,
    enabled,
  })
}

export function useFinanceCategories(enabled = true) {
  return useQuery({
    queryKey: financeKeys.categories(),
    queryFn: getFinanceCategories,
    enabled,
  })
}

export function useFinanceTransactions(params: TransactionSearchParams, enabled = true) {
  return useQuery({
    queryKey: financeKeys.transactions(params),
    queryFn: () => getFinanceTransactions(params),
    enabled,
  })
}

export function useFinanceTransaction(id?: number, enabled = true) {
  return useQuery({
    queryKey: financeKeys.transaction(id ?? 0),
    queryFn: () => getFinanceTransaction(id!),
    enabled: enabled && Boolean(id),
  })
}

export function useFinanceConfig(enabled = true) {
  return useQuery({
    queryKey: financeKeys.config(),
    queryFn: getFinanceConfig,
    enabled,
    staleTime: 5 * 60 * 1000,
  })
}

export function useFinanceOverview(params: OverviewParams, enabled = true) {
  return useQuery({
    queryKey: financeKeys.overview(params),
    queryFn: () => getFinanceOverview(params),
    enabled,
  })
}

export function useMonthlyReport(params: MonthlyReportParams, enabled = true) {
  return useQuery({
    queryKey: financeKeys.monthlyReport(params),
    queryFn: () => getMonthlyReport(params),
    enabled,
  })
}

export function useYearlyReport(params: YearlyReportParams, enabled = true) {
  return useQuery({
    queryKey: financeKeys.yearlyReport(params),
    queryFn: () => getYearlyReport(params),
    enabled,
  })
}

export function useCompareDefaults(enabled = true) {
  return useQuery({
    queryKey: financeKeys.compareDefaults(),
    queryFn: getCompareDefaults,
    enabled,
  })
}

export function useComparePeriods(params: ComparePeriodsParams, enabled = true) {
  return useQuery({
    queryKey: financeKeys.compare(params),
    queryFn: () => getComparePeriods(params),
    enabled,
    placeholderData: undefined,
  })
}

export function useAccountBalances(params?: AccountBalanceParams, enabled = true) {
  return useQuery({
    queryKey: financeKeys.accountBalances(params),
    queryFn: () => getAccountBalances(params),
    enabled,
  })
}

export function useStudentDebtDetails(enabled = true) {
  return useQuery({
    queryKey: financeKeys.studentDebt(),
    queryFn: getStudentDebtDetails,
    enabled,
  })
}

export function usePaymentReconciliation(params?: ReconciliationParams, enabled = true) {
  return useQuery({
    queryKey: financeKeys.reconciliation(params),
    queryFn: () => getPaymentReconciliation(params),
    enabled,
  })
}

export function useFinancialPeriods(enabled = true) {
  return useQuery({
    queryKey: financeKeys.periods(),
    queryFn: getFinancialPeriods,
    enabled,
  })
}

export function useCreateFinanceAccount() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: AccountPayload) => createFinanceAccount(payload),
    onSuccess: () => invalidateFinanceQueries(queryClient),
  })
}

export function useUpdateFinanceAccount() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: AccountPayload }) =>
      updateFinanceAccount(id, payload),
    onSuccess: () => invalidateFinanceQueries(queryClient),
  })
}

export function useActivateFinanceAccount() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => activateFinanceAccount(id),
    onSuccess: () => invalidateFinanceQueries(queryClient),
  })
}

export function useDeactivateFinanceAccount() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deactivateFinanceAccount(id),
    onSuccess: () => invalidateFinanceQueries(queryClient),
  })
}

export function useCreateFinanceCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CategoryPayload) => createFinanceCategory(payload),
    onSuccess: () => invalidateFinanceQueries(queryClient),
  })
}

export function useUpdateFinanceCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: CategoryPayload }) =>
      updateFinanceCategory(id, payload),
    onSuccess: () => invalidateFinanceQueries(queryClient),
  })
}

export function useActivateFinanceCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => activateFinanceCategory(id),
    onSuccess: () => invalidateFinanceQueries(queryClient),
  })
}

export function useDeactivateFinanceCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deactivateFinanceCategory(id),
    onSuccess: () => invalidateFinanceQueries(queryClient),
  })
}

export function usePostManualIncome() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: ManualIncomePayload) => postManualIncome(payload),
    onSuccess: () => {
      invalidateFinanceQueries(queryClient)
      invalidateRelatedMoneyQueries(queryClient)
    },
  })
}

export function usePostManualExpense() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: ManualExpensePayload) => postManualExpense(payload),
    onSuccess: () => {
      invalidateFinanceQueries(queryClient)
      invalidateRelatedMoneyQueries(queryClient)
    },
  })
}

export function usePostTransfer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: TransferPayload) => postTransfer(payload),
    onSuccess: () => {
      invalidateFinanceQueries(queryClient)
      invalidateRelatedMoneyQueries(queryClient)
    },
  })
}

export function useCancelTransaction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: CancelPayload }) =>
      cancelTransaction(id, payload),
    onSuccess: () => {
      invalidateFinanceQueries(queryClient)
      invalidateRelatedMoneyQueries(queryClient)
    },
  })
}

export function useCloseFinancialPeriod() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      year,
      month,
      payload,
    }: {
      year: number
      month: number
      payload?: ClosePeriodPayload
    }) => closeFinancialPeriod(year, month, payload),
    onSuccess: () => invalidateFinanceQueries(queryClient),
  })
}

export function useReopenFinancialPeriod() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      year,
      month,
      payload,
    }: {
      year: number
      month: number
      payload: ReopenPeriodPayload
    }) => reopenFinancialPeriod(year, month, payload),
    onSuccess: () => invalidateFinanceQueries(queryClient),
  })
}

export function useExportFinanceReport() {
  return useMutation({
    mutationFn: (params?: ExportReportParams) => exportFinanceReport(params),
  })
}

export function useRepairPaymentLedger() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: { paymentId: number }) => repairPaymentLedger(payload),
    onSuccess: () => {
      invalidateFinanceQueries(queryClient)
      invalidateRelatedMoneyQueries(queryClient)
    },
  })
}
