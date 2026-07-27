import { Button, Card, Segmented, Space, Table, Typography } from 'antd'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { StatusTag } from '../../../components/common/StatusTag'
import { classroomDetailPath } from '../../classrooms/classroomRoutes'
import type { ClassSession } from '../../classSessions/classSessionTypes'
import { useMySessions } from '../meQueries'

const { Title, Text } = Typography

type SessionFilter = 'TODAY' | 'UPCOMING' | 'HISTORY' | 'ALL'

export function TeacherSessionsPage() {
  const navigate = useNavigate()
  const sessionsQuery = useMySessions()
  const [filter, setFilter] = useState<SessionFilter>('TODAY')

  const sessions = useMemo(() => {
    const all = [...(sessionsQuery.data ?? [])].sort((a, b) => {
      const dateCompare = a.sessionDate.localeCompare(b.sessionDate)
      if (dateCompare !== 0) {
        return dateCompare
      }
      return a.startTime.localeCompare(b.startTime)
    })

    const today = dayjs().format('YYYY-MM-DD')
    if (filter === 'TODAY') {
      return all.filter((session) => session.sessionDate === today)
    }
    if (filter === 'UPCOMING') {
      return all.filter((session) => session.sessionDate > today)
    }
    if (filter === 'HISTORY') {
      return all.filter((session) => session.sessionDate < today)
    }
    return all
  }, [filter, sessionsQuery.data])

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Buổi học
        </Title>
        <Text type="secondary">Buổi học của các lớp bạn phụ trách.</Text>
      </Space>

      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Segmented
            value={filter}
            onChange={(value) => setFilter(value as SessionFilter)}
            options={[
              { label: 'Hôm nay', value: 'TODAY' },
              { label: 'Sắp tới', value: 'UPCOMING' },
              { label: 'Lịch sử', value: 'HISTORY' },
              { label: 'Tất cả', value: 'ALL' },
            ]}
          />

          <Table<ClassSession>
            rowKey="id"
            loading={sessionsQuery.isLoading}
            dataSource={sessions}
            locale={{ emptyText: 'Không có buổi học phù hợp bộ lọc.' }}
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
            pagination={{ pageSize: 20 }}
          />
        </Space>
      </Card>
    </Space>
  )
}
