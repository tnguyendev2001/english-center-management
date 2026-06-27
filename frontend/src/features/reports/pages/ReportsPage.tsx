import { Card, Empty, Space, Typography } from 'antd'

const { Title, Text } = Typography

export function ReportsPage() {
  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Báo cáo
        </Title>
        <Text type="secondary">Khu vực báo cáo chi tiết sẽ được triển khai ở các phase sau.</Text>
      </Space>

      <Card>
        <Empty description="Chưa có báo cáo chi tiết" />
      </Card>
    </Space>
  )
}
