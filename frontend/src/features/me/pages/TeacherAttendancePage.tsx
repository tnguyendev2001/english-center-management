import { Button, Card, Space, Table, Typography } from 'antd'
import dayjs from 'dayjs'
import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { StatusTag } from '../../../components/common/StatusTag'
import { classroomDetailPath } from '../../classrooms/classroomRoutes'
import type { ClassSession } from '../../classSessions/classSessionTypes'
import { useMySessions } from '../meQueries'

const { Title, Text } = Typography

export function TeacherAttendancePage() {
  const navigate = useNavigate()
  const sessionsQuery = useMySessions()

  const todaySessions = useMemo(() => {
    const today = dayjs().format('YYYY-MM-DD')
    return (sessionsQuery.data ?? [])
      .filter((session) => session.sessionDate === today && session.status !== 'CANCELED')
      .sort((a, b) => a.startTime.localeCompare(b.startTime))
  }, [sessionsQuery.data])

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Điểm danh
        </Title>
        <Text type="secondary">Điểm danh nhanh các buổi học hôm nay của lớp bạn phụ trách.</Text>
      </Space>

      <Card>
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
      </Card>
    </Space>
  )
}
