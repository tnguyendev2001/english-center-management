import {
  Button,
  Card,
  Col,
  Collapse,
  DatePicker,
  Input,
  InputNumber,
  Row,
  Select,
  Space,
  Statistic,
  Switch,
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
import { useCallback, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ActiveFilterTags } from '../../../components/common/ActiveFilterTags'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import {
  STUDENT_SEARCH_PLACEHOLDER,
  studentKeywordFields,
  studentNameColumn,
} from '../../../components/common/studentDisplay'
import { matchesKeyword, paginateItems } from '../../../utils/clientPagination'
import { useClassrooms } from '../../classrooms/classroomQueries'
import { StudentInvoiceListDrawer } from '../../financial/components/StudentInvoiceListDrawer'
import type { StudentTuitionSummary } from '../../financial/financialSummaryTypes'
import { PaymentFormModal } from '../../payments/components/PaymentFormModal'
import { useCreatePayment } from '../../payments/paymentQueries'
import type { CreatePaymentPayload, Payment } from '../../payments/paymentTypes'
import { useTuitionPackages } from '../../tuitionPackages/tuitionPackageQueries'
import { InvoiceDetailModal } from '../components/InvoiceDetailModal'
import { useInvoiceSummary, useInvoices, useTuitionStudentSummaries } from '../invoiceQueries'
import {
  INVOICE_STATUS_LABELS,
  formatEstimatedEffectiveTo,
  type Invoice,
  type InvoiceStatus,
} from '../invoiceTypes'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

type InvoiceTab = 'by-cycle' | 'by-student'

const FETCH_SIZE = 100

function summaryRowKey(summary: StudentTuitionSummary) {
  return `${summary.studentId}-${summary.classroomId}`
}

function parseOptionalBoolean(value: string | null): boolean | undefined {
  if (value === 'true') return true
  if (value === 'false') return false
  return undefined
}

function parseOptionalNumber(value: string | null): number | undefined {
  if (value == null || value === '') return undefined
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
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
  const [searchParams, setSearchParams] = useSearchParams()
  const tab = (searchParams.get('tab') === 'by-student' ? 'by-student' : 'by-cycle') as InvoiceTab
  const keyword = searchParams.get('keyword') ?? ''
  const statusFilter = (searchParams.get('status') as InvoiceStatus | null) ?? undefined
  const classroomId = parseOptionalNumber(searchParams.get('classroomId'))
  const packageId = parseOptionalNumber(searchParams.get('packageId'))
  const cycleNo = parseOptionalNumber(searchParams.get('cycleNo'))
  const overdue = parseOptionalBoolean(searchParams.get('overdue'))
  const hasRemainingDebt = parseOptionalBoolean(searchParams.get('hasRemainingDebt'))
  const dueFrom = searchParams.get('dueFrom') ?? undefined
  const dueTo = searchParams.get('dueTo') ?? undefined
  const effectiveFrom = searchParams.get('effectiveFrom') ?? undefined
  const effectiveTo = searchParams.get('effectiveTo') ?? undefined

  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [collectingInvoice, setCollectingInvoice] = useState<Invoice>()
  const [successPayment, setSuccessPayment] = useState<Payment>()
  const [detailInvoice, setDetailInvoice] = useState<Invoice>()
  const [detailSummary, setDetailSummary] = useState<StudentTuitionSummary>()

  const patchParams = useCallback(
    (updates: Record<string, string | undefined | null>) => {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev)
          for (const [key, value] of Object.entries(updates)) {
            if (value == null || value === '') {
              next.delete(key)
            } else {
              next.set(key, value)
            }
          }
          return next
        },
        { replace: true },
      )
      setPage(0)
    },
    [setSearchParams],
  )

  const invoiceParams = useMemo(
    () => ({
      keyword: keyword || undefined,
      status: statusFilter,
      classroomId,
      packageId,
      cycleNo,
      overdue,
      hasRemainingDebt,
      dueFrom,
      dueTo,
      effectiveFrom,
      effectiveTo,
      page: 0,
      size: FETCH_SIZE,
    }),
    [
      classroomId,
      cycleNo,
      dueFrom,
      dueTo,
      effectiveFrom,
      effectiveTo,
      hasRemainingDebt,
      keyword,
      overdue,
      packageId,
      statusFilter,
    ],
  )

  const isStudentTab = tab === 'by-student'
  const summaryParams = useMemo(() => ({ classroomId }), [classroomId])
  const studentSummariesQuery = useTuitionStudentSummaries(summaryParams, isStudentTab)
  const invoicesQuery = useInvoices(invoiceParams, !isStudentTab)
  const collectibleQuery = useInvoices(
    { classroomId, hasRemainingDebt: true, page: 0, size: FETCH_SIZE },
    isStudentTab,
  )
  const summaryQuery = useInvoiceSummary(classroomId)
  const classroomsQuery = useClassrooms({ page: 0, size: 100 })
  const packagesQuery = useTuitionPackages({ page: 0, size: 100 })
  const createPayment = useCreatePayment()

  const filteredSummaries = useMemo(() => {
    return (studentSummariesQuery.data?.data ?? []).filter((summary) =>
      matchesKeyword(keyword, ...studentKeywordFields(summary), summary.classroomName),
    )
  }, [keyword, studentSummariesQuery.data?.data])

  const pagedSummaries = useMemo(
    () => paginateItems(filteredSummaries, page, size),
    [filteredSummaries, page, size],
  )

  const invoices = invoicesQuery.data?.data ?? []
  const pagedInvoices = useMemo(() => paginateItems(invoices, page, size), [invoices, page, size])

  const dueRange: [Dayjs, Dayjs] | null =
    dueFrom && dueTo ? [dayjs(dueFrom), dayjs(dueTo)] : null
  const effectiveRange: [Dayjs, Dayjs] | null =
    effectiveFrom && effectiveTo ? [dayjs(effectiveFrom), dayjs(effectiveTo)] : null

  const classroomName = classroomsQuery.data?.data?.find((item) => item.id === classroomId)?.className
  const packageName = packagesQuery.data?.data?.find((item) => item.id === packageId)?.name

  const studentSummaryColumns: ColumnsType<StudentTuitionSummary> = [
    { title: 'Mã HV', dataIndex: 'studentCode', key: 'studentCode' },
    studentNameColumn(),
    { title: 'Lớp', dataIndex: 'classroomName', key: 'classroomName' },
    {
      title: 'Tổng học phí',
      dataIndex: 'totalTuitionAmount',
      key: 'totalTuitionAmount',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Đã TT',
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
    { title: 'Số HĐ', dataIndex: 'totalInvoiceCount', key: 'totalInvoiceCount' },
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
    { title: 'Mã HĐ', dataIndex: 'invoiceCode', key: 'invoiceCode', fixed: 'left' },
    {
      title: 'Học viên',
      key: 'student',
      render: (_, invoice) => (
        <Space direction="vertical" size={0}>
          <Text>{invoice.studentName}</Text>
          <Text type="secondary">{invoice.studentCode}</Text>
        </Space>
      ),
    },
    {
      title: 'Lớp',
      key: 'classroom',
      render: (_, invoice) => invoice.classroomCode || invoice.classroomName,
    },
    {
      title: 'Kỳ học phí',
      dataIndex: 'billingLabel',
      key: 'billingLabel',
      render: (value?: string | null) => value || '-',
    },
    {
      title: 'Gói học',
      dataIndex: 'packageNameSnapshot',
      key: 'packageNameSnapshot',
    },
    {
      title: 'Áp dụng từ',
      dataIndex: 'effectiveFrom',
      key: 'effectiveFrom',
      render: (value?: string | null) => (value ? dayjs(value).format('DD/MM/YYYY') : '-'),
    },
    {
      title: 'Dự kiến đến',
      key: 'estimatedEffectiveTo',
      render: (_, invoice) =>
        formatEstimatedEffectiveTo(invoice.estimatedEffectiveTo, invoice.totalSessionsSnapshot),
    },
    {
      title: 'Hạn TT',
      dataIndex: 'dueDate',
      key: 'dueDate',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
    },
    {
      title: 'Phải đóng',
      dataIndex: 'finalAmount',
      key: 'finalAmount',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Đã TT',
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
      render: (status: string) => <StatusTag status={status} labels={INVOICE_STATUS_LABELS} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      fixed: 'right',
      render: (_, invoice) => (
        <Space size="small" wrap>
          {invoice.status === 'UNPAID' || invoice.status === 'PARTIALLY_PAID' ? (
            <Button type="link" onClick={() => setCollectingInvoice(invoice)}>
              Thu tiền
            </Button>
          ) : null}
          <Button type="link" onClick={() => setDetailInvoice(invoice)}>
            Xem chi tiết
          </Button>
          <Link to={`/print/invoices/${invoice.id}`}>
            In phiếu học phí
          </Link>
        </Space>
      ),
    },
  ]

  function handleCollectFromSummary(summary: StudentTuitionSummary) {
    const invoice = findCollectibleInvoice(
      collectibleQuery.data?.data ?? [],
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

  function handleCreatePayment(payload: CreatePaymentPayload) {
    if (!collectingInvoice) return

    createPayment.mutate(
      { invoiceId: collectingInvoice.id, payload },
      {
        onSuccess: (payment) => {
          message.success('Đã thu tiền')
          setSuccessPayment(payment)
        },
        onError: showErrorMessage,
      },
    )
  }

  function closePaymentModal() {
    setCollectingInvoice(undefined)
    setSuccessPayment(undefined)
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
          Học phí
        </Title>
        <Text type="secondary">Quản lý hóa đơn học phí theo kỳ và theo học viên.</Text>
      </Space>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={summaryQuery.isLoading}>
            <Statistic
              title="Tổng còn phải thu"
              value={summaryQuery.data?.totalRemainingCollectible ?? 0}
              formatter={(value) => moneyFormatter(value as number)}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={summaryQuery.isLoading}>
            <Statistic title="Hóa đơn chưa thanh toán" value={summaryQuery.data?.unpaidCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={summaryQuery.isLoading}>
            <Statistic
              title="Hóa đơn thanh toán một phần"
              value={summaryQuery.data?.partiallyPaidCount ?? 0}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={summaryQuery.isLoading}>
            <Statistic
              title="Đã thu tháng này"
              value={summaryQuery.data?.collectedThisMonth ?? 0}
              formatter={(value) => moneyFormatter(value as number)}
            />
          </Card>
        </Col>
      </Row>

      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Tabs
            activeKey={tab}
            onChange={(next) => patchParams({ tab: next === 'by-student' ? 'by-student' : undefined })}
            items={[
              { key: 'by-cycle', label: 'Theo kỳ học phí' },
              { key: 'by-student', label: 'Theo học viên' },
            ]}
          />

          <Space wrap>
            <Input.Search
              allowClear
              placeholder={
                isStudentTab
                  ? STUDENT_SEARCH_PLACEHOLDER
                  : 'Tìm mã HĐ, học viên, lớp...'
              }
              style={{ width: 280 }}
              value={keyword}
              onChange={(event) => patchParams({ keyword: event.target.value || undefined })}
            />
            <Select
              allowClear
              placeholder="Lớp học"
              style={{ width: 200 }}
              value={classroomId}
              onChange={(value) =>
                patchParams({ classroomId: value != null ? String(value) : undefined })
              }
              options={(classroomsQuery.data?.data ?? []).map((classroom) => ({
                label: classroom.className,
                value: classroom.id,
              }))}
            />
            {!isStudentTab ? (
              <Select
                allowClear
                placeholder="Trạng thái"
                style={{ width: 200 }}
                value={statusFilter}
                onChange={(value) => patchParams({ status: value })}
                options={Object.entries(INVOICE_STATUS_LABELS).map(([value, label]) => ({
                  value,
                  label,
                }))}
              />
            ) : null}
          </Space>

          {!isStudentTab ? (
            <Collapse
              items={[
                {
                  key: 'advanced',
                  label: 'Bộ lọc nâng cao',
                  children: (
                    <Space wrap>
                      <RangePicker
                        allowClear
                        format="DD/MM/YYYY"
                        placeholder={['Hạn TT từ', 'Hạn TT đến']}
                        value={dueRange}
                        onChange={(values) =>
                          patchParams({
                            dueFrom: values?.[0]?.format('YYYY-MM-DD'),
                            dueTo: values?.[1]?.format('YYYY-MM-DD'),
                          })
                        }
                      />
                      <RangePicker
                        allowClear
                        format="DD/MM/YYYY"
                        placeholder={['Áp dụng từ', 'Áp dụng đến']}
                        value={effectiveRange}
                        onChange={(values) =>
                          patchParams({
                            effectiveFrom: values?.[0]?.format('YYYY-MM-DD'),
                            effectiveTo: values?.[1]?.format('YYYY-MM-DD'),
                          })
                        }
                      />
                      <Select
                        allowClear
                        placeholder="Gói học"
                        style={{ width: 200 }}
                        value={packageId}
                        onChange={(value) =>
                          patchParams({ packageId: value != null ? String(value) : undefined })
                        }
                        options={(packagesQuery.data?.data ?? []).map((item) => ({
                          label: item.name,
                          value: item.id,
                        }))}
                      />
                      <InputNumber
                        min={1}
                        placeholder="Số kỳ"
                        value={cycleNo}
                        onChange={(value) =>
                          patchParams({
                            cycleNo: value != null ? String(value) : undefined,
                          })
                        }
                      />
                      <Space>
                        <Text>Quá hạn</Text>
                        <Switch
                          checked={overdue === true}
                          onChange={(checked) =>
                            patchParams({ overdue: checked ? 'true' : undefined })
                          }
                        />
                      </Space>
                      <Space>
                        <Text>Còn nợ</Text>
                        <Switch
                          checked={hasRemainingDebt === true}
                          onChange={(checked) =>
                            patchParams({ hasRemainingDebt: checked ? 'true' : undefined })
                          }
                        />
                      </Space>
                    </Space>
                  ),
                },
              ]}
            />
          ) : null}

          <ActiveFilterTags
            tags={[
              ...(statusFilter
                ? [
                    {
                      key: 'status',
                      label: INVOICE_STATUS_LABELS[statusFilter],
                      onClose: () => patchParams({ status: undefined }),
                    },
                  ]
                : []),
              ...(overdue
                ? [
                    {
                      key: 'overdue',
                      label: 'Quá hạn',
                      color: 'red',
                      onClose: () => patchParams({ overdue: undefined }),
                    },
                  ]
                : []),
              ...(hasRemainingDebt
                ? [
                    {
                      key: 'hasRemainingDebt',
                      label: 'Còn nợ',
                      onClose: () => patchParams({ hasRemainingDebt: undefined }),
                    },
                  ]
                : []),
              ...(classroomId && classroomName
                ? [
                    {
                      key: 'classroom',
                      label: `Lớp: ${classroomName}`,
                      onClose: () => patchParams({ classroomId: undefined }),
                    },
                  ]
                : []),
              ...(packageId && packageName
                ? [
                    {
                      key: 'package',
                      label: `Gói: ${packageName}`,
                      onClose: () => patchParams({ packageId: undefined }),
                    },
                  ]
                : []),
            ]}
            onClearAll={() =>
              patchParams({
                status: undefined,
                overdue: undefined,
                hasRemainingDebt: undefined,
                classroomId: undefined,
                packageId: undefined,
                cycleNo: undefined,
                dueFrom: undefined,
                dueTo: undefined,
                effectiveFrom: undefined,
                effectiveTo: undefined,
                keyword: undefined,
              })
            }
          />

          {isStudentTab ? (
            <Table
              rowKey={summaryRowKey}
              columns={studentSummaryColumns}
              dataSource={pagedSummaries}
              loading={studentSummariesQuery.isLoading}
              pagination={{
                current: page + 1,
                pageSize: size,
                total: filteredSummaries.length,
                showSizeChanger: true,
              }}
              onChange={handleTableChange}
              locale={{ emptyText: 'Chưa có hóa đơn học phí.' }}
              scroll={{ x: 1100 }}
            />
          ) : (
            <Table
              rowKey="id"
              columns={columns}
              dataSource={pagedInvoices}
              loading={invoicesQuery.isLoading}
              pagination={{
                current: page + 1,
                pageSize: size,
                total: invoices.length,
                showSizeChanger: true,
              }}
              onChange={handleTableChange}
              locale={{ emptyText: 'Chưa có hóa đơn học phí.' }}
              scroll={{ x: 1600 }}
            />
          )}
        </Space>
      </Card>

      <PaymentFormModal
        open={Boolean(collectingInvoice)}
        invoice={collectingInvoice}
        submitting={createPayment.isPending}
        successPayment={successPayment}
        onCancel={closePaymentModal}
        onSubmit={handleCreatePayment}
        onCloseSuccess={closePaymentModal}
        onViewInvoice={(invoiceId) => {
          const invoice =
            invoices.find((item) => item.id === invoiceId) ??
            collectibleQuery.data?.data?.find((item) => item.id === invoiceId) ??
            collectingInvoice
          if (invoice) {
            setDetailInvoice(invoice)
          }
        }}
      />

      <InvoiceDetailModal
        open={Boolean(detailInvoice)}
        invoice={detailInvoice}
        onClose={() => setDetailInvoice(undefined)}
        onCollect={(invoice) => setCollectingInvoice(invoice)}
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
