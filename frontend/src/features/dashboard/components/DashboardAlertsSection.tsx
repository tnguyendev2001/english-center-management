import type { ReactNode } from 'react'
import {
  AlertOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import { Badge, Button, Card, Empty, List, Space, Tag, Typography } from 'antd'
import { useNavigate } from 'react-router-dom'
import {
  dashboardActionLabel,
  dashboardDestinationPath,
} from '../dashboardDestinations'
import type { DashboardAlert, DashboardAlertSeverity } from '../dashboardTypes'

const { Text } = Typography

const severityMeta: Record<
  DashboardAlertSeverity,
  { color: string; label: string; icon: ReactNode }
> = {
  CRITICAL: { color: 'red', label: 'Khẩn cấp', icon: <ExclamationCircleOutlined /> },
  WARNING: { color: 'orange', label: 'Cần xử lý', icon: <WarningOutlined /> },
  INFO: { color: 'blue', label: 'Thông tin', icon: <InfoCircleOutlined /> },
}

interface DashboardAlertsSectionProps {
  alerts?: DashboardAlert[]
  loading?: boolean
  error?: boolean
  onRetry?: () => void
}

export function DashboardAlertsSection({
  alerts,
  loading,
  error,
  onRetry,
}: DashboardAlertsSectionProps) {
  const navigate = useNavigate()

  function handleNavigate(alert: DashboardAlert) {
    const path = dashboardDestinationPath(alert.type)
    const target = path !== '/' ? path : alert.actionUrl
    navigate(target)
    if (target.includes('#')) {
      const hash = target.split('#')[1]
      window.setTimeout(() => {
        document.getElementById(hash)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }, 50)
    }
  }

  if (error) {
    return (
      <Card title="Cần xử lý hôm nay">
        <AlertOutlined style={{ marginRight: 8 }} />
        <Text type="danger">Không tải được danh sách việc cần xử lý.</Text>
        {onRetry ? (
          <Button type="link" onClick={onRetry}>
            Thử lại
          </Button>
        ) : null}
      </Card>
    )
  }

  return (
    <Card title="Cần xử lý hôm nay" loading={loading}>
      {!loading && (alerts?.length ?? 0) === 0 ? (
        <Empty description="Hiện không có công việc cần xử lý." image={Empty.PRESENTED_IMAGE_SIMPLE} />
      ) : (
        <List
          dataSource={alerts ?? []}
          renderItem={(alert) => {
            const meta = severityMeta[alert.severity]
            const actionLabel = dashboardActionLabel(alert.type, alert.actionLabel)
            return (
              <List.Item
                actions={[
                  <Button key="action" type="primary" onClick={() => handleNavigate(alert)}>
                    {actionLabel}
                  </Button>,
                ]}
              >
                <List.Item.Meta
                  avatar={
                    <Tag icon={meta.icon} color={meta.color}>
                      {meta.label}
                    </Tag>
                  }
                  title={
                    <Space>
                      <span>{alert.title}</span>
                      <Badge count={alert.count} overflowCount={999} style={{ backgroundColor: '#1677ff' }} />
                    </Space>
                  }
                  description={alert.description}
                />
              </List.Item>
            )
          }}
        />
      )}
    </Card>
  )
}
