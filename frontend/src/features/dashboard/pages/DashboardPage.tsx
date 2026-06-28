import { Button, Card, Col, Empty, message, Row, Space, Statistic, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import dayjs from 'dayjs'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { PaymentFormModal } from '../../payments/components/PaymentFormModal'
import { useCreatePayment } from '../../payments/paymentQueries'
import type { CreatePaymentPayload } from '../../payments/paymentTypes'
import type { PaymentMethod } from '../../payments/paymentTypes'
import type { Payment } from '../../payments/paymentTypes'
import { debtItemToInvoice, type DebtReportItem } from '../../reports/reportTypes'
import type { DashboardTodaySession, SessionWarning } from '../dashboardTypes'
import {
  useDashboardDebtAlerts,
  useDashboardRecentPayments,
  useDashboardSummary,
  useDashboardTodaySessions,
  useSessionWarnings,
} from '../dashboardQueries'

const { Title, Text } = Typography

const paymentMethodLabels: Record<PaymentMethod, string> = {
  CASH: 'Tiền mặt',
  BANK_TRANSFER: 'Chuyển khoản',
  OTHER: 'Khác',
}

export function DashboardPage() {
  const navigate = useNavigate()
  const summaryQuery = useDashboardSummary()
  const todaySessionsQuery = useDashboardTodaySessions()
  const debtAlertsQuery = useDashboardDebtAlerts(10)
  const warningsQuery = useSessionWarnings({ remainingThreshold: 2 })
  const recentPaymentsQuery = useDashboardRecentPayments(10)
  const createPayment = useCreatePayment()
  const [collectingDebt, setCollectingDebt] = useState<DebtReportItem>()

  const summary = summaryQuery.data

  const todaySessionColumns: ColumnsType<DashboardTodaySession> = [
    {
      title: 'Giờ học',
      key: 'time',
      render: (_, session) => `${session.startTime.slice(0, 5)} - ${session.endTime.slice(0, 5)}`,
    },
    { title: 'Lớp', dataIndex: 'classroomName', key: 'classroomName' },
    { title: 'Giáo viên', dataIndex: 'teacherName', key: 'teacherName' },
    {
      title: 'Phòng học',
      dataIndex: 'room',
      key: 'room',
      render: (value?: string | null) => value ?? '-',
    },
    { title: 'Sĩ số', dataIndex: 'activeStudentCount', key: 'activeStudentCount' },
    {
      title: 'Trạng thái điểm danh',
      key: 'attendanceStatus',
      render: (_, session) => <TodaySessionAttendanceTag status={session.attendanceStatus} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, session) => (
        <Space size="small" wrap>
          {session.attendanceStatus !== 'CANCELED' && (
            <Button type="link" onClick={() => navigate(`/classrooms/${session.classroomId}`)}>
              Điểm danh
            </Button>
          )}
          <Button type="link" onClick={() => navigate(`/classrooms/${session.classroomId}`)}>
            Xem lớp
          </Button>
        </Space>
      ),
    },
  ]

  const debtColumns: ColumnsType<DebtReportItem> = [
    { title: 'Học viên', dataIndex: 'studentName', key: 'studentName' },
    { title: 'Lớp', dataIndex: 'classroomName', key: 'classroomName' },
    {
      title: 'Gói học',
      dataIndex: 'latestPackageName',
      key: 'latestPackageName',
      render: (value?: string | null) => value ?? '-',
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
      title: 'Thao tác',
      key: 'actions',
      render: (_, item) => (
        <Space size="small" wrap>
          <Button type="link" onClick={() => setCollectingDebt(item)}>
            Thu tiền
          </Button>
          <Button type="link" onClick={() => navigate(`/invoices?keyword=${item.invoiceCode}`)}>
            Xem học phí
          </Button>
          <Button type="link" onClick={() => navigate(`/students/${item.studentId}`)}>
            Xem học viên
          </Button>
        </Space>
      ),
    },
  ]

  const warningColumns: ColumnsType<SessionWarning> = [
    { title: 'Học viên', dataIndex: 'studentName', key: 'studentName' },
    { title: 'Lớp', dataIndex: 'classroomName', key: 'classroomName' },
    { title: 'Tổng buổi', dataIndex: 'totalSessions', key: 'totalSessions' },
    { title: 'Đã dùng', dataIndex: 'usedSessions', key: 'usedSessions' },
    { title: 'Còn lại', dataIndex: 'remainingSessions', key: 'remainingSessions' },
    {
      title: 'Trạng thái',
      key: 'warningType',
      render: (_, record) => <SessionWarningTag warning={record} />,
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

  const paymentColumns: ColumnsType<Payment> = [
    {
      title: 'Ngày thanh toán',
      dataIndex: 'paymentDate',
      key: 'paymentDate',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
    },
    { title: 'Học viên', dataIndex: 'studentName', key: 'studentName' },
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
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <StatusTag status={status} />,
    },
  ]

  function handleCreatePayment(payload: CreatePaymentPayload) {
    if (!collectingDebt) {
      return
    }

    createPayment.mutate(
      { invoiceId: collectingDebt.invoiceId, payload },
      {
        onSuccess: () => {
          message.success('Đã thu tiền')
          setCollectingDebt(undefined)
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
          Tổng quan
        </Title>
        <Text type="secondary">Theo dõi nhanh hoạt động hàng ngày và các việc cần xử lý.</Text>
      </Space>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={8} xl={4}>
          <Card loading={summaryQuery.isLoading}>
            <Statistic title="Học viên đang học" value={summary?.totalActiveStudents ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8} xl={4}>
          <Card loading={summaryQuery.isLoading}>
            <Statistic title="Lớp đang học" value={summary?.totalActiveClassrooms ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8} xl={4}>
          <Card loading={summaryQuery.isLoading}>
            <Statistic title="Lớp học hôm nay" value={summary?.upcomingSessionsToday ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8} xl={4}>
          <Card loading={summaryQuery.isLoading}>
            <Statistic
              title="Doanh thu tháng này"
              value={summary?.totalRevenueThisMonth ?? 0}
              formatter={(value) => <MoneyText value={Number(value)} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8} xl={4}>
          <Card loading={summaryQuery.isLoading}>
            <Statistic
              title="Công nợ hiện tại"
              value={summary?.totalDebtAmount ?? 0}
              formatter={(value) => <MoneyText value={Number(value)} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8} xl={4}>
          <Card loading={summaryQuery.isLoading}>
            <Statistic title="Học viên còn nợ" value={summary?.totalStudentsWithDebt ?? 0} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={8}>
          <Card size="small" loading={summaryQuery.isLoading}>
            <Statistic
              title="Học viên hết buổi"
              value={summary?.totalStudentsWithDepletedSessions ?? 0}
              valueStyle={{ color: '#cf1322', fontSize: 20 }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card size="small" loading={summaryQuery.isLoading}>
            <Statistic
              title="Học viên sắp hết buổi"
              value={summary?.totalStudentsWithLowSessions ?? 0}
              valueStyle={{ color: '#d46b08', fontSize: 20 }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card size="small" loading={summaryQuery.isLoading}>
            <Statistic
              title="Buổi bù đang có"
              value={summary?.totalPendingMakeupCredits ?? 0}
              valueStyle={{ fontSize: 20 }}
            />
          </Card>
        </Col>
      </Row>

      <Card title="Lịch học hôm nay">
        <Table<DashboardTodaySession>
          rowKey="sessionId"
          loading={todaySessionsQuery.isLoading}
          dataSource={todaySessionsQuery.data ?? []}
          columns={todaySessionColumns}
          pagination={false}
          locale={{ emptyText: <Empty description="Không có lớp học hôm nay" /> }}
        />
      </Card>

      <Card title="Học viên còn nợ">
        <Table<DebtReportItem>
          rowKey="invoiceId"
          loading={debtAlertsQuery.isLoading}
          dataSource={debtAlertsQuery.data ?? []}
          columns={debtColumns}
          pagination={false}
          locale={{ emptyText: <Empty description="Không có công nợ" /> }}
        />
      </Card>

      <Card title="Học viên sắp hết / hết buổi">
        <Table<SessionWarning>
          rowKey="enrollmentId"
          loading={warningsQuery.isLoading}
          dataSource={warningsQuery.data ?? []}
          columns={warningColumns}
          pagination={{ pageSize: 10, showSizeChanger: true }}
          locale={{ emptyText: <Empty description="Không có học viên cần gia hạn" /> }}
        />
      </Card>

      <Card title="Thanh toán gần đây">
        <Table<Payment>
          rowKey="id"
          loading={recentPaymentsQuery.isLoading}
          dataSource={recentPaymentsQuery.data ?? []}
          columns={paymentColumns}
          pagination={false}
          locale={{ emptyText: <Empty description="Chưa có thanh toán gần đây" /> }}
        />
      </Card>

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

function TodaySessionAttendanceTag({ status }: { status: DashboardTodaySession['attendanceStatus'] }) {
  if (status === 'CANCELED') {
    return <Tag color="red">Đã hủy</Tag>
  }
  if (status === 'MARKED') {
    return <Tag color="green">Đã điểm danh</Tag>
  }
  return <Tag color="gold">Chưa điểm danh</Tag>
}

function SessionWarningTag({ warning }: { warning: SessionWarning }) {
  if (warning.remainingSessions <= 0 || warning.warningType === 'DEPLETED' || warning.warningType === 'OVERUSED') {
    return <Tag color="red">Hết buổi</Tag>
  }
  return <Tag color="orange">Sắp hết</Tag>
}
