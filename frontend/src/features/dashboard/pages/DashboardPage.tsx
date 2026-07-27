import { Alert, Button, Card, Col, Empty, message, Row, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import dayjs from 'dayjs'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { MoneyText } from '../../../components/common/MoneyText'
import { studentCodeColumn, studentNameColumn } from '../../../components/common/studentDisplay'
import { PaymentFormModal } from '../../payments/components/PaymentFormModal'
import { useCreatePayment } from '../../payments/paymentQueries'
import type { CreatePaymentPayload } from '../../payments/paymentTypes'
import { debtItemToInvoice, type DebtReportItem } from '../../reports/reportTypes'
import type {
  DashboardPendingAttendance,
  DashboardTodaySession,
  SessionWarning,
} from '../dashboardTypes'
import { classroomDetailPath } from '../../classrooms/classroomRoutes'
import { ClickableStatCard } from '../components/ClickableStatCard'
import { DashboardAlertsSection } from '../components/DashboardAlertsSection'
import { useDashboardOverview } from '../dashboardQueries'

const { Title, Text } = Typography

export function DashboardPage() {
  const navigate = useNavigate()
  const overviewQuery = useDashboardOverview()
  const createPayment = useCreatePayment()
  const [collectingDebt, setCollectingDebt] = useState<DebtReportItem>()

  useEffect(() => {
    if (window.location.hash === '#pending-attendance') {
      document.getElementById('pending-attendance')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, [overviewQuery.data])

  const overview = overviewQuery.data
  const summary = overview?.summary
  const loading = overviewQuery.isLoading
  const hasError = overviewQuery.isError

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
            <Button
              type="link"
              onClick={() =>
                navigate(
                  classroomDetailPath(session.classroomId, {
                    tab: 'attendance',
                    sessionId: session.sessionId,
                  }),
                )
              }
            >
              Điểm danh
            </Button>
          )}
          <Button type="link" onClick={() => navigate(classroomDetailPath(session.classroomId))}>
            Xem lớp
          </Button>
        </Space>
      ),
    },
  ]

  const pendingColumns: ColumnsType<DashboardPendingAttendance> = [
    {
      title: 'Ngày',
      dataIndex: 'sessionDate',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
    },
    {
      title: 'Giờ',
      key: 'time',
      render: (_, session) => `${session.startTime.slice(0, 5)} - ${session.endTime.slice(0, 5)}`,
    },
    { title: 'Lớp', dataIndex: 'classroomName' },
    {
      title: 'Giáo viên',
      dataIndex: 'teacherName',
      render: (value?: string | null) => value ?? '-',
    },
    { title: 'Đủ điều kiện', dataIndex: 'eligibleStudentCount' },
    { title: 'Đã điểm danh', dataIndex: 'markedCount' },
    { title: 'Còn thiếu', dataIndex: 'missingCount' },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, session) => (
        <Button
          type="link"
          onClick={() =>
            navigate(
              classroomDetailPath(session.classroomId, {
                tab: 'attendance',
                sessionId: session.sessionId,
              }),
            )
          }
        >
          Điểm danh
        </Button>
      ),
    },
  ]

  const debtColumns: ColumnsType<DebtReportItem> = [
    studentCodeColumn(),
    studentNameColumn(),
    { title: 'Lớp', dataIndex: 'classroomName', key: 'classroomName' },
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
          <Button type="link" onClick={() => navigate(`/students/${item.studentId}`)}>
            Xem học viên
          </Button>
        </Space>
      ),
    },
  ]

  const renewalColumns: ColumnsType<SessionWarning> = [
    studentCodeColumn(),
    studentNameColumn(),
    { title: 'Lớp', dataIndex: 'classroomName' },
    { title: 'Tổng buổi', dataIndex: 'totalSessions' },
    { title: 'Đã dùng', dataIndex: 'usedSessions' },
    { title: 'Còn lại', dataIndex: 'remainingSessions' },
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
        </Space>
      ),
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

  if (hasError) {
    return (
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Title level={2} style={{ margin: 0 }}>
          Tổng quan
        </Title>
        <Alert
          type="error"
          showIcon
          message="Không tải được dữ liệu tổng quan"
          description="Vui lòng thử lại. Hệ thống không hiển thị số liệu mặc định bằng 0 khi tải thất bại."
          action={
            <Button onClick={() => void overviewQuery.refetch()}>Thử lại</Button>
          }
        />
      </Space>
    )
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Tổng quan
        </Title>
        <Text type="secondary">Không gian làm việc hàng ngày: theo dõi vấn đề và xử lý nhanh.</Text>
      </Space>

      <DashboardAlertsSection
        alerts={overview?.alerts}
        loading={loading}
        error={false}
        onRetry={() => void overviewQuery.refetch()}
      />

      <div>
        <Title level={4}>Tổng quan</Title>
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} lg={8} xl={4}>
            <ClickableStatCard
              loading={loading}
              title="Học viên đang học"
              value={summary?.activeStudents ?? 0}
              href="/students"
            />
          </Col>
          <Col xs={24} sm={12} lg={8} xl={4}>
            <ClickableStatCard
              loading={loading}
              title="Lớp đang hoạt động"
              value={summary?.activeClassrooms ?? 0}
              href="/classrooms"
            />
          </Col>
          <Col xs={24} sm={12} lg={8} xl={4}>
            <ClickableStatCard
              loading={loading}
              title="Lớp học hôm nay"
              value={summary?.todayClasses ?? 0}
              href="/dashboard#today-sessions"
            />
          </Col>
          <Col xs={24} sm={12} lg={8} xl={4}>
            <ClickableStatCard
              loading={loading}
              title="Doanh thu tháng này"
              value={summary?.monthlyRevenue ?? 0}
              formatter={(value) => <MoneyText value={Number(value)} />}
            />
          </Col>
          <Col xs={24} sm={12} lg={8} xl={4}>
            <ClickableStatCard
              loading={loading}
              title="Công nợ hiện tại"
              value={summary?.currentDebt ?? 0}
              href="/debts?status=OUTSTANDING"
              formatter={(value) => <MoneyText value={Number(value)} />}
            />
          </Col>
          <Col xs={24} sm={12} lg={8} xl={4}>
            <ClickableStatCard
              loading={loading}
              title="Học viên còn nợ"
              value={summary?.studentsWithDebt ?? 0}
              href="/debts?status=OUTSTANDING"
            />
          </Col>
        </Row>

        <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
          <Col xs={24} sm={8}>
            <ClickableStatCard
              size="small"
              loading={loading}
              title="Học viên hết buổi"
              value={summary?.studentsOutOfSessions ?? 0}
              href="/students?remaining=ZERO"
              valueStyle={{ color: '#cf1322', fontSize: 20 }}
            />
          </Col>
          <Col xs={24} sm={8}>
            <ClickableStatCard
              size="small"
              loading={loading}
              title="Học viên sắp hết buổi"
              value={summary?.studentsNearlyOutOfSessions ?? 0}
              href="/students?remaining=LOW"
              valueStyle={{ color: '#d46b08', fontSize: 20 }}
            />
          </Col>
          <Col xs={24} sm={8}>
            <ClickableStatCard
              size="small"
              loading={loading}
              title="Buổi chưa điểm danh"
              value={summary?.incompleteAttendance ?? 0}
              href="/dashboard#pending-attendance"
              valueStyle={{ fontSize: 20 }}
            />
          </Col>
        </Row>
      </div>

      <div id="today-sessions">
        <Card title="Công việc hôm nay · Lịch học hôm nay">
          <Table<DashboardTodaySession>
            rowKey="sessionId"
            loading={loading}
            dataSource={overview?.todaySessions ?? []}
            columns={todaySessionColumns}
            pagination={false}
            locale={{ emptyText: <Empty description="Không có lớp học hôm nay." /> }}
          />
        </Card>
      </div>

      <div id="pending-attendance">
        <Card title="Buổi chưa điểm danh">
          <Table<DashboardPendingAttendance>
            rowKey="sessionId"
            loading={loading}
            dataSource={overview?.pendingAttendance ?? []}
            columns={pendingColumns}
            pagination={false}
            locale={{ emptyText: <Empty description="Không có buổi học cần điểm danh." /> }}
          />
        </Card>
      </div>

      <Card title="Học viên cần gia hạn">
        <Table<SessionWarning>
          rowKey="enrollmentId"
          loading={loading}
          dataSource={overview?.studentsNeedingRenewal ?? []}
          columns={renewalColumns}
          pagination={false}
          locale={{ emptyText: <Empty description="Không có học viên cần gia hạn." /> }}
        />
      </Card>

      <Card title="Hóa đơn cần thu">
        <Table<DebtReportItem>
          rowKey="invoiceId"
          loading={loading}
          dataSource={overview?.overdueInvoices ?? []}
          columns={debtColumns}
          pagination={false}
          locale={{ emptyText: <Empty description="Không có hóa đơn quá hạn." /> }}
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
