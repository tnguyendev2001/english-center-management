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
import { useSearchParams } from 'react-router-dom'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import {
  STUDENT_SEARCH_PLACEHOLDER,
  studentCodeColumn,
  studentNameColumn,
} from '../../../components/common/studentDisplay'
import { useClassrooms } from '../../classrooms/classroomQueries'
import { useDebtStudentSummaries } from '../../debts/debtQueries'
import { StudentInvoiceListDrawer } from '../../financial/components/StudentInvoiceListDrawer'
import type { StudentDebtSummary, StudentTuitionSummary } from '../../financial/financialSummaryTypes'
import { PaymentFormModal } from '../../payments/components/PaymentFormModal'
import { useCreatePayment } from '../../payments/paymentQueries'
import type { CreatePaymentPayload } from '../../payments/paymentTypes'
import { useRevenueSummary } from '../../revenue/revenueQueries'
import { useTuitionPackages } from '../../tuitionPackages/tuitionPackageQueries'
import { InvoiceDetailModal } from '../components/InvoiceDetailModal'
import { getCollectibleInvoice } from '../findCollectibleInvoice'
import { useInvoices, useTuitionOverview, useTuitionStudentSummaries } from '../invoiceQueries'
import type { Invoice, InvoiceSearchParams, InvoiceStatus } from '../invoiceTypes'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

type TuitionTab = 'student' | 'debt' | 'invoice'

const invoiceStatusLabels = {
  UNPAID: 'Chưa đóng',
  PARTIALLY_PAID: 'Đóng một phần',
  PAID: 'Đã đóng',
  CANCELED: 'Đã hủy',
  REPLACED: 'Đã thay thế do đổi gói',
}

function parseTab(value: string | null, hasKeyword: boolean): TuitionTab {
  if (value === 'debt' || value === 'invoice' || value === 'student') {
    return value
  }

  return hasKeyword ? 'invoice' : 'student'
}

export function InvoiceListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const tab = parseTab(searchParams.get('tab'), Boolean(searchParams.get('keyword')))
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [keyword, setKeyword] = useState(() => searchParams.get('keyword') ?? '')
  const [statusFilter, setStatusFilter] = useState<InvoiceStatus>()
  const [classroomId, setClassroomId] = useState<number>()
  const [packageName, setPackageName] = useState<string>()
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>()
  const [collectingInvoice, setCollectingInvoice] = useState<Invoice>()
  const [collectingKey, setCollectingKey] = useState<string>()
  const [detailInvoice, setDetailInvoice] = useState<Invoice>()
  const [detailStudent, setDetailStudent] = useState<{
    studentId: number
    studentCode: string
    studentName: string
    currentClassroomName?: string | null
  }>()

  const studentParams = useMemo(
    () => ({
      keyword: keyword || undefined,
      page,
      size,
    }),
    [keyword, page, size],
  )

  const invoiceParams: InvoiceSearchParams = useMemo(
    () => ({
      status: statusFilter,
      classroomId,
      keyword: keyword || undefined,
      packageName,
      dueFrom: dateRange?.[0].format('YYYY-MM-DD'),
      dueTo: dateRange?.[1].format('YYYY-MM-DD'),
      page,
      size,
    }),
    [classroomId, dateRange, keyword, packageName, page, size, statusFilter],
  )

  const studentSummariesQuery = useTuitionStudentSummaries(studentParams, tab === 'student')
  const debtSummariesQuery = useDebtStudentSummaries(studentParams, tab === 'debt')
  const invoicesQuery = useInvoices(invoiceParams, tab === 'invoice')
  const overviewQuery = useTuitionOverview()
  const revenueQuery = useRevenueSummary()
  const classroomsQuery = useClassrooms({ page: 0, size: 100 })
  const packagesQuery = useTuitionPackages({ page: 0, size: 100 })
  const createPayment = useCreatePayment()

  const studentColumns: ColumnsType<StudentTuitionSummary> = [
    studentCodeColumn(),
    {
      ...studentNameColumn(),
      render: (value: string, summary) => (
        <Button type="link" onClick={() => openStudentDetail(summary)}>
          {value}
        </Button>
      ),
    },
    {
      title: 'Lớp hiện tại',
      dataIndex: 'currentClassroomName',
      key: 'currentClassroomName',
      render: (value?: string | null) => value || '-',
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
            <Button
              type="link"
              loading={collectingKey === String(summary.studentId)}
              onClick={() => handleCollect(summary.studentId)}
            >
              Thu tiền
            </Button>
          ) : null}
          <Button type="link" onClick={() => openStudentDetail(summary)}>
            Xem chi tiết
          </Button>
        </Space>
      ),
    },
  ]

  const debtColumns: ColumnsType<StudentDebtSummary> = [
    studentCodeColumn(),
    {
      ...studentNameColumn(),
      render: (value: string, summary) => (
        <Button type="link" onClick={() => openStudentDetail(summary)}>
          {value}
        </Button>
      ),
    },
    {
      title: 'Lớp hiện tại',
      dataIndex: 'currentClassroomName',
      key: 'currentClassroomName',
      render: (value?: string | null) => value || '-',
    },
    {
      title: 'Tổng công nợ',
      dataIndex: 'totalRemainingDebt',
      key: 'totalRemainingDebt',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Số hóa đơn nợ',
      dataIndex: 'debtInvoiceCount',
      key: 'debtInvoiceCount',
    },
    {
      title: 'Hóa đơn chưa đóng',
      dataIndex: 'unpaidCount',
      key: 'unpaidCount',
    },
    {
      title: 'Hóa đơn đóng một phần',
      dataIndex: 'partialCount',
      key: 'partialCount',
    },
    {
      title: 'Hạn đóng gần nhất',
      dataIndex: 'nearestDueDate',
      key: 'nearestDueDate',
      render: (value?: string | null) => (value ? dayjs(value).format('DD/MM/YYYY') : '-'),
    },
    {
      title: 'Trạng thái',
      key: 'status',
      render: () => <Tag color="orange">Còn nợ</Tag>,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, summary) => (
        <Space size="small">
          <Button
            type="link"
            loading={collectingKey === String(summary.studentId)}
            onClick={() => handleCollect(summary.studentId)}
          >
            Thu tiền
          </Button>
          <Button type="link" onClick={() => openStudentDetail(summary)}>
            Xem chi tiết
          </Button>
        </Space>
      ),
    },
  ]

  const invoiceColumns: ColumnsType<Invoice> = [
    {
      title: 'Mã hóa đơn',
      dataIndex: 'invoiceCode',
      key: 'invoiceCode',
    },
    studentCodeColumn(),
    studentNameColumn(),
    {
      title: 'Lớp phát sinh',
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
      title: 'Hạn đóng',
      dataIndex: 'dueDate',
      key: 'dueDate',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
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

  function openStudentDetail(summary: StudentTuitionSummary | StudentDebtSummary) {
    setDetailStudent({
      studentId: summary.studentId,
      studentCode: summary.studentCode,
      studentName: summary.studentName,
      currentClassroomName: summary.currentClassroomName,
    })
  }

  async function handleCollect(studentId: number) {
    setCollectingKey(String(studentId))

    try {
      const invoice = await getCollectibleInvoice(studentId)

      if (!invoice) {
        message.warning('Không tìm thấy hóa đơn cần thu')
        return
      }

      setCollectingInvoice(invoice)
    } catch (error) {
      showErrorMessage(error)
    } finally {
      setCollectingKey(undefined)
    }
  }

  function handleTableChange(pagination: TablePaginationConfig) {
    setPage((pagination.current ?? 1) - 1)
    setSize(pagination.pageSize ?? 10)
  }

  function handleTabChange(nextTab: string) {
    const nextParams = new URLSearchParams(searchParams)
    nextParams.set('tab', nextTab)
    setSearchParams(nextParams, { replace: true })
    setPage(0)
  }

  function handleKeywordChange(value: string) {
    setKeyword(value)
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

  const currentTable =
    tab === 'student' ? (
      <Table
        rowKey="studentId"
        columns={studentColumns}
        dataSource={studentSummariesQuery.data?.data ?? []}
        loading={studentSummariesQuery.isLoading}
        pagination={{
          current: page + 1,
          pageSize: size,
          total: studentSummariesQuery.data?.meta?.totalElements ?? 0,
          showSizeChanger: true,
        }}
        onChange={handleTableChange}
        locale={{ emptyText: 'Không có học phí' }}
        scroll={{ x: 1100 }}
      />
    ) : tab === 'debt' ? (
      <Table
        rowKey="studentId"
        columns={debtColumns}
        dataSource={debtSummariesQuery.data?.data ?? []}
        loading={debtSummariesQuery.isLoading}
        pagination={{
          current: page + 1,
          pageSize: size,
          total: debtSummariesQuery.data?.meta?.totalElements ?? 0,
          showSizeChanger: true,
        }}
        onChange={handleTableChange}
        locale={{ emptyText: 'Không có công nợ' }}
        scroll={{ x: 1200 }}
      />
    ) : (
      <Table
        rowKey="id"
        columns={invoiceColumns}
        dataSource={invoicesQuery.data?.data ?? []}
        loading={invoicesQuery.isLoading}
        pagination={{
          current: page + 1,
          pageSize: size,
          total: invoicesQuery.data?.meta?.totalElements ?? 0,
          showSizeChanger: true,
        }}
        onChange={handleTableChange}
        locale={{ emptyText: 'Không có hóa đơn' }}
        scroll={{ x: 1200 }}
      />
    )

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Học phí
        </Title>
        <Text type="secondary">Theo dõi học phí, công nợ và hóa đơn theo học viên.</Text>
      </Space>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={5}>
          <Card loading={overviewQuery.isLoading}>
            <Statistic
              title="Tổng cần thu"
              value={overviewQuery.data?.totalOutstanding ?? 0}
              formatter={(value) => `${Number(value).toLocaleString('en-US')} VND`}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={4}>
          <Card loading={overviewQuery.isLoading}>
            <Statistic title="Số học viên còn nợ" value={overviewQuery.data?.studentsWithDebt ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={5}>
          <Card loading={overviewQuery.isLoading}>
            <Statistic title="Hóa đơn chưa đóng" value={overviewQuery.data?.unpaidInvoiceCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={5}>
          <Card loading={overviewQuery.isLoading}>
            <Statistic title="Hóa đơn đóng một phần" value={overviewQuery.data?.partialInvoiceCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={5}>
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
              { key: 'student', label: 'Theo học viên' },
              { key: 'debt', label: 'Công nợ' },
              { key: 'invoice', label: 'Hóa đơn' },
            ]}
          />

          <Space wrap>
            <Input.Search
              allowClear
              placeholder={STUDENT_SEARCH_PLACEHOLDER}
              style={{ width: 320 }}
              value={keyword}
              onChange={(event) => handleKeywordChange(event.target.value)}
            />
            {tab === 'invoice' ? (
              <>
                <Select
                  allowClear
                  placeholder="Trạng thái hóa đơn"
                  style={{ width: 180 }}
                  value={statusFilter}
                  onChange={(value) => {
                    setStatusFilter(value)
                    setPage(0)
                  }}
                  options={[
                    { label: 'Chưa đóng', value: 'UNPAID' },
                    { label: 'Đóng một phần', value: 'PARTIALLY_PAID' },
                    { label: 'Đã đóng', value: 'PAID' },
                    { label: 'Đã thay thế', value: 'REPLACED' },
                    { label: 'Đã hủy', value: 'CANCELED' },
                  ]}
                />
                <Select
                  allowClear
                  placeholder="Lớp phát sinh"
                  style={{ width: 220 }}
                  value={classroomId}
                  onChange={(value) => {
                    setClassroomId(value)
                    setPage(0)
                  }}
                  options={(classroomsQuery.data?.data ?? []).map((classroom) => ({
                    label: classroom.className,
                    value: classroom.id,
                  }))}
                />
                <Select
                  allowClear
                  placeholder="Gói học phí"
                  style={{ width: 220 }}
                  value={packageName}
                  onChange={(value) => {
                    setPackageName(value)
                    setPage(0)
                  }}
                  options={(packagesQuery.data?.data ?? []).map((tuitionPackage) => ({
                    label: tuitionPackage.name,
                    value: tuitionPackage.name,
                  }))}
                />
                <RangePicker
                  allowClear
                  format="DD/MM/YYYY"
                  placeholder={['Hạn từ', 'Hạn đến']}
                  value={dateRange}
                  onChange={(values) => {
                    if (values?.[0] && values[1]) {
                      setDateRange([values[0], values[1]])
                    } else {
                      setDateRange(undefined)
                    }
                    setPage(0)
                  }}
                />
              </>
            ) : null}
          </Space>

          {currentTable}
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
        open={Boolean(detailStudent)}
        studentId={detailStudent?.studentId}
        studentCode={detailStudent?.studentCode}
        studentName={detailStudent?.studentName}
        currentClassroomName={detailStudent?.currentClassroomName}
        onClose={() => setDetailStudent(undefined)}
        onCollect={(invoice) => {
          setDetailStudent(undefined)
          setCollectingInvoice(invoice)
        }}
      />
    </Space>
  )
}
