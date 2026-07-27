import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Dropdown,
  Input,
  Row,
  Segmented,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import type { MenuProps } from 'antd'
import type { TablePaginationConfig } from 'antd/es/table'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import dayjs, { type Dayjs } from 'dayjs'
import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { MoneyText } from '../../../components/common/MoneyText'
import { formatStudentLabel } from '../../../components/common/studentDisplay'
import { AccountFormModal } from '../components/AccountFormModal'
import { CategoryFormModal } from '../components/CategoryFormModal'
import { ClosePeriodModal } from '../components/ClosePeriodModal'
import {
  CreateTransactionDrawer,
  type TransactionPrefill,
} from '../components/CreateTransactionDrawer'
import { ReopenPeriodModal } from '../components/ReopenPeriodModal'
import { CancelTransactionModal } from '../components/CancelTransactionModal'
import { TransactionDetailDrawer } from '../components/TransactionDetailDrawer'
import { downloadFinanceReportBlob } from '../financeApi'
import { listActiveTemplates } from '../financePreferences'
import {
  useActivateFinanceAccount,
  useActivateFinanceCategory,
  useCloseFinancialPeriod,
  useCompareDefaults,
  useComparePeriods,
  useCreateFinanceAccount,
  useCreateFinanceCategory,
  useDeactivateFinanceAccount,
  useDeactivateFinanceCategory,
  useExportFinanceReport,
  useFinanceAccounts,
  useFinanceCategories,
  useFinanceConfig,
  useFinanceOverview,
  useFinanceTransactions,
  useFinancialPeriods,
  useMonthlyReport,
  usePaymentReconciliation,
  usePostManualExpense,
  usePostManualIncome,
  usePostTransfer,
  useReopenFinancialPeriod,
  useRepairPaymentLedger,
  useCancelTransaction,
  useUpdateFinanceAccount,
  useUpdateFinanceCategory,
  useYearlyReport,
} from '../financeQueries'
import type {
  AccountPayload,
  CashTransaction,
  CategoryPayload,
  ClosePeriodPayload,
  CompareMetricDifference,
  ComparisonMode,
  ComparisonTrend,
  FinanceScope,
  FinancialAccount,
  FinancialAccountType,
  FinancialPeriod,
  ManualExpensePayload,
  ManualIncomePayload,
  ReopenPeriodPayload,
  CancelPayload,
  TransactionCategory,
  TransactionDirection,
  TransactionSourceType,
  TransactionStatus,
  TransferPayload,
} from '../financeTypes'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

const PIE_COLORS = ['#1677ff', '#52c41a', '#faad14', '#eb2f96', '#722ed1', '#13c2c2', '#fa541c', '#2f54eb']

const accountTypeLabels: Record<FinancialAccountType, string> = {
  CASH: 'Tiền mặt',
  BANK: 'Ngân hàng',
  OTHER: 'Khác',
}

const statusLabels: Record<TransactionStatus, string> = {
  DRAFT: 'Nháp',
  POSTED: 'Đã ghi sổ',
  CANCELED: 'Đã hủy',
}

const sourceLabels: Record<TransactionSourceType, string> = {
  PAYMENT: 'Thanh toán học phí',
  MANUAL_INCOME: 'Thu thủ công',
  MANUAL_EXPENSE: 'Chi thủ công',
  TRANSFER: 'Chuyển tiền',
  OPENING_BALANCE: 'Số dư đầu kỳ',
  ADJUSTMENT: 'Điều chỉnh',
  REVERSAL: 'Hủy (legacy)',
  REFUND: 'Hoàn tiền',
}

const directionLabels: Record<TransactionDirection, string> = {
  IN: 'Thu',
  OUT: 'Chi',
}

const periodStatusLabels: Record<string, string> = {
  OPEN: 'Đang mở',
  CLOSED: 'Đã chốt',
}

const mismatchTypeLabels: Record<string, string> = {
  MISSING_LEDGER: 'Thiếu bút toán',
  ORPHAN_LEDGER: 'Bút toán mồ côi',
  DUPLICATE: 'Trùng lặp',
  AMOUNT_MISMATCH: 'Lệch số tiền',
  CANCELLATION_MISMATCH: 'Lệch hủy',
  ACCOUNT_MISMATCH: 'Lệch tài khoản',
  DATE_MISMATCH: 'Lệch ngày',
}

function showErrorMessage(error: unknown) {
  if (isAxiosError(error)) {
    message.error(error.response?.data?.message ?? 'Có lỗi xảy ra')
    return
  }
  message.error('Có lỗi xảy ra')
}

function moneyColor(value: number, positive = '#389e0d', negative = '#cf1322') {
  if (value > 0) return positive
  if (value < 0) return negative
  return undefined
}

function NetCashFlowText({ value }: { value: number }) {
  return (
    <span style={{ color: moneyColor(value) }}>
      <MoneyText value={value} />
    </span>
  )
}

function BalanceText({ value }: { value: number }) {
  return (
    <span style={{ color: value < 0 ? '#cf1322' : undefined }}>
      <MoneyText value={value} />
    </span>
  )
}

function trendLabel(trend: ComparisonTrend, metric: 'income' | 'expense' | 'net' | 'balance' | 'debt') {
  if (trend === 'UNCHANGED') {
    return 'Không đổi'
  }
  if (metric === 'expense' || metric === 'debt') {
    return trend === 'INCREASE' ? 'Tăng' : 'Giảm'
  }
  return trend === 'INCREASE' ? 'Tăng' : 'Giảm'
}

function trendColor(trend: ComparisonTrend, metric: 'income' | 'expense' | 'net' | 'balance' | 'debt') {
  if (trend === 'UNCHANGED') {
    return undefined
  }
  if (metric === 'expense' || metric === 'debt') {
    return trend === 'INCREASE' ? '#cf1322' : '#389e0d'
  }
  if (metric === 'income' || metric === 'net' || metric === 'balance') {
    return trend === 'INCREASE' ? '#389e0d' : '#cf1322'
  }
  return undefined
}

function CompareMetricCard({
  title,
  currentLabel,
  comparisonLabel,
  currentValue,
  comparisonValue,
  difference,
  metric,
  loading,
}: {
  title: string
  currentLabel: string
  comparisonLabel: string
  currentValue: number
  comparisonValue: number
  difference: CompareMetricDifference
  metric: 'income' | 'expense' | 'net' | 'balance' | 'debt'
  loading: boolean
}) {
  const color = trendColor(difference.trend, metric)
  return (
    <Card loading={loading} title={title}>
      <Space direction="vertical" size={4} style={{ width: '100%' }}>
        <Text>
          {currentLabel}:{' '}
          {metric === 'debt' ? (
            <span style={{ color: currentValue > 0 ? '#d46b08' : undefined }}>
              <MoneyText value={currentValue} />
            </span>
          ) : metric === 'balance' || metric === 'net' ? (
            <BalanceText value={currentValue} />
          ) : (
            <MoneyText value={currentValue} />
          )}
        </Text>
        <Text>
          {comparisonLabel}: <MoneyText value={comparisonValue} />
        </Text>
        <Text style={{ color }}>
          {trendLabel(difference.trend, metric)}:{' '}
          <MoneyText value={Math.abs(difference.amount)} />
          {difference.percentageUnavailable
            ? ' (Không thể tính phần trăm)'
            : ` (${difference.percentage != null && difference.percentage > 0 ? '+' : ''}${difference.percentage ?? 0}%)`}
        </Text>
      </Space>
    </Card>
  )
}

export function FinancePage() {
  const now = dayjs()
  const [searchParams, setSearchParams] = useSearchParams()
  const tabFromUrl = searchParams.get('tab')
  const balanceFilter = searchParams.get('balance') === 'NEGATIVE' ? 'NEGATIVE' : undefined
  const reconStatusFilter =
    searchParams.get('reconStatus') === 'MISMATCHED' || searchParams.get('reconStatus') === 'MATCHED'
      ? (searchParams.get('reconStatus') as 'MISMATCHED' | 'MATCHED')
      : undefined
  const initialTab =
    tabFromUrl &&
    ['overview', 'ledger', 'accounts', 'categories', 'reconciliation', 'periods', 'reports'].includes(tabFromUrl)
      ? tabFromUrl
      : 'overview'
  const [activeTab, setActiveTab] = useState(initialTab)
  const [overviewScope, setOverviewScope] = useState<FinanceScope>('MONTH')
  const [overviewMonth, setOverviewMonth] = useState<Dayjs>(now)

  const [txPage, setTxPage] = useState(0)
  const [txSize, setTxSize] = useState(20)
  const [txKeyword, setTxKeyword] = useState('')
  const [txRange, setTxRange] = useState<[Dayjs, Dayjs] | null>([
    now.startOf('month'),
    now.endOf('month'),
  ])
  const [txAccountId, setTxAccountId] = useState<number>()
  const [txCategoryId, setTxCategoryId] = useState<number>()
  const [txDirection, setTxDirection] = useState<TransactionDirection>()
  const [txSourceType, setTxSourceType] = useState<TransactionSourceType>()
  const [txStatus, setTxStatus] = useState<TransactionStatus>()

  const [createTxOpen, setCreateTxOpen] = useState(false)
  const [createTxPrefill, setCreateTxPrefill] = useState<TransactionPrefill | null>(null)
  const [txFiltersOpen, setTxFiltersOpen] = useState(false)
  const [categorySearch, setCategorySearch] = useState('')
  const [cancelOpen, setCancelOpen] = useState(false)
  const [detailOpen, setDetailOpen] = useState(false)
  const [selectedTx, setSelectedTx] = useState<CashTransaction>()

  const [accountModalOpen, setAccountModalOpen] = useState(false)
  const [editingAccount, setEditingAccount] = useState<FinancialAccount>()

  const [categoryModalOpen, setCategoryModalOpen] = useState(false)
  const [editingCategory, setEditingCategory] = useState<TransactionCategory>()

  const [reportMonth, setReportMonth] = useState<Dayjs>(now)
  const [reportYear, setReportYear] = useState<Dayjs>(now)
  const [compareMode, setCompareMode] = useState<ComparisonMode>('PREVIOUS_MONTH')
  const [compareCurrent, setCompareCurrent] = useState<Dayjs>(now)
  const [compareAgainst, setCompareAgainst] = useState<Dayjs>(now.subtract(1, 'month'))
  const [compareDefaultsReady, setCompareDefaultsReady] = useState(false)
  const [reportSubTab, setReportSubTab] = useState('monthly')

  const [reconRange, setReconRange] = useState<[Dayjs, Dayjs] | null>([
    now.startOf('month'),
    now.endOf('month'),
  ])

  const [closeOpen, setCloseOpen] = useState(false)
  const [reopenOpen, setReopenOpen] = useState(false)
  const [selectedPeriod, setSelectedPeriod] = useState<FinancialPeriod>()
  const [periodPicker, setPeriodPicker] = useState<Dayjs>(now.subtract(1, 'month'))

  const overviewParams = useMemo(
    () =>
      overviewScope === 'ALL'
        ? { scope: 'ALL' as const }
        : {
            scope: 'MONTH' as const,
            year: overviewMonth.year(),
            month: overviewMonth.month() + 1,
          },
    [overviewMonth, overviewScope],
  )

  const txParams = useMemo(
    () => ({
      fromDate: txRange?.[0]?.format('YYYY-MM-DD'),
      toDate: txRange?.[1]?.format('YYYY-MM-DD'),
      accountId: txAccountId,
      categoryId: txCategoryId,
      direction: txDirection,
      sourceType: txSourceType,
      status: txStatus,
      keyword: txKeyword || undefined,
      page: txPage,
      size: txSize,
    }),
    [
      txAccountId,
      txCategoryId,
      txDirection,
      txKeyword,
      txPage,
      txRange,
      txSize,
      txSourceType,
      txStatus,
    ],
  )

  const monthlyParams = useMemo(
    () => ({ year: reportMonth.year(), month: reportMonth.month() + 1 }),
    [reportMonth],
  )

  const yearlyParams = useMemo(() => ({ fromYear: reportYear.year() }), [reportYear])

  const compareParams = useMemo(
    () => ({
      mode: compareMode,
      currentYear: compareCurrent.year(),
      currentMonth: compareCurrent.month() + 1,
      comparisonYear: compareAgainst.year(),
      comparisonMonth: compareAgainst.month() + 1,
    }),
    [compareAgainst, compareCurrent, compareMode],
  )

  const reconParams = useMemo(
    () => ({
      fromDate: reconRange?.[0]?.format('YYYY-MM-DD'),
      toDate: reconRange?.[1]?.format('YYYY-MM-DD'),
    }),
    [reconRange],
  )

  const configQuery = useFinanceConfig()
  const accountsQuery = useFinanceAccounts()
  const categoriesQuery = useFinanceCategories()
  const overviewQuery = useFinanceOverview(overviewParams, Boolean(configQuery.data))

  const minSelectableMonth = useMemo(() => {
    if (!configQuery.data?.minSelectableMonth) {
      return null
    }
    return dayjs(`${configQuery.data.minSelectableMonth}-01`).startOf('month')
  }, [configQuery.data?.minSelectableMonth])

  const maxSelectableMonth = useMemo(() => {
    if (!configQuery.data?.maxSelectableMonth) {
      return null
    }
    return dayjs(`${configQuery.data.maxSelectableMonth}-01`).startOf('month')
  }, [configQuery.data?.maxSelectableMonth])

  const businessMonth = useMemo(() => {
    const businessDate = configQuery.data?.businessDate ?? configQuery.data?.today
    if (!businessDate) {
      return null
    }
    return dayjs(businessDate).startOf('month')
  }, [configQuery.data?.businessDate, configQuery.data?.today])

  const disableFinanceMonth = (current: Dayjs | null) => {
    if (!current) {
      return false
    }
    if (minSelectableMonth && current.isBefore(minSelectableMonth, 'month')) {
      return true
    }
    if (maxSelectableMonth && current.isAfter(maxSelectableMonth, 'month')) {
      return true
    }
    return false
  }

  const disableFinanceYear = (current: Dayjs | null) => {
    if (!current) {
      return false
    }
    if (minSelectableMonth && current.year() < minSelectableMonth.year()) {
      return true
    }
    if (maxSelectableMonth && current.year() > maxSelectableMonth.year()) {
      return true
    }
    return false
  }

  const disableClosePeriodMonth = (current: Dayjs | null) => {
    if (disableFinanceMonth(current)) {
      return true
    }
    // Current unfinished business month cannot be closed yet.
    return Boolean(businessMonth && current && !current.isBefore(businessMonth, 'month'))
  }

  function isCloseablePeriod(year: number, month: number) {
    if (!businessMonth) {
      return false
    }
    return dayjs(`${year}-${String(month).padStart(2, '0')}-01`).isBefore(businessMonth, 'month')
  }

  useEffect(() => {
    if (!minSelectableMonth || !maxSelectableMonth) {
      return
    }
    const clampMonth = (value: Dayjs) => {
      if (value.isBefore(minSelectableMonth, 'month')) {
        return minSelectableMonth
      }
      if (value.isAfter(maxSelectableMonth, 'month')) {
        return maxSelectableMonth
      }
      return value
    }
    setOverviewMonth((current) => clampMonth(current))
    setReportMonth((current) => clampMonth(current))
    setCompareCurrent((current) => clampMonth(current))
    setCompareAgainst((current) => clampMonth(current))
    setPeriodPicker((current) => {
      const preferred = businessMonth?.subtract(1, 'month') ?? minSelectableMonth
      if (current.isBefore(minSelectableMonth, 'month') || current.isAfter(maxSelectableMonth, 'month')) {
        return clampMonth(preferred)
      }
      if (businessMonth && !current.isBefore(businessMonth, 'month')) {
        return clampMonth(preferred)
      }
      return current
    })
    if (reportYear.year() < minSelectableMonth.year()) {
      setReportYear(minSelectableMonth)
    }
    if (maxSelectableMonth && reportYear.year() > maxSelectableMonth.year()) {
      setReportYear(maxSelectableMonth)
    }
  }, [minSelectableMonth, maxSelectableMonth, businessMonth])
  const transactionsQuery = useFinanceTransactions(txParams, activeTab === 'ledger')
  const monthlyQuery = useMonthlyReport(monthlyParams, activeTab === 'reports' && reportSubTab === 'monthly')
  const yearlyQuery = useYearlyReport(yearlyParams, activeTab === 'reports' && reportSubTab === 'yearly')
  const compareDefaultsQuery = useCompareDefaults(
    activeTab === 'reports' && reportSubTab === 'compare',
  )
  const compareQuery = useComparePeriods(
    compareParams,
    activeTab === 'reports' &&
      reportSubTab === 'compare' &&
      compareDefaultsReady &&
      !(
        compareParams.currentYear === compareParams.comparisonYear &&
        compareParams.currentMonth === compareParams.comparisonMonth
      ),
  )
  const reconciliationQuery = usePaymentReconciliation(reconParams, activeTab === 'reconciliation')
  const periodsQuery = useFinancialPeriods(activeTab === 'periods' || activeTab === 'overview')

  useEffect(() => {
    if (!compareDefaultsQuery.data || compareDefaultsReady) {
      return
    }
    const defaults = compareDefaultsQuery.data
    setCompareCurrent(
      dayjs(`${defaults.currentYear}-${String(defaults.currentMonth).padStart(2, '0')}-01`),
    )
    setCompareAgainst(
      dayjs(
        `${defaults.comparisonYear}-${String(defaults.comparisonMonth).padStart(2, '0')}-01`,
      ),
    )
    setCompareMode('PREVIOUS_MONTH')
    setCompareDefaultsReady(true)
  }, [compareDefaultsQuery.data, compareDefaultsReady])

  function applyCompareMode(mode: ComparisonMode, current: Dayjs = compareCurrent) {
    setCompareMode(mode)
    if (mode === 'PREVIOUS_MONTH') {
      setCompareAgainst(current.subtract(1, 'month'))
      return
    }
    if (mode === 'SAME_MONTH_PREVIOUS_YEAR') {
      setCompareAgainst(current.subtract(1, 'year'))
    }
  }

  function handleCompareCurrentChange(value: Dayjs | null) {
    if (!value) {
      return
    }
    setCompareCurrent(value)
    if (compareMode !== 'CUSTOM') {
      applyCompareMode(compareMode, value)
    }
  }

  const postIncome = usePostManualIncome()
  const postExpense = usePostManualExpense()
  const postTransfer = usePostTransfer()
  const cancelTx = useCancelTransaction()
  const createAccount = useCreateFinanceAccount()
  const updateAccount = useUpdateFinanceAccount()
  const activateAccount = useActivateFinanceAccount()
  const deactivateAccount = useDeactivateFinanceAccount()
  const createCategory = useCreateFinanceCategory()
  const updateCategory = useUpdateFinanceCategory()
  const activateCategory = useActivateFinanceCategory()
  const deactivateCategory = useDeactivateFinanceCategory()
  const closePeriod = useCloseFinancialPeriod()
  const reopenPeriod = useReopenFinancialPeriod()
  const exportReport = useExportFinanceReport()
  const repairPaymentLedger = useRepairPaymentLedger()

  const accounts = useMemo(() => {
    const all = accountsQuery.data ?? []
    if (balanceFilter === 'NEGATIVE') {
      return all.filter((account) => account.currentBalance < 0)
    }
    return all
  }, [accountsQuery.data, balanceFilter])

  function changeFinanceTab(nextTab: string) {
    setActiveTab(nextTab)
    const next = new URLSearchParams(searchParams)
    if (nextTab === 'overview') {
      next.delete('tab')
    } else {
      next.set('tab', nextTab)
    }
    if (nextTab !== 'accounts') {
      next.delete('balance')
    }
    if (nextTab !== 'reconciliation') {
      next.delete('reconStatus')
    }
    setSearchParams(next, { replace: true })
  }

  function clearBalanceFilter() {
    const next = new URLSearchParams(searchParams)
    next.delete('balance')
    setSearchParams(next, { replace: true })
  }
  const categories = categoriesQuery.data ?? []
  const overviewLoading = overviewQuery.isLoading || overviewQuery.isFetching
  const overview = overviewLoading ? undefined : overviewQuery.data

  const incomeCategories = useMemo(() => {
    const keyword = categorySearch.trim().toLowerCase()
    return categories
      .filter((item) => item.direction === 'INCOME')
      .filter(
        (item) =>
          !keyword ||
          item.name.toLowerCase().includes(keyword) ||
          item.code.toLowerCase().includes(keyword),
      )
      .sort((a, b) => {
        if (a.systemCategory !== b.systemCategory) {
          return a.systemCategory ? -1 : 1
        }
        return a.displayOrder - b.displayOrder || a.name.localeCompare(b.name, 'vi')
      })
  }, [categories, categorySearch])

  const expenseCategories = useMemo(() => {
    const keyword = categorySearch.trim().toLowerCase()
    return categories
      .filter((item) => item.direction === 'EXPENSE')
      .filter(
        (item) =>
          !keyword ||
          item.name.toLowerCase().includes(keyword) ||
          item.code.toLowerCase().includes(keyword),
      )
      .sort((a, b) => {
        if (a.systemCategory !== b.systemCategory) {
          return a.systemCategory ? -1 : 1
        }
        return a.displayOrder - b.displayOrder || a.name.localeCompare(b.name, 'vi')
      })
  }, [categories, categorySearch])

  const advancedFilterCount = useMemo(() => {
    let count = 0
    if (txAccountId != null) count += 1
    if (txCategoryId != null) count += 1
    if (txDirection) count += 1
    if (txSourceType) count += 1
    if (txStatus) count += 1
    return count
  }, [txAccountId, txCategoryId, txDirection, txSourceType, txStatus])

  const recentManualTemplates = useMemo(() => {
    const rows = transactionsQuery.data?.data ?? []
    return rows
      .filter(
        (row) =>
          row.status === 'POSTED' &&
          (row.sourceType === 'MANUAL_EXPENSE' || row.sourceType === 'MANUAL_INCOME'),
      )
      .slice(0, 5)
  }, [transactionsQuery.data?.data])

  const isSettingsTab = ['accounts', 'categories', 'reconciliation', 'periods'].includes(activeTab)

  function openCreateTransaction(prefill?: TransactionPrefill | null) {
    setCreateTxPrefill(prefill ?? null)
    setCreateTxOpen(true)
  }

  function openRepeatTransaction(row: CashTransaction) {
    if (row.sourceType !== 'MANUAL_EXPENSE' && row.sourceType !== 'MANUAL_INCOME') {
      return
    }
    openCreateTransaction({
      type: row.sourceType === 'MANUAL_EXPENSE' ? 'EXPENSE' : 'INCOME',
      transactionDate: dayjs().format('YYYY-MM-DD'),
      accountId: row.accountId,
      categoryId: row.categoryId ?? undefined,
      amount: row.amount,
      description: row.description,
    })
  }

  const barChartData = useMemo(
    () =>
      (overview?.incomeExpenseChart ?? []).map((item) => ({
        date:
          overview?.chartGrouping === 'MONTH'
            ? dayjs(item.date).format('MM/YYYY')
            : dayjs(item.date).format('DD/MM'),
        Thu: Number(item.income),
        Chi: Number(item.expense),
      })),
    [overview?.chartGrouping, overview?.incomeExpenseChart],
  )

  const expensePieData = useMemo(
    () =>
      (overview?.expenseBreakdown ?? []).map((item) => ({
        name: item.categoryName,
        value: Number(item.amount),
      })),
    [overview?.expenseBreakdown],
  )

  const incomePieData = useMemo(
    () =>
      (overview?.incomeBreakdown ?? []).map((item) => ({
        name: item.categoryName,
        value: Number(item.amount),
      })),
    [overview?.incomeBreakdown],
  )

  const transactionColumns: ColumnsType<CashTransaction> = [
    {
      title: 'Ngày',
      dataIndex: 'transactionDate',
      key: 'transactionDate',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
    },
    {
      title: 'Mã GD',
      dataIndex: 'transactionCode',
      key: 'transactionCode',
    },
    {
      title: 'Nội dung',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: 'Danh mục',
      key: 'category',
      render: (_, row) => row.categoryName ?? '-',
    },
    {
      title: 'Tài khoản',
      key: 'account',
      render: (_, row) => row.accountName,
    },
    {
      title: 'Nguồn',
      dataIndex: 'sourceType',
      key: 'sourceType',
      render: (value: TransactionSourceType, row) =>
        value === 'PAYMENT' && row.sourceId ? (
          <Link to="/payments">{sourceLabels[value]}</Link>
        ) : (
          sourceLabels[value] ?? value
        ),
    },
    {
      title: 'Thu',
      dataIndex: 'incomeAmount',
      key: 'incomeAmount',
      align: 'right',
      render: (value: number) => (value > 0 ? <MoneyText value={value} /> : '-'),
    },
    {
      title: 'Chi',
      dataIndex: 'expenseAmount',
      key: 'expenseAmount',
      align: 'right',
      render: (value: number) => (value > 0 ? <MoneyText value={value} /> : '-'),
    },
    {
      title: 'Số dư sau GD',
      dataIndex: 'balanceAfter',
      key: 'balanceAfter',
      align: 'right',
      render: (value: number) => <BalanceText value={value} />,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: TransactionStatus) => (
        <Tag color={status === 'POSTED' ? 'green' : status === 'CANCELED' ? 'red' : 'default'}>
          {statusLabels[status]}
        </Tag>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, row) => {
        const canCancel = row.status === 'POSTED' && row.sourceType !== 'PAYMENT'
        const isPayment = row.sourceType === 'PAYMENT'
        const canRepeat =
          row.sourceType === 'MANUAL_EXPENSE' || row.sourceType === 'MANUAL_INCOME'

        return (
          <Space>
            <Button
              type="link"
              onClick={() => {
                setSelectedTx(row)
                setDetailOpen(true)
              }}
            >
              Xem
            </Button>
            {canRepeat ? (
              <Button type="link" onClick={() => openRepeatTransaction(row)}>
                Nhập lại
              </Button>
            ) : null}
            {isPayment && row.status === 'POSTED' ? (
              <Tooltip title="Vui lòng hủy thanh toán tại chức năng Thanh toán.">
                <Button type="link" disabled>
                  Hủy giao dịch
                </Button>
              </Tooltip>
            ) : canCancel ? (
              <Button
                type="link"
                danger
                onClick={() => {
                  setSelectedTx(row)
                  setCancelOpen(true)
                }}
              >
                Hủy giao dịch
              </Button>
            ) : null}
          </Space>
        )
      },
    },
  ]

  const accountColumns: ColumnsType<FinancialAccount> = [
    { title: 'Mã', dataIndex: 'code', key: 'code' },
    { title: 'Tên', dataIndex: 'name', key: 'name' },
    {
      title: 'Loại',
      dataIndex: 'type',
      key: 'type',
      render: (type: FinancialAccountType) => accountTypeLabels[type],
    },
    {
      title: 'Số dư hiện tại',
      dataIndex: 'currentBalance',
      key: 'currentBalance',
      align: 'right',
      render: (value: number) => <BalanceText value={value} />,
    },
    {
      title: 'GD gần nhất',
      dataIndex: 'lastTransactionDate',
      key: 'lastTransactionDate',
      render: (value?: string | null) => (value ? dayjs(value).format('DD/MM/YYYY') : '-'),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'active',
      key: 'active',
      render: (active: boolean) => (
        <Tag color={active ? 'green' : 'default'}>{active ? 'Đang dùng' : 'Ngừng'}</Tag>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, account) => (
        <Space>
          <Button
            type="link"
            onClick={() => {
              setEditingAccount(account)
              setAccountModalOpen(true)
            }}
          >
            Sửa
          </Button>
          {account.active ? (
            <Button
              type="link"
              danger
              loading={deactivateAccount.isPending}
              onClick={() =>
                deactivateAccount.mutate(account.id, {
                  onSuccess: () => message.success('Đã ngừng tài khoản'),
                  onError: showErrorMessage,
                })
              }
            >
              Ngừng
            </Button>
          ) : (
            <Button
              type="link"
              loading={activateAccount.isPending}
              onClick={() =>
                activateAccount.mutate(account.id, {
                  onSuccess: () => message.success('Đã kích hoạt tài khoản'),
                  onError: showErrorMessage,
                })
              }
            >
              Kích hoạt
            </Button>
          )}
        </Space>
      ),
    },
  ]

  function categoryColumns(): ColumnsType<TransactionCategory> {
    return [
      { title: 'Tên', dataIndex: 'name', key: 'name' },
      { title: 'Mã', dataIndex: 'code', key: 'code', width: 140 },
      {
        title: 'Loại',
        key: 'kind',
        width: 110,
        render: (_, row) => (row.systemCategory ? <Tag>Hệ thống</Tag> : <Tag color="blue">Tùy chỉnh</Tag>),
      },
      {
        title: 'Trạng thái',
        dataIndex: 'active',
        key: 'active',
        width: 120,
        render: (active: boolean) => (
          <Tag color={active ? 'green' : 'default'}>{active ? 'Đang dùng' : 'Ngừng'}</Tag>
        ),
      },
      {
        title: 'Thao tác',
        key: 'actions',
        render: (_, category) => (
          <Space>
            <Button
              type="link"
              onClick={() => {
                setEditingCategory(category)
                setCategoryModalOpen(true)
              }}
            >
              Sửa
            </Button>
            {category.active ? (
              <Button
                type="link"
                danger
                disabled={category.systemCategory}
                loading={deactivateCategory.isPending}
                onClick={() =>
                  deactivateCategory.mutate(category.id, {
                    onSuccess: () => message.success('Đã ngừng danh mục'),
                    onError: showErrorMessage,
                  })
                }
              >
                Ngừng
              </Button>
            ) : (
              <Button
                type="link"
                loading={activateCategory.isPending}
                onClick={() =>
                  activateCategory.mutate(category.id, {
                    onSuccess: () => message.success('Đã kích hoạt danh mục'),
                    onError: showErrorMessage,
                  })
                }
              >
                Kích hoạt
              </Button>
            )}
          </Space>
        ),
      },
    ]
  }

  const periodColumns: ColumnsType<FinancialPeriod> = [
    {
      title: 'Kỳ',
      key: 'period',
      render: (_, row) => `${String(row.month).padStart(2, '0')}/${row.year}`,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'CLOSED' ? 'default' : 'green'}>
          {periodStatusLabels[status] ?? status}
        </Tag>
      ),
    },
    {
      title: 'Thu (snapshot)',
      dataIndex: 'totalIncomeSnapshot',
      key: 'totalIncomeSnapshot',
      align: 'right',
      render: (value?: number | null) => <MoneyText value={value} />,
    },
    {
      title: 'Chi (snapshot)',
      dataIndex: 'totalExpenseSnapshot',
      key: 'totalExpenseSnapshot',
      align: 'right',
      render: (value?: number | null) => <MoneyText value={value} />,
    },
    {
      title: 'Số dư cuối',
      dataIndex: 'closingBalanceSnapshot',
      key: 'closingBalanceSnapshot',
      align: 'right',
      render: (value?: number | null) =>
        value == null ? '-' : <BalanceText value={value} />,
    },
    {
      title: 'Công nợ',
      dataIndex: 'outstandingDebtSnapshot',
      key: 'outstandingDebtSnapshot',
      align: 'right',
      render: (value?: number | null) => (
        <span style={{ color: value && value > 0 ? '#d46b08' : undefined }}>
          <MoneyText value={value} />
        </span>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, period) =>
        period.status === 'OPEN' ? (
          <Tooltip
            title={
              isCloseablePeriod(period.year, period.month)
                ? undefined
                : 'Không thể khóa tháng hiện tại trước khi tháng kết thúc.'
            }
          >
            <Button
              type="link"
              disabled={!isCloseablePeriod(period.year, period.month)}
              onClick={() => {
                setSelectedPeriod(period)
                setCloseOpen(true)
              }}
            >
              Chốt kỳ
            </Button>
          </Tooltip>
        ) : (
          <Button
            type="link"
            danger
            onClick={() => {
              setSelectedPeriod(period)
              setReopenOpen(true)
            }}
          >
            Mở lại
          </Button>
        ),
    },
  ]

  function handleTxTableChange(pagination: TablePaginationConfig) {
    setTxPage((pagination.current ?? 1) - 1)
    setTxSize(pagination.pageSize ?? 20)
  }

  function handleIncomeSubmit(payload: ManualIncomePayload) {
    postIncome.mutate(payload, {
      onSuccess: () => {
        message.success('Đã lưu khoản thu')
        setCreateTxOpen(false)
        setCreateTxPrefill(null)
      },
      onError: showErrorMessage,
    })
  }

  function handleExpenseSubmit(payload: ManualExpensePayload) {
    postExpense.mutate(payload, {
      onSuccess: () => {
        message.success('Đã lưu khoản chi')
        setCreateTxOpen(false)
        setCreateTxPrefill(null)
      },
      onError: showErrorMessage,
    })
  }

  function handleTransferSubmit(payload: TransferPayload) {
    postTransfer.mutate(payload, {
      onSuccess: () => {
        message.success('Đã chuyển tiền')
        setCreateTxOpen(false)
        setCreateTxPrefill(null)
      },
      onError: showErrorMessage,
    })
  }

  async function handleInlineCreateCategory(payload: CategoryPayload) {
    const created = await createCategory.mutateAsync(payload)
    message.success('Đã thêm danh mục')
    return created
  }

  function handleCancelSubmit(payload: CancelPayload) {
    if (!selectedTx) return
    cancelTx.mutate(
      { id: selectedTx.id, payload },
      {
        onSuccess: () => {
          message.success('Đã hủy giao dịch')
          setCancelOpen(false)
          setSelectedTx(undefined)
        },
        onError: showErrorMessage,
      },
    )
  }

  function handleAccountSubmit(payload: AccountPayload) {
    if (editingAccount) {
      updateAccount.mutate(
        { id: editingAccount.id, payload },
        {
          onSuccess: () => {
            message.success('Đã cập nhật tài khoản')
            setAccountModalOpen(false)
            setEditingAccount(undefined)
          },
          onError: showErrorMessage,
        },
      )
      return
    }

    createAccount.mutate(payload, {
      onSuccess: () => {
        message.success('Đã thêm tài khoản')
        setAccountModalOpen(false)
      },
      onError: showErrorMessage,
    })
  }

  function handleCategorySubmit(payload: CategoryPayload) {
    if (editingCategory) {
      updateCategory.mutate(
        { id: editingCategory.id, payload },
        {
          onSuccess: () => {
            message.success('Đã cập nhật danh mục')
            setCategoryModalOpen(false)
            setEditingCategory(undefined)
          },
          onError: showErrorMessage,
        },
      )
      return
    }

    createCategory.mutate(payload, {
      onSuccess: () => {
        message.success('Đã thêm danh mục')
        setCategoryModalOpen(false)
      },
      onError: showErrorMessage,
    })
  }

  function handleClosePeriod(payload: ClosePeriodPayload) {
    if (!selectedPeriod) return
    closePeriod.mutate(
      { year: selectedPeriod.year, month: selectedPeriod.month, payload },
      {
        onSuccess: () => {
          message.success('Đã chốt kỳ')
          setCloseOpen(false)
          setSelectedPeriod(undefined)
        },
        onError: showErrorMessage,
      },
    )
  }

  function handleReopenPeriod(payload: ReopenPeriodPayload) {
    if (!selectedPeriod) return
    reopenPeriod.mutate(
      { year: selectedPeriod.year, month: selectedPeriod.month, payload },
      {
        onSuccess: () => {
          message.success('Đã mở lại kỳ')
          setReopenOpen(false)
          setSelectedPeriod(undefined)
        },
        onError: showErrorMessage,
      },
    )
  }

  function handleExport() {
    exportReport.mutate(
      {
        scope: 'MONTH',
        year: reportMonth.year(),
        month: reportMonth.month() + 1,
      },
      {
        onSuccess: (blob) => {
          downloadFinanceReportBlob(
            blob,
            `bao-cao-thu-chi-${reportMonth.format('YYYY-MM')}.xlsx`,
          )
          message.success('Đã tải báo cáo Excel')
        },
        onError: showErrorMessage,
      },
    )
  }

  const isAllScope = overviewScope === 'ALL'
  const incomeLabel = isAllScope ? 'Tổng thu' : 'Thu tháng'
  const expenseLabel = isAllScope ? 'Tổng chi' : 'Chi tháng'
  const netLabel = isAllScope ? 'Dòng tiền ròng toàn kỳ' : 'Dòng tiền ròng tháng'
  const chartTitle = isAllScope ? 'Thu / Chi theo tháng' : 'Thu / Chi theo ngày'

  const overviewTab = (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Space wrap align="center">
        <Segmented
          value={overviewScope}
          onChange={(value) => setOverviewScope(value as FinanceScope)}
          options={[
            { label: 'Tất cả', value: 'ALL' },
            { label: 'Theo tháng', value: 'MONTH' },
          ]}
        />
        {overviewScope === 'MONTH' ? (
          <>
            <Text>Tháng:</Text>
            <DatePicker
              picker="month"
              format="MM/YYYY"
              value={overviewMonth}
              onChange={(value) => value && setOverviewMonth(value)}
              allowClear={false}
              disabledDate={disableFinanceMonth}
            />
          </>
        ) : (
          <Text type="secondary">Từ ngày bắt đầu hoạt động đến hiện tại</Text>
        )}
      </Space>

      <Spin spinning={overviewLoading || configQuery.isLoading}>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          {configQuery.data?.hasDataBeyondBusinessMonth ? (
            <Alert
              type="warning"
              showIcon
              message="Có giao dịch có ngày lớn hơn ngày hiện tại. Vui lòng kiểm tra lại."
              description={
                configQuery.data.maxSelectableMonth
                  ? `Tháng mới nhất có dữ liệu: ${dayjs(`${configQuery.data.maxSelectableMonth}-01`).format('MM/YYYY')}`
                  : undefined
              }
            />
          ) : null}
          {overview?.hasNegativeBalance ? (
            <Alert type="error" showIcon message="Có tài khoản đang âm số dư" />
          ) : null}
          {overview && !overview.reconciliationMatched ? (
            <Alert
              type="warning"
              showIcon
              message="Đối soát thanh toán chưa khớp"
              description="Vui lòng kiểm tra tab Đối soát để xem chi tiết lệch."
            />
          ) : null}
          {(overview?.warnings ?? []).map((warning) => (
            <Alert key={warning} type="warning" showIcon message={warning} />
          ))}

          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="Tổng số dư"
                  valueRender={() => <BalanceText value={overview?.totalBalance ?? 0} />}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title={incomeLabel}
                  valueRender={() => <MoneyText value={overview?.totalIncome} />}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title={expenseLabel}
                  valueRender={() => <MoneyText value={overview?.totalExpense} />}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title={netLabel}
                  valueRender={() => <NetCashFlowText value={overview?.netCashFlow ?? 0} />}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="Công nợ học viên"
                  valueRender={() => (
                    <span style={{ color: (overview?.outstandingDebt ?? 0) > 0 ? '#d46b08' : undefined }}>
                      <MoneyText value={overview?.outstandingDebt} />
                    </span>
                  )}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="Tiền mặt"
                  valueRender={() => <BalanceText value={overview?.cashBalance ?? 0} />}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="Ngân hàng"
                  valueRender={() => <BalanceText value={overview?.bankBalance ?? 0} />}
                />
              </Card>
            </Col>
          </Row>

          <Row gutter={[16, 16]}>
            <Col xs={24} lg={14}>
              <Card title={chartTitle} loading={overviewLoading}>
                <div style={{ width: '100%', height: 300 }}>
                  <ResponsiveContainer>
                    <BarChart data={barChartData}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="date" />
                      <YAxis />
                      <RechartsTooltip />
                      <Legend />
                      <Bar dataKey="Thu" fill="#52c41a" />
                      <Bar dataKey="Chi" fill="#ff4d4f" />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </Card>
            </Col>
            <Col xs={24} lg={10}>
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                <Card title="Cơ cấu thu" loading={overviewLoading}>
                  <div style={{ width: '100%', height: 140 }}>
                    {incomePieData.length ? (
                      <ResponsiveContainer>
                        <PieChart>
                          <Pie data={incomePieData} dataKey="value" nameKey="name" outerRadius={55} label>
                            {incomePieData.map((_, index) => (
                              <Cell key={index} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                            ))}
                          </Pie>
                          <RechartsTooltip />
                        </PieChart>
                      </ResponsiveContainer>
                    ) : (
                      <Text type="secondary">Chưa có dữ liệu thu trong kỳ</Text>
                    )}
                  </div>
                </Card>
                <Card title="Cơ cấu chi" loading={overviewLoading}>
                  <div style={{ width: '100%', height: 140 }}>
                    {expensePieData.length ? (
                      <ResponsiveContainer>
                        <PieChart>
                          <Pie data={expensePieData} dataKey="value" nameKey="name" outerRadius={55} label>
                            {expensePieData.map((_, index) => (
                              <Cell key={index} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                            ))}
                          </Pie>
                          <RechartsTooltip />
                        </PieChart>
                      </ResponsiveContainer>
                    ) : (
                      <Text type="secondary">Chưa có dữ liệu chi trong kỳ</Text>
                    )}
                  </div>
                </Card>
              </Space>
            </Col>
          </Row>

          <Row gutter={[16, 16]}>
            <Col xs={24} lg={12}>
              <Card title="Số dư tài khoản" loading={overviewLoading}>
                <Table
                  rowKey="accountId"
                  size="small"
                  pagination={false}
                  dataSource={overview?.accountBalances ?? []}
                  columns={[
                    { title: 'Tài khoản', dataIndex: 'accountName', key: 'accountName' },
                    {
                      title: 'Loại',
                      dataIndex: 'type',
                      key: 'type',
                      render: (type: FinancialAccountType) => accountTypeLabels[type],
                    },
                    {
                      title: 'Số dư',
                      dataIndex: 'balance',
                      key: 'balance',
                      align: 'right',
                      render: (value: number) => <BalanceText value={value} />,
                    },
                  ]}
                />
              </Card>
            </Col>
            <Col xs={24} lg={12}>
              <Card title="Top công nợ" loading={overviewLoading}>
                <Table
                  rowKey={(row) => `${row.studentId}-${row.classroomId}`}
                  size="small"
                  pagination={false}
                  dataSource={overview?.topDebtStudents ?? []}
                  columns={[
                    {
                      title: 'Học viên',
                      key: 'student',
                      render: (_, row) => formatStudentLabel(row.studentCode, row.studentName),
                    },
                    { title: 'Lớp', dataIndex: 'classroomName', key: 'classroomName' },
                    {
                      title: 'Còn nợ',
                      dataIndex: 'remainingDebt',
                      key: 'remainingDebt',
                      align: 'right',
                      render: (value: number) => (
                        <span style={{ color: '#d46b08' }}>
                          <MoneyText value={value} />
                        </span>
                      ),
                    },
                  ]}
                />
              </Card>
            </Col>
          </Row>

          <Card title="Giao dịch gần đây" loading={overviewLoading}>
            <Table
              rowKey="id"
              size="small"
              pagination={false}
              dataSource={overview?.recentTransactions ?? []}
              columns={transactionColumns.filter((column) => column.key !== 'actions')}
            />
          </Card>
        </Space>
      </Spin>
    </Space>
  )

  const savedTemplates = listActiveTemplates()
  const templateMenuItems: MenuProps['items'] = [
    ...savedTemplates.map((template) => ({
      key: `tpl-${template.id}`,
      label: template.name,
      onClick: () =>
        openCreateTransaction({
          type: template.type,
          accountId: template.accountId,
          destinationAccountId: template.destinationAccountId,
          categoryId: template.categoryId,
          amount: template.amount,
          description: template.description,
        }),
    })),
    ...(recentManualTemplates.length
      ? [
          { type: 'divider' as const },
          {
            key: 'recent-header',
            label: 'Gần đây',
            disabled: true,
          },
          ...recentManualTemplates.map((row) => ({
            key: `recent-${row.id}`,
            label: `${row.description} (${dayjs(row.transactionDate).format('DD/MM')})`,
            onClick: () => openRepeatTransaction(row),
          })),
        ]
      : []),
  ]

  const ledgerTab = (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Space wrap>
        <Button type="primary" onClick={() => openCreateTransaction()}>
          + Tạo giao dịch
        </Button>
        <Dropdown menu={{ items: templateMenuItems }} disabled={!templateMenuItems.length}>
          <Button>Giao dịch thường dùng</Button>
        </Dropdown>
      </Space>

      <Space wrap>
        <RangePicker
          format="DD/MM/YYYY"
          value={txRange}
          onChange={(value) => {
            setTxRange(value as [Dayjs, Dayjs] | null)
            setTxPage(0)
          }}
        />
        <Input.Search
          allowClear
          placeholder="Tìm mã GD, nội dung..."
          style={{ width: 260 }}
          onSearch={(value) => {
            setTxKeyword(value.trim())
            setTxPage(0)
          }}
        />
        <Badge count={advancedFilterCount} size="small">
          <Button
            type={txFiltersOpen || advancedFilterCount > 0 ? 'default' : 'text'}
            onClick={() => setTxFiltersOpen((open) => !open)}
          >
            Bộ lọc
          </Button>
        </Badge>
      </Space>

      {txFiltersOpen ? (
        <Space wrap>
          <Select
            allowClear
            placeholder="Tài khoản"
            style={{ width: 200 }}
            value={txAccountId}
            onChange={(value) => {
              setTxAccountId(value)
              setTxPage(0)
            }}
            options={accounts.map((account) => ({
              label: account.name,
              value: account.id,
            }))}
          />
          <Select
            allowClear
            showSearch
            optionFilterProp="label"
            placeholder="Danh mục"
            style={{ width: 200 }}
            value={txCategoryId}
            onChange={(value) => {
              setTxCategoryId(value)
              setTxPage(0)
            }}
            options={categories.map((category) => ({
              label: category.name,
              value: category.id,
            }))}
          />
          <Select
            allowClear
            placeholder="Thu/Chi"
            style={{ width: 120 }}
            value={txDirection}
            onChange={(value) => {
              setTxDirection(value)
              setTxPage(0)
            }}
            options={Object.entries(directionLabels).map(([value, label]) => ({ value, label }))}
          />
          <Select
            allowClear
            placeholder="Nguồn"
            style={{ width: 180 }}
            value={txSourceType}
            onChange={(value) => {
              setTxSourceType(value)
              setTxPage(0)
            }}
            options={Object.entries(sourceLabels).map(([value, label]) => ({ value, label }))}
          />
          <Select
            allowClear
            placeholder="Trạng thái"
            style={{ width: 140 }}
            value={txStatus}
            onChange={(value) => {
              setTxStatus(value)
              setTxPage(0)
            }}
            options={Object.entries(statusLabels).map(([value, label]) => ({ value, label }))}
          />
        </Space>
      ) : null}

      <Table
        rowKey="id"
        loading={transactionsQuery.isLoading}
        columns={transactionColumns}
        dataSource={transactionsQuery.data?.data ?? []}
        pagination={{
          current: txPage + 1,
          pageSize: txSize,
          total: transactionsQuery.data?.meta?.totalElements ?? 0,
          showSizeChanger: true,
        }}
        onChange={handleTxTableChange}
        scroll={{ x: 1200 }}
        rowClassName={(row) => (row.status === 'CANCELED' ? 'finance-tx-canceled' : '')}
      />
    </Space>
  )

  const accountsTab = (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Space wrap>
        <Button
          type="primary"
          onClick={() => {
            setEditingAccount(undefined)
            setAccountModalOpen(true)
          }}
        >
          Thêm tài khoản
        </Button>
        <Select
          allowClear
          placeholder="Số dư tài khoản"
          style={{ width: 200 }}
          value={balanceFilter}
          onChange={(value?: 'NEGATIVE') => {
            const next = new URLSearchParams(searchParams)
            if (value) {
              next.set('balance', value)
            } else {
              next.delete('balance')
            }
            next.set('tab', 'accounts')
            setSearchParams(next, { replace: true })
          }}
          options={[{ value: 'NEGATIVE', label: 'Số dư âm' }]}
        />
        {balanceFilter === 'NEGATIVE' ? (
          <Tag color="red" closable onClose={clearBalanceFilter}>
            Bộ lọc: Tài khoản đang âm
          </Tag>
        ) : null}
      </Space>
      <Row gutter={[16, 16]}>
        {accounts.map((account) => (
          <Col xs={24} sm={12} lg={8} key={account.id}>
            <Card
              title={`${account.code} - ${account.name}`}
              extra={
                <Tag color={account.active ? 'green' : 'default'}>
                  {account.active ? 'Đang dùng' : 'Ngừng'}
                </Tag>
              }
            >
              <Space direction="vertical">
                <Text type="secondary">{accountTypeLabels[account.type]}</Text>
                <Title level={4} style={{ margin: 0, color: account.currentBalance < 0 ? '#cf1322' : undefined }}>
                  <MoneyText value={account.currentBalance} />
                </Title>
              </Space>
            </Card>
          </Col>
        ))}
      </Row>
      <Table
        rowKey="id"
        loading={accountsQuery.isLoading}
        columns={accountColumns}
        dataSource={accounts}
        pagination={false}
        locale={{ emptyText: balanceFilter === 'NEGATIVE' ? 'Không có tài khoản đang âm.' : 'Chưa có tài khoản.' }}
      />
    </Space>
  )

  const categoriesTab = (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Space wrap>
        <Button
          type="primary"
          onClick={() => {
            setEditingCategory(undefined)
            setCategoryModalOpen(true)
          }}
        >
          Thêm danh mục
        </Button>
        <Input.Search
          allowClear
          placeholder="Tìm danh mục theo tên hoặc mã"
          style={{ width: 280 }}
          value={categorySearch}
          onChange={(event) => setCategorySearch(event.target.value)}
        />
      </Space>
      <Card title="Danh mục thu" loading={categoriesQuery.isLoading}>
        <Table
          rowKey="id"
          size="small"
          pagination={false}
          columns={categoryColumns()}
          dataSource={incomeCategories}
        />
      </Card>
      <Card title="Danh mục chi" loading={categoriesQuery.isLoading}>
        <Table
          rowKey="id"
          size="small"
          pagination={false}
          columns={categoryColumns()}
          dataSource={expenseCategories}
        />
      </Card>
    </Space>
  )

  const monthly = monthlyQuery.data
  const yearly = yearlyQuery.data
  const compare = compareQuery.data

  const reportsTab = (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Space wrap>
        <Button type="primary" loading={exportReport.isPending} onClick={handleExport}>
          Xuất Excel
        </Button>
      </Space>

      <Tabs
        activeKey={reportSubTab}
        onChange={setReportSubTab}
        items={[
          {
            key: 'monthly',
            label: 'Báo cáo tháng',
            children: (
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                <DatePicker
                  picker="month"
                  format="MM/YYYY"
                  value={reportMonth}
                  onChange={(value) => value && setReportMonth(value)}
                  allowClear={false}
                  disabledDate={disableFinanceMonth}
                />
                <Row gutter={[16, 16]}>
                  <Col xs={24} sm={12} md={6}>
                    <Card>
                      <Statistic
                        title="Thu"
                        valueRender={() => <MoneyText value={monthly?.summary.totalIncome} />}
                      />
                    </Card>
                  </Col>
                  <Col xs={24} sm={12} md={6}>
                    <Card>
                      <Statistic
                        title="Chi"
                        valueRender={() => <MoneyText value={monthly?.summary.totalExpense} />}
                      />
                    </Card>
                  </Col>
                  <Col xs={24} sm={12} md={6}>
                    <Card>
                      <Statistic
                        title="Dòng tiền ròng"
                        valueRender={() => (
                          <NetCashFlowText value={monthly?.summary.netCashFlow ?? 0} />
                        )}
                      />
                    </Card>
                  </Col>
                  <Col xs={24} sm={12} md={6}>
                    <Card>
                      <Statistic
                        title="Số dư cuối"
                        valueRender={() => (
                          <BalanceText value={monthly?.summary.closingBalance ?? 0} />
                        )}
                      />
                    </Card>
                  </Col>
                </Row>
                <Row gutter={[16, 16]}>
                  <Col xs={24} lg={12}>
                    <Card title="Cơ cấu thu" loading={monthlyQuery.isLoading}>
                      <Table
                        rowKey="categoryId"
                        size="small"
                        pagination={false}
                        dataSource={monthly?.incomeBreakdown ?? []}
                        columns={[
                          { title: 'Danh mục', dataIndex: 'categoryName', key: 'categoryName' },
                          {
                            title: 'Số tiền',
                            dataIndex: 'amount',
                            key: 'amount',
                            align: 'right',
                            render: (value: number) => <MoneyText value={value} />,
                          },
                        ]}
                      />
                    </Card>
                  </Col>
                  <Col xs={24} lg={12}>
                    <Card title="Cơ cấu chi" loading={monthlyQuery.isLoading}>
                      <Table
                        rowKey="categoryId"
                        size="small"
                        pagination={false}
                        dataSource={monthly?.expenseBreakdown ?? []}
                        columns={[
                          { title: 'Danh mục', dataIndex: 'categoryName', key: 'categoryName' },
                          {
                            title: 'Số tiền',
                            dataIndex: 'amount',
                            key: 'amount',
                            align: 'right',
                            render: (value: number) => <MoneyText value={value} />,
                          },
                        ]}
                      />
                    </Card>
                  </Col>
                </Row>
              </Space>
            ),
          },
          {
            key: 'yearly',
            label: 'Báo cáo năm',
            children: (
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                <DatePicker
                  picker="year"
                  format="YYYY"
                  value={reportYear}
                  onChange={(value) => value && setReportYear(value)}
                  allowClear={false}
                  disabledDate={disableFinanceYear}
                />
                <Row gutter={[16, 16]}>
                  <Col xs={24} sm={12} md={6}>
                    <Card>
                      <Statistic
                        title="Tổng thu năm"
                        valueRender={() => <MoneyText value={yearly?.totalIncome} />}
                      />
                    </Card>
                  </Col>
                  <Col xs={24} sm={12} md={6}>
                    <Card>
                      <Statistic
                        title="Tổng chi năm"
                        valueRender={() => <MoneyText value={yearly?.totalExpense} />}
                      />
                    </Card>
                  </Col>
                  <Col xs={24} sm={12} md={6}>
                    <Card>
                      <Statistic
                        title="Dòng tiền ròng năm"
                        valueRender={() => (
                          <NetCashFlowText value={yearly?.annualNetCashFlow ?? 0} />
                        )}
                      />
                    </Card>
                  </Col>
                  <Col xs={24} sm={12} md={6}>
                    <Card>
                      <Statistic
                        title="Công nợ cuối năm"
                        valueRender={() => (
                          <span style={{ color: (yearly?.yearEndStudentDebt ?? 0) > 0 ? '#d46b08' : undefined }}>
                            <MoneyText value={yearly?.yearEndStudentDebt} />
                          </span>
                        )}
                      />
                    </Card>
                  </Col>
                </Row>
                <Card title="Theo tháng" loading={yearlyQuery.isLoading}>
                  <Table
                    rowKey="month"
                    size="small"
                    pagination={false}
                    dataSource={yearly?.months ?? []}
                    columns={[
                      {
                        title: 'Tháng',
                        dataIndex: 'month',
                        key: 'month',
                        render: (month: number) => String(month).padStart(2, '0'),
                      },
                      {
                        title: 'Thu',
                        dataIndex: 'income',
                        key: 'income',
                        align: 'right',
                        render: (value: number) => <MoneyText value={value} />,
                      },
                      {
                        title: 'Chi',
                        dataIndex: 'expense',
                        key: 'expense',
                        align: 'right',
                        render: (value: number) => <MoneyText value={value} />,
                      },
                      {
                        title: 'Dòng tiền ròng',
                        dataIndex: 'netCashFlow',
                        key: 'netCashFlow',
                        align: 'right',
                        render: (value: number) => <NetCashFlowText value={value} />,
                      },
                      {
                        title: 'Số dư cuối',
                        dataIndex: 'closingBalance',
                        key: 'closingBalance',
                        align: 'right',
                        render: (value: number) => <BalanceText value={value} />,
                      },
                    ]}
                  />
                </Card>
              </Space>
            ),
          },
          {
            key: 'compare',
            label: 'So sánh kỳ',
            children: (
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                <Space wrap align="center">
                  <Text>Loại so sánh:</Text>
                  <Segmented
                    value={compareMode}
                    onChange={(value) => applyCompareMode(value as ComparisonMode)}
                    options={[
                      { label: 'Tháng trước', value: 'PREVIOUS_MONTH' },
                      { label: 'Cùng kỳ năm trước', value: 'SAME_MONTH_PREVIOUS_YEAR' },
                      { label: 'Tùy chọn', value: 'CUSTOM' },
                    ]}
                  />
                </Space>

                <Space wrap align="center">
                  <Text>Kỳ hiện tại:</Text>
                  <DatePicker
                    picker="month"
                    format="MM/YYYY"
                    value={compareCurrent}
                    onChange={handleCompareCurrentChange}
                    allowClear={false}
                    disabledDate={disableFinanceMonth}
                  />
                  <Text>so với</Text>
                  <DatePicker
                    picker="month"
                    format="MM/YYYY"
                    value={compareAgainst}
                    onChange={(value) => {
                      if (!value) return
                      setCompareMode('CUSTOM')
                      setCompareAgainst(value)
                    }}
                    allowClear={false}
                    disabledDate={disableFinanceMonth}
                    disabled={compareMode !== 'CUSTOM'}
                  />
                </Space>

                {compareParams.currentYear === compareParams.comparisonYear &&
                compareParams.currentMonth === compareParams.comparisonMonth ? (
                  <Alert
                    type="warning"
                    showIcon
                    message="Kỳ hiện tại và kỳ so sánh phải khác nhau."
                  />
                ) : compareQuery.isFetching || compareDefaultsQuery.isLoading ? (
                  <Spin />
                ) : compareQuery.isError ? (
                  <Alert
                    type="warning"
                    showIcon
                    message={
                      isAxiosError(compareQuery.error)
                        ? compareQuery.error.response?.data?.message ??
                          'Không thể so sánh các kỳ đã chọn'
                        : 'Không thể so sánh các kỳ đã chọn'
                    }
                  />
                ) : compare ? (
                  <>
                    <Alert type="info" showIcon message={compare.message ?? ''} />
                    {!compare.comparisonPeriod.hasTransactions &&
                    compare.suggestion &&
                    compare.nearestEarlierYearWithData &&
                    compare.nearestEarlierMonthWithData ? (
                      <Button
                        type="link"
                        style={{ paddingLeft: 0 }}
                        onClick={() => {
                          setCompareMode('CUSTOM')
                          setCompareAgainst(
                            dayjs(
                              `${compare.nearestEarlierYearWithData}-${String(
                                compare.nearestEarlierMonthWithData,
                              ).padStart(2, '0')}-01`,
                            ),
                          )
                        }}
                      >
                        {compare.suggestion}
                        {` (${String(compare.nearestEarlierMonthWithData).padStart(2, '0')}/${compare.nearestEarlierYearWithData})`}
                      </Button>
                    ) : null}

                    <Row gutter={[16, 16]}>
                      <Col xs={24} md={12} lg={8}>
                        <CompareMetricCard
                          title="Thu"
                          currentLabel={compare.currentPeriod.label}
                          comparisonLabel={compare.comparisonPeriod.label}
                          currentValue={compare.current.income}
                          comparisonValue={compare.comparison.income}
                          difference={compare.income}
                          metric="income"
                          loading={compareQuery.isFetching}
                        />
                      </Col>
                      <Col xs={24} md={12} lg={8}>
                        <CompareMetricCard
                          title="Chi"
                          currentLabel={compare.currentPeriod.label}
                          comparisonLabel={compare.comparisonPeriod.label}
                          currentValue={compare.current.expense}
                          comparisonValue={compare.comparison.expense}
                          difference={compare.expense}
                          metric="expense"
                          loading={compareQuery.isFetching}
                        />
                      </Col>
                      <Col xs={24} md={12} lg={8}>
                        <CompareMetricCard
                          title="Dòng tiền ròng"
                          currentLabel={compare.currentPeriod.label}
                          comparisonLabel={compare.comparisonPeriod.label}
                          currentValue={compare.current.netCashFlow}
                          comparisonValue={compare.comparison.netCashFlow}
                          difference={compare.netCashFlow}
                          metric="net"
                          loading={compareQuery.isFetching}
                        />
                      </Col>
                      <Col xs={24} md={12} lg={8}>
                        <CompareMetricCard
                          title="Số dư cuối kỳ"
                          currentLabel={compare.currentPeriod.label}
                          comparisonLabel={compare.comparisonPeriod.label}
                          currentValue={compare.current.closingBalance}
                          comparisonValue={compare.comparison.closingBalance}
                          difference={compare.closingBalance}
                          metric="balance"
                          loading={compareQuery.isFetching}
                        />
                      </Col>
                      <Col xs={24} md={12} lg={8}>
                        <CompareMetricCard
                          title="Công nợ cuối kỳ"
                          currentLabel={compare.currentPeriod.label}
                          comparisonLabel={compare.comparisonPeriod.label}
                          currentValue={compare.current.outstandingDebt}
                          comparisonValue={compare.comparison.outstandingDebt}
                          difference={compare.outstandingDebt}
                          metric="debt"
                          loading={compareQuery.isFetching}
                        />
                      </Col>
                    </Row>
                  </>
                ) : (
                  <Alert type="info" showIcon message="Chọn hai kỳ để bắt đầu so sánh." />
                )}
              </Space>
            ),
          },
        ]}
      />
    </Space>
  )

  const reconciliation = reconciliationQuery.data
  const reconciliationTab = (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Space wrap>
        <RangePicker
          format="DD/MM/YYYY"
          value={reconRange}
          onChange={(value) => setReconRange(value as [Dayjs, Dayjs] | null)}
        />
        <Select
          allowClear
          placeholder="Trạng thái đối soát"
          style={{ width: 220 }}
          value={reconStatusFilter}
          onChange={(value?: 'MATCHED' | 'MISMATCHED') => {
            const next = new URLSearchParams(searchParams)
            if (value) {
              next.set('reconStatus', value)
            } else {
              next.delete('reconStatus')
            }
            next.set('tab', 'reconciliation')
            setSearchParams(next, { replace: true })
          }}
          options={[
            { value: 'MATCHED', label: 'Khớp' },
            { value: 'MISMATCHED', label: 'Không khớp' },
          ]}
        />
        {reconStatusFilter ? (
          <Tag
            color={reconStatusFilter === 'MISMATCHED' ? 'red' : 'green'}
            closable
            onClose={() => {
              const next = new URLSearchParams(searchParams)
              next.delete('reconStatus')
              setSearchParams(next, { replace: true })
            }}
          >
            {reconStatusFilter === 'MISMATCHED' ? 'Không khớp' : 'Khớp'}
          </Tag>
        ) : null}
      </Space>

      {reconciliation && reconciliation.status === 'MISMATCHED' ? (
        <Alert type="error" showIcon message="Đối soát chưa khớp — cần kiểm tra các lệch bên dưới" />
      ) : reconciliation ? (
        <Alert type="success" showIcon message="Đối soát khớp" />
      ) : null}

      {reconStatusFilter && reconciliation && reconciliation.status !== reconStatusFilter ? (
        <Alert
          type="info"
          showIcon
          message={
            reconStatusFilter === 'MISMATCHED'
              ? 'Kết quả hiện tại đang khớp — không có lệch để hiển thị.'
              : 'Kết quả hiện tại đang không khớp.'
          }
        />
      ) : null}

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic title="Số Payment VALID" value={reconciliation?.totalValidPayments ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Tổng tiền Payment"
              valueRender={() => <MoneyText value={reconciliation?.totalPaymentAmount} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Tổng sổ học phí"
              valueRender={() => <MoneyText value={reconciliation?.totalTuitionLedgerAmount} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Chênh lệch"
              valueRender={() => <NetCashFlowText value={reconciliation?.difference ?? 0} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic title="Khớp" value={reconciliation?.matchedCount ?? 0} />
          </Card>
        </Col>
      </Row>

      <Card title="Chi tiết lệch" loading={reconciliationQuery.isLoading}>
        <Table
          rowKey={(_, index) => String(index)}
          size="small"
          dataSource={reconciliation?.mismatches ?? []}
          pagination={{ pageSize: 20 }}
          columns={[
            {
              title: 'Loại lệch',
              dataIndex: 'mismatchType',
              key: 'mismatchType',
              render: (value: string) => mismatchTypeLabels[value] ?? value,
            },
            {
              title: 'Thanh toán',
              key: 'payment',
              render: (_, row) =>
                row.paymentCode ? (
                  <Link to="/payments">{row.paymentCode}</Link>
                ) : (
                  '-'
                ),
            },
            {
              title: 'Giao dịch sổ',
              dataIndex: 'transactionCode',
              key: 'transactionCode',
              render: (value?: string | null) => value || '-',
            },
            {
              title: 'Chi tiết',
              dataIndex: 'detail',
              key: 'detail',
            },
            {
              title: 'Thao tác',
              key: 'actions',
              render: (_, row) =>
                row.mismatchType === 'MISSING_LEDGER' && row.paymentId ? (
                  <Button
                    size="small"
                    type="link"
                    loading={repairPaymentLedger.isPending}
                    onClick={() => {
                      repairPaymentLedger.mutate(
                        { paymentId: row.paymentId! },
                        {
                          onSuccess: (result) => {
                            message.success(result.message)
                          },
                          onError: (error) => {
                            message.error(
                              isAxiosError(error)
                                ? error.response?.data?.message || 'Sửa lệch thất bại'
                                : 'Sửa lệch thất bại',
                            )
                          },
                        },
                      )
                    }}
                  >
                    Tạo bút toán
                  </Button>
                ) : (
                  '-'
                ),
            },
          ]}
        />
      </Card>
    </Space>
  )

  const periodsTab = (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Space wrap>
        <DatePicker
          picker="month"
          format="MM/YYYY"
          value={periodPicker}
          onChange={(value) => value && setPeriodPicker(value)}
          disabledDate={disableClosePeriodMonth}
        />
        <Tooltip
          title={
            isCloseablePeriod(periodPicker.year(), periodPicker.month() + 1)
              ? undefined
              : 'Không thể khóa tháng hiện tại trước khi tháng kết thúc.'
          }
        >
          <Button
            type="primary"
            disabled={!isCloseablePeriod(periodPicker.year(), periodPicker.month() + 1)}
            onClick={() => {
              setSelectedPeriod({
                id: 0,
                year: periodPicker.year(),
                month: periodPicker.month() + 1,
                status: 'OPEN',
                createdAt: '',
                updatedAt: '',
              })
              setCloseOpen(true)
            }}
          >
            Chốt kỳ đã chọn
          </Button>
        </Tooltip>
      </Space>
      <Table
        rowKey={(row) => `${row.year}-${row.month}`}
        loading={periodsQuery.isLoading}
        columns={periodColumns}
        dataSource={periodsQuery.data ?? []}
        pagination={false}
        locale={{ emptyText: 'Chưa có kỳ nào được chốt. Chọn tháng ở trên để chốt kỳ.' }}
      />
    </Space>
  )

  const settingsMenuItems: MenuProps['items'] = [
    { key: 'accounts', label: 'Tài khoản' },
    { key: 'categories', label: 'Danh mục thu chi' },
    { key: 'reconciliation', label: 'Đối soát Payment' },
    { key: 'periods', label: 'Khóa sổ tháng' },
  ]

  const settingsTitle: Record<string, string> = {
    accounts: 'Tài khoản',
    categories: 'Danh mục thu chi',
    reconciliation: 'Đối soát Payment',
    periods: 'Khóa sổ tháng',
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
        <div>
          <Title level={3} style={{ marginBottom: 4 }}>
            Quản lý thu chi
          </Title>
          <Text type="secondary">Theo dõi dòng tiền, giao dịch và báo cáo hàng ngày</Text>
        </div>
        <Dropdown
          menu={{
            items: settingsMenuItems,
            onClick: ({ key }) => changeFinanceTab(key),
          }}
        >
          <Button>Cài đặt tài chính</Button>
        </Dropdown>
      </div>

      {isSettingsTab ? (
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Space wrap>
            <Button onClick={() => changeFinanceTab('overview')}>← Quay lại</Button>
            <Title level={4} style={{ margin: 0 }}>
              {settingsTitle[activeTab]}
            </Title>
          </Space>
          {activeTab === 'accounts' ? accountsTab : null}
          {activeTab === 'categories' ? categoriesTab : null}
          {activeTab === 'reconciliation' ? reconciliationTab : null}
          {activeTab === 'periods' ? periodsTab : null}
        </Space>
      ) : (
        <Tabs
          activeKey={activeTab}
          onChange={changeFinanceTab}
          items={[
            { key: 'overview', label: 'Tổng quan', children: overviewTab },
            { key: 'ledger', label: 'Giao dịch', children: ledgerTab },
            { key: 'reports', label: 'Báo cáo', children: reportsTab },
          ]}
        />
      )}

      <CreateTransactionDrawer
        open={createTxOpen}
        submitting={postIncome.isPending || postExpense.isPending || postTransfer.isPending}
        accounts={accounts}
        categories={categories}
        prefill={createTxPrefill}
        creatingCategory={createCategory.isPending}
        onCancel={() => {
          setCreateTxOpen(false)
          setCreateTxPrefill(null)
        }}
        onSubmitExpense={handleExpenseSubmit}
        onSubmitIncome={handleIncomeSubmit}
        onSubmitTransfer={handleTransferSubmit}
        onCreateCategory={handleInlineCreateCategory}
      />
      <CancelTransactionModal
        open={cancelOpen}
        transaction={selectedTx}
        submitting={cancelTx.isPending}
        onCancel={() => {
          setCancelOpen(false)
          setSelectedTx(undefined)
        }}
        onSubmit={handleCancelSubmit}
      />
      <TransactionDetailDrawer
        open={detailOpen}
        transaction={selectedTx}
        onClose={() => {
          setDetailOpen(false)
          setSelectedTx(undefined)
        }}
      />
      <AccountFormModal
        open={accountModalOpen}
        initialAccount={editingAccount}
        submitting={createAccount.isPending || updateAccount.isPending}
        onCancel={() => {
          setAccountModalOpen(false)
          setEditingAccount(undefined)
        }}
        onSubmit={handleAccountSubmit}
      />
      <CategoryFormModal
        open={categoryModalOpen}
        initialCategory={editingCategory}
        categories={categories}
        submitting={createCategory.isPending || updateCategory.isPending}
        onCancel={() => {
          setCategoryModalOpen(false)
          setEditingCategory(undefined)
        }}
        onSubmit={handleCategorySubmit}
      />
      <ClosePeriodModal
        open={closeOpen}
        period={selectedPeriod}
        submitting={closePeriod.isPending}
        onCancel={() => {
          setCloseOpen(false)
          setSelectedPeriod(undefined)
        }}
        onSubmit={handleClosePeriod}
      />
      <ReopenPeriodModal
        open={reopenOpen}
        period={selectedPeriod}
        submitting={reopenPeriod.isPending}
        onCancel={() => {
          setReopenOpen(false)
          setSelectedPeriod(undefined)
        }}
        onSubmit={handleReopenPeriod}
      />
    </Space>
  )
}
