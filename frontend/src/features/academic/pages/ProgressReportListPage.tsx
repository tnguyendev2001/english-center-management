import { Button, Card, Select, Space, Table, Typography } from 'antd'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { StatusTag } from '../../../components/common/StatusTag'
import { useProgressReports } from '../academicQueries'
import {
  PROGRESS_REPORT_STATUS_LABELS,
  type ProgressReport,
  type ProgressReportStatus,
} from '../academicTypes'
import { useClassroomSelectOptions } from '../hooks/useClassroomStudents'

const { Title, Text } = Typography

export function ProgressReportListPage() {
  const [classroomId, setClassroomId] = useState<number>()
  const [status, setStatus] = useState<ProgressReportStatus>()
  const [page, setPage] = useState(0)
  const { options: classroomOptions } = useClassroomSelectOptions()

  const params = useMemo(
    () => ({ classroomId, status, page, size: 20 }),
    [classroomId, page, status],
  )
  const reportsQuery = useProgressReports(params)

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Tổng kết học viên
        </Title>
        <Text type="secondary">Danh sách phiếu tổng kết học tập theo lớp / kỳ.</Text>
      </Space>

      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Space wrap>
            <Select
              allowClear
              placeholder="Lớp học"
              style={{ width: 220 }}
              value={classroomId}
              options={classroomOptions}
              onChange={(value) => {
                setClassroomId(value)
                setPage(0)
              }}
            />
            <Select
              allowClear
              placeholder="Trạng thái"
              style={{ width: 160 }}
              value={status}
              options={Object.entries(PROGRESS_REPORT_STATUS_LABELS).map(([value, label]) => ({
                value,
                label,
              }))}
              onChange={(value) => {
                setStatus(value)
                setPage(0)
              }}
            />
          </Space>

          <Table<ProgressReport>
            rowKey="id"
            loading={reportsQuery.isLoading}
            dataSource={reportsQuery.data?.data ?? []}
            pagination={{
              current: page + 1,
              pageSize: 20,
              total: reportsQuery.data?.meta?.totalElements ?? 0,
              onChange: (next) => setPage(next - 1),
            }}
            columns={[
              { title: 'Mã phiếu', dataIndex: 'id', width: 90 },
              { title: 'Học viên', dataIndex: 'studentId', width: 100 },
              { title: 'Lớp', dataIndex: 'classroomId', width: 90 },
              { title: 'Kỳ', dataIndex: 'evaluationPeriodId', width: 90 },
              {
                title: 'Trạng thái',
                dataIndex: 'status',
                width: 130,
                render: (value: ProgressReportStatus) => (
                  <StatusTag status={value} labels={PROGRESS_REPORT_STATUS_LABELS} />
                ),
              },
              {
                title: 'Công bố',
                dataIndex: 'publishedAt',
                render: (value?: string | null) =>
                  value ? dayjs(value).format('DD/MM/YYYY HH:mm') : '-',
              },
              {
                title: 'Thao tác',
                key: 'actions',
                width: 120,
                render: (_, record) => (
                  <Link to={`/print/progress-reports/${record.id}`} target="_blank">
                    <Button size="small" type="primary">
                      In
                    </Button>
                  </Link>
                ),
              },
            ]}
          />
        </Space>
      </Card>
    </Space>
  )
}
