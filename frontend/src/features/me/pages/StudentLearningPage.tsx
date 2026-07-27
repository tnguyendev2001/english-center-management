import { Card, DatePicker, Empty, Progress, Segmented, Space, Table, Tabs, Typography } from 'antd'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { StatusTag } from '../../../components/common/StatusTag'
import { formatDaysOfWeek } from '../../classrooms/classroomTypes'
import {
  useMyAttendance,
  useMyClasses,
  useMyProgress,
  useMySchedule,
} from '../meQueries'
import type { StudentAttendanceItem, StudentClassItem, StudentScheduleItem } from '../studentTypes'
import type { Enrollment } from '../../enrollments/enrollmentTypes'

const { Title, Text } = Typography

type ScheduleFilter = 'UPCOMING' | 'PAST' | 'ALL'

export function StudentLearningPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const activeTab = searchParams.get('tab') ?? 'classes'
  const classesQuery = useMyClasses()
  const scheduleQuery = useMySchedule()
  const attendanceQuery = useMyAttendance()
  const progressQuery = useMyProgress()
  const [scheduleFilter, setScheduleFilter] = useState<ScheduleFilter>('UPCOMING')
  const [month, setMonth] = useState<Dayjs | null>(null)

  const scheduleRows = useMemo(() => {
    const today = dayjs().format('YYYY-MM-DD')
    let rows = [...(scheduleQuery.data ?? [])]

    if (month) {
      const start = month.startOf('month').format('YYYY-MM-DD')
      const end = month.endOf('month').format('YYYY-MM-DD')
      rows = rows.filter((session) => session.sessionDate >= start && session.sessionDate <= end)
    }

    if (scheduleFilter === 'UPCOMING') {
      rows = rows.filter((session) => session.sessionDate >= today)
      rows.sort((a, b) =>
        a.sessionDate === b.sessionDate
          ? a.startTime.localeCompare(b.startTime)
          : a.sessionDate.localeCompare(b.sessionDate),
      )
    } else if (scheduleFilter === 'PAST') {
      rows = rows.filter((session) => session.sessionDate < today)
      rows.sort((a, b) =>
        a.sessionDate === b.sessionDate
          ? b.startTime.localeCompare(a.startTime)
          : b.sessionDate.localeCompare(a.sessionDate),
      )
    } else {
      rows.sort((a, b) =>
        a.sessionDate === b.sessionDate
          ? a.startTime.localeCompare(b.startTime)
          : a.sessionDate.localeCompare(b.sessionDate),
      )
    }

    return rows
  }, [month, scheduleFilter, scheduleQuery.data])

  function setTab(tab: string) {
    setSearchParams({ tab })
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Việc học của tôi
        </Title>
        <Text type="secondary">Lớp học, lịch học, điểm danh và tiến độ của bạn.</Text>
      </Space>

      <Card>
        <Tabs
          activeKey={activeTab}
          onChange={setTab}
          items={[
            {
              key: 'classes',
              label: 'Lớp học',
              children: (
                <Table<StudentClassItem>
                  rowKey="enrollmentId"
                  loading={classesQuery.isLoading}
                  dataSource={classesQuery.data ?? []}
                  locale={{ emptyText: 'Bạn chưa được ghi danh vào lớp học nào.' }}
                  pagination={false}
                  columns={[
                    { title: 'Tên lớp', dataIndex: 'className' },
                    { title: 'Trình độ', dataIndex: 'level' },
                    {
                      title: 'Giáo viên',
                      dataIndex: 'teacherName',
                      render: (value?: string | null) => value || '-',
                    },
                    {
                      title: 'Lịch học',
                      key: 'schedule',
                      render: (_, item) =>
                        `${formatDaysOfWeek(item.daysOfWeek)}, ${item.startTime.slice(0, 5)} - ${item.endTime.slice(0, 5)}`,
                    },
                    {
                      title: 'Phòng',
                      dataIndex: 'room',
                      render: (value?: string | null) => value || '-',
                    },
                    {
                      title: 'Ngày bắt đầu học',
                      dataIndex: 'learningStartDate',
                      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
                    },
                    {
                      title: 'Trạng thái ghi danh',
                      dataIndex: 'enrollmentStatus',
                      render: (status: string) => <StatusTag status={status} />,
                    },
                  ]}
                />
              ),
            },
            {
              key: 'schedule',
              label: 'Lịch học',
              children: (
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                  <Space wrap>
                    <Segmented
                      value={scheduleFilter}
                      onChange={(value) => setScheduleFilter(value as ScheduleFilter)}
                      options={[
                        { label: 'Sắp tới', value: 'UPCOMING' },
                        { label: 'Đã qua', value: 'PAST' },
                        { label: 'Tất cả', value: 'ALL' },
                      ]}
                    />
                    <DatePicker
                      picker="month"
                      allowClear
                      placeholder="Lọc theo tháng"
                      value={month}
                      onChange={setMonth}
                      format="MM/YYYY"
                    />
                  </Space>
                  <Table<StudentScheduleItem>
                    rowKey="sessionId"
                    loading={scheduleQuery.isLoading}
                    dataSource={scheduleRows}
                    locale={{
                      emptyText:
                        scheduleFilter === 'UPCOMING'
                          ? 'Hiện chưa có buổi học sắp tới.'
                          : 'Không có buổi học phù hợp bộ lọc.',
                    }}
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
                    pagination={{ pageSize: 20 }}
                  />
                </Space>
              ),
            },
            {
              key: 'attendance',
              label: 'Điểm danh',
              children: (
                <Table<StudentAttendanceItem>
                  rowKey="id"
                  loading={attendanceQuery.isLoading}
                  dataSource={attendanceQuery.data ?? []}
                  locale={{ emptyText: 'Chưa có dữ liệu điểm danh.' }}
                  columns={[
                    {
                      title: 'Ngày',
                      dataIndex: 'sessionDate',
                      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
                    },
                    { title: 'Lớp học', dataIndex: 'classroomName' },
                    {
                      title: 'Trạng thái điểm danh',
                      dataIndex: 'status',
                      render: (status: string) => <StatusTag status={status} />,
                    },
                    {
                      title: 'Ghi chú',
                      dataIndex: 'note',
                      render: (value?: string | null) => value || '-',
                    },
                  ]}
                  pagination={{ pageSize: 20 }}
                />
              ),
            },
            {
              key: 'progress',
              label: 'Tiến độ',
              children:
                (progressQuery.data ?? []).length === 0 ? (
                  <Empty description="Bạn chưa được ghi danh vào lớp học nào." />
                ) : (
                  <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                    {(progressQuery.data ?? []).map((item: Enrollment) => {
                      const total = item.totalSessions || 0
                      const used = item.usedSessions || 0
                      const percent = total > 0 ? Math.min(Math.round((used / total) * 100), 100) : 0
                      return (
                        <Card key={item.id} size="small">
                          <Space direction="vertical" style={{ width: '100%' }} size={8}>
                            <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
                              <Text strong>{item.classroomName}</Text>
                              <StatusTag status={item.status} />
                            </Space>
                            <Text>
                              Tổng {total} buổi · Đã học {used} · Còn lại {item.remainingSessions}
                            </Text>
                            <Progress percent={percent} />
                            {item.packageNameSnapshot ? (
                              <Text type="secondary">Gói hiện tại: {item.packageNameSnapshot}</Text>
                            ) : null}
                          </Space>
                        </Card>
                      )
                    })}
                  </Space>
                ),
            },
          ]}
        />
      </Card>
    </Space>
  )
}
