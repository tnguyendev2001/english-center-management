import { Alert, Button, Card, Col, Empty, List, Row, Space, Table, Typography } from 'antd'
import dayjs from 'dayjs'
import { Link, useNavigate } from 'react-router-dom'
import { StatusTag } from '../../../components/common/StatusTag'
import { classroomDetailPath } from '../../classrooms/classroomRoutes'
import { ClickableStatCard } from '../../dashboard/components/ClickableStatCard'
import { DashboardAlertsSection } from '../../dashboard/components/DashboardAlertsSection'
import { useDashboardOverview } from '../../dashboard/dashboardQueries'
import { useTeacherDashboard } from '../meQueries'

const { Title, Text } = Typography

export function TeacherDashboardPage() {
  const navigate = useNavigate()
  const overviewQuery = useDashboardOverview()
  const dashboardQuery = useTeacherDashboard()
  const overview = overviewQuery.data
  const data = dashboardQuery.data
  const loading = overviewQuery.isLoading || dashboardQuery.isLoading

  if (overviewQuery.isError) {
    return (
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Title level={2} style={{ margin: 0 }}>
          Tổng quan giáo viên
        </Title>
        <Alert
          type="error"
          showIcon
          message="Không tải được dữ liệu tổng quan"
          action={<Button onClick={() => void overviewQuery.refetch()}>Thử lại</Button>}
        />
      </Space>
    )
  }

  if (!loading && overview && (overview.summary.assignedClassrooms ?? 0) === 0) {
    return (
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Title level={2} style={{ margin: 0 }}>
          Tổng quan giáo viên
        </Title>
        <Empty description="Bạn chưa được phân công lớp học. Trang tổng quan sẽ có dữ liệu sau khi quản trị viên phân công lớp." />
      </Space>
    )
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Tổng quan giáo viên
        </Title>
        <Text type="secondary">Dữ liệu chỉ từ các lớp bạn đang phụ trách.</Text>
      </Space>

      <DashboardAlertsSection
        alerts={overview?.alerts}
        loading={overviewQuery.isLoading}
        onRetry={() => void overviewQuery.refetch()}
      />

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <ClickableStatCard
            loading={loading}
            title="Số lớp đang phụ trách"
            value={overview?.summary.assignedClassrooms ?? 0}
            href="/me/classrooms"
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <ClickableStatCard
            loading={loading}
            title="Tổng học viên đang học"
            value={overview?.summary.activeStudents ?? 0}
            href="/me/students"
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <ClickableStatCard
            loading={loading}
            title="Buổi học hôm nay"
            value={overview?.summary.todayClasses ?? 0}
            href="/me/sessions"
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <ClickableStatCard
            loading={loading}
            title="Buổi chưa hoàn tất điểm danh"
            value={overview?.summary.incompleteAttendance ?? 0}
            href="/me/attendance"
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Lớp học hôm nay" loading={loading}>
            <List
              dataSource={overview?.todaySessions ?? []}
              locale={{ emptyText: 'Không có lớp học hôm nay.' }}
              renderItem={(item) => (
                <List.Item
                  actions={[
                    <Button
                      key="attendance"
                      type="link"
                      onClick={() =>
                        navigate(
                          classroomDetailPath(item.classroomId, {
                            tab: 'attendance',
                            sessionId: item.sessionId,
                          }),
                        )
                      }
                    >
                      Điểm danh
                    </Button>,
                  ]}
                >
                  <List.Item.Meta
                    title={item.classroomName}
                    description={`${item.startTime.slice(0, 5)} - ${item.endTime.slice(0, 5)} · Phòng ${item.room || '-'} · ${item.activeStudentCount} học viên`}
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card title="Buổi chưa điểm danh" loading={loading}>
            <List
              dataSource={overview?.pendingAttendance ?? []}
              locale={{ emptyText: 'Không có buổi học cần điểm danh.' }}
              renderItem={(item) => (
                <List.Item
                  actions={[
                    <Button
                      key="go"
                      type="link"
                      onClick={() =>
                        navigate(
                          classroomDetailPath(item.classroomId, {
                            tab: 'attendance',
                            sessionId: item.sessionId,
                          }),
                        )
                      }
                    >
                      Điểm danh
                    </Button>,
                  ]}
                >
                  <List.Item.Meta
                    title={item.classroomName}
                    description={`${dayjs(item.sessionDate).format('DD/MM/YYYY')} · thiếu ${item.missingCount}/${item.eligibleStudentCount}`}
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Buổi học sắp tới" loading={dashboardQuery.isLoading}>
            <Table
              rowKey="id"
              size="small"
              pagination={false}
              dataSource={data?.upcomingSessions ?? []}
              locale={{ emptyText: 'Không có buổi sắp tới' }}
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
                  title: 'Trạng thái',
                  dataIndex: 'status',
                  render: (status: string) => <StatusTag status={status} />,
                },
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card title="Lớp đang phụ trách" loading={dashboardQuery.isLoading}>
            <List
              dataSource={data?.assignedClassrooms ?? []}
              locale={{ emptyText: 'Chưa có lớp' }}
              renderItem={(classroom) => (
                <List.Item
                  actions={[
                    <Link key="view" to={`/classrooms/${classroom.id}`}>
                      Xem lớp
                    </Link>,
                  ]}
                >
                  <List.Item.Meta
                    title={`${classroom.classCode} - ${classroom.className}`}
                    description={`${classroom.level} · ${classroom.activeStudentCount ?? 0} học viên · ${classroom.status}`}
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </Space>
  )
}
