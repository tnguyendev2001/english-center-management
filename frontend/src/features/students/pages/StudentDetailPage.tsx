import { Card, Descriptions, Empty, Space, Spin, Table, Typography } from 'antd'
import dayjs from 'dayjs'
import { useParams } from 'react-router-dom'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { useMakeupCredits } from '../../makeupCredits/makeupCreditQueries'
import {
  formatRemainingSessions,
  formatTotalAvailableSessions,
  LearningProgressWarning,
} from '../../studentPackages/components/LearningProgressWarning'
import { useStudentPackages } from '../../studentPackages/studentPackageQueries'
import type { StudentPackageProgress } from '../../studentPackages/studentPackageTypes'
import { useStudentDetail } from '../studentQueries'

const { Title, Text } = Typography

export function StudentDetailPage() {
  const { id } = useParams()
  const studentId = Number(id)
  const studentQuery = useStudentDetail(studentId)
  const studentPackagesQuery = useStudentPackages(studentId)
  const makeupCreditsQuery = useMakeupCredits()

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
  const makeupCredits = (makeupCreditsQuery.data ?? []).filter((credit) => credit.studentId === studentId)

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

      <Card title="Tiến độ học">
        <Table
          rowKey="id"
          dataSource={studentPackagesQuery.data ?? []}
          loading={studentPackagesQuery.isLoading}
          pagination={false}
          columns={[
            { title: 'Lớp học', dataIndex: 'classroomName', key: 'classroomName' },
            { title: 'Gói học phí', dataIndex: 'packageName', key: 'packageName' },
            {
              title: 'Học phí',
              dataIndex: 'price',
              key: 'price',
              render: (value: number) => <MoneyText value={value} />,
            },
            { title: 'Tổng buổi', dataIndex: 'totalSessions', key: 'totalSessions' },
            { title: 'Đã học', dataIndex: 'usedSessions', key: 'usedSessions' },
            {
              title: 'Còn lại',
              key: 'remainingSessions',
              render: (_: unknown, record: StudentPackageProgress) => formatRemainingSessions(record),
            },
            {
              title: 'Cảnh báo',
              key: 'warningMessage',
              render: (_: unknown, record: StudentPackageProgress) => (
                <LearningProgressWarning progress={record} />
              ),
            },
            { title: 'Buổi bù', dataIndex: 'makeupAvailableSessions', key: 'makeupAvailableSessions' },
            {
              title: 'Tổng khả dụng',
              key: 'totalAvailableSessions',
              render: (_: unknown, record: StudentPackageProgress) => formatTotalAvailableSessions(record),
            },
            {
              title: 'Trạng thái',
              dataIndex: 'status',
              key: 'status',
              render: (status: string) => <StatusTag status={status} />,
            },
            {
              title: 'Ngày bắt đầu',
              dataIndex: 'startDate',
              key: 'startDate',
              render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
            },
          ]}
        />
      </Card>

      <Card title="Buổi bù">
        <Table
          rowKey="id"
          dataSource={makeupCredits}
          loading={makeupCreditsQuery.isLoading}
          pagination={false}
          columns={[
            { title: 'Lớp học', dataIndex: 'classroomName', key: 'classroomName' },
            {
              title: 'Nguồn',
              dataIndex: 'reason',
              key: 'reason',
              render: (reason: string) => (reason === 'EXCUSED_ABSENCE' ? 'Xin nghỉ' : reason),
            },
            { title: 'Số buổi bù', dataIndex: 'creditSessions', key: 'creditSessions' },
            { title: 'Đã dùng', dataIndex: 'usedSessions', key: 'usedSessions' },
            {
              title: 'Trạng thái',
              dataIndex: 'status',
              key: 'status',
              render: (status: string) => <StatusTag status={status} />,
            },
            {
              title: 'Ghi chú',
              dataIndex: 'note',
              key: 'note',
              render: (value?: string | null) => value || '-',
            },
          ]}
        />
      </Card>
    </Space>
  )
}
