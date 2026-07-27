import { Card, Select, Space, Table, Typography } from 'antd'
import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { ActiveFilterTags } from '../../../components/common/ActiveFilterTags'
import { StatusTag } from '../../../components/common/StatusTag'
import { useUrlEnumParam } from '../../../hooks/useUrlEnumParam'
import { useMyStudents } from '../meQueries'
import type { TeacherStudentItem } from '../meTypes'

const { Title, Text } = Typography

const REMAINING_OPTIONS = ['ZERO', 'LOW'] as const
type RemainingFilter = (typeof REMAINING_OPTIONS)[number]

const REMAINING_LABELS: Record<RemainingFilter, string> = {
  ZERO: 'Đã hết buổi',
  LOW: 'Sắp hết buổi',
}

export function TeacherProgressPage() {
  const studentsQuery = useMyStudents()
  const remainingParam = useUrlEnumParam('remaining', REMAINING_OPTIONS)
  const remainingFilter = remainingParam.value

  const filteredStudents = useMemo(() => {
    const all = studentsQuery.data ?? []
    if (remainingFilter === 'ZERO') {
      return all.filter((student) => student.remainingSessions <= 0)
    }
    if (remainingFilter === 'LOW') {
      return all.filter(
        (student) => student.remainingSessions > 0 && student.remainingSessions <= 2,
      )
    }
    return all
  }, [remainingFilter, studentsQuery.data])

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Tiến độ học tập
        </Title>
        <Text type="secondary">
          {remainingFilter
            ? `Tiến độ – ${REMAINING_LABELS[remainingFilter]}`
            : 'Tiến độ buổi học của học viên trong các lớp bạn phụ trách.'}
        </Text>
      </Space>

      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Select
            allowClear
            placeholder="Tình trạng số buổi"
            style={{ width: 220 }}
            value={remainingFilter}
            onChange={(value) => remainingParam.setValue(value)}
            options={[
              { value: 'ZERO', label: 'Đã hết buổi' },
              { value: 'LOW', label: 'Sắp hết buổi' },
            ]}
          />

          <ActiveFilterTags
            tags={
              remainingFilter
                ? [
                    {
                      key: 'remaining',
                      label: REMAINING_LABELS[remainingFilter],
                      color: remainingFilter === 'ZERO' ? 'red' : 'orange',
                      onClose: () => remainingParam.clear(),
                    },
                  ]
                : []
            }
          />

          <Table<TeacherStudentItem>
            rowKey="studentId"
            loading={studentsQuery.isLoading}
            dataSource={filteredStudents}
            locale={{ emptyText: 'Không có học viên khớp bộ lọc.' }}
            columns={[
              {
                title: 'Học viên',
                key: 'student',
                render: (_, record) => (
                  <Link to={`/students/${record.studentId}`}>
                    {record.studentCode} - {record.fullName}
                  </Link>
                ),
              },
              {
                title: 'Lớp',
                dataIndex: 'classroomNames',
                render: (names: string[]) => names.join(', ') || '-',
              },
              {
                title: 'Trạng thái',
                dataIndex: 'learningStatus',
                render: (status: string) => <StatusTag status={status} />,
              },
              { title: 'Đã học', dataIndex: 'usedSessions' },
              { title: 'Còn lại', dataIndex: 'remainingSessions' },
            ]}
            pagination={{ pageSize: 20 }}
          />
        </Space>
      </Card>
    </Space>
  )
}
