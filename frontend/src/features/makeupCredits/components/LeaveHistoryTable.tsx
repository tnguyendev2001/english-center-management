import { Button, Popconfirm, Table, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import dayjs from 'dayjs'
import { Link } from 'react-router-dom'
import { StatusTag } from '../../../components/common/StatusTag'
import { formatStudentLabel } from '../../../components/common/studentDisplay'
import { classroomDetailPath } from '../../classrooms/classroomRoutes'
import { useCancelMakeupCredit } from '../makeupCreditQueries'
import type { MakeupCredit, MakeupCreditReason, MakeupCreditStatus } from '../makeupCreditTypes'
import {
  canCancelLeaveRecord,
  formatLeaveSessionLabel,
  leaveReasonLabels,
  leaveStatusLabels,
} from '../makeupCreditTypes'

interface LeaveHistoryTableProps {
  dataSource: MakeupCredit[]
  loading?: boolean
  showStudent?: boolean
  pagination?: false
}

export function LeaveHistoryTable({
  dataSource,
  loading,
  showStudent = true,
  pagination,
}: LeaveHistoryTableProps) {
  const cancelLeave = useCancelMakeupCredit()

  function handleCancel(id: number) {
    cancelLeave.mutate(id, {
      onSuccess: () => {
        message.success('Đã hủy ghi nhận nghỉ phép')
      },
      onError: (error: unknown) => {
        if (isAxiosError(error)) {
          message.error(error.response?.data?.message ?? 'Có lỗi xảy ra')
          return
        }
        message.error('Có lỗi xảy ra')
      },
    })
  }

  const columns: ColumnsType<MakeupCredit> = [
    ...(showStudent
      ? [
          {
            title: 'Học viên',
            key: 'student',
            render: (_: unknown, record: MakeupCredit) =>
              formatStudentLabel(record.studentCode, record.studentName),
          },
        ]
      : []),
    {
      title: 'Lớp',
      dataIndex: 'classroomName',
      key: 'classroomName',
      render: (classroomName: string, record: MakeupCredit) => (
        <Link to={classroomDetailPath(record.classroomId)}>{classroomName}</Link>
      ),
    },
    {
      title: 'Buổi học đã nghỉ',
      key: 'sourceSessionNo',
      render: (_: unknown, record: MakeupCredit) => formatLeaveSessionLabel(record),
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
      render: (reason: MakeupCreditReason) => leaveReasonLabels[reason] ?? reason,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: MakeupCreditStatus) => (
        <StatusTag status={status} labels={leaveStatusLabels} />
      ),
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
      render: (_: unknown, record: MakeupCredit) =>
        canCancelLeaveRecord(record.status) ? (
          <Popconfirm
            title="Hủy ghi nhận nghỉ phép?"
            description="Chỉ hủy bản ghi theo dõi nghỉ phép. Điểm danh xin nghỉ trên buổi học không đổi."
            okText="Hủy ghi nhận"
            cancelText="Đóng"
            okButtonProps={{ danger: true }}
            onConfirm={() => handleCancel(record.id)}
          >
            <Button type="link" danger loading={cancelLeave.isPending}>
              Hủy ghi nhận nghỉ phép
            </Button>
          </Popconfirm>
        ) : null,
    },
  ]

  return (
    <Table<MakeupCredit>
      rowKey="id"
      loading={loading}
      dataSource={dataSource}
      pagination={pagination}
      columns={columns}
    />
  )
}
