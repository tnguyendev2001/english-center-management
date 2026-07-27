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
  Typography,
  message,
} from 'antd'
import type { TablePaginationConfig } from 'antd/es/table'
import type { ColumnsType } from 'antd/es/table'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { isAxiosError } from 'axios'
import { useCallback, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { ActiveFilterTags } from '../../../components/common/ActiveFilterTags'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { useClassrooms } from '../../classrooms/classroomQueries'
import {
  DEBT_STATUS_LABELS,
  INVOICE_STATUS_LABELS,
  type Invoice,
} from '../../invoices/invoiceTypes'
import { InvoiceDetailModal } from '../../invoices/components/InvoiceDetailModal'
import { PaymentFormModal } from '../../payments/components/PaymentFormModal'
import { useCreatePayment } from '../../payments/paymentQueries'
import type { CreatePaymentPayload, Payment } from '../../payments/paymentTypes'
import { useTuitionPackages } from '../../tuitionPackages/tuitionPackageQueries'
import { useDebts } from '../debtQueries'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

const FETCH_SIZE = 100

const DEBT_STATUS_OPTIONS = ['OUTSTANDING', 'OVERDUE', 'DUE_TODAY', 'NOT_DUE', 'PARTIALLY_PAID'] as const
type DebtStatusFilter = (typeof DEBT_STATUS_OPTIONS)[number]

const DEBT_FILTER_LABELS: Record<DebtStatusFilter, string> = {
  OUTSTANDING: 'Còn nợ',
  OVERDUE: 'Quá hạn',
  DUE_TODAY: 'Đến hạn hôm nay',
  NOT_DUE: 'Chưa đến hạn',
  PARTIALLY_PAID: 'Thanh toán một phần',
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

export function DebtPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const statusRaw = searchParams.get('status')
  const statusFilter =
    statusRaw && (DEBT_STATUS_OPTIONS as readonly string[]).includes(statusRaw)
      ? (statusRaw as DebtStatusFilter)
      : undefined
  const keyword = searchParams.get('keyword') ?? ''
  const classroomId = parseOptionalNumber(searchParams.get('classroomId'))
  const packageId = parseOptionalNumber(searchParams.get('packageId'))
  const cycleNo = parseOptionalNumber(searchParams.get('cycleNo'))
  const overdue = parseOptionalBoolean(searchParams.get('overdue'))
  const multipleInvoices = parseOptionalBoolean(searchParams.get('multipleInvoices'))
  const dueFrom = searchParams.get('dueFrom') ?? undefined
  const dueTo = searchParams.get('dueTo') ?? undefined
  const remainingFrom = parseOptionalNumber(searchParams.get('remainingFrom'))
  const remainingTo = parseOptionalNumber(searchParams.get('remainingTo'))

  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [collectingInvoice, setCollectingInvoice] = useState<Invoice>()
  const [successPayment, setSuccessPayment] = useState<Payment>()
  const [detailInvoice, setDetailInvoice] = useState<Invoice>()

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

  const debtsParams = useMemo(
    () => ({
      keyword: keyword || undefined,
      classroomId,
      packageId,
      cycleNo,
      overdue,
      multipleInvoices,
      dueFrom,
      dueTo,
      remainingFrom,
      remainingTo,
      status: statusFilter,
      page: 0,
      size: FETCH_SIZE,
    }),
    [
      classroomId,
      cycleNo,
      dueFrom,
      dueTo,
      keyword,
      multipleInvoices,
      overdue,
      packageId,
      remainingFrom,
      remainingTo,
      statusFilter,
    ],
  )

  const debtsQuery = useDebts(debtsParams)
  const classroomsQuery = useClassrooms({ page: 0, size: 100 })
  const packagesQuery = useTuitionPackages({ page: 0, size: 100 })
  const createPayment = useCreatePayment()

  const debts = debtsQuery.data?.data ?? []
  const pagedDebts = useMemo(() => {
    const from = page * size
    return debts.slice(from, from + size)
  }, [debts, page, size])

  const debtTotals = useMemo(() => {
    return {
      totalRemaining: debts.reduce((sum, invoice) => sum + Number(invoice.remainingAmount ?? 0), 0),
      invoiceCount: debts.length,
      overdueCount: debts.filter((invoice) => (invoice.overdueDays ?? 0) > 0).length,
    }
  }, [debts])

  const dueRange: [Dayjs, Dayjs] | null =
    dueFrom && dueTo ? [dayjs(dueFrom), dayjs(dueTo)] : null

  const classroomName = classroomsQuery.data?.data?.find((item) => item.id === classroomId)?.className
  const packageName = packagesQuery.data?.data?.find((item) => item.id === packageId)?.name

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
      title: 'Hạn TT',
      dataIndex: 'dueDate',
      key: 'dueDate',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
    },
    {
      title: 'Số ngày quá hạn',
      dataIndex: 'overdueDays',
      key: 'overdueDays',
      render: (value?: number | null) => (value && value > 0 ? value : '-'),
    },
    {
      title: 'Còn lại',
      dataIndex: 'remainingAmount',
      key: 'remainingAmount',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Trạng thái công nợ',
      key: 'debtStatus',
      render: (_, invoice) => {
        const status = invoice.debtStatus
        if (!status) {
          return <StatusTag status={invoice.status} labels={INVOICE_STATUS_LABELS} />
        }
        return (
          <StatusTag
            status={status}
            labels={DEBT_STATUS_LABELS}
          />
        )
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      fixed: 'right',
      render: (_, invoice) => (
        <Space size="small" wrap>
          <Button type="link" onClick={() => setCollectingInvoice(invoice)}>
            Thu tiền
          </Button>
          <Button type="link" onClick={() => setDetailInvoice(invoice)}>
            Xem chi tiết
          </Button>
          <Button type="link" onClick={() => navigate(`/students/${invoice.studentId}`)}>
            Xem học viên
          </Button>
          <Link to={`/print/invoices/${invoice.id}`}>
            In phiếu học phí
          </Link>
        </Space>
      ),
    },
  ]

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
  const subtitle = statusFilter
    ? `Công nợ – ${DEBT_FILTER_LABELS[statusFilter]}`
    : 'Theo dõi các hóa đơn học phí còn phải thu.'

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Công nợ
        </Title>
        <Text type="secondary">{subtitle}</Text>
      </Space>

      <Row gutter={16}>
        <Col xs={24} md={8}>
          <Card loading={debtsQuery.isLoading}>
            <Statistic
              title="Tổng còn phải thu"
              value={debtTotals.totalRemaining}
              formatter={(value) => moneyFormatter(value as number)}
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card loading={debtsQuery.isLoading}>
            <Statistic title="Số hóa đơn còn nợ" value={debtTotals.invoiceCount} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card loading={debtsQuery.isLoading}>
            <Statistic title="Hóa đơn quá hạn" value={debtTotals.overdueCount} />
          </Card>
        </Col>
      </Row>

      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Space wrap>
            <Input.Search
              allowClear
              placeholder="Tìm mã HĐ, học viên..."
              style={{ width: 280 }}
              value={keyword}
              onChange={(event) => patchParams({ keyword: event.target.value || undefined })}
            />
            <Select
              allowClear
              placeholder="Trạng thái công nợ"
              style={{ width: 200 }}
              value={statusFilter}
              onChange={(value) => patchParams({ status: value })}
              options={DEBT_STATUS_OPTIONS.map((value) => ({
                value,
                label: DEBT_FILTER_LABELS[value],
              }))}
            />
            <Select
              allowClear
              placeholder="Lớp học"
              style={{ width: 220 }}
              value={classroomId}
              onChange={(value) =>
                patchParams({ classroomId: value != null ? String(value) : undefined })
              }
              options={(classroomsQuery.data?.data ?? []).map((classroom) => ({
                label: classroom.className,
                value: classroom.id,
              }))}
            />
            <Space>
              <Text>Nợ nhiều hóa đơn</Text>
              <Switch
                checked={multipleInvoices === true}
                onChange={(checked) =>
                  patchParams({ multipleInvoices: checked ? 'true' : undefined })
                }
              />
            </Space>
          </Space>

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
                        patchParams({ cycleNo: value != null ? String(value) : undefined })
                      }
                    />
                    <InputNumber
                      min={0}
                      placeholder="Còn lại từ"
                      value={remainingFrom}
                      onChange={(value) =>
                        patchParams({
                          remainingFrom: value != null ? String(value) : undefined,
                        })
                      }
                    />
                    <InputNumber
                      min={0}
                      placeholder="Còn lại đến"
                      value={remainingTo}
                      onChange={(value) =>
                        patchParams({
                          remainingTo: value != null ? String(value) : undefined,
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
                  </Space>
                ),
              },
            ]}
          />

          <ActiveFilterTags
            tags={[
              ...(statusFilter
                ? [
                    {
                      key: 'status',
                      label: DEBT_FILTER_LABELS[statusFilter],
                      color: statusFilter === 'OVERDUE' ? 'red' : 'orange',
                      onClose: () => patchParams({ status: undefined }),
                    },
                  ]
                : []),
              ...(multipleInvoices
                ? [
                    {
                      key: 'multipleInvoices',
                      label: 'Nợ nhiều hóa đơn',
                      color: 'orange',
                      onClose: () => patchParams({ multipleInvoices: undefined }),
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
                multipleInvoices: undefined,
                overdue: undefined,
                classroomId: undefined,
                packageId: undefined,
                cycleNo: undefined,
                dueFrom: undefined,
                dueTo: undefined,
                remainingFrom: undefined,
                remainingTo: undefined,
                keyword: undefined,
              })
            }
          />

          <Table
            rowKey="id"
            columns={columns}
            dataSource={pagedDebts}
            loading={debtsQuery.isLoading}
            pagination={{
              current: page + 1,
              pageSize: size,
              total: debts.length,
              showSizeChanger: true,
            }}
            onChange={handleTableChange}
            locale={{ emptyText: 'Không có khoản học phí chưa thanh toán.' }}
            scroll={{ x: 1400 }}
          />
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
          const invoice = debts.find((item) => item.id === invoiceId) ?? collectingInvoice
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
    </Space>
  )
}
