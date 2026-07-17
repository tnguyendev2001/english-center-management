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

export function MakeupCreditPage() {
  const makeupCreditsQuery = useMakeupCredits()

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Buổi bù
        </Title>
        <Text type="secondary">Theo dõi các buổi bù được tạo từ xin nghỉ hoặc điều chỉnh.</Text>
      </Space>

      <Card>
        <Table<MakeupCredit>
          rowKey="id"
          loading={makeupCreditsQuery.isLoading}
          dataSource={makeupCreditsQuery.data ?? []}
          columns={[
            studentCodeColumn(),
            studentNameColumn(),
            { title: 'Lớp học', dataIndex: 'classroomName', key: 'classroomName' },
            {
              title: 'Buổi nguồn',
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
            { title: 'Số buổi bù', dataIndex: 'creditSessions', key: 'creditSessions' },
            { title: 'Đã dùng', dataIndex: 'usedSessions', key: 'usedSessions' },
            {
              title: 'Trạng thái',
              dataIndex: 'status',
              key: 'status',
              render: (status: string) => <StatusTag status={status} />,
            },
            {
              title: 'Ghi chú',
              dataIndex: 'note',
              key: 'note',
              render: (value?: string | null) => value || '-',
            },
          ]}
        />
      </Card>
    </Space>
  )
}
