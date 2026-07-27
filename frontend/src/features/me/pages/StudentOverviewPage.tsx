import { Card, Col, Empty, Progress, Row, Space, Statistic, Table, Typography } from 'antd'
import dayjs from 'dayjs'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { useStudentDashboard } from '../meQueries'
import type { StudentAttendanceItem, StudentScheduleItem } from '../studentTypes'
import type { Enrollment } from '../../enrollments/enrollmentTypes'
import type { Invoice } from '../../invoices/invoiceTypes'

const { Title, Text } = Typography

export function StudentOverviewPage() {
  const dashboardQuery = useStudentDashboard()
  const data = dashboardQuery.data

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Tổng quan
        </Title>
        <Text type="secondary">Thông tin học tập và học phí của bạn.</Text>
      </Space>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={8} xl={4}>
          <Card loading={dashboardQuery.isLoading}>
            <Statistic title="Lớp đang học" value={data?.activeClassCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8} xl={5}>
          <Card loading={dashboardQuery.isLoading}>
            <Statistic
              title="Buổi học tiếp theo"
              value={
                data?.nextSession
                  ? `${dayjs(data.nextSession.sessionDate).format('DD/MM')} ${data.nextSession.startTime.slice(0, 5)}`
                  : '—'
              }
            />
            {data?.nextSession ? (
              <Text type="secondary">{data.nextSession.classroomName}</Text>
            ) : null}
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8} xl={5}>
          <Card loading={dashboardQuery.isLoading}>
            <Statistic title="Đã dùng" value={data?.usedSessions ?? 0} suffix="buổi" />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8} xl={5}>
          <Card loading={dashboardQuery.isLoading}>
            <Statistic title="Còn lại" value={data?.remainingSessions ?? 0} suffix="buổi" />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8} xl={5}>
          <Card loading={dashboardQuery.isLoading}>
            <Statistic
              title="Tổng công nợ"
              value={Number(data?.totalOutstandingDebt ?? 0)}
              formatter={(value) => <MoneyText value={Number(value)} />}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Lịch học sắp tới" loading={dashboardQuery.isLoading}>
            <Table<StudentScheduleItem>
              rowKey="sessionId"
              size="small"
              pagination={false}
              dataSource={data?.upcomingSessions ?? []}
              locale={{ emptyText: 'Hiện chưa có buổi học sắp tới.' }}
              columns={[
                {
                  title: 'Ngày',
                  dataIndex: 'sessionDate',
                  render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
                },
                { title: 'Lớp', dataIndex: 'classroomName' },
                {
                  title: 'Giờ',
                  key: 'time',
                  render: (_, session) =>
                    `${session.startTime.slice(0, 5)} - ${session.endTime.slice(0, 5)}`,
                },
                {
                  title: 'Phòng',
                  dataIndex: 'room',
                  render: (value?: string | null) => value || '-',
                },
                {
                  title: 'Trạng thái',
                  dataIndex: 'status',
                  render: (status: string) => <StatusTag status={status} />,
                },
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card title="Tiến độ học tập" loading={dashboardQuery.isLoading}>
            {(data?.progressItems ?? []).length === 0 ? (
              <Empty description="Bạn chưa được ghi danh vào lớp học nào." />
            ) : (
              <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                {(data?.progressItems ?? []).map((item: Enrollment) => {
                  const total = item.totalSessions || 0
                  const used = item.usedSessions || 0
                  const percent = total > 0 ? Math.min(Math.round((used / total) * 100), 100) : 0
                  return (
                    <Card key={item.id} size="small">
                      <Space direction="vertical" style={{ width: '100%' }} size={4}>
                        <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
                          <Text strong>{item.classroomName}</Text>
                          <StatusTag status={item.status} />
                        </Space>
                        <Text type="secondary">
                          {used}/{total} buổi · còn {item.remainingSessions} buổi
                        </Text>
                        <Progress percent={percent} size="small" />
                      </Space>
                    </Card>
                  )
                })}
              </Space>
            )}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Điểm danh gần đây" loading={dashboardQuery.isLoading}>
            <Table<StudentAttendanceItem>
              rowKey="id"
              size="small"
              pagination={false}
              dataSource={data?.recentAttendance ?? []}
              locale={{ emptyText: 'Chưa có dữ liệu điểm danh.' }}
              columns={[
                {
                  title: 'Ngày',
                  dataIndex: 'sessionDate',
                  render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
                },
                { title: 'Lớp', dataIndex: 'classroomName' },
                {
                  title: 'Trạng thái',
                  dataIndex: 'status',
                  render: (status: string) => <StatusTag status={status} />,
                },
                {
                  title: 'Ghi chú',
                  dataIndex: 'note',
                  render: (value?: string | null) => value || '-',
                },
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card title="Học phí cần chú ý" loading={dashboardQuery.isLoading}>
            {(data?.attentionInvoices ?? []).length === 0 ? (
              <Empty description="Bạn không có khoản học phí chưa thanh toán." />
            ) : (
              <Table<Invoice>
                rowKey="id"
                size="small"
                pagination={false}
                dataSource={data?.attentionInvoices ?? []}
                columns={[
                  { title: 'Hóa đơn', dataIndex: 'invoiceCode' },
                  { title: 'Lớp học', dataIndex: 'classroomName' },
                  {
                    title: 'Tổng tiền',
                    dataIndex: 'finalAmount',
                    render: (value: number) => <MoneyText value={value} />,
                  },
                  {
                    title: 'Đã thanh toán',
                    dataIndex: 'paidAmount',
                    render: (value: number) => <MoneyText value={value} />,
                  },
                  {
                    title: 'Còn lại',
                    dataIndex: 'remainingAmount',
                    render: (value: number) => <MoneyText value={value} />,
                  },
                  {
                    title: 'Trạng thái',
                    dataIndex: 'status',
                    render: (status: string) => <StatusTag status={status} />,
                  },
                ]}
              />
            )}
          </Card>
        </Col>
      </Row>
    </Space>
  )
}
