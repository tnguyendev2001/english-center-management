import { Button, Card, Descriptions, Empty, Space, Spin, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { useAuth } from '../../auth/AuthContext'
import { useClassPackages } from '../../classPackages/classPackageQueries'
import { RenewAllPackagesModal } from '../../classrooms/components/RenewAllPackagesModal'
import { classroomDetailPath } from '../../classrooms/classroomRoutes'
import { useMakeupCredits } from '../../makeupCredits/makeupCreditQueries'
import type { MakeupCredit } from '../../makeupCredits/makeupCreditTypes'
import {
  EnrollmentLifecycleModal,
  type EnrollmentLifecycleAction,
} from '../../enrollments/components/EnrollmentLifecycleModal'
import { EnrollmentStatusHistoryDrawer } from '../../enrollments/components/EnrollmentStatusHistoryDrawer'
import {
  DEBT_STATUS_LABELS,
  INVOICE_STATUS_LABELS,
  formatEstimatedEffectiveTo,
  type Invoice,
} from '../../invoices/invoiceTypes'
import { useInvoices } from '../../invoices/invoiceQueries'
import { useStudentPackages } from '../../studentPackages/studentPackageQueries'
import type { EnrollmentLearningProgress } from '../../studentPackages/studentPackageTypes'
import { useStudentDetail } from '../studentQueries'

const { Title, Text } = Typography

export function StudentDetailPage() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const { id } = useParams()
  const studentId = Number(id)
  const studentQuery = useStudentDetail(studentId)
  const studentPackagesQuery = useStudentPackages(studentId)
  const makeupCreditsQuery = useMakeupCredits()
  const invoicesQuery = useInvoices(
    { studentId, page: 0, size: 50 },
    isAdmin && Number.isFinite(studentId),
  )
  const [lifecycleAction, setLifecycleAction] = useState<{
    action: EnrollmentLifecycleAction
    enrollment: EnrollmentLearningProgress
  }>()
  const [historyEnrollment, setHistoryEnrollment] = useState<EnrollmentLearningProgress>()
  const [renewingProgress, setRenewingProgress] = useState<EnrollmentLearningProgress>()
  const classPackagesQuery = useClassPackages(renewingProgress?.classroomId ?? Number.NaN)

  const teacherTuitionRows = useMemo(() => {
    return (studentPackagesQuery.data ?? [])
      .filter((item) => item.latestStudentPackageId != null)
      .map((item) => ({
        key: String(item.enrollmentId),
        classroomName: item.classroomName,
        packageName: item.latestPackageName ?? '-',
        packagePrice: item.latestPackagePrice,
        totalSessions: item.latestPackageTotalSessions ?? item.totalSessions,
      }))
  }, [studentPackagesQuery.data])

  if (!Number.isFinite(studentId)) {
    return <Empty description="Không tìm thấy học viên" />
  }

  if (studentQuery.isLoading) {
    return <Spin />
  }

  if (!studentQuery.data) {
    return <Empty description="Không tìm thấy học viên" />
  }

  const student = studentQuery.data
  const makeupCredits = (makeupCreditsQuery.data ?? []).filter((credit) => credit.studentId === studentId)

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          {student.fullName}
        </Title>
        <Text type="secondary">Thông tin cơ bản của học viên.</Text>
      </Space>

      <Card>
        <Descriptions column={1} bordered>
          <Descriptions.Item label="Mã học viên">{student.studentCode}</Descriptions.Item>
          <Descriptions.Item label="Họ tên">{student.fullName}</Descriptions.Item>
          <Descriptions.Item label="Số điện thoại">{student.phone || '-'}</Descriptions.Item>
          <Descriptions.Item label="Tên phụ huynh">{student.parentName || '-'}</Descriptions.Item>
          <Descriptions.Item label="SĐT phụ huynh">{student.parentPhone || '-'}</Descriptions.Item>
          <Descriptions.Item label="Địa chỉ">{student.address || '-'}</Descriptions.Item>
          <Descriptions.Item label="Trạng thái">
            <StatusTag status={student.status} />
          </Descriptions.Item>
          <Descriptions.Item label="Ghi chú">{student.note || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="Tiến độ học">
        <Table
          rowKey="enrollmentId"
          dataSource={studentPackagesQuery.data ?? []}
          loading={studentPackagesQuery.isLoading}
          pagination={false}
          columns={
            [
              {
                title: 'Lớp học',
                dataIndex: 'classroomName',
                key: 'classroomName',
                render: (classroomName: string, record: EnrollmentLearningProgress) => (
                  <Link to={classroomDetailPath(record.classroomId)}>{classroomName}</Link>
                ),
              },
              {
                title: 'Gói gần nhất',
                dataIndex: 'latestPackageName',
                key: 'latestPackageName',
              },
              ...(isAdmin
                ? [
                    {
                      title: 'Học phí',
                      key: 'latestPackagePrice',
                      render: (_: unknown, record: EnrollmentLearningProgress) =>
                        record.latestPackagePrice != null ? (
                          <MoneyText value={record.latestPackagePrice} />
                        ) : (
                          '-'
                        ),
                    },
                  ]
                : []),
              { title: 'Tổng buổi', dataIndex: 'totalSessions', key: 'totalSessions' },
              { title: 'Đã học', dataIndex: 'usedSessions', key: 'usedSessions' },
              {
                title: 'Còn lại',
                key: 'remainingSessions',
                render: (_: unknown, record: EnrollmentLearningProgress) => record.remainingSessions,
              },
              {
                title: 'Buổi bù',
                dataIndex: 'makeupAvailableSessions',
                key: 'makeupAvailableSessions',
              },
              {
                title: 'Thời gian học',
                key: 'learningDates',
                render: (_: unknown, record: EnrollmentLearningProgress) =>
                  `${dayjs(record.startDate).format('DD/MM/YYYY')} - ${
                    record.endDate ? dayjs(record.endDate).format('DD/MM/YYYY') : 'nay'
                  }`,
              },
              {
                title: 'Trạng thái',
                dataIndex: 'status',
                key: 'status',
                render: (status: string) => (
                  <StatusTag status={status} labels={{ CANCELED: 'Đã hủy ghi danh' }} />
                ),
              },
              {
                title: 'Thao tác',
                key: 'actions',
                render: (_: unknown, record: EnrollmentLearningProgress) => (
                  <Space wrap size={0}>
                    {isAdmin && record.status === 'ACTIVE' ? (
                      <>
                        <Button
                          type="link"
                          onClick={() => setLifecycleAction({ action: 'hold', enrollment: record })}
                        >
                          Bảo lưu
                        </Button>
                        <Button
                          type="link"
                          onClick={() => setLifecycleAction({ action: 'stop', enrollment: record })}
                        >
                          Ngừng học
                        </Button>
                        <Button
                          type="link"
                          onClick={() =>
                            setLifecycleAction({ action: 'transfer', enrollment: record })
                          }
                        >
                          Chuyển lớp
                        </Button>
                        <Button type="link" onClick={() => setRenewingProgress(record)}>
                          Gia hạn học
                        </Button>
                        <Button
                          type="link"
                          danger
                          onClick={() =>
                            setLifecycleAction({ action: 'cancel', enrollment: record })
                          }
                        >
                          Hủy ghi danh
                        </Button>
                      </>
                    ) : null}
                    {isAdmin && record.status === 'ON_HOLD' ? (
                      <>
                        <Button
                          type="link"
                          onClick={() =>
                            setLifecycleAction({ action: 'reactivate', enrollment: record })
                          }
                        >
                          Học lại
                        </Button>
                        <Button
                          type="link"
                          onClick={() => setLifecycleAction({ action: 'stop', enrollment: record })}
                        >
                          Ngừng học
                        </Button>
                      </>
                    ) : null}
                    {isAdmin && record.status === 'STOPPED' ? (
                      <Button
                        type="link"
                        onClick={() =>
                          setLifecycleAction({ action: 'reactivate', enrollment: record })
                        }
                      >
                        Học lại
                      </Button>
                    ) : null}
                    <Button type="link" onClick={() => setHistoryEnrollment(record)}>
                      Lịch sử
                    </Button>
                    <Link to={classroomDetailPath(record.classroomId, { tab: 'attendance' })}>
                      Điểm danh lớp
                    </Link>
                  </Space>
                ),
              },
            ] as ColumnsType<EnrollmentLearningProgress>
          }
        />
      </Card>

      {isAdmin ? (
        <Card title="Học phí / hóa đơn">
          <Table<Invoice>
            rowKey="id"
            dataSource={invoicesQuery.data?.data ?? []}
            loading={invoicesQuery.isLoading}
            pagination={false}
            locale={{ emptyText: 'Chưa có hóa đơn học phí.' }}
            scroll={{ x: 1000 }}
            columns={[
              { title: 'Mã HĐ', dataIndex: 'invoiceCode', key: 'invoiceCode' },
              {
                title: 'Kỳ học phí',
                dataIndex: 'billingLabel',
                key: 'billingLabel',
                render: (value?: string | null) => value || '-',
              },
              {
                title: 'Gói học',
                dataIndex: 'packageNameSnapshot',
                key: 'packageNameSnapshot',
              },
              {
                title: 'Áp dụng từ',
                dataIndex: 'effectiveFrom',
                key: 'effectiveFrom',
                render: (value?: string | null) =>
                  value ? dayjs(value).format('DD/MM/YYYY') : '-',
              },
              {
                title: 'Dự kiến đến',
                key: 'estimatedEffectiveTo',
                render: (_, invoice) =>
                  formatEstimatedEffectiveTo(
                    invoice.estimatedEffectiveTo,
                    invoice.totalSessionsSnapshot,
                  ),
              },
              {
                title: 'Hạn TT',
                dataIndex: 'dueDate',
                key: 'dueDate',
                render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
              },
              {
                title: 'Còn lại',
                dataIndex: 'remainingAmount',
                key: 'remainingAmount',
                render: (value: number) => <MoneyText value={value} />,
              },
              {
                title: 'Trạng thái',
                key: 'status',
                render: (_, invoice) =>
                  invoice.debtStatus ? (
                    <StatusTag status={invoice.debtStatus} labels={DEBT_STATUS_LABELS} />
                  ) : (
                    <StatusTag status={invoice.status} labels={INVOICE_STATUS_LABELS} />
                  ),
              },
              {
                title: 'Thao tác',
                key: 'actions',
                render: (_, invoice) => (
                  <Link to={`/print/invoices/${invoice.id}`}>
                    Xem phiếu học phí
                  </Link>
                ),
              },
            ]}
          />
        </Card>
      ) : (
        <Card title="Tóm tắt học phí (chỉ xem)">
          <Table
            rowKey="key"
            dataSource={teacherTuitionRows}
            loading={studentPackagesQuery.isLoading}
            pagination={false}
            locale={{ emptyText: 'Chưa có thông tin gói học phí.' }}
            columns={[
              { title: 'Lớp', dataIndex: 'classroomName', key: 'classroomName' },
              { title: 'Gói học', dataIndex: 'packageName', key: 'packageName' },
              {
                title: 'Học phí gói',
                dataIndex: 'packagePrice',
                key: 'packagePrice',
                render: (value?: number | null) =>
                  value != null ? <MoneyText value={value} /> : '-',
              },
              { title: 'Số buổi', dataIndex: 'totalSessions', key: 'totalSessions' },
            ]}
          />
          <Text type="secondary">
            Giáo viên xem tóm tắt gói học. Phiếu học phí chi tiết mở khi có mã hóa đơn được cấp quyền.
          </Text>
        </Card>
      )}

      <Card title="Buổi bù">
        <Table
          rowKey="id"
          dataSource={makeupCredits}
          loading={makeupCreditsQuery.isLoading}
          pagination={false}
          columns={[
            {
              title: 'Lớp học',
              dataIndex: 'classroomName',
              key: 'classroomName',
              render: (classroomName: string, record: MakeupCredit) => (
                <Link to={classroomDetailPath(record.classroomId)}>{classroomName}</Link>
              ),
            },
            {
              title: 'Nguồn',
              dataIndex: 'reason',
              key: 'reason',
              render: (reason: string) => (reason === 'EXCUSED_ABSENCE' ? 'Xin nghỉ' : reason),
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

      <EnrollmentLifecycleModal
        open={Boolean(lifecycleAction)}
        action={lifecycleAction?.action ?? 'stop'}
        enrollment={
          lifecycleAction
            ? {
                id: lifecycleAction.enrollment.enrollmentId,
                studentCode: lifecycleAction.enrollment.studentCode,
                studentName: lifecycleAction.enrollment.studentName,
                classroomId: lifecycleAction.enrollment.classroomId,
                classroomName: lifecycleAction.enrollment.classroomName,
                remainingSessions: lifecycleAction.enrollment.remainingSessions,
              }
            : undefined
        }
        onCancel={() => setLifecycleAction(undefined)}
      />
      <EnrollmentStatusHistoryDrawer
        open={Boolean(historyEnrollment)}
        enrollmentId={historyEnrollment?.enrollmentId}
        title={historyEnrollment?.classroomName}
        onClose={() => setHistoryEnrollment(undefined)}
      />

      {renewingProgress ? (
        <RenewAllPackagesModal
          open
          classroomId={renewingProgress.classroomId}
          classPackages={classPackagesQuery.data ?? []}
          initialEnrollmentIds={[renewingProgress.enrollmentId]}
          onCancel={() => setRenewingProgress(undefined)}
          onSuccess={() => {
            void studentPackagesQuery.refetch()
            setRenewingProgress(undefined)
          }}
        />
      ) : null}
    </Space>
  )
}
