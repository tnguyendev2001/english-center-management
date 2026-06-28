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
import { matchesKeyword, paginateItems } from '../../../utils/clientPagination'
import { useClassrooms } from '../../classrooms/classroomQueries'
import { useDebts } from '../../debts/debtQueries'
import { PaymentFormModal } from '../../payments/components/PaymentFormModal'
import { useCreatePayment } from '../../payments/paymentQueries'
import type { CreatePaymentPayload } from '../../payments/paymentTypes'
import { useRevenueSummary } from '../../revenue/revenueQueries'
import { InvoiceDetailModal } from '../components/InvoiceDetailModal'
import { useInvoices } from '../invoiceQueries'
import type { Invoice, InvoiceSearchParams, InvoiceStatus } from '../invoiceTypes'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

type InvoiceTab = 'needs-collection' | 'paid' | 'canceled' | 'all'

const invoiceStatusLabels = {
  UNPAID: 'Chưa đóng',
  PARTIALLY_PAID: 'Đóng một phần',
  PAID: 'Đã đóng',
  CANCELED: 'Đã hủy',
}

const FETCH_SIZE = 100

export function InvoiceListPage() {
  const [tab, setTab] = useState<InvoiceTab>('needs-collection')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState<InvoiceStatus>()
  const [classroomId, setClassroomId] = useState<number>()
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>()
  const [collectingInvoice, setCollectingInvoice] = useState<Invoice>()
  const [detailInvoice, setDetailInvoice] = useState<Invoice>()

  const invoiceParams: InvoiceSearchParams = useMemo(
    () => ({
      status: tab === 'paid' ? 'PAID' : tab === 'canceled' ? 'CANCELED' : undefined,
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

  const useDebtsQuery = tab === 'needs-collection'
  const invoicesQuery = useInvoices(invoiceParams, !useDebtsQuery)
  const debtsQuery = useDebts(debtsParams, useDebtsQuery)
  const debtSummaryQuery = useDebts({ page: 0, size: FETCH_SIZE })
  const revenueQuery = useRevenueSummary()
  const classroomsQuery = useClassrooms({ page: 0, size: 100 })
  const createPayment = useCreatePayment()

  const rawInvoices = useDebtsQuery ? (debtsQuery.data?.data ?? []) : (invoicesQuery.data?.data ?? [])
  const isLoading = useDebtsQuery ? debtsQuery.isLoading : invoicesQuery.isLoading

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

      if (!matchesKeyword(keyword, invoice.studentName, invoice.invoiceCode)) {
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

  const columns: ColumnsType<Invoice> = [
    {
      title: 'Mã học phí',
      dataIndex: 'invoiceCode',
      key: 'invoiceCode',
    },
    {
      title: 'Học viên',
      dataIndex: 'studentName',
      key: 'studentName',
    },
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
        ]

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Học phí
        </Title>
        <Text type="secondary">Quản lý học phí và thu tiền theo trạng thái.</Text>
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
              { key: 'needs-collection', label: 'Cần thu' },
              { key: 'paid', label: 'Đã đóng' },
              { key: 'canceled', label: 'Đã hủy' },
              { key: 'all', label: 'Tất cả' },
            ]}
          />

          <Space wrap>
            <Input.Search
              allowClear
              placeholder="Tìm học viên hoặc mã học phí"
              style={{ width: 280 }}
              value={keyword}
              onChange={(event) => handleKeywordChange(event.target.value)}
            />
            {(tab === 'needs-collection' || tab === 'all') && (
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
            <RangePicker
              allowClear
              format="DD/MM/YYYY"
              placeholder={['Hạn từ', 'Hạn đến']}
              value={dateRange}
              onChange={handleDateRangeChange}
            />
          </Space>

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
    </Space>
  )
}
