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
}

export function StatusTag({ status }: StatusTagProps) {
  const config = statusConfigs[status] ?? { label: status, color: 'default' }

  return <Tag color={config.color}>{config.label}</Tag>
}
