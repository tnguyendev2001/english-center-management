import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type {
  AccountBalance,
  AccountBalanceParams,
  AccountPayload,
  CashTransaction,
  CategoryBreakdownItem,
  CategoryBreakdownParams,
  CategoryPayload,
  ClosePeriodPayload,
  CompareDefaults,
  ComparePeriods,
  ComparePeriodsParams,
  ExportReportParams,
  FinanceConfig,
  FinanceOverview,
  FinancialAccount,
  FinancialPeriod,
  ManualExpensePayload,
  ManualIncomePayload,
  MonthlyReport,
  MonthlyReportParams,
  OverviewParams,
  PaymentReconciliation,
  PeriodSummary,
  ReconciliationParams,
  ReopenPeriodPayload,
  CancelPayload,
  StudentDebtItem,
  TransactionCategory,
  TransactionSearchParams,
  TransferPayload,
  TransferResponse,
  YearlyReport,
  YearlyReportParams,
} from './financeTypes'

export async function getFinanceAccounts() {
  const response = await httpClient.get<ApiResponse<FinancialAccount[]>>('/finance/accounts')
  return response.data.data
}

export async function createFinanceAccount(payload: AccountPayload) {
  const response = await httpClient.post<ApiResponse<FinancialAccount>>('/finance/accounts', payload)
  return response.data.data
}

export async function updateFinanceAccount(id: number, payload: AccountPayload) {
  const response = await httpClient.put<ApiResponse<FinancialAccount>>(`/finance/accounts/${id}`, payload)
  return response.data.data
}

export async function activateFinanceAccount(id: number) {
  const response = await httpClient.patch<ApiResponse<FinancialAccount>>(
    `/finance/accounts/${id}/activate`,
  )
  return response.data.data
}

export async function deactivateFinanceAccount(id: number) {
  const response = await httpClient.patch<ApiResponse<FinancialAccount>>(
    `/finance/accounts/${id}/deactivate`,
  )
  return response.data.data
}

export async function getAccountBalance(id: number, params?: AccountBalanceParams) {
  const response = await httpClient.get<ApiResponse<AccountBalance>>(
    `/finance/accounts/${id}/balance`,
    { params },
  )
  return response.data.data
}

export async function getFinanceCategories() {
  const response = await httpClient.get<ApiResponse<TransactionCategory[]>>('/finance/categories')
  return response.data.data
}

export async function createFinanceCategory(payload: CategoryPayload) {
  const response = await httpClient.post<ApiResponse<TransactionCategory>>(
    '/finance/categories',
    payload,
  )
  return response.data.data
}

export async function updateFinanceCategory(id: number, payload: CategoryPayload) {
  const response = await httpClient.put<ApiResponse<TransactionCategory>>(
    `/finance/categories/${id}`,
    payload,
  )
  return response.data.data
}

export async function activateFinanceCategory(id: number) {
  const response = await httpClient.patch<ApiResponse<TransactionCategory>>(
    `/finance/categories/${id}/activate`,
  )
  return response.data.data
}

export async function deactivateFinanceCategory(id: number) {
  const response = await httpClient.patch<ApiResponse<TransactionCategory>>(
    `/finance/categories/${id}/deactivate`,
  )
  return response.data.data
}

export async function getFinanceTransactions(params: TransactionSearchParams) {
  const response = await httpClient.get<ApiResponse<CashTransaction[]>>('/finance/transactions', {
    params,
  })
  return response.data
}

export async function getFinanceTransaction(id: number) {
  const response = await httpClient.get<ApiResponse<CashTransaction>>(`/finance/transactions/${id}`)
  return response.data.data
}

export async function postManualIncome(payload: ManualIncomePayload) {
  const response = await httpClient.post<ApiResponse<CashTransaction>>(
    '/finance/transactions/income',
    payload,
  )
  return response.data.data
}

export async function postManualExpense(payload: ManualExpensePayload) {
  const response = await httpClient.post<ApiResponse<CashTransaction>>(
    '/finance/transactions/expense',
    payload,
  )
  return response.data.data
}

export async function postTransfer(payload: TransferPayload) {
  const response = await httpClient.post<ApiResponse<TransferResponse>>(
    '/finance/transactions/transfer',
    payload,
  )
  return response.data.data
}

export async function cancelTransaction(id: number, payload: CancelPayload) {
  const response = await httpClient.post<ApiResponse<CashTransaction>>(
    `/finance/transactions/${id}/cancel`,
    payload,
  )
  return response.data.data
}

export async function getFinanceConfig() {
  const response = await httpClient.get<ApiResponse<FinanceConfig>>('/finance/config')
  return response.data.data
}

export async function getFinanceOverview(params: OverviewParams) {
  const response = await httpClient.get<ApiResponse<FinanceOverview>>('/finance/overview', {
    params,
  })
  return response.data.data
}

export async function getMonthlyReport(params: MonthlyReportParams) {
  const response = await httpClient.get<ApiResponse<MonthlyReport>>('/finance/reports/monthly', {
    params,
  })
  return response.data.data
}

export async function getYearlyReport(params: YearlyReportParams) {
  const response = await httpClient.get<ApiResponse<YearlyReport>>('/finance/reports/yearly', {
    params,
  })
  return response.data.data
}

export async function getPeriodSummary(params: MonthlyReportParams) {
  const response = await httpClient.get<ApiResponse<PeriodSummary>>(
    '/finance/reports/period-summary',
    { params },
  )
  return response.data.data
}

export async function getCategoryBreakdown(params: CategoryBreakdownParams) {
  const response = await httpClient.get<ApiResponse<CategoryBreakdownItem[]>>(
    '/finance/reports/category-breakdown',
    { params },
  )
  return response.data.data
}

export async function getAccountBalances(params?: AccountBalanceParams) {
  const response = await httpClient.get<ApiResponse<AccountBalance[]>>(
    '/finance/reports/account-balances',
    { params },
  )
  return response.data.data
}

export async function getStudentDebtDetails() {
  const response = await httpClient.get<ApiResponse<StudentDebtItem[]>>(
    '/finance/reports/student-debt',
  )
  return response.data.data
}

export async function getCompareDefaults() {
  const response = await httpClient.get<ApiResponse<CompareDefaults>>(
    '/finance/reports/compare/defaults',
  )
  return response.data.data
}

export async function getComparePeriods(params: ComparePeriodsParams) {
  const response = await httpClient.get<ApiResponse<ComparePeriods>>('/finance/reports/compare', {
    params: {
      currentYear: params.currentYear,
      currentMonth: params.currentMonth,
      comparisonYear: params.comparisonYear,
      comparisonMonth: params.comparisonMonth,
    },
  })
  return response.data.data
}

export async function getPaymentReconciliation(params?: ReconciliationParams) {
  const response = await httpClient.get<ApiResponse<PaymentReconciliation>>(
    '/finance/reconciliation/payments',
    { params },
  )
  return response.data.data
}

export async function repairPaymentLedger(payload: { paymentId: number }) {
  const response = await httpClient.post<
    ApiResponse<{
      paymentId: number
      paymentCode: string
      created: boolean
      message: string
    }>
  >('/finance/reconciliation/payments/repair', payload)
  return response.data.data
}

export async function exportFinanceReport(params?: ExportReportParams) {
  const response = await httpClient.get<Blob>('/finance/reports/export', {
    params,
    responseType: 'blob',
  })
  return response.data
}

export async function getFinancialPeriods() {
  const response = await httpClient.get<ApiResponse<FinancialPeriod[]>>('/finance/periods')
  return response.data.data
}

export async function getFinancialPeriod(year: number, month: number) {
  const response = await httpClient.get<ApiResponse<FinancialPeriod>>(
    `/finance/periods/${year}/${month}`,
  )
  return response.data.data
}

export async function closeFinancialPeriod(
  year: number,
  month: number,
  payload?: ClosePeriodPayload,
) {
  const response = await httpClient.post<ApiResponse<FinancialPeriod>>(
    `/finance/periods/${year}/${month}/close`,
    payload ?? {},
  )
  return response.data.data
}

export async function reopenFinancialPeriod(
  year: number,
  month: number,
  payload: ReopenPeriodPayload,
) {
  const response = await httpClient.post<ApiResponse<FinancialPeriod>>(
    `/finance/periods/${year}/${month}/reopen`,
    payload,
  )
  return response.data.data
}

export function downloadFinanceReportBlob(blob: Blob, filename = 'finance-report.xlsx') {
  const url = window.URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.URL.revokeObjectURL(url)
}
