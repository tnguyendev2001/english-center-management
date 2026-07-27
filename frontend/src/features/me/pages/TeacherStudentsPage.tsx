import { Card, Space, Table, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { StatusTag } from '../../../components/common/StatusTag'
import { useMyStudents } from '../meQueries'
import type { TeacherStudentItem } from '../meTypes'

const { Title, Text } = Typography

export function TeacherStudentsPage() {
  const studentsQuery = useMyStudents()

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Học viên của tôi
        </Title>
        <Text type="secondary">
          Học viên thuộc các lớp bạn phụ trách. Không hiển thị thông tin tài chính.
        </Text>
      </Space>

      <Card>
        <Table<TeacherStudentItem>
          rowKey="studentId"
          loading={studentsQuery.isLoading}
          dataSource={studentsQuery.data ?? []}
          locale={{
            emptyText: 'Chưa có học viên trong các lớp được phân công.',
          }}
          columns={[
            {
              title: 'Mã học viên',
              dataIndex: 'studentCode',
              render: (code: string, record) => (
                <Link to={`/students/${record.studentId}`}>{code}</Link>
              ),
            },
            { title: 'Họ tên', dataIndex: 'fullName' },
            {
              title: 'Lớp phụ trách',
              dataIndex: 'classroomNames',
              render: (names: string[]) => names.join(', ') || '-',
            },
            {
              title: 'Trạng thái học',
              dataIndex: 'learningStatus',
              render: (status: string) => <StatusTag status={status} />,
            },
            { title: 'Đã học', dataIndex: 'usedSessions' },
            { title: 'Còn lại', dataIndex: 'remainingSessions' },
            {
              title: 'Buổi học tiếp theo',
              dataIndex: 'nextSessionLabel',
              render: (value?: string | null) => value || '-',
            },
            {
              title: 'Thao tác',
              key: 'actions',
              render: (_, record) => <Link to={`/students/${record.studentId}`}>Xem hồ sơ</Link>,
            },
          ]}
          pagination={{ pageSize: 20 }}
        />
      </Card>
    </Space>
  )
}
