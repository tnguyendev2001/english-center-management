import { Alert, Drawer, Empty, Spin, Timeline, Typography } from 'antd'
import dayjs from 'dayjs'
import { StatusTag } from '../../../components/common/StatusTag'
import { useEnrollmentStatusHistory } from '../enrollmentQueries'
import type { EnrollmentStatus } from '../enrollmentTypes'

const { Text } = Typography

const enrollmentStatusLabels: Record<EnrollmentStatus, string> = {
  ACTIVE: 'Đang học',
  ON_HOLD: 'Bảo lưu',
  STOPPED: 'Đã ngừng học',
  TRANSFERRED: 'Đã chuyển lớp',
  CANCELED: 'Đã hủy ghi danh',
}

interface EnrollmentStatusHistoryDrawerProps {
  open: boolean
  enrollmentId?: number
  title?: string
  onClose: () => void
}

export function EnrollmentStatusHistoryDrawer({
  open,
  enrollmentId,
  title,
  onClose,
}: EnrollmentStatusHistoryDrawerProps) {
  const historyQuery = useEnrollmentStatusHistory(enrollmentId, open)

  return (
    <Drawer
      title={title ? `Lịch sử trạng thái - ${title}` : 'Lịch sử trạng thái'}
      width={480}
      open={open}
      onClose={onClose}
      destroyOnHidden
    >
      {historyQuery.isLoading ? <Spin /> : null}
      {historyQuery.isError ? (
        <Alert type="error" showIcon message="Không thể tải lịch sử trạng thái" />
      ) : null}
      {historyQuery.isSuccess && historyQuery.data.length === 0 ? (
        <Empty description="Chưa có lịch sử trạng thái" />
      ) : null}
      {historyQuery.isSuccess && historyQuery.data.length > 0 ? (
        <Timeline
          items={[...historyQuery.data]
            .sort(
              (left, right) =>
                dayjs(right.effectiveFrom).valueOf() - dayjs(left.effectiveFrom).valueOf() ||
                right.id - left.id,
            )
            .map((item) => ({
              children: (
                <div>
                  <StatusTag status={item.status} labels={enrollmentStatusLabels} />
                  <div>
                    <Text type="secondary">Từ ngày: </Text>
                    {formatDate(item.effectiveFrom)}
                  </div>
                  <div>
                    <Text type="secondary">Đến trước ngày: </Text>
                    {item.effectiveTo ? formatDate(item.effectiveTo) : 'Hiện tại'}
                  </div>
                  <div>
                    <Text type="secondary">Lý do: </Text>
                    {item.reason || '-'}
                  </div>
                </div>
              ),
            }))}
        />
      ) : null}
    </Drawer>
  )
}

function formatDate(value: string) {
  return dayjs(value).format('DD/MM/YYYY')
}
