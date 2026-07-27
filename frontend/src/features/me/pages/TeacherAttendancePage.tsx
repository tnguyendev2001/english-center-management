import { Button, Card, Select, Space, Table, Typography } from 'antd'
import dayjs from 'dayjs'
import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { ActiveFilterTags } from '../../../components/common/ActiveFilterTags'
import { StatusTag } from '../../../components/common/StatusTag'
import { useUrlEnumParam } from '../../../hooks/useUrlEnumParam'
import { classroomDetailPath } from '../../classrooms/classroomRoutes'
import { useDashboardOverview } from '../../dashboard/dashboardQueries'
import type { DashboardPendingAttendance } from '../../dashboard/dashboardTypes'
import type { ClassSession } from '../../classSessions/classSessionTypes'
import { useMySessions } from '../meQueries'

const { Title, Text } = Typography

const STATUS_OPTIONS = ['TODAY', 'PENDING'] as const
type AttendanceStatusFilter = (typeof STATUS_OPTIONS)[number]

const STATUS_LABELS: Record<AttendanceStatusFilter, string> = {
  TODAY: 'Hôm nay',
  PENDING: 'Chưa điểm danh',
}

export function TeacherAttendancePage() {
  const navigate = useNavigate()
  const sessionsQuery = useMySessions()
  const overviewQuery = useDashboardOverview()
  const statusParam = useUrlEnumParam('status', STATUS_OPTIONS)
  const statusFilter: AttendanceStatusFilter = statusParam.value ?? 'TODAY'

  const todaySessions = useMemo(() => {
    const today = dayjs().format('YYYY-MM-DD')
    return (sessionsQuery.data ?? [])
      .filter((session) => session.sessionDate === today && session.status !== 'CANCELED')
      .sort((a, b) => a.startTime.localeCompare(b.startTime))
  }, [sessionsQuery.data])

  const pendingSessions = overviewQuery.data?.pendingAttendance ?? []

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Điểm danh
        </Title>
        <Text type="secondary">
          {statusFilter === 'PENDING'
            ? 'Các buổi đã qua chưa hoàn tất điểm danh trong lớp bạn phụ trách.'
            : 'Điểm danh nhanh các buổi học hôm nay của lớp bạn phụ trách.'}
        </Text>
      </Space>

      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Select
            style={{ width: 220 }}
            value={statusFilter}
            onChange={(value: AttendanceStatusFilter) => statusParam.setValue(value)}
            options={[
              { value: 'TODAY', label: 'Hôm nay' },
              { value: 'PENDING', label: 'Chưa điểm danh' },
            ]}
          />

          <ActiveFilterTags
            tags={[
              {
                key: 'status',
                label: STATUS_LABELS[statusFilter],
                color: statusFilter === 'PENDING' ? 'red' : 'blue',
                onClose: () => statusParam.setValue('TODAY'),
              },
            ]}
          />

          {statusFilter === 'PENDING' ? (
            <Table<DashboardPendingAttendance>
              rowKey="sessionId"
              loading={overviewQuery.isLoading}
              dataSource={pendingSessions}
              locale={{ emptyText: 'Không có buổi học cần điểm danh.' }}
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
                { title: 'Đủ điều kiện', dataIndex: 'eligibleStudentCount' },
                { title: 'Đã điểm danh', dataIndex: 'markedCount' },
                { title: 'Còn thiếu', dataIndex: 'missingCount' },
                {
                  title: 'Thao tác',
                  key: 'actions',
                  render: (_, session) => (
                    <Button
                      type="primary"
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
              ]}
              pagination={false}
            />
          ) : (
            <Table<ClassSession>
              rowKey="id"
              loading={sessionsQuery.isLoading}
              dataSource={todaySessions}
              locale={{ emptyText: 'Hôm nay không có buổi học cần điểm danh.' }}
              columns={[
                { title: 'Lớp', dataIndex: 'classroomName' },
                {
                  title: 'Giờ',
                  key: 'time',
                  render: (_, session) =>
                    `${session.startTime.slice(0, 5)} - ${session.endTime.slice(0, 5)}`,
                },
                {
                  title: 'Trạng thái buổi',
                  dataIndex: 'status',
                  render: (status: string) => <StatusTag status={status} />,
                },
                {
                  title: 'Thao tác',
                  key: 'actions',
                  render: (_, session) => (
                    <Button
                      type="primary"
                      onClick={() =>
                        navigate(
                          classroomDetailPath(session.classroomId, {
                            tab: 'attendance',
                            sessionId: session.id,
                          }),
                        )
                      }
                    >
                      Điểm danh
                    </Button>
                  ),
                },
              ]}
              pagination={false}
            />
          )}
        </Space>
      </Card>
    </Space>
  )
}
