import { Card, Space, Table, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { StatusTag } from '../../../components/common/StatusTag'
import { useMyStudents } from '../meQueries'
import type { TeacherStudentItem } from '../meTypes'

const { Title, Text } = Typography

export function TeacherProgressPage() {
  const studentsQuery = useMyStudents()

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Tiến độ học tập
        </Title>
        <Text type="secondary">Tiến độ buổi học của học viên trong các lớp bạn phụ trách.</Text>
      </Space>

      <Card>
        <Table<TeacherStudentItem>
          rowKey="studentId"
          loading={studentsQuery.isLoading}
          dataSource={studentsQuery.data ?? []}
          locale={{ emptyText: 'Chưa có dữ liệu tiến độ.' }}
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
      </Card>
    </Space>
  )
}
