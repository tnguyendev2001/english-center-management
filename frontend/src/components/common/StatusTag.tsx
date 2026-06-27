import { Tag } from 'antd'

type StatusConfig = {
  label: string
  color: string
}

const statusConfigs: Record<string, StatusConfig> = {
  ACTIVE: { label: 'Đang học', color: 'green' },
  ON_HOLD: { label: 'Tạm nghỉ', color: 'gold' },
  INACTIVE: { label: 'Ngừng học', color: 'default' },
}

interface StatusTagProps {
  status: string
  labels?: Partial<Record<string, string>>
}

export function StatusTag({ status, labels }: StatusTagProps) {
  const config = statusConfigs[status] ?? { label: status, color: 'default' }

  return <Tag color={config.color}>{labels?.[status] ?? config.label}</Tag>
}
