import { Button, Card, Col, Input, Row, Select, Space, Statistic, Table, Tag, Typography, message } from 'antd'
import type { TablePaginationConfig } from 'antd/es/table'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { isAxiosError } from 'axios'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ActiveFilterTags } from '../../../components/common/ActiveFilterTags'
import { MoneyText } from '../../../components/common/MoneyText'
import {
  STUDENT_SEARCH_PLACEHOLDER,
  studentCodeColumn,
  studentKeywordFields,
  studentNameColumn,
} from '../../../components/common/studentDisplay'
import { useUrlEnumParam } from '../../../hooks/useUrlEnumParam'
import { matchesKeyword, paginateItems } from '../../../utils/clientPagination'
import { useClassrooms } from '../../classrooms/classroomQueries'
import { StudentInvoiceListDrawer } from '../../financial/components/StudentInvoiceListDrawer'
import type { StudentDebtSummary } from '../../financial/financialSummaryTypes'
import type { Invoice } from '../../invoices/invoiceTypes'
import { PaymentFormModal } from '../../payments/components/PaymentFormModal'
import { useCreatePayment } from '../../payments/paymentQueries'
import type { CreatePaymentPayload } from '../../payments/paymentTypes'
import { useRevenueSummary } from '../../revenue/revenueQueries'
import { useDebts, useDebtStudentSummaries } from '../debtQueries'

const { Title, Text } = Typography

const FETCH_SIZE = 100

const DEBT_STATUS_OPTIONS = ['OUTSTANDING', 'OVERDUE', 'MULTIPLE_UNPAID'] as const
type DebtStatusFilter = (typeof DEBT_STATUS_OPTIONS)[number]

const DEBT_STATUS_LABELS: Record<DebtStatusFilter, string> = {
  OUTSTANDING: 'Còn nợ',
  OVERDUE: 'Quá hạn',
  MULTIPLE_UNPAID: 'Nợ nhiều hóa đơn',
}

function summaryRowKey(summary: StudentDebtSummary) {
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

export function DebtPage() {
  const navigate = useNavigate()
  const statusParam = useUrlEnumParam('status', DEBT_STATUS_OPTIONS)
  const statusFilter = statusParam.value
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [classroomId, setClassroomId] = useState<number>()
  const [collectingInvoice, setCollectingInvoice] = useState<Invoice>()
  const [detailSummary, setDetailSummary] = useState<StudentDebtSummary>()

  const summaryParams = useMemo(() => ({ classroomId }), [classroomId])
  const debtsParams = useMemo(() => ({ page: 0, size: FETCH_SIZE }), [])

  const summariesQuery = useDebtStudentSummaries(summaryParams)
  const debtsQuery = useDebts(debtsParams)
  const revenueQuery = useRevenueSummary()
  const classroomsQuery = useClassrooms({ page: 0, size: 100 })
  const createPayment = useCreatePayment()

  const filteredSummaries = useMemo(() => {
    const today = dayjs().startOf('day')
    const allSummaries = summariesQuery.data?.data ?? []

    const multiUnpaidStudentIds = new Set<number>()
    if (statusFilter === 'MULTIPLE_UNPAID') {
      const invoiceCountByStudent = new Map<number, number>()
      for (const summary of allSummaries) {
        invoiceCountByStudent.set(
          summary.studentId,
          (invoiceCountByStudent.get(summary.studentId) ?? 0) + summary.debtInvoiceCount,
        )
      }
      for (const [studentId, count] of invoiceCountByStudent) {
        if (count >= 2) {
          multiUnpaidStudentIds.add(studentId)
        }
      }
    }

    return allSummaries.filter((summary) => {
      if (classroomId && summary.classroomId !== classroomId) {
        return false
      }

      if (statusFilter === 'OVERDUE') {
        if (!summary.nearestDueDate || !dayjs(summary.nearestDueDate).isBefore(today)) {
          return false
        }
      }

      if (statusFilter === 'OUTSTANDING' && summary.totalRemainingDebt <= 0) {
        return false
      }

      if (statusFilter === 'MULTIPLE_UNPAID' && !multiUnpaidStudentIds.has(summary.studentId)) {
        return false
      }

      return matchesKeyword(keyword, ...studentKeywordFields(summary), summary.classroomName)
    })
  }, [classroomId, keyword, statusFilter, summariesQuery.data?.data])

  const pagedSummaries = useMemo(
    () => paginateItems(filteredSummaries, page, size),
    [filteredSummaries, page, size],
  )

  const debtTotals = useMemo(() => {
    return {
      totalRemaining: filteredSummaries.reduce((sum, summary) => sum + summary.totalRemainingDebt, 0),
      studentCount: filteredSummaries.length,
    }
  }, [filteredSummaries])

  function handleStatusChange(value: DebtStatusFilter | undefined) {
    statusParam.setValue(value)
    setPage(0)
  }

  const classroomName = classroomsQuery.data?.data?.find((item) => item.id === classroomId)?.className

  const columns: ColumnsType<StudentDebtSummary> = [
    studentCodeColumn(),
    studentNameColumn(),
    {
      title: 'Lớp',
      dataIndex: 'classroomName',
      key: 'classroomName',
    },
    {
      title: 'Tổng còn nợ',
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
      render: (_, summary) => {
        const overdue =
          summary.nearestDueDate != null && dayjs(summary.nearestDueDate).isBefore(dayjs().startOf('day'))
        return <Tag color={overdue ? 'red' : 'orange'}>{overdue ? 'Quá hạn' : 'Còn nợ'}</Tag>
      },
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, summary) => (
        <Space size="small">
          <Button type="link" onClick={() => handleCollectFromSummary(summary)}>
            Thu tiền
          </Button>
          <Button type="link" onClick={() => setDetailSummary(summary)}>
            Xem chi tiết công nợ
          </Button>
          <Button type="link" onClick={() => navigate(`/students/${summary.studentId}`)}>
            Xem học viên
          </Button>
        </Space>
      ),
    },
  ]

  function handleCollectFromSummary(summary: StudentDebtSummary) {
    const invoice = findCollectibleInvoice(
      debtsQuery.data?.data ?? [],
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

  function handleKeywordChange(value: string) {
    setKeyword(value)
    setPage(0)
  }

  function handleClassroomChange(value?: number) {
    setClassroomId(value)
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

  const subtitle = statusFilter
    ? `Công nợ – ${DEBT_STATUS_LABELS[statusFilter]}`
    : 'Theo dõi công nợ theo học viên và lớp học.'

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
          <Card loading={summariesQuery.isLoading}>
            <Statistic
              title="Tổng còn nợ"
              value={debtTotals.totalRemaining}
              formatter={(value) => `${Number(value).toLocaleString('en-US')} VND`}
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card loading={summariesQuery.isLoading}>
            <Statistic title="Số học viên còn nợ" value={debtTotals.studentCount} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card loading={revenueQuery.isLoading}>
            <Statistic
              title="Doanh thu tháng này"
              value={revenueQuery.data?.monthRevenue ?? 0}
              formatter={(value) => `${Number(value).toLocaleString('en-US')} VND`}
            />
          </Card>
        </Col>
      </Row>

      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Space wrap>
            <Input.Search
              allowClear
              placeholder={STUDENT_SEARCH_PLACEHOLDER}
              style={{ width: 320 }}
              value={keyword}
              onChange={(event) => handleKeywordChange(event.target.value)}
            />
            <Select
              allowClear
              placeholder="Trạng thái công nợ"
              style={{ width: 200 }}
              value={statusFilter}
              onChange={(value) => handleStatusChange(value)}
              options={[
                { value: 'OUTSTANDING', label: 'Còn nợ' },
                { value: 'OVERDUE', label: 'Quá hạn' },
                { value: 'MULTIPLE_UNPAID', label: 'Nợ nhiều hóa đơn' },
              ]}
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
          </Space>

          <ActiveFilterTags
            tags={[
              ...(statusFilter
                ? [
                    {
                      key: 'status',
                      label: DEBT_STATUS_LABELS[statusFilter],
                      color: statusFilter === 'OVERDUE' ? 'red' : 'orange',
                      onClose: () => handleStatusChange(undefined),
                    },
                  ]
                : []),
              ...(classroomId && classroomName
                ? [
                    {
                      key: 'classroom',
                      label: `Lớp: ${classroomName}`,
                      onClose: () => handleClassroomChange(undefined),
                    },
                  ]
                : []),
            ]}
            onClearAll={() => {
              handleStatusChange(undefined)
              handleClassroomChange(undefined)
            }}
          />

          <Table
            rowKey={summaryRowKey}
            columns={columns}
            dataSource={pagedSummaries}
            loading={summariesQuery.isLoading}
            pagination={{
              current: page + 1,
              pageSize: size,
              total: filteredSummaries.length,
              showSizeChanger: true,
            }}
            onChange={handleTableChange}
            locale={{ emptyText: 'Không có công nợ' }}
            scroll={{ x: 1100 }}
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
