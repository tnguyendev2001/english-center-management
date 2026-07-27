import { Button, Card, Col, Empty, List, Row, Space, Statistic, Table, Typography } from 'antd'
import dayjs from 'dayjs'
import { Link, useNavigate } from 'react-router-dom'
import { StatusTag } from '../../../components/common/StatusTag'
import { classroomDetailPath } from '../../classrooms/classroomRoutes'
import { useTeacherDashboard } from '../meQueries'

const { Title, Text } = Typography

export function TeacherDashboardPage() {
  const navigate = useNavigate()
  const dashboardQuery = useTeacherDashboard()
  const data = dashboardQuery.data

  if (!dashboardQuery.isLoading && data && data.assignedClassroomCount === 0) {
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

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={dashboardQuery.isLoading}>
            <Statistic title="Số lớp đang phụ trách" value={data?.assignedClassroomCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={dashboardQuery.isLoading}>
            <Statistic title="Tổng học viên đang học" value={data?.activeStudentCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={dashboardQuery.isLoading}>
            <Statistic title="Buổi học hôm nay" value={data?.todaySessionCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={dashboardQuery.isLoading}>
            <Statistic title="Buổi chưa hoàn tất điểm danh" value={data?.incompleteAttendanceCount ?? 0} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Lớp học hôm nay" loading={dashboardQuery.isLoading}>
            <List
              dataSource={data?.todayClasses ?? []}
              locale={{ emptyText: 'Không có buổi học hôm nay' }}
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
                    title={item.className}
                    description={`${item.startTime.slice(0, 5)} - ${item.endTime.slice(0, 5)} · Phòng ${item.room || '-'} · ${item.studentCount} học viên`}
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>

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
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Cần xử lý" loading={dashboardQuery.isLoading}>
            <List
              dataSource={data?.actionItems ?? []}
              locale={{ emptyText: 'Không có việc cần xử lý' }}
              renderItem={(item) => (
                <List.Item
                  actions={
                    item.classroomId && item.sessionId
                      ? [
                          <Button
                            key="go"
                            type="link"
                            onClick={() =>
                              navigate(
                                classroomDetailPath(item.classroomId!, {
                                  tab: 'attendance',
                                  sessionId: item.sessionId!,
                                }),
                              )
                            }
                          >
                            Xử lý
                          </Button>,
                        ]
                      : undefined
                  }
                >
                  {item.message}
                </List.Item>
              )}
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
