import {
  Button,
  Card,
  Col,
  DatePicker,
  Empty,
  Input,
  message,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import dayjs, { type Dayjs } from 'dayjs'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import {
  STUDENT_SEARCH_PLACEHOLDER,
  studentCodeColumn,
  studentNameColumn,
} from '../../../components/common/studentDisplay'
import type { Invoice, InvoiceStatus } from '../../invoices/invoiceTypes'
import { PaymentFormModal } from '../../payments/components/PaymentFormModal'
import { useCreatePayment } from '../../payments/paymentQueries'
import { useClassrooms } from '../../classrooms/classroomQueries'
import type { CreatePaymentPayload, Payment, PaymentMethod, PaymentStatus } from '../../payments/paymentTypes'
import {
  debtItemToInvoice,
  type AttendanceReportItem,
  type AttendanceReportStatus,
  type DebtReportItem,
  type EnrollmentProgressReportItem,
} from '../reportTypes'
import {
  useAttendanceReport,
  useDebtReport,
  useDebtReportSummary,
  useEnrollmentProgressReport,
  useInvoiceReport,
  usePaymentReport,
} from '../reportQueries'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

const invoiceStatusLabels: Record<string, string> = {
  UNPAID: 'Chưa đóng',
  PARTIALLY_PAID: 'Đóng một phần',
  PAID: 'Đã đóng',
  CANCELED: 'Đã hủy',
  REPLACED: 'Đã thay thế do đổi gói',
}

const paymentMethodLabels: Record<PaymentMethod, string> = {
  CASH: 'Tiền mặt',
  BANK_TRANSFER: 'Chuyển khoản',
  OTHER: 'Khác',
}

const attendanceStatusLabels: Record<AttendanceReportStatus, string> = {
  PRESENT: 'Có mặt',
  ABSENT: 'Vắng',
  EXCUSED: 'Xin nghỉ',
}

const progressFilterOptions = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'DEPLETED', label: 'Hết buổi' },
  { value: 'LOW', label: 'Sắp hết buổi' },
  { value: 'OK', label: 'Bình thường' },
]

export function ReportsPage() {
  const [activeTab, setActiveTab] = useState('payments')

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Báo cáo
        </Title>
        <Text type="secondary">
          Xem chi tiết thanh toán, công nợ, học phí, điểm danh và tiến độ học để đối soát.
        </Text>
      </Space>

      <Card>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            { key: 'payments', label: 'Thanh toán', children: <PaymentReportTab /> },
            { key: 'debts', label: 'Công nợ', children: <DebtReportTab /> },
            { key: 'invoices', label: 'Học phí', children: <InvoiceReportTab /> },
            { key: 'attendance', label: 'Điểm danh', children: <AttendanceReportTab /> },
            { key: 'progress', label: 'Tiến độ học', children: <EnrollmentProgressReportTab /> },
          ]}
        />
      </Card>
    </Space>
  )
}

function PaymentReportTab() {
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs] | null>(null)
  const [keyword, setKeyword] = useState('')
  const [classroomId, setClassroomId] = useState<number>()
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>()
  const [paymentStatus, setPaymentStatus] = useState<PaymentStatus>()
  const classroomsQuery = useClassrooms({ page: 0, size: 100 })

  const params = useMemo(
    () => ({
      fromDate: dateRange?.[0]?.format('YYYY-MM-DD'),
      toDate: dateRange?.[1]?.format('YYYY-MM-DD'),
      keyword: keyword || undefined,
      classroomId,
      paymentMethod,
      paymentStatus,
      page: 0,
      size: 200,
    }),
    [dateRange, keyword, classroomId, paymentMethod, paymentStatus],
  )

  const reportQuery = usePaymentReport(params)
  const report = reportQuery.data

  const columns: ColumnsType<Payment> = [
    {
      title: 'Ngày thanh toán',
      dataIndex: 'paymentDate',
      key: 'paymentDate',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
    },
    { title: 'Mã thanh toán', dataIndex: 'paymentCode', key: 'paymentCode' },
    studentCodeColumn(),
    studentNameColumn(),
    { title: 'Lớp', dataIndex: 'classroomName', key: 'classroomName' },
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
      render: (method: PaymentMethod) => paymentMethodLabels[method] ?? method,
    },
    { title: 'Mã hóa đơn', dataIndex: 'invoiceCode', key: 'invoiceCode' },
    {
      title: 'Ghi chú',
      dataIndex: 'note',
      key: 'note',
      render: (value?: string | null) => value ?? '-',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <StatusTag status={status} />,
    },
  ]

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Space wrap>
        <RangePicker value={dateRange} onChange={(values) => setDateRange(values as [Dayjs, Dayjs] | null)} />
        <Input.Search
          allowClear
          placeholder={STUDENT_SEARCH_PLACEHOLDER}
          style={{ width: 240 }}
          onSearch={setKeyword}
        />
        <Select
          allowClear
          placeholder="Lớp"
          style={{ width: 200 }}
          options={(classroomsQuery.data?.data ?? []).map((classroom) => ({
            value: classroom.id,
            label: classroom.className,
          }))}
          onChange={(value) => setClassroomId(value)}
        />
        <Select
          allowClear
          placeholder="Phương thức"
          style={{ width: 160 }}
          options={Object.entries(paymentMethodLabels).map(([value, label]) => ({ value, label }))}
          onChange={(value) => setPaymentMethod(value)}
        />
        <Select
          allowClear
          placeholder="Trạng thái"
          style={{ width: 160 }}
          options={[
            { value: 'VALID', label: 'Hợp lệ' },
            { value: 'CANCELED', label: 'Đã hủy' },
          ]}
          onChange={(value) => setPaymentStatus(value)}
        />
      </Space>

      <Row gutter={16}>
        <Col xs={24} sm={12} md={6}>
          <Card loading={reportQuery.isLoading}>
            <Statistic
              title="Tổng doanh thu"
              value={report?.totalRevenue ?? 0}
              formatter={(value) => <MoneyText value={Number(value)} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card loading={reportQuery.isLoading}>
            <Statistic title="Số phiếu thu" value={report?.paymentCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card loading={reportQuery.isLoading}>
            <Statistic
              title="Tiền mặt"
              value={report?.cashAmount ?? 0}
              formatter={(value) => <MoneyText value={Number(value)} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card loading={reportQuery.isLoading}>
            <Statistic
              title="Chuyển khoản"
              value={report?.bankTransferAmount ?? 0}
              formatter={(value) => <MoneyText value={Number(value)} />}
            />
          </Card>
        </Col>
      </Row>

      <Table
        rowKey="id"
        loading={reportQuery.isLoading}
        dataSource={report?.payments ?? []}
        columns={columns}
        pagination={{ pageSize: 15, showSizeChanger: true }}
        locale={{ emptyText: <Empty description="Chưa có thanh toán trong khoảng thời gian này" /> }}
      />
    </Space>
  )
}

function DebtReportTab() {
  const navigate = useNavigate()
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<InvoiceStatus>()
  const [collectingDebt, setCollectingDebt] = useState<DebtReportItem>()
  const createPayment = useCreatePayment()
  const summaryQuery = useDebtReportSummary()
  const debtsQuery = useDebtReport({ keyword: keyword || undefined, status, page: 0, size: 200 })

  const columns: ColumnsType<DebtReportItem> = [
    studentCodeColumn(),
    studentNameColumn(),
    { title: 'Lớp', dataIndex: 'classroomName', key: 'classroomName' },
    {
      title: 'Gói học',
      dataIndex: 'latestPackageName',
      key: 'latestPackageName',
      render: (value?: string | null) => value ?? '-',
    },
    {
      title: 'Số tiền cần đóng',
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
      title: 'Còn nợ',
      dataIndex: 'remainingAmount',
      key: 'remainingAmount',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Hạn đóng',
      dataIndex: 'dueDate',
      key: 'dueDate',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (value: string) => <StatusTag status={value} labels={invoiceStatusLabels} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, item) => (
        <Space size="small" wrap>
          <Button type="link" onClick={() => setCollectingDebt(item)}>
            Thu tiền
          </Button>
          <Button type="link" onClick={() => navigate(`/invoices?keyword=${item.invoiceCode}`)}>
            Xem chi tiết học phí
          </Button>
          <Button type="link" onClick={() => navigate(`/students/${item.studentId}`)}>
            Xem học viên
          </Button>
        </Space>
      ),
    },
  ]

  function handleCreatePayment(payload: CreatePaymentPayload) {
    if (!collectingDebt) return
    createPayment.mutate(
      { invoiceId: collectingDebt.invoiceId, payload },
      {
        onSuccess: () => {
          message.success('Đã thu tiền')
          setCollectingDebt(undefined)
        },
        onError: (error) => {
          if (isAxiosError(error)) {
            message.error(error.response?.data?.message ?? 'Có lỗi xảy ra')
          }
        },
      },
    )
  }

  const summary = summaryQuery.data

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Space wrap>
        <Input.Search allowClear placeholder={STUDENT_SEARCH_PLACEHOLDER} style={{ width: 240 }} onSearch={setKeyword} />
        <Select
          allowClear
          placeholder="Trạng thái học phí"
          style={{ width: 200 }}
          options={[
            { value: 'UNPAID', label: 'Chưa đóng' },
            { value: 'PARTIALLY_PAID', label: 'Đóng một phần' },
          ]}
          onChange={(value) => setStatus(value)}
        />
      </Space>

      <Row gutter={16}>
        <Col xs={24} sm={12} md={6}>
          <Card loading={summaryQuery.isLoading}>
            <Statistic
              title="Tổng công nợ"
              value={summary?.totalDebtAmount ?? 0}
              formatter={(value) => <MoneyText value={Number(value)} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card loading={summaryQuery.isLoading}>
            <Statistic title="Số học viên còn nợ" value={summary?.studentsWithDebtCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card loading={summaryQuery.isLoading}>
            <Statistic title="Hóa đơn chưa đóng" value={summary?.unpaidInvoiceCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card loading={summaryQuery.isLoading}>
            <Statistic title="Hóa đơn đóng một phần" value={summary?.partiallyPaidInvoiceCount ?? 0} />
          </Card>
        </Col>
      </Row>

      <Table
        rowKey="invoiceId"
        loading={debtsQuery.isLoading}
        dataSource={debtsQuery.data?.data ?? []}
        columns={columns}
        pagination={{ pageSize: 15, showSizeChanger: true }}
        locale={{ emptyText: <Empty description="Không có công nợ" /> }}
      />

      <PaymentFormModal
        open={Boolean(collectingDebt)}
        invoice={collectingDebt ? debtItemToInvoice(collectingDebt) : undefined}
        submitting={createPayment.isPending}
        onCancel={() => setCollectingDebt(undefined)}
        onSubmit={handleCreatePayment}
      />
    </Space>
  )
}

function InvoiceReportTab() {
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<InvoiceStatus>()
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs] | null>(null)

  const params = useMemo(
    () => ({
      keyword: keyword || undefined,
      status,
      fromDate: dateRange?.[0]?.format('YYYY-MM-DD'),
      toDate: dateRange?.[1]?.format('YYYY-MM-DD'),
      page: 0,
      size: 200,
    }),
    [keyword, status, dateRange],
  )

  const invoicesQuery = useInvoiceReport(params)

  const columns: ColumnsType<Invoice> = [
    { title: 'Mã học phí', dataIndex: 'invoiceCode', key: 'invoiceCode' },
    studentCodeColumn(),
    studentNameColumn(),
    { title: 'Lớp', dataIndex: 'classroomName', key: 'classroomName' },
    { title: 'Gói học', dataIndex: 'packageNameSnapshot', key: 'packageNameSnapshot' },
    { title: 'Số buổi', dataIndex: 'totalSessionsSnapshot', key: 'totalSessionsSnapshot' },
    {
      title: 'Số tiền gốc',
      dataIndex: 'amount',
      key: 'amount',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Giảm giá',
      dataIndex: 'discountAmount',
      key: 'discountAmount',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Cần đóng',
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
      render: (value: string) => <StatusTag status={value} labels={invoiceStatusLabels} />,
    },
    {
      title: 'Ngày tạo',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
    },
  ]

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Space wrap>
        <Input.Search allowClear placeholder="Tìm theo mã hoặc tên học viên, mã học phí" style={{ width: 280 }} onSearch={setKeyword} />
        <Select
          allowClear
          placeholder="Trạng thái"
          style={{ width: 200 }}
          options={Object.entries(invoiceStatusLabels).map(([value, label]) => ({ value, label }))}
          onChange={(value) => setStatus(value)}
        />
        <RangePicker value={dateRange} onChange={(values) => setDateRange(values as [Dayjs, Dayjs] | null)} />
      </Space>

      <Table
        rowKey="id"
        loading={invoicesQuery.isLoading}
        dataSource={invoicesQuery.data?.data ?? []}
        columns={columns}
        pagination={{ pageSize: 15, showSizeChanger: true }}
      />
    </Space>
  )
}

function AttendanceReportTab() {
  const [keyword, setKeyword] = useState('')
  const [sessionDate, setSessionDate] = useState<Dayjs | null>(null)
  const [status, setStatus] = useState<AttendanceReportStatus>()

  const params = useMemo(
    () => ({
      keyword: keyword || undefined,
      sessionDate: sessionDate?.format('YYYY-MM-DD'),
      status,
      page: 0,
      size: 200,
    }),
    [keyword, sessionDate, status],
  )

  const reportQuery = useAttendanceReport(params)
  const report = reportQuery.data
  const summary = report?.summary

  const columns: ColumnsType<AttendanceReportItem> = [
    {
      title: 'Ngày học',
      dataIndex: 'sessionDate',
      key: 'sessionDate',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
    },
    { title: 'Lớp', dataIndex: 'classroomName', key: 'classroomName' },
    studentCodeColumn(),
    studentNameColumn(),
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (value: AttendanceReportStatus) => (
        <Tag color={value === 'PRESENT' ? 'green' : value === 'ABSENT' ? 'red' : 'gold'}>
          {attendanceStatusLabels[value]}
        </Tag>
      ),
    },
    {
      title: 'Ghi chú',
      dataIndex: 'note',
      key: 'note',
      render: (value?: string | null) => value ?? '-',
    },
  ]

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Space wrap>
        <Input.Search allowClear placeholder={STUDENT_SEARCH_PLACEHOLDER} style={{ width: 240 }} onSearch={setKeyword} />
        <DatePicker
          placeholder="Ngày học"
          value={sessionDate}
          onChange={(value) => setSessionDate(value)}
          format="DD/MM/YYYY"
        />
        <Select
          allowClear
          placeholder="Trạng thái điểm danh"
          style={{ width: 180 }}
          options={Object.entries(attendanceStatusLabels).map(([value, label]) => ({ value, label }))}
          onChange={(value) => setStatus(value)}
        />
      </Space>

      <Row gutter={16}>
        <Col xs={24} sm={12} md={6}>
          <Card loading={reportQuery.isLoading}>
            <Statistic title="Tổng lượt điểm danh" value={summary?.totalCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card loading={reportQuery.isLoading}>
            <Statistic title="Có mặt" value={summary?.presentCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card loading={reportQuery.isLoading}>
            <Statistic title="Vắng" value={summary?.absentCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card loading={reportQuery.isLoading}>
            <Statistic title="Xin nghỉ" value={summary?.excusedCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card loading={reportQuery.isLoading}>
            <Statistic
              title="Tỷ lệ có mặt"
              value={summary?.presentRate ?? 0}
              precision={1}
              suffix="%"
            />
          </Card>
        </Col>
      </Row>

      <Table
        rowKey="id"
        loading={reportQuery.isLoading}
        dataSource={report?.items ?? []}
        columns={columns}
        pagination={{ pageSize: 15, showSizeChanger: true }}
      />
    </Space>
  )
}

function EnrollmentProgressReportTab() {
  const navigate = useNavigate()
  const [keyword, setKeyword] = useState('')
  const [warningFilter, setWarningFilter] = useState('ALL')
  const progressQuery = useEnrollmentProgressReport()

  const filteredData = useMemo(() => {
    return (progressQuery.data ?? []).filter((item) => {
      const matchesKeyword =
        !keyword ||
        item.studentCode.toLowerCase().includes(keyword.toLowerCase()) ||
        item.studentName.toLowerCase().includes(keyword.toLowerCase()) ||
        item.classroomName.toLowerCase().includes(keyword.toLowerCase())

      const matchesWarning =
        warningFilter === 'ALL' ||
        (warningFilter === 'DEPLETED' && (item.warningType === 'DEPLETED' || item.warningType === 'OVERUSED')) ||
        (warningFilter === 'LOW' && item.warningType === 'LOW') ||
        (warningFilter === 'OK' && (item.warningType === 'OK' || item.warningType === 'NONE'))

      return matchesKeyword && matchesWarning
    })
  }, [progressQuery.data, keyword, warningFilter])

  const columns: ColumnsType<EnrollmentProgressReportItem> = [
    studentCodeColumn(),
    studentNameColumn(),
    { title: 'Lớp', dataIndex: 'classroomName', key: 'classroomName' },
    { title: 'Tổng buổi', dataIndex: 'totalSessions', key: 'totalSessions' },
    { title: 'Đã dùng', dataIndex: 'usedSessions', key: 'usedSessions' },
    { title: 'Còn lại', dataIndex: 'remainingSessions', key: 'remainingSessions' },
    {
      title: 'Tỷ lệ đã dùng',
      key: 'usageRate',
      render: (_, record) => {
        if (record.totalSessions <= 0) return '0%'
        return `${Math.round((record.usedSessions / record.totalSessions) * 100)}%`
      },
    },
    {
      title: 'Gói gần nhất',
      dataIndex: 'latestPackageName',
      key: 'latestPackageName',
      render: (value?: string | null) => value ?? '-',
    },
    {
      title: 'Trạng thái',
      key: 'warningType',
      render: (_, record) => <ProgressWarningTag item={record} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, record) => (
        <Space size="small" wrap>
          <Button type="link" onClick={() => navigate(`/classrooms/${record.classroomId}`)}>
            Gia hạn
          </Button>
          <Button type="link" onClick={() => navigate(`/students/${record.studentId}`)}>
            Xem học viên
          </Button>
          <Button type="link" onClick={() => navigate(`/classrooms/${record.classroomId}`)}>
            Xem lớp
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Space wrap>
        <Input.Search allowClear placeholder={STUDENT_SEARCH_PLACEHOLDER} style={{ width: 260 }} onSearch={setKeyword} />
        <Select
          style={{ width: 180 }}
          value={warningFilter}
          options={progressFilterOptions}
          onChange={setWarningFilter}
        />
      </Space>

      <Table
        rowKey="enrollmentId"
        loading={progressQuery.isLoading}
        dataSource={filteredData}
        columns={columns}
        pagination={{ pageSize: 15, showSizeChanger: true }}
        locale={{ emptyText: <Empty description="Không có học viên cần gia hạn" /> }}
      />
    </Space>
  )
}

function ProgressWarningTag({ item }: { item: EnrollmentProgressReportItem }) {
  if (item.warningType === 'DEPLETED' || item.warningType === 'OVERUSED') {
    return <Tag color="red">Hết buổi</Tag>
  }
  if (item.warningType === 'LOW') {
    return <Tag color="orange">Sắp hết</Tag>
  }
  return <Tag color="green">Bình thường</Tag>
}
