import {
  Button,
  Card,
  Col,
  DatePicker,
  Input,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd'
import type { TablePaginationConfig } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { isAxiosError } from 'axios'
import { useMemo, useState } from 'react'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import {
  STUDENT_SEARCH_PLACEHOLDER,
  studentCodeColumn,
  studentKeywordFields,
  studentNameColumn,
} from '../../../components/common/studentDisplay'
import { matchesKeyword, paginateItems } from '../../../utils/clientPagination'
import { useClassrooms } from '../../classrooms/classroomQueries'
import { StudentInvoiceListDrawer } from '../../financial/components/StudentInvoiceListDrawer'
import type { StudentTuitionSummary } from '../../financial/financialSummaryTypes'
import { useDebts } from '../../debts/debtQueries'
import { PaymentFormModal } from '../../payments/components/PaymentFormModal'
import { useCreatePayment } from '../../payments/paymentQueries'
import type { CreatePaymentPayload } from '../../payments/paymentTypes'
import { useRevenueSummary } from '../../revenue/revenueQueries'
import { InvoiceDetailModal } from '../components/InvoiceDetailModal'
import { useInvoices, useTuitionStudentSummaries } from '../invoiceQueries'
import type { Invoice, InvoiceSearchParams, InvoiceStatus } from '../invoiceTypes'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

type InvoiceTab = 'by-student' | 'needs-collection' | 'paid' | 'replaced' | 'canceled' | 'all'

const invoiceStatusLabels = {
  UNPAID: 'Chưa đóng',
  PARTIALLY_PAID: 'Đóng một phần',
  PAID: 'Đã đóng',
  CANCELED: 'Đã hủy',
  REPLACED: 'Đã thay thế do đổi gói',
}

const FETCH_SIZE = 100

function summaryRowKey(summary: StudentTuitionSummary) {
  return `${summary.studentId}-${summary.classroomId}`
}

function findCollectibleInvoice(invoices: Invoice[], studentId: number, classroomId: number) {
  return invoices
    .filter(
      (invoice) =>
        invoice.studentId === studentId &&
        invoice.classroomId === classroomId &&
        (invoice.status === 'UNPAID' || invoice.status === 'PARTIALLY_PAID'),
    )
    .sort((left, right) => dayjs(left.dueDate).valueOf() - dayjs(right.dueDate).valueOf())[0]
}

export function InvoiceListPage() {
  const [tab, setTab] = useState<InvoiceTab>('by-student')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState<InvoiceStatus>()
  const [classroomId, setClassroomId] = useState<number>()
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>()
  const [collectingInvoice, setCollectingInvoice] = useState<Invoice>()
  const [detailInvoice, setDetailInvoice] = useState<Invoice>()
  const [detailSummary, setDetailSummary] = useState<StudentTuitionSummary>()

  const summaryParams = useMemo(() => ({ classroomId }), [classroomId])

  const invoiceParams: InvoiceSearchParams = useMemo(
    () => ({
      status:
        tab === 'paid'
          ? 'PAID'
          : tab === 'replaced'
            ? 'REPLACED'
            : tab === 'canceled'
              ? 'CANCELED'
              : undefined,
      classroomId,
      page: 0,
      size: FETCH_SIZE,
    }),
    [classroomId, tab],
  )

  const debtsParams = useMemo(
    () => ({
      page: 0,
      size: FETCH_SIZE,
    }),
    [],
  )

  const isStudentTab = tab === 'by-student'
  const useDebtsQuery = tab === 'needs-collection'
  const studentSummariesQuery = useTuitionStudentSummaries(summaryParams, isStudentTab)
  const invoicesQuery = useInvoices(invoiceParams, !isStudentTab && !useDebtsQuery)
  const debtsQuery = useDebts(debtsParams, !isStudentTab && useDebtsQuery)
  const collectibleInvoicesQuery = useDebts(debtsParams, isStudentTab)
  const debtSummaryQuery = useDebts({ page: 0, size: FETCH_SIZE })
  const revenueQuery = useRevenueSummary()
  const classroomsQuery = useClassrooms({ page: 0, size: 100 })
  const createPayment = useCreatePayment()

  const rawInvoices = useDebtsQuery ? (debtsQuery.data?.data ?? []) : (invoicesQuery.data?.data ?? [])
  const isLoading = isStudentTab
    ? studentSummariesQuery.isLoading
    : useDebtsQuery
      ? debtsQuery.isLoading
      : invoicesQuery.isLoading

  const filteredSummaries = useMemo(() => {
    return (studentSummariesQuery.data?.data ?? []).filter((summary) => {
      if (classroomId && summary.classroomId !== classroomId) {
        return false
      }

      return matchesKeyword(keyword, ...studentKeywordFields(summary), summary.classroomName)
    })
  }, [classroomId, keyword, studentSummariesQuery.data?.data])

  const pagedSummaries = useMemo(
    () => paginateItems(filteredSummaries, page, size),
    [filteredSummaries, page, size],
  )

  const filteredInvoices = useMemo(() => {
    return rawInvoices.filter((invoice) => {
      if (tab === 'all' && statusFilter && invoice.status !== statusFilter) {
        return false
      }

      if (tab === 'needs-collection' && statusFilter && invoice.status !== statusFilter) {
        return false
      }

      if (classroomId && invoice.classroomId !== classroomId) {
        return false
      }

      if (!matchesKeyword(keyword, ...studentKeywordFields(invoice), invoice.invoiceCode)) {
        return false
      }

      if (dateRange) {
        const dueDate = dayjs(invoice.dueDate)
        if (dueDate.isBefore(dateRange[0], 'day') || dueDate.isAfter(dateRange[1], 'day')) {
          return false
        }
      }

      return true
    })
  }, [classroomId, dateRange, keyword, rawInvoices, statusFilter, tab])

  const pagedInvoices = useMemo(
    () => paginateItems(filteredInvoices, page, size),
    [filteredInvoices, page, size],
  )

  const debtSummary = useMemo(() => {
    const debts = debtSummaryQuery.data?.data ?? []
    return {
      totalRemaining: debts.reduce((sum, invoice) => sum + invoice.remainingAmount, 0),
      unpaidCount: debts.filter((invoice) => invoice.status === 'UNPAID').length,
      partialCount: debts.filter((invoice) => invoice.status === 'PARTIALLY_PAID').length,
    }
  }, [debtSummaryQuery.data?.data])

  const studentSummaryColumns: ColumnsType<StudentTuitionSummary> = [
    studentCodeColumn(),
    studentNameColumn(),
    {
      title: 'Lớp',
      dataIndex: 'classroomName',
      key: 'classroomName',
    },
    {
      title: 'Tổng học phí',
      dataIndex: 'totalTuitionAmount',
      key: 'totalTuitionAmount',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Đã đóng',
      dataIndex: 'totalPaidAmount',
      key: 'totalPaidAmount',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Còn nợ',
      dataIndex: 'remainingDebt',
      key: 'remainingDebt',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Số hóa đơn',
      dataIndex: 'totalInvoiceCount',
      key: 'totalInvoiceCount',
    },
    {
      title: 'Chưa đóng',
      dataIndex: 'unpaidCount',
      key: 'unpaidCount',
    },
    {
      title: 'Đóng một phần',
      dataIndex: 'partialCount',
      key: 'partialCount',
    },
    {
      title: 'Đã đóng',
      dataIndex: 'paidCount',
      key: 'paidCount',
    },
    {
      title: 'Trạng thái',
      key: 'status',
      render: (_, summary) => (
        <Space size={[4, 4]} wrap>
          {summary.remainingDebt > 0 ? <Tag color="orange">Còn nợ</Tag> : <Tag color="green">Đã đóng đủ</Tag>}
          {summary.hasReplacedInvoices ? <Tag color="purple">Có hóa đơn thay thế</Tag> : null}
        </Space>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, summary) => (
        <Space size="small">
          {summary.remainingDebt > 0 ? (
            <Button type="link" onClick={() => handleCollectFromSummary(summary)}>
              Thu tiền
            </Button>
          ) : null}
          <Button type="link" onClick={() => setDetailSummary(summary)}>
            Xem chi tiết
          </Button>
        </Space>
      ),
    },
  ]

  const columns: ColumnsType<Invoice> = [
    {
      title: 'Mã học phí',
      dataIndex: 'invoiceCode',
      key: 'invoiceCode',
    },
    studentCodeColumn(),
    studentNameColumn(),
    {
      title: 'Lớp học',
      dataIndex: 'classroomName',
      key: 'classroomName',
    },
    {
      title: 'Gói học phí',
      dataIndex: 'packageNameSnapshot',
      key: 'packageNameSnapshot',
    },
    {
      title: 'Phải đóng',
      dataIndex: 'finalAmount',
      key: 'finalAmount',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Đã đóng',
      dataIndex: 'paidAmount',
      key: 'paidAmount',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Còn lại',
      dataIndex: 'remainingAmount',
      key: 'remainingAmount',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <StatusTag status={status} labels={invoiceStatusLabels} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, invoice) => (
        <Space size="small">
          {invoice.status === 'UNPAID' ? (
            <Button type="link" onClick={() => setCollectingInvoice(invoice)}>
              Thu tiền
            </Button>
          ) : null}
          {invoice.status === 'PARTIALLY_PAID' ? (
            <Button type="link" onClick={() => setCollectingInvoice(invoice)}>
              Thu tiếp
            </Button>
          ) : null}
          <Button type="link" onClick={() => setDetailInvoice(invoice)}>
            Xem chi tiết
          </Button>
        </Space>
      ),
    },
  ]

  function handleCollectFromSummary(summary: StudentTuitionSummary) {
    const invoice = findCollectibleInvoice(
      collectibleInvoicesQuery.data?.data ?? [],
      summary.studentId,
      summary.classroomId,
    )

    if (!invoice) {
      message.warning('Không tìm thấy hóa đơn cần thu')
      return
    }

    setCollectingInvoice(invoice)
  }

  function handleTableChange(pagination: TablePaginationConfig) {
    setPage((pagination.current ?? 1) - 1)
    setSize(pagination.pageSize ?? 10)
  }

  function handleTabChange(nextTab: string) {
    setTab(nextTab as InvoiceTab)
    setStatusFilter(undefined)
    setPage(0)
  }

  function handleKeywordChange(value: string) {
    setKeyword(value)
    setPage(0)
  }

  function handleStatusFilterChange(value?: InvoiceStatus) {
    setStatusFilter(value)
    setPage(0)
  }

  function handleClassroomChange(value?: number) {
    setClassroomId(value)
    setPage(0)
  }

  function handleDateRangeChange(values: [Dayjs | null, Dayjs | null] | null) {
    if (values?.[0] && values[1]) {
      setDateRange([values[0], values[1]])
    } else {
      setDateRange(undefined)
    }
    setPage(0)
  }

  function handleCreatePayment(payload: CreatePaymentPayload) {
    if (!collectingInvoice) {
      return
    }

    createPayment.mutate(
      { invoiceId: collectingInvoice.id, payload },
      {
        onSuccess: () => {
          message.success('Đã thu tiền')
          setCollectingInvoice(undefined)
        },
        onError: showErrorMessage,
      },
    )
  }

  function showErrorMessage(error: unknown) {
    if (isAxiosError(error)) {
      message.error(error.response?.data?.message ?? 'Có lỗi xảy ra')
      return
    }

    message.error('Có lỗi xảy ra')
  }

  const statusFilterOptions =
    tab === 'needs-collection'
      ? [
          { label: 'Chưa đóng', value: 'UNPAID' as const },
          { label: 'Đóng một phần', value: 'PARTIALLY_PAID' as const },
        ]
      : [
          { label: 'Chưa đóng', value: 'UNPAID' as const },
          { label: 'Đóng một phần', value: 'PARTIALLY_PAID' as const },
          { label: 'Đã đóng', value: 'PAID' as const },
          { label: 'Đã hủy', value: 'CANCELED' as const },
          { label: 'Đã thay thế do đổi gói', value: 'REPLACED' as const },
        ]

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Học phí
        </Title>
        <Text type="secondary">Quản lý học phí và thu tiền theo học viên hoặc từng hóa đơn.</Text>
      </Space>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={debtSummaryQuery.isLoading}>
            <Statistic
              title="Tổng cần thu"
              value={debtSummary.totalRemaining}
              formatter={(value) => `${Number(value).toLocaleString('en-US')} VND`}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={debtSummaryQuery.isLoading}>
            <Statistic title="Số hóa đơn chưa đóng" value={debtSummary.unpaidCount} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={debtSummaryQuery.isLoading}>
            <Statistic title="Số hóa đơn đóng một phần" value={debtSummary.partialCount} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={revenueQuery.isLoading}>
            <Statistic
              title="Đã thu tháng này"
              value={revenueQuery.data?.monthRevenue ?? 0}
              formatter={(value) => `${Number(value).toLocaleString('en-US')} VND`}
            />
          </Card>
        </Col>
      </Row>

      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Tabs
            activeKey={tab}
            onChange={handleTabChange}
            items={[
              { key: 'by-student', label: 'Theo học viên' },
              { key: 'needs-collection', label: 'Cần thu' },
              { key: 'paid', label: 'Đã đóng' },
              { key: 'replaced', label: 'Đã thay thế' },
              { key: 'canceled', label: 'Đã hủy' },
              { key: 'all', label: 'Tất cả' },
            ]}
          />

          <Space wrap>
            <Input.Search
              allowClear
              placeholder={isStudentTab ? STUDENT_SEARCH_PLACEHOLDER : 'Tìm theo mã hoặc tên học viên, mã học phí'}
              style={{ width: 280 }}
              value={keyword}
              onChange={(event) => handleKeywordChange(event.target.value)}
            />
            {!isStudentTab && (tab === 'needs-collection' || tab === 'all') && (
              <Select
                allowClear
                placeholder="Trạng thái"
                style={{ width: 180 }}
                value={statusFilter}
                onChange={handleStatusFilterChange}
                options={statusFilterOptions}
              />
            )}
            <Select
              allowClear
              placeholder="Lớp học"
              style={{ width: 220 }}
              value={classroomId}
              onChange={handleClassroomChange}
              options={(classroomsQuery.data?.data ?? []).map((classroom) => ({
                label: classroom.className,
                value: classroom.id,
              }))}
            />
            {!isStudentTab && (
              <RangePicker
                allowClear
                format="DD/MM/YYYY"
                placeholder={['Hạn từ', 'Hạn đến']}
                value={dateRange}
                onChange={handleDateRangeChange}
              />
            )}
          </Space>

          {isStudentTab ? (
            <Table
              rowKey={summaryRowKey}
              columns={studentSummaryColumns}
              dataSource={pagedSummaries}
              loading={isLoading}
              pagination={{
                current: page + 1,
                pageSize: size,
                total: filteredSummaries.length,
                showSizeChanger: true,
              }}
              onChange={handleTableChange}
              locale={{ emptyText: 'Không có học phí cần thu' }}
              scroll={{ x: 1200 }}
            />
          ) : (
            <Table
              rowKey="id"
              columns={columns}
              dataSource={pagedInvoices}
              loading={isLoading}
              pagination={{
                current: page + 1,
                pageSize: size,
                total: filteredInvoices.length,
                showSizeChanger: true,
              }}
              onChange={handleTableChange}
            />
          )}
        </Space>
      </Card>

      <PaymentFormModal
        open={Boolean(collectingInvoice)}
        invoice={collectingInvoice}
        submitting={createPayment.isPending}
        onCancel={() => setCollectingInvoice(undefined)}
        onSubmit={handleCreatePayment}
      />

      <InvoiceDetailModal
        open={Boolean(detailInvoice)}
        invoice={detailInvoice}
        onClose={() => setDetailInvoice(undefined)}
      />

      <StudentInvoiceListDrawer
        open={Boolean(detailSummary)}
        studentId={detailSummary?.studentId}
        classroomId={detailSummary?.classroomId}
        studentCode={detailSummary?.studentCode}
        studentName={detailSummary?.studentName}
        classroomName={detailSummary?.classroomName}
        onClose={() => setDetailSummary(undefined)}
        onCollect={(invoice) => {
          setDetailSummary(undefined)
          setCollectingInvoice(invoice)
        }}
      />
    </Space>
  )
}
