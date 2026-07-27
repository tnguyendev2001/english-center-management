import { Tag } from 'antd'

type StatusConfig = {
  label: string
  color: string
}

const statusConfigs: Record<string, StatusConfig> = {
  ACTIVE: { label: 'Đang học', color: 'green' },
  ON_HOLD: { label: 'Bảo lưu', color: 'gold' },
  INACTIVE: { label: 'Ngừng học', color: 'default' },
  STOPPED: { label: 'Đã ngừng học', color: 'default' },
  TRANSFERRED: { label: 'Đã chuyển lớp', color: 'blue' },
  PLANNED: { label: 'Dự kiến', color: 'blue' },
  ONGOING: { label: 'Đang học', color: 'green' },
  COMPLETED: { label: 'Hoàn thành', color: 'default' },
  CANCELED: { label: 'Đã hủy', color: 'red' },
  UNPAID: { label: 'Chưa thanh toán', color: 'red' },
  PARTIALLY_PAID: { label: 'Thanh toán một phần', color: 'gold' },
  PAID: { label: 'Đã thanh toán', color: 'green' },
  REPLACED: { label: 'Đã thay thế', color: 'default' },
  NOT_DUE: { label: 'Chưa đến hạn', color: 'blue' },
  DUE_TODAY: { label: 'Đến hạn hôm nay', color: 'gold' },
  OVERDUE: { label: 'Quá hạn', color: 'red' },
  VALID: { label: 'Hợp lệ', color: 'green' },
  WARNING: { label: 'Cảnh báo', color: 'gold' },
  INVALID: { label: 'Lỗi', color: 'red' },
  SKIPPED_DUPLICATE_ENROLLMENT: { label: 'Trùng ghi danh', color: 'default' },
  SCHEDULED: { label: 'Đã lên lịch', color: 'blue' },
  PRESENT: { label: 'Có mặt', color: 'green' },
  ABSENT: { label: 'Vắng', color: 'red' },
  EXCUSED: { label: 'Xin nghỉ', color: 'gold' },
  AVAILABLE: { label: 'Đã ghi nhận', color: 'green' },
  // MakeupCredit USED is not a V1 user-facing leave workflow; keep fallback for legacy rows only.
  USED: { label: 'Đã ghi nhận', color: 'default' },
}

interface StatusTagProps {
  status: string
  labels?: Partial<Record<string, string>>
}

export function StatusTag({ status, labels }: StatusTagProps) {
  const config = statusConfigs[status] ?? { label: status, color: 'default' }

  return <Tag color={config.color}>{labels?.[status] ?? config.label}</Tag>
}
