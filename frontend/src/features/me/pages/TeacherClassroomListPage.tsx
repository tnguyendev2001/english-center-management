import { Button, Card, Empty, Modal, Space, Table, Typography, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { StatusTag } from '../../../components/common/StatusTag'
import { classroomDetailPath } from '../../classrooms/classroomRoutes'
import type { Classroom } from '../../classrooms/classroomTypes'
import { formatDaysOfWeek } from '../../classrooms/classroomTypes'
import { CreateClassSessionModal } from '../../classSessions/components/CreateClassSessionModal'
import { useCreateClassSession } from '../../classSessions/classSessionQueries'
import type { ClassSession } from '../../classSessions/classSessionTypes'
import type { CreateClassSessionPayload } from '../../classSessions/classSessionTypes'
import { useMyClassrooms, useMySessions } from '../meQueries'

const { Title, Text } = Typography

export function TeacherClassroomListPage() {
  const navigate = useNavigate()
  const classroomsQuery = useMyClassrooms()
  const sessionsQuery = useMySessions()
  const createSession = useCreateClassSession()
  const [creatingFor, setCreatingFor] = useState<Classroom>()

  const sessionHints = useMemo(
    () => buildSessionHints(sessionsQuery.data ?? []),
    [sessionsQuery.data],
  )

  const columns: ColumnsType<Classroom> = [
    {
      title: 'Mã lớp',
      dataIndex: 'classCode',
      render: (code: string, classroom) => <Link to={`/classrooms/${classroom.id}`}>{code}</Link>,
    },
    { title: 'Tên lớp', dataIndex: 'className' },
    { title: 'Trình độ', dataIndex: 'level' },
    {
      title: 'Phòng',
      dataIndex: 'room',
      render: (value?: string | null) => value || '-',
    },
    {
      title: 'Lịch học',
      key: 'schedule',
      render: (_, classroom) =>
        `${formatDaysOfWeek(classroom.daysOfWeek)}, ${classroom.startTime.slice(0, 5)} - ${classroom.endTime.slice(0, 5)}`,
    },
    {
      title: 'Ngày bắt đầu',
      dataIndex: 'startDate',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
    },
    {
      title: 'Số học viên',
      dataIndex: 'activeStudentCount',
      render: (value?: number) => value ?? 0,
    },
    {
      title: 'Buổi học gần nhất',
      key: 'latestSession',
      render: (_, classroom) => sessionHints.get(classroom.id)?.latestLabel ?? '-',
    },
    {
      title: 'Buổi học tiếp theo',
      key: 'nextSession',
      render: (_, classroom) => sessionHints.get(classroom.id)?.nextLabel ?? '-',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      render: (status: string) => <StatusTag status={status} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, classroom) => (
        <Space size={0} wrap>
          <Button type="link" onClick={() => navigate(`/classrooms/${classroom.id}`)}>
            Xem lớp
          </Button>
          <Button
            type="link"
            onClick={() => navigate(classroomDetailPath(classroom.id, { tab: 'students' }))}
          >
            Xem học viên
          </Button>
          <Button
            type="link"
            onClick={() => navigate(classroomDetailPath(classroom.id, { tab: 'sessions' }))}
          >
            Xem buổi học
          </Button>
          <Button
            type="link"
            onClick={() => navigate(classroomDetailPath(classroom.id, { tab: 'attendance' }))}
          >
            Điểm danh
          </Button>
          {classroom.status === 'PLANNED' || classroom.status === 'ONGOING' ? (
            <Button type="link" onClick={() => setCreatingFor(classroom)}>
              Tạo buổi học
            </Button>
          ) : null}
        </Space>
      ),
    },
  ]

  const classrooms = classroomsQuery.data ?? []

  function handleCreate(payload: CreateClassSessionPayload) {
    createSession.mutate(payload, {
      onSuccess: (session) => {
        setCreatingFor(undefined)
        message.success('Tạo buổi học thành công.')
        Modal.confirm({
          title: 'Tạo buổi học thành công.',
          content: 'Bạn có muốn mở trang điểm danh cho buổi học này không?',
          okText: 'Đi tới điểm danh',
          cancelText: 'Đóng',
          onOk: () => {
            navigate(
              classroomDetailPath(session.classroomId, {
                tab: 'attendance',
                sessionId: session.id,
              }),
            )
          },
        })
      },
      onError: (error) => {
        message.error(
          isAxiosError(error)
            ? error.response?.data?.message ?? 'Không thể tạo buổi học'
            : 'Không thể tạo buổi học',
        )
      },
    })
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Lớp học của tôi
        </Title>
        <Text type="secondary">Chỉ hiển thị các lớp bạn được phân công phụ trách.</Text>
      </Space>

      <Card>
        {!classroomsQuery.isLoading && classrooms.length === 0 ? (
          <Empty description="Bạn chưa được phân công lớp học nào. Vui lòng liên hệ quản trị viên." />
        ) : (
          <Table
            rowKey="id"
            columns={columns}
            dataSource={classrooms}
            loading={classroomsQuery.isLoading || sessionsQuery.isLoading}
            pagination={false}
          />
        )}
      </Card>

      <CreateClassSessionModal
        open={Boolean(creatingFor)}
        classroom={creatingFor}
        submitting={createSession.isPending}
        onCancel={() => setCreatingFor(undefined)}
        onSubmit={handleCreate}
      />
    </Space>
  )
}

function buildSessionHints(sessions: ClassSession[]) {
  const today = dayjs().startOf('day')
  const map = new Map<number, { latestLabel: string; nextLabel: string }>()

  const byClassroom = new Map<number, ClassSession[]>()
  for (const session of sessions) {
    const list = byClassroom.get(session.classroomId) ?? []
    list.push(session)
    byClassroom.set(session.classroomId, list)
  }

  for (const [classroomId, list] of byClassroom) {
    const sorted = [...list].sort((a, b) => {
      const dateCompare = a.sessionDate.localeCompare(b.sessionDate)
      if (dateCompare !== 0) {
        return dateCompare
      }
      return a.startTime.localeCompare(b.startTime)
    })

    const pastOrToday = [...sorted]
      .reverse()
      .find((session) => !dayjs(session.sessionDate).isAfter(today, 'day'))
    const next = sorted.find((session) => dayjs(session.sessionDate).isAfter(today, 'day'))

    map.set(classroomId, {
      latestLabel: pastOrToday
        ? `${dayjs(pastOrToday.sessionDate).format('DD/MM/YYYY')} ${pastOrToday.startTime.slice(0, 5)}`
        : '-',
      nextLabel: next
        ? `${dayjs(next.sessionDate).format('DD/MM/YYYY')} ${next.startTime.slice(0, 5)}`
        : '-',
    })
  }

  return map
}
