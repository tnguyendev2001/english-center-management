import { Card, Descriptions, Empty, Space, Spin, Typography } from 'antd'
import { useParams } from 'react-router-dom'
import { StatusTag } from '../../../components/common/StatusTag'
import { useStudentDetail } from '../studentQueries'

const { Title, Text } = Typography

export function StudentDetailPage() {
  const { id } = useParams()
  const studentId = Number(id)
  const studentQuery = useStudentDetail(studentId)

  if (!Number.isFinite(studentId)) {
    return <Empty description="Không tìm thấy học viên" />
  }

  if (studentQuery.isLoading) {
    return <Spin />
  }

  if (!studentQuery.data) {
    return <Empty description="Không tìm thấy học viên" />
  }

  const student = studentQuery.data

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          {student.fullName}
        </Title>
        <Text type="secondary">Thông tin cơ bản của học viên.</Text>
      </Space>

      <Card>
        <Descriptions column={1} bordered>
          <Descriptions.Item label="Mã học viên">{student.studentCode}</Descriptions.Item>
          <Descriptions.Item label="Họ tên">{student.fullName}</Descriptions.Item>
          <Descriptions.Item label="Số điện thoại">{student.phone || '-'}</Descriptions.Item>
          <Descriptions.Item label="Tên phụ huynh">{student.parentName || '-'}</Descriptions.Item>
          <Descriptions.Item label="SĐT phụ huynh">{student.parentPhone || '-'}</Descriptions.Item>
          <Descriptions.Item label="Địa chỉ">{student.address || '-'}</Descriptions.Item>
          <Descriptions.Item label="Trạng thái">
            <StatusTag status={student.status} />
          </Descriptions.Item>
          <Descriptions.Item label="Ghi chú">{student.note || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>
    </Space>
  )
}
