import { Tag } from 'antd'

type StatusConfig = {
  label: string
  color: string
}

const statusConfigs: Record<string, StatusConfig> = {
  ACTIVE: { label: 'Đang học', color: 'green' },
  ON_HOLD: { label: 'Tạm nghỉ', color: 'gold' },
  INACTIVE: { label: 'Ngừng học', color: 'default' },
  PLANNED: { label: 'Dự kiến', color: 'blue' },
  ONGOING: { label: 'Đang học', color: 'green' },
  COMPLETED: { label: 'Hoàn thành', color: 'default' },
  CANCELED: { label: 'Đã hủy', color: 'red' },
  UNPAID: { label: 'Chưa đóng', color: 'red' },
  PARTIALLY_PAID: { label: 'Đóng một phần', color: 'gold' },
  PAID: { label: 'Đã đóng', color: 'green' },
  REPLACED: { label: 'Đã thay thế do đổi gói', color: 'default' },
  VALID: { label: 'Hợp lệ', color: 'green' },
  SCHEDULED: { label: 'Đã lên lịch', color: 'blue' },
  PRESENT: { label: 'Có mặt', color: 'green' },
  ABSENT: { label: 'Vắng', color: 'red' },
  EXCUSED: { label: 'Xin nghỉ', color: 'gold' },
  AVAILABLE: { label: 'Còn buổi bù', color: 'green' },
  USED: { label: 'Đã dùng', color: 'default' },
}

interface StatusTagProps {
  status: string
  labels?: Partial<Record<string, string>>
}

export function StatusTag({ status, labels }: StatusTagProps) {
  const config = statusConfigs[status] ?? { label: status, color: 'default' }

  return <Tag color={config.color}>{labels?.[status] ?? config.label}</Tag>
}
