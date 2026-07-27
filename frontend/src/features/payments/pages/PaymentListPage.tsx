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
import type { TablePaginationConfig } from 'antd/es/table'
import type { ColumnsType } from 'antd/es/table'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { isAxiosError } from 'axios'
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import {
  STUDENT_SEARCH_PLACEHOLDER,
  studentKeywordFields,
  studentNameColumn,
} from '../../../components/common/studentDisplay'
import { matchesKeyword, paginateItems } from '../../../utils/clientPagination'
import { useClassrooms } from '../../classrooms/classroomQueries'
import { StudentPaymentHistoryDrawer } from '../../financial/components/StudentPaymentHistoryDrawer'
import type { StudentPaymentSummary } from '../../financial/financialSummaryTypes'
import { CancelPaymentModal } from '../components/CancelPaymentModal'
import { useCancelPayment, usePayments, usePaymentStudentSummaries } from '../paymentQueries'
import {
  PAYMENT_METHOD_LABELS,
  PAYMENT_STATUS_LABELS,
  type CancelPaymentPayload,
  type Payment,
  type PaymentMethod,
  type PaymentSearchParams,
  type PaymentStatus,
} from '../paymentTypes'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

type PaymentTab = 'valid' | 'by-student' | 'canceled' | 'all'

const FETCH_SIZE = 100

function defaultMonthRange(): [Dayjs, Dayjs] {
  const today = dayjs()
  return [today.startOf('month'), today]
}

function summaryRowKey(summary: StudentPaymentSummary) {
  return `${summary.studentId}-${summary.classroomId}`
}

export function PaymentListPage() {
  const [tab, setTab] = useState<PaymentTab>('valid')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [classroomId, setClassroomId] = useState<number>()
  const [methodFilter, setMethodFilter] = useState<PaymentMethod>()
  const [statusFilter, setStatusFilter] = useState<PaymentStatus>()
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>(defaultMonthRange)
  const [cancelingPayment, setCancelingPayment] = useState<Payment>()
  const [historySummary, setHistorySummary] = useState<StudentPaymentSummary>()

  const params: PaymentSearchParams = useMemo(
    () => ({
      page: 0,
      size: FETCH_SIZE,
    }),
    [],
  )

  const summaryParams = useMemo(
    () => ({
      fromDate: dateRange[0].format('YYYY-MM-DD'),
      toDate: dateRange[1].format('YYYY-MM-DD'),
    }),
    [dateRange],
  )

  const isStudentTab = tab === 'by-student'
  const paymentsQuery = usePayments(params)
  const studentSummariesQuery = usePaymentStudentSummaries(summaryParams, isStudentTab)
  const classroomsQuery = useClassrooms({ page: 0, size: 100 })
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

      if (classroomId && payment.classroomId !== classroomId) {
        return false
      }

      if (
        !matchesKeyword(
          keyword,
          ...studentKeywordFields(payment),
          payment.invoiceCode,
          payment.paymentCode,
          payment.billingLabel ?? '',
        )
      ) {
        return false
      }

      const paymentDate = dayjs(payment.paymentDate)
      if (paymentDate.isBefore(dateRange[0], 'day') || paymentDate.isAfter(dateRange[1], 'day')) {
        return false
      }

      return true
    })
  }, [classroomId, dateRange, keyword, methodFilter, paymentsQuery.data?.data, statusFilter, tab])

  const pagedPayments = useMemo(
    () => paginateItems(filteredPayments, page, size),
    [filteredPayments, page, size],
  )

  const filteredStudentSummaries = useMemo(() => {
    return (studentSummariesQuery.data?.data ?? []).filter((summary) => {
      if (classroomId && summary.classroomId !== classroomId) {
        return false
      }

      return matchesKeyword(keyword, ...studentKeywordFields(summary), summary.classroomName)
    })
  }, [classroomId, keyword, studentSummariesQuery.data?.data])

  const pagedStudentSummaries = useMemo(
    () => paginateItems(filteredStudentSummaries, page, size),
    [filteredStudentSummaries, page, size],
  )

  const rangeSummary = useMemo(() => {
    const validPayments = filteredPayments.filter((payment) => payment.status === 'VALID')

    return {
      totalCollected: validPayments.reduce((sum, payment) => sum + payment.amount, 0),
      paymentCount: validPayments.length,
      cash: validPayments
        .filter((payment) => payment.method === 'CASH')
        .reduce((sum, payment) => sum + payment.amount, 0),
      bankTransfer: validPayments
        .filter((payment) => payment.method === 'BANK_TRANSFER')
        .reduce((sum, payment) => sum + payment.amount, 0),
    }
  }, [filteredPayments])

  const studentSummaryColumns: ColumnsType<StudentPaymentSummary> = [
    { title: 'Mã HV', dataIndex: 'studentCode', key: 'studentCode' },
    studentNameColumn(),
    {
      title: 'Lớp',
      dataIndex: 'classroomName',
      key: 'classroomName',
    },
    {
      title: 'Tổng đã đóng',
      dataIndex: 'totalPaidAmount',
      key: 'totalPaidAmount',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Số lần thanh toán',
      dataIndex: 'paymentCount',
      key: 'paymentCount',
    },
    {
      title: 'Lần thanh toán gần nhất',
      dataIndex: 'lastPaymentDate',
      key: 'lastPaymentDate',
      render: (value?: string | null) => (value ? dayjs(value).format('DD/MM/YYYY') : '-'),
    },
    {
      title: 'Phương thức gần nhất',
      dataIndex: 'lastPaymentMethod',
      key: 'lastPaymentMethod',
      render: (method?: PaymentMethod | null) => (method ? PAYMENT_METHOD_LABELS[method] : '-'),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, summary) => (
        <Button type="link" onClick={() => setHistorySummary(summary)}>
          Xem lịch sử thanh toán
        </Button>
      ),
    },
  ]

  const columns: ColumnsType<Payment> = [
    {
      title: 'Ngày thu',
      dataIndex: 'paymentDate',
      key: 'paymentDate',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
    },
    {
      title: 'Mã phiếu',
      dataIndex: 'paymentCode',
      key: 'paymentCode',
    },
    {
      title: 'Học viên',
      key: 'student',
      render: (_, payment) => (
        <Space direction="vertical" size={0}>
          <Text>{payment.studentName}</Text>
          <Text type="secondary">{payment.studentCode}</Text>
        </Space>
      ),
    },
    {
      title: 'Lớp',
      key: 'classroom',
      render: (_, payment) => payment.classroomCode || payment.classroomName,
    },
    {
      title: 'Kỳ học phí',
      dataIndex: 'billingLabel',
      key: 'billingLabel',
      render: (value?: string | null) => value || '-',
    },
    {
      title: 'Mã HĐ',
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
      render: (method: PaymentMethod) => PAYMENT_METHOD_LABELS[method],
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <StatusTag status={status} labels={PAYMENT_STATUS_LABELS} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, payment) => (
        <Space size="small" wrap>
          <Link to={`/print/payments/${payment.id}`}>
            In phiếu thu
          </Link>
          {payment.status === 'VALID' ? (
            <Button type="link" danger onClick={() => setCancelingPayment(payment)}>
              Hủy
            </Button>
          ) : (
            <Button type="link" onClick={() => showCancelReason(payment)}>
              Xem lý do hủy
            </Button>
          )}
        </Space>
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
          {payment.canceledBy ? <Text type="secondary">Người hủy: {payment.canceledBy}</Text> : null}
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

  function handleClassroomChange(value?: number) {
    setClassroomId(value)
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

  const moneyFormatter = (value: number | string) => `${Number(value).toLocaleString('vi-VN')} VND`

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Thanh toán
        </Title>
        <Text type="secondary">Lịch sử thu tiền và in phiếu thu theo kỳ học phí.</Text>
      </Space>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={paymentsQuery.isLoading}>
            <Statistic
              title="Tổng đã thu"
              value={rangeSummary.totalCollected}
              formatter={(value) => moneyFormatter(value as number)}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={paymentsQuery.isLoading}>
            <Statistic title="Số phiếu thu" value={rangeSummary.paymentCount} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={paymentsQuery.isLoading}>
            <Statistic
              title="Tiền mặt"
              value={rangeSummary.cash}
              formatter={(value) => moneyFormatter(value as number)}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={paymentsQuery.isLoading}>
            <Statistic
              title="Chuyển khoản"
              value={rangeSummary.bankTransfer}
              formatter={(value) => moneyFormatter(value as number)}
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
              { key: 'valid', label: 'Phiếu thu hợp lệ' },
              { key: 'by-student', label: 'Theo học viên' },
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
              placeholder={
                isStudentTab ? STUDENT_SEARCH_PLACEHOLDER : 'Tìm học viên, mã HĐ, kỳ học phí...'
              }
              style={{ width: 280 }}
              value={keyword}
              onChange={(event) => handleKeywordChange(event.target.value)}
            />
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
            )}
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

          {isStudentTab ? (
            <Table
              rowKey={summaryRowKey}
              columns={studentSummaryColumns}
              dataSource={pagedStudentSummaries}
              loading={studentSummariesQuery.isLoading}
              pagination={{
                current: page + 1,
                pageSize: size,
                total: filteredStudentSummaries.length,
                showSizeChanger: true,
              }}
              onChange={handleTableChange}
              locale={{ emptyText: 'Chưa có giao dịch thanh toán.' }}
              scroll={{ x: 900 }}
            />
          ) : (
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
              locale={{ emptyText: 'Chưa có giao dịch thanh toán.' }}
              scroll={{ x: 1200 }}
            />
          )}
        </Space>
      </Card>

      <CancelPaymentModal
        open={Boolean(cancelingPayment)}
        payment={cancelingPayment}
        submitting={cancelPayment.isPending}
        onCancel={() => setCancelingPayment(undefined)}
        onSubmit={handleCancelPayment}
      />

      <StudentPaymentHistoryDrawer
        open={Boolean(historySummary)}
        studentId={historySummary?.studentId}
        classroomId={historySummary?.classroomId}
        studentCode={historySummary?.studentCode}
        studentName={historySummary?.studentName}
        classroomName={historySummary?.classroomName}
        fromDate={summaryParams.fromDate}
        toDate={summaryParams.toDate}
        onClose={() => setHistorySummary(undefined)}
      />
    </Space>
  )
}
