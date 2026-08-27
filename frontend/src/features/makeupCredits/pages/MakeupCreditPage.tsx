import { Card, Space, Typography } from 'antd'
import { LeaveHistoryTable } from '../components/LeaveHistoryTable'
import { useMakeupCredits } from '../makeupCreditQueries'

const { Title, Text } = Typography

export function MakeupCreditPage() {
  const makeupCreditsQuery = useMakeupCredits()

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Theo dõi nghỉ phép
        </Title>
        <Text type="secondary">
          Theo dõi các buổi học viên được phép nghỉ và không bị tính buổi học.
        </Text>
      </Space>

      <Card title="Lịch sử nghỉ phép">
        <LeaveHistoryTable
          dataSource={makeupCreditsQuery.data ?? []}
          loading={makeupCreditsQuery.isLoading}
        />
      </Card>
    </Space>
  )
}
