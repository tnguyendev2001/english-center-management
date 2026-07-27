import { Card, Space, Table, Typography } from 'antd'
import dayjs from 'dayjs'
import { StatusTag } from '../../../components/common/StatusTag'
import { studentCodeColumn, studentNameColumn } from '../../../components/common/studentDisplay'
import { useMakeupCredits } from '../makeupCreditQueries'
import type { MakeupCredit, MakeupCreditReason } from '../makeupCreditTypes'

const { Title, Text } = Typography

const reasonLabels: Record<MakeupCreditReason, string> = {
  EXCUSED_ABSENCE: 'Xin nghỉ',
  CLASS_CANCELED: 'Hủy buổi học',
  MANUAL_ADJUSTMENT: 'Điều chỉnh thủ công',
}

/** V1 leave-tracking statuses only. AVAILABLE = recorded leave; no USED workflow. */
const leaveStatusLabels = {
  AVAILABLE: 'Đã ghi nhận',
  CANCELED: 'Đã hủy',
}

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
        <Table<MakeupCredit>
          rowKey="id"
          loading={makeupCreditsQuery.isLoading}
          dataSource={makeupCreditsQuery.data ?? []}
          columns={[
            studentCodeColumn(),
            studentNameColumn(),
            { title: 'Lớp', dataIndex: 'classroomName', key: 'classroomName' },
            {
              title: 'Buổi học đã nghỉ',
              key: 'missedSession',
              render: (_: unknown, record: MakeupCredit) =>
                record.sourceSessionDate
                  ? `Buổi ngày ${dayjs(record.sourceSessionDate).format('DD/MM/YYYY')}`
                  : '-',
            },
            {
              title: 'Ngày nghỉ',
              dataIndex: 'sourceSessionDate',
              key: 'sourceSessionDate',
              render: (value?: string | null) => (value ? dayjs(value).format('DD/MM/YYYY') : '-'),
            },
            {
              title: 'Lý do',
              dataIndex: 'reason',
              key: 'reason',
              render: (reason: MakeupCreditReason) => reasonLabels[reason],
            },
            {
              title: 'Trạng thái',
              dataIndex: 'status',
              key: 'status',
              render: (status: string) => <StatusTag status={status} labels={leaveStatusLabels} />,
            },
            {
              title: 'Ghi chú',
              dataIndex: 'note',
              key: 'note',
              render: (value?: string | null) => value || '-',
            },
            {
              title: 'Thao tác',
              key: 'actions',
              render: () => '-',
            },
          ]}
        />
      </Card>
    </Space>
  )
}
