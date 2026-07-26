export type FinancialAccountType = 'CASH' | 'BANK' | 'OTHER'
export type CategoryDirection = 'INCOME' | 'EXPENSE'
export type TransactionDirection = 'IN' | 'OUT'
export type TransactionSourceType =
  | 'PAYMENT'
  | 'MANUAL_INCOME'
  | 'MANUAL_EXPENSE'
  | 'TRANSFER'
  | 'OPENING_BALANCE'
  | 'ADJUSTMENT'
  | 'REVERSAL'
  | 'REFUND'
export type TransactionStatus = 'DRAFT' | 'POSTED' | 'CANCELED'
export type FinancialPeriodStatus = 'OPEN' | 'CLOSED'
export type ReconciliationStatus = 'MATCHED' | 'MISMATCHED'
export type FinanceScope = 'MONTH' | 'ALL'
export type ChartGrouping = 'DAY' | 'MONTH'

export interface FinancialAccount {
  id: number
  code: string
  name: string
  type: FinancialAccountType
  openingBalance: number
  openingBalanceDate: string
  currentBalance: number
  negativeBalance: boolean
  active: boolean
  note?: string | null
  displayOrder: number
  lastTransactionDate?: string | null
  createdAt: string
  updatedAt: string
}

export interface TransactionCategory {
  id: number
  code: string
  name: string
  direction: CategoryDirection
  parentId?: number | null
  active: boolean
  systemCategory: boolean
  displayOrder: number
  createdAt: string
  updatedAt: string
}

export interface CashTransaction {
  id: number
  transactionCode: string
  transactionDate: string
  accountId: number
  accountCode: string
  accountName: string
  direction: TransactionDirection
  categoryId?: number | null
  categoryCode?: string | null
  categoryName?: string | null
  amount: number
  incomeAmount: number
  expenseAmount: number
  balanceAfter: number
  status: TransactionStatus
  sourceType: TransactionSourceType
  sourceId?: number | null
  referenceNo?: string | null
  payerOrPayee?: string | null
  description: string
  attachmentReference?: string | null
  transferGroupId?: string | null
  originalTransactionId?: number | null
  originalTransactionCode?: string | null
  reversalTransactionId?: number | null
  reversalTransactionCode?: string | null
  cancelReason?: string | null
  postedAt?: string | null
  canceledAt?: string | null
  canceledBy?: string | null
  createdAt: string
  createdBy?: string | null
  updatedAt: string
  updatedBy?: string | null
  warnings?: string[] | null
}

export interface TransferResponse {
  transferGroupId: string
  outflow: CashTransaction
  inflow: CashTransaction
  warnings?: string[] | null
}

export interface AccountBalance {
  accountId: number
  accountCode: string
  accountName: string
  type: FinancialAccountType
  openingBalance: number
  openingBalanceDate: string
  inflow: number
  outflow: number
  balance: number
  negativeBalance: boolean
  active: boolean
  asOfDate: string
}

export interface CategoryBreakdownItem {
  categoryId: number
  categoryCode: string
  categoryName: string
  direction: CategoryDirection
  amount: number
}

export interface DailyCashFlowItem {
  date: string
  income: number
  expense: number
  netCashFlow: number
}

export interface StudentDebtItem {
  studentId: number
  studentCode: string
  studentName: string
  classroomId: number
  classroomName: string
  remainingDebt: number
  debtInvoiceCount: number
}

export interface PeriodSummary {
  year: number
  month: number
  fromDate: string
  toDate: string
  periodStatus: string
  openingBalance: number
  totalIncome: number
  tuitionIncome: number
  otherIncome: number
  totalExpense: number
  netCashFlow: number
  closingBalance: number
  formulaReconciles: boolean
  openingDebt: number
  newInvoicesAmount: number
  paymentsCollected: number
  closingDebt: number
  fromSnapshot: boolean
  warnings?: string[] | null
}

export interface FinanceConfig {
  financeStartDate: string
  financeStartYear: number
  financeStartMonth: number
  businessDate: string
  minSelectableMonth: string
  maxSelectableMonth: string
  latestTransactionDate?: string | null
  latestPaymentDate?: string | null
  latestFinancialDataDate?: string | null
  hasDataBeyondBusinessMonth: boolean
  /** @deprecated use businessDate */
  today?: string
}

export interface FinanceOverview {
  scope: FinanceScope
  periodStart: string
  periodEnd: string
  financeStartDate: string
  businessDate: string
  minSelectableMonth: string
  maxSelectableMonth: string
  latestTransactionDate?: string | null
  latestPaymentDate?: string | null
  hasDataBeyondBusinessMonth: boolean
  year?: number | null
  month?: number | null
  totalBalance: number
  cashBalance: number
  bankBalance: number
  totalIncome: number
  totalExpense: number
  netCashFlow: number
  outstandingDebt: number
  hasNegativeBalance: boolean
  reconciliationMatched: boolean
  chartGrouping: ChartGrouping
  warnings?: string[] | null
  accountBalances: AccountBalance[]
  expenseBreakdown: CategoryBreakdownItem[]
  incomeBreakdown: CategoryBreakdownItem[]
  incomeExpenseChart: DailyCashFlowItem[]
  recentTransactions: CashTransaction[]
  topDebtStudents: StudentDebtItem[]
}

export interface MonthlyReport {
  summary: PeriodSummary
  expenseBreakdown: CategoryBreakdownItem[]
  incomeBreakdown: CategoryBreakdownItem[]
  accountBalances: AccountBalance[]
  dailyCashFlow: DailyCashFlowItem[]
  transactions: CashTransaction[]
  expenseByCode: Record<string, number>
}

export interface YearlyMonthRow {
  month: number
  income: number
  expense: number
  netCashFlow: number
  closingBalance: number
  closingStudentDebt: number
}

export interface YearlyReport {
  year: number
  months: YearlyMonthRow[]
  totalIncome: number
  totalExpense: number
  annualNetCashFlow: number
  highestIncomeMonth?: number | null
  highestExpenseMonth?: number | null
  largestExpenseCategoryCode?: string | null
  largestExpenseCategoryName?: string | null
  averageMonthlyIncome: number
  averageMonthlyExpense: number
  yearEndBalance: number
  yearEndStudentDebt: number
  incomeChangeVsPreviousYear?: number | null
  expenseChangeVsPreviousYear?: number | null
  netCashFlowChangeVsPreviousYear?: number | null
  expenseBreakdown: CategoryBreakdownItem[]
}

export type ComparisonMode = 'PREVIOUS_MONTH' | 'SAME_MONTH_PREVIOUS_YEAR' | 'CUSTOM'
export type ComparisonTrend = 'INCREASE' | 'DECREASE' | 'UNCHANGED'

export interface ComparePeriodInfo {
  year: number
  month: number
  label: string
  hasData: boolean
  hasTransactions: boolean
  hasFinancialState: boolean
}

export interface ComparePeriodMetrics {
  income: number
  expense: number
  netCashFlow: number
  closingBalance: number
  outstandingDebt: number
}

export interface CompareMetricDifference {
  amount: number
  percentage?: number | null
  trend: ComparisonTrend
  percentageUnavailable: boolean
}

export interface ComparePeriods {
  currentPeriod: ComparePeriodInfo
  comparisonPeriod: ComparePeriodInfo
  current: ComparePeriodMetrics
  comparison: ComparePeriodMetrics
  income: CompareMetricDifference
  expense: CompareMetricDifference
  netCashFlow: CompareMetricDifference
  closingBalance: CompareMetricDifference
  outstandingDebt: CompareMetricDifference
  message?: string | null
  suggestion?: string | null
  nearestEarlierYearWithData?: number | null
  nearestEarlierMonthWithData?: number | null
}

export interface CompareDefaults {
  currentYear: number
  currentMonth: number
  comparisonYear: number
  comparisonMonth: number
  monthsWithData: string[]
}

export interface PaymentReconciliationMismatch {
  mismatchType: string
  paymentId?: number | null
  paymentCode?: string | null
  cashTransactionId?: number | null
  transactionCode?: string | null
  detail: string
}

export interface PaymentReconciliation {
  totalValidPayments: number
  totalPaymentAmount: number
  totalTuitionLedgerAmount: number
  difference: number
  matchedCount: number
  missingLedgerCount: number
  orphanLedgerCount: number
  duplicateCount: number
  amountMismatchCount: number
  cancellationMismatchCount: number
  accountMismatchCount: number
  dateMismatchCount: number
  status: ReconciliationStatus
  mismatches: PaymentReconciliationMismatch[]
}

export interface FinancialPeriod {
  id: number
  year: number
  month: number
  status: FinancialPeriodStatus
  closedAt?: string | null
  closedBy?: string | null
  reopenedAt?: string | null
  reopenedBy?: string | null
  reopenReason?: string | null
  note?: string | null
  openingBalanceSnapshot?: number | null
  totalIncomeSnapshot?: number | null
  totalExpenseSnapshot?: number | null
  closingBalanceSnapshot?: number | null
  outstandingDebtSnapshot?: number | null
  createdAt: string
  updatedAt: string
}

export interface ManualIncomePayload {
  transactionDate: string
  accountId: number
  categoryId: number
  amount: number
  payerOrPayee?: string | null
  referenceNo?: string | null
  description: string
  attachmentReference?: string | null
}

export type ManualExpensePayload = ManualIncomePayload

export interface TransferPayload {
  transactionDate: string
  sourceAccountId: number
  destinationAccountId: number
  amount: number
  description: string
  referenceNo?: string | null
}

export interface CancelPayload {
  reason: string
  canceledBy?: string | null
}

export interface AccountPayload {
  code: string
  name: string
  type: FinancialAccountType
  openingBalance: number
  openingBalanceDate: string
  note?: string | null
  displayOrder?: number | null
}

export interface CategoryPayload {
  code: string
  name: string
  direction: CategoryDirection
  parentId?: number | null
  displayOrder?: number | null
}

export interface ClosePeriodPayload {
  note?: string | null
  overrideReconciliationMismatch?: boolean
  closedBy?: string | null
}

export interface ReopenPeriodPayload {
  reason: string
  reopenedBy?: string | null
}

export interface TransactionSearchParams {
  fromDate?: string
  toDate?: string
  year?: number
  month?: number
  accountId?: number
  categoryId?: number
  direction?: TransactionDirection
  sourceType?: TransactionSourceType
  status?: TransactionStatus
  keyword?: string
  page: number
  size: number
}

export interface OverviewParams {
  scope: FinanceScope
  year?: number
  month?: number
}

export interface MonthlyReportParams {
  year: number
  month: number
}

export interface YearlyReportParams {
  fromYear: number
  toYear?: number
}

export interface ComparePeriodsParams {
  mode: ComparisonMode
  currentYear: number
  currentMonth: number
  comparisonYear: number
  comparisonMonth: number
}

export interface ReconciliationParams {
  fromDate?: string
  toDate?: string
}

export interface ExportReportParams {
  scope?: FinanceScope
  year?: number
  month?: number
}

export interface AccountBalanceParams {
  asOfDate?: string
}

export interface CategoryBreakdownParams {
  fromDate: string
  toDate: string
  direction: CategoryDirection
}
