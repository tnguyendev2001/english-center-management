import {
  Button,
  Card,
  Col,
  DatePicker,
  Input,
  Modal,
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
import { useRevenueSummary } from '../../revenue/revenueQueries'
import { CancelPaymentModal } from '../components/CancelPaymentModal'
import { useCancelPayment, usePayments } from '../paymentQueries'
import type {
  CancelPaymentPayload,
  Payment,
  PaymentMethod,
  PaymentSearchParams,
  PaymentStatus,
} from '../paymentTypes'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

type PaymentTab = 'valid' | 'canceled' | 'all'

const paymentMethodLabels: Record<PaymentMethod, string> = {
  CASH: 'Tiền mặt',
  BANK_TRANSFER: 'Chuyển khoản',
  OTHER: 'Khác',
}

const paymentStatusLabels = {
  VALID: 'Hợp lệ',
  CANCELED: 'Đã hủy',
}

const FETCH_SIZE = 100

function defaultMonthRange(): [Dayjs, Dayjs] {
  const today = dayjs()
  return [today.startOf('month'), today]
}

export function PaymentListPage() {
  const [tab, setTab] = useState<PaymentTab>('valid')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [methodFilter, setMethodFilter] = useState<PaymentMethod>()
  const [statusFilter, setStatusFilter] = useState<PaymentStatus>()
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>(defaultMonthRange)
  const [cancelingPayment, setCancelingPayment] = useState<Payment>()

  const params: PaymentSearchParams = useMemo(
    () => ({
      page: 0,
      size: FETCH_SIZE,
    }),
    [],
  )

  const paymentsQuery = usePayments(params)
  const revenueQuery = useRevenueSummary()
  const cancelPayment = useCancelPayment()

  const filteredPayments = useMemo(() => {
    return (paymentsQuery.data?.data ?? []).filter((payment) => {
      if (tab === 'valid' && payment.status !== 'VALID') {
        return false
      }

      if (tab === 'canceled' && payment.status !== 'CANCELED') {
        return false
      }

      if (statusFilter && payment.status !== statusFilter) {
        return false
      }

      if (methodFilter && payment.method !== methodFilter) {
        return false
      }

      if (!matchesKeyword(keyword, payment.studentName, payment.invoiceCode)) {
        return false
      }

      const paymentDate = dayjs(payment.paymentDate)
      if (paymentDate.isBefore(dateRange[0], 'day') || paymentDate.isAfter(dateRange[1], 'day')) {
        return false
      }

      return true
    })
  }, [dateRange, keyword, methodFilter, paymentsQuery.data?.data, statusFilter, tab])

  const pagedPayments = useMemo(
    () => paginateItems(filteredPayments, page, size),
    [filteredPayments, page, size],
  )

  const methodSummary = useMemo(() => {
    const monthStart = dayjs().startOf('month')
    const today = dayjs()
    const validPayments = (paymentsQuery.data?.data ?? []).filter((payment) => {
      if (payment.status !== 'VALID') {
        return false
      }

      const paymentDate = dayjs(payment.paymentDate)
      return !paymentDate.isBefore(monthStart, 'day') && !paymentDate.isAfter(today, 'day')
    })

    return {
      cash: validPayments
        .filter((payment) => payment.method === 'CASH')
        .reduce((sum, payment) => sum + payment.amount, 0),
      bankTransfer: validPayments
        .filter((payment) => payment.method === 'BANK_TRANSFER')
        .reduce((sum, payment) => sum + payment.amount, 0),
    }
  }, [paymentsQuery.data?.data])

  const columns: ColumnsType<Payment> = [
    {
      title: 'Ngày thu',
      dataIndex: 'paymentDate',
      key: 'paymentDate',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
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
      title: 'Mã học phí',
      dataIndex: 'invoiceCode',
      key: 'invoiceCode',
    },
    {
      title: 'Số tiền',
      dataIndex: 'amount',
      key: 'amount',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Phương thức',
      dataIndex: 'method',
      key: 'method',
      render: (method: PaymentMethod) => paymentMethodLabels[method],
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <StatusTag status={status} labels={paymentStatusLabels} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, payment) =>
        payment.status === 'VALID' ? (
          <Button type="link" danger onClick={() => setCancelingPayment(payment)}>
            Hủy
          </Button>
        ) : (
          <Button type="link" onClick={() => showCancelReason(payment)}>
            Xem lý do hủy
          </Button>
        ),
    },
  ]

  function showCancelReason(payment: Payment) {
    Modal.info({
      title: 'Lý do hủy thanh toán',
      content: (
        <Space direction="vertical" size={4}>
          <Text>Mã thanh toán: {payment.paymentCode}</Text>
          <Text>Lý do: {payment.cancelReason || 'Không có ghi nhận'}</Text>
          {payment.canceledAt ? (
            <Text type="secondary">
              Thời gian hủy: {dayjs(payment.canceledAt).format('DD/MM/YYYY HH:mm')}
            </Text>
          ) : null}
        </Space>
      ),
      okText: 'Đóng',
    })
  }

  function handleTableChange(pagination: TablePaginationConfig) {
    setPage((pagination.current ?? 1) - 1)
    setSize(pagination.pageSize ?? 10)
  }

  function handleTabChange(nextTab: string) {
    setTab(nextTab as PaymentTab)
    setStatusFilter(undefined)
    setPage(0)
  }

  function handleKeywordChange(value: string) {
    setKeyword(value)
    setPage(0)
  }

  function handleMethodChange(value?: PaymentMethod) {
    setMethodFilter(value)
    setPage(0)
  }

  function handleStatusChange(value?: PaymentStatus) {
    setStatusFilter(value)
    setPage(0)
  }

  function handleDateRangeChange(values: [Dayjs | null, Dayjs | null] | null) {
    if (values?.[0] && values[1]) {
      setDateRange([values[0], values[1]])
    } else {
      setDateRange(defaultMonthRange())
    }
    setPage(0)
  }

  function handleCancelPayment(payload: CancelPaymentPayload) {
    if (!cancelingPayment) {
      return
    }

    cancelPayment.mutate(
      { paymentId: cancelingPayment.id, payload },
      {
        onSuccess: () => {
          message.success('Đã hủy thanh toán')
          setCancelingPayment(undefined)
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

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Thanh toán
        </Title>
        <Text type="secondary">Lịch sử thu tiền và đối soát thanh toán.</Text>
      </Space>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={revenueQuery.isLoading}>
            <Statistic
              title="Thu hôm nay"
              value={revenueQuery.data?.todayRevenue ?? 0}
              formatter={(value) => `${Number(value).toLocaleString('en-US')} VND`}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={revenueQuery.isLoading}>
            <Statistic
              title="Thu tháng này"
              value={revenueQuery.data?.monthRevenue ?? 0}
              formatter={(value) => `${Number(value).toLocaleString('en-US')} VND`}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={paymentsQuery.isLoading}>
            <Statistic
              title="Tiền mặt"
              value={methodSummary.cash}
              formatter={(value) => `${Number(value).toLocaleString('en-US')} VND`}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={paymentsQuery.isLoading}>
            <Statistic
              title="Chuyển khoản"
              value={methodSummary.bankTransfer}
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
              { key: 'valid', label: 'Hợp lệ' },
              { key: 'canceled', label: 'Đã hủy' },
              { key: 'all', label: 'Tất cả' },
            ]}
          />

          <Space wrap>
            <RangePicker
              allowClear={false}
              format="DD/MM/YYYY"
              placeholder={['Từ ngày', 'Đến ngày']}
              value={dateRange}
              onChange={handleDateRangeChange}
            />
            <Input.Search
              allowClear
              placeholder="Tìm học viên hoặc mã học phí"
              style={{ width: 280 }}
              value={keyword}
              onChange={(event) => handleKeywordChange(event.target.value)}
            />
            <Select
              allowClear
              placeholder="Phương thức"
              style={{ width: 180 }}
              value={methodFilter}
              onChange={handleMethodChange}
              options={[
                { label: 'Tiền mặt', value: 'CASH' },
                { label: 'Chuyển khoản', value: 'BANK_TRANSFER' },
                { label: 'Khác', value: 'OTHER' },
              ]}
            />
            {tab === 'all' && (
              <Select
                allowClear
                placeholder="Trạng thái"
                style={{ width: 160 }}
                value={statusFilter}
                onChange={handleStatusChange}
                options={[
                  { label: 'Hợp lệ', value: 'VALID' },
                  { label: 'Đã hủy', value: 'CANCELED' },
                ]}
              />
            )}
          </Space>

          <Table
            rowKey="id"
            columns={columns}
            dataSource={pagedPayments}
            loading={paymentsQuery.isLoading}
            pagination={{
              current: page + 1,
              pageSize: size,
              total: filteredPayments.length,
              showSizeChanger: true,
            }}
            onChange={handleTableChange}
          />
        </Space>
      </Card>

      <CancelPaymentModal
        open={Boolean(cancelingPayment)}
        payment={cancelingPayment}
        submitting={cancelPayment.isPending}
        onCancel={() => setCancelingPayment(undefined)}
        onSubmit={handleCancelPayment}
      />
    </Space>
  )
}
