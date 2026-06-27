import { Card, Space, Table, Typography } from 'antd'
import dayjs from 'dayjs'
import { StatusTag } from '../../../components/common/StatusTag'
import { useTodaySessions } from '../../classSessions/classSessionQueries'
import type { ClassSession } from '../../classSessions/classSessionTypes'

const { Title, Text } = Typography

export function DashboardPage() {
  const todaySessionsQuery = useTodaySessions()

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Dashboard
        </Title>
        <Text type="secondary">Theo dõi nhanh các buổi học trong ngày.</Text>
      </Space>

      <Card title="Buổi học hôm nay">
        <Table<ClassSession>
          rowKey="id"
          loading={todaySessionsQuery.isLoading}
          dataSource={todaySessionsQuery.data ?? []}
          pagination={false}
          columns={[
            { title: 'Lớp học', dataIndex: 'classroomName', key: 'classroomName' },
            {
              title: 'Ngày học',
              dataIndex: 'sessionDate',
              key: 'sessionDate',
              render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
            },
            {
              title: 'Giờ học',
              key: 'time',
              render: (_, session) => `${session.startTime.slice(0, 5)} - ${session.endTime.slice(0, 5)}`,
            },
            {
              title: 'Trạng thái',
              dataIndex: 'status',
              key: 'status',
              render: (status: string) => <StatusTag status={status} />,
            },
          ]}
        />
      </Card>
    </Space>
  )
}
