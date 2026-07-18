import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
  message,
  Modal,
  Popconfirm,
  Space,
  Spin,
  Table,
  Tabs,
  Tooltip,
  Typography,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import dayjs from 'dayjs'
import { useMemo, useState, useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useParams, useSearchParams } from 'react-router-dom'
import { MoneyText } from '../../../components/common/MoneyText'
import { formatStudentLabel } from '../../../components/common/studentDisplay'
import { StatusTag } from '../../../components/common/StatusTag'
import { AddClassPackageModal } from '../../classPackages/components/AddClassPackageModal'
import { RenewAllPackagesModal } from '../components/RenewAllPackagesModal'
import {
  useAddClassPackage,
  useClassPackages,
  useDeactivateClassPackage,
} from '../../classPackages/classPackageQueries'
import type { ClassPackage } from '../../classPackages/classPackageTypes'
import { AttendanceMarkPanel } from '../../attendance/components/AttendanceMarkPanel'
import { attendanceKeys } from '../../attendance/attendanceQueries'
import { CancelSessionModal } from '../../classSessions/components/CancelSessionModal'
import { GenerateSessionsModal } from '../../classSessions/components/GenerateSessionsModal'
import {
  useCancelClassSession,
  useClassSessions,
  useCorrectionCancelClassSession,
  useGenerateClassSessions,
  useRestoreClassSession,
} from '../../classSessions/classSessionQueries'
import type {
  CancelClassSessionPayload,
  ClassSession,
  GenerateClassSessionsPayload,
} from '../../classSessions/classSessionTypes'
import { EnrollStudentModal } from '../../enrollments/components/EnrollStudentModal'
import { useEnrollments, useEnrollStudent } from '../../enrollments/enrollmentQueries'
import type { EnrollStudentPayload } from '../../enrollments/enrollmentTypes'
import { ChangePackageModal } from '../../studentPackages/components/ChangePackageModal'
import {
  useChangePackage,
  useClassroomStudentPackages,
  usePreviewChangePackage,
} from '../../studentPackages/studentPackageQueries'
import { buildProgressByEnrollmentId } from '../../studentPackages/studentPackageUtils'
import type {
  ChangePackagePayload,
  ChangePackagePreviewPayload,
  EnrollmentLearningProgress,
} from '../../studentPackages/studentPackageTypes'
import { useTuitionPackages } from '../../tuitionPackages/tuitionPackageQueries'
import type { TuitionPackageSearchParams } from '../../tuitionPackages/tuitionPackageTypes'
import { useClassroomDetail, useEligibleStudents } from '../classroomQueries'
import { parseClassroomDetailSearchParams } from '../classroomRoutes'
import { formatDaysOfWeek } from '../classroomTypes'

const { Title, Text } = Typography

type CancelSessionMode = 'normal' | 'correction'

export function ClassroomDetailPage() {
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const classroomId = Number(id)
  const queryClient = useQueryClient()
  const initialSearch = useMemo(
    () => parseClassroomDetailSearchParams(searchParams),
    [searchParams],
  )
  const [addPackageModalOpen, setAddPackageModalOpen] = useState(false)
  const [enrollModalOpen, setEnrollModalOpen] = useState(false)
  const [renewalModalOpen, setRenewalModalOpen] = useState(false)
  const [generateSessionsOpen, setGenerateSessionsOpen] = useState(false)
  const [cancelingSession, setCancelingSession] = useState<ClassSession>()
  const [cancelSessionMode, setCancelSessionMode] = useState<CancelSessionMode>('normal')
  const [activeTab, setActiveTab] = useState(initialSearch.tab)
  const [attendanceSessionId, setAttendanceSessionId] = useState<number | undefined>(
    initialSearch.tab === 'attendance' ? initialSearch.sessionId : undefined,
  )
  const [changingPackage, setChangingPackage] = useState<EnrollmentLearningProgress>()
  const tuitionPackageParams: TuitionPackageSearchParams = useMemo(
    () => ({
      page: 0,
      size: 100,
    }),
    [],
  )
  const classroomQuery = useClassroomDetail(classroomId)
  const classPackagesQuery = useClassPackages(classroomId)
  const tuitionPackagesQuery = useTuitionPackages(tuitionPackageParams)
  const eligibleStudentsQuery = useEligibleStudents(classroomId, enrollModalOpen)
  const sessionsQuery = useClassSessions({ classroomId, page: 0, size: 100 })
  const enrollmentsQuery = useEnrollments({ page: 0, size: 100 })
  const studentPackagesQuery = useClassroomStudentPackages(classroomId)
  const addClassPackage = useAddClassPackage(classroomId)
  const deactivateClassPackage = useDeactivateClassPackage(classroomId)
  const enrollStudent = useEnrollStudent()
  const generateSessions = useGenerateClassSessions()
  const cancelClassSession = useCancelClassSession()
  const correctionCancelClassSession = useCorrectionCancelClassSession()
  const restoreClassSession = useRestoreClassSession()
  const previewChangePackage = usePreviewChangePackage(changingPackage?.latestStudentPackageId ?? undefined)
  const changePackage = useChangePackage(changingPackage?.latestStudentPackageId ?? undefined)

  useEffect(() => {
    const { tab, sessionId } = parseClassroomDetailSearchParams(searchParams)
    setActiveTab(tab)

    if (tab === 'attendance') {
      setAttendanceSessionId(sessionId)
    }
  }, [searchParams])

  if (!Number.isFinite(classroomId)) {
    return <Empty description="Không tìm thấy lớp học" />
  }

  if (classroomQuery.isLoading) {
    return <Spin />
  }

  if (!classroomQuery.data) {
    return <Empty description="Không tìm thấy lớp học" />
  }

  const classroom = classroomQuery.data
  const classPackages = classPackagesQuery.data ?? []
  const canEnroll = classroom.status === 'PLANNED' || classroom.status === 'ONGOING'
  const canMarkAttendance = classroom.status === 'ONGOING'
  const activeEnrollments = (enrollmentsQuery.data?.data ?? []).filter(
    (enrollment) => enrollment.classroomId === classroomId && enrollment.status === 'ACTIVE',
  )
  const progressByEnrollmentId = buildProgressByEnrollmentId(studentPackagesQuery.data ?? [])
  const activeProgress = studentPackagesQuery.data ?? []
  const outOfSessionsCount = activeProgress.filter(
    (progress: EnrollmentLearningProgress) => progress.remainingSessions <= 0,
  ).length
  const lowSessionsCount = activeProgress.filter(
    (progress: EnrollmentLearningProgress) =>
      progress.remainingSessions > 0 && progress.remainingSessions <= 2,
  ).length

  function getChangePackageDisabledReason(progress?: EnrollmentLearningProgress) {
    if (!progress?.latestStudentPackageId) {
      return 'Chưa có gói học phí'
    }

    if (classroom.status === 'COMPLETED' || classroom.status === 'CANCELED') {
      return 'Không thể đổi gói khi lớp đã kết thúc hoặc đã hủy'
    }

    const hasAlternativePackage = classPackages.some(
      (classPackage) =>
        classPackage.active &&
        classPackage.tuitionPackageStatus === 'ACTIVE' &&
        classPackage.tuitionPackageId !== progress.latestTuitionPackageId,
    )

    if (!hasAlternativePackage) {
      return 'Lớp chưa có gói học phí khác đang áp dụng'
    }

    return undefined
  }

  function openAttendance(sessionId: number) {
    setAttendanceSessionId(sessionId)
    setActiveTab('attendance')
  }

  const packageColumns: ColumnsType<ClassPackage> = [
    {
      title: 'Tên gói',
      dataIndex: 'packageName',
      key: 'packageName',
    },
    {
      title: 'Buổi/tuần',
      dataIndex: 'sessionsPerWeek',
      key: 'sessionsPerWeek',
      render: (value?: number | null) => value ?? '-',
    },
    {
      title: 'Tổng số buổi',
      dataIndex: 'totalSessions',
      key: 'totalSessions',
    },
    {
      title: 'Số tháng dự kiến',
      dataIndex: 'expectedMonths',
      key: 'expectedMonths',
      render: (value?: number | null) => value ?? '-',
    },
    {
      title: 'Học phí',
      dataIndex: 'price',
      key: 'price',
      render: (price: number) => <MoneyText value={price} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, classPackage) => (
        <Popconfirm
          title="Ngừng áp dụng gói học phí?"
          description="Gói học phí sẽ không còn được chọn cho lớp này."
          okText="Ngừng áp dụng"
          cancelText="Hủy"
          onConfirm={() => handleDeactivatePackage(classPackage.tuitionPackageId)}
        >
          <Button type="link" danger loading={deactivateClassPackage.isPending}>
            Ngừng áp dụng
          </Button>
        </Popconfirm>
      ),
    },
  ]

  const infoTab = (
    <Descriptions column={1} bordered>
      <Descriptions.Item label="Mã lớp">{classroom.classCode}</Descriptions.Item>
      <Descriptions.Item label="Tên lớp">{classroom.className}</Descriptions.Item>
      <Descriptions.Item label="Trình độ">{classroom.level}</Descriptions.Item>
      <Descriptions.Item label="Giáo viên">{classroom.teacherName}</Descriptions.Item>
      <Descriptions.Item label="Phòng học">{classroom.room || '-'}</Descriptions.Item>
      <Descriptions.Item label="Ngày bắt đầu">
        {dayjs(classroom.startDate).format('DD/MM/YYYY')}
      </Descriptions.Item>
      <Descriptions.Item label="Ngày kết thúc dự kiến">
        {classroom.expectedEndDate ? dayjs(classroom.expectedEndDate).format('DD/MM/YYYY') : '-'}
      </Descriptions.Item>
      <Descriptions.Item label="Lịch học">
        {formatDaysOfWeek(classroom.daysOfWeek)}, {formatTime(classroom.startTime)} - {formatTime(classroom.endTime)}
      </Descriptions.Item>
      <Descriptions.Item label="Trạng thái">
        <StatusTag status={classroom.status} />
      </Descriptions.Item>
      <Descriptions.Item label="Ghi chú">{classroom.note || '-'}</Descriptions.Item>
    </Descriptions>
  )

  const packagesTab = (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
        <Text type="secondary">
          Chỉ các gói đang liên kết với lớp mới được chọn khi ghi danh học viên sau này.
        </Text>
        <Button type="primary" onClick={() => setAddPackageModalOpen(true)}>
          Thêm gói học phí
        </Button>
      </Space>

      <Table
        rowKey="id"
        columns={packageColumns}
        dataSource={classPackages}
        loading={classPackagesQuery.isLoading}
        pagination={false}
      />
    </Space>
  )

  const sessionColumns: ColumnsType<ClassSession> = [
    {
      title: 'Buổi',
      dataIndex: 'sessionNo',
      key: 'sessionNo',
    },
    {
      title: 'Ngày học',
      dataIndex: 'sessionDate',
      key: 'sessionDate',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
    },
    {
      title: 'Giờ học',
      key: 'time',
      render: (_, session) => `${formatTime(session.startTime)} - ${formatTime(session.endTime)}`,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <StatusTag status={status} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, session) => (
        <Space wrap>
          {session.status === 'CANCELED' ? (
            <Popconfirm
              title="Khôi phục buổi học?"
              description="Buổi học sẽ chuyển về trạng thái đã lên lịch và có thể điểm danh lại."
              okText="Khôi phục"
              cancelText="Đóng"
              onConfirm={() => handleRestoreSession(session.id)}
            >
              <Button type="link" loading={restoreClassSession.isPending}>
                Khôi phục
              </Button>
            </Popconfirm>
          ) : session.status === 'COMPLETED' ? (
            canMarkAttendance ? (
              <>
                <Button type="link" onClick={() => openAttendance(session.id)}>
                  Xem/Sửa điểm danh
                </Button>
                <Button
                  type="link"
                  danger
                  onClick={() => {
                    setCancelSessionMode('correction')
                    setCancelingSession(session)
                  }}
                >
                  Hoàn tác điểm danh & hủy buổi
                </Button>
              </>
            ) : null
          ) : canMarkAttendance ? (
            <>
              <Button type="link" onClick={() => openAttendance(session.id)}>
                Điểm danh
              </Button>
              <Button
                type="link"
                danger
                onClick={() => {
                  setCancelSessionMode('normal')
                  setCancelingSession(session)
                }}
              >
                Hủy buổi
              </Button>
            </>
          ) : null}
        </Space>
      ),
    },
  ]

  const sessionsTab = (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
        <Text type="secondary">Tạo lịch học từ lịch tuần của lớp và điểm danh từng buổi.</Text>
        <Button type="primary" onClick={() => setGenerateSessionsOpen(true)}>
          Tạo lịch học
        </Button>
      </Space>

      <Table
        rowKey="id"
        columns={sessionColumns}
        dataSource={sessionsQuery.data?.data ?? []}
        loading={sessionsQuery.isLoading}
        pagination={false}
      />
    </Space>
  )

  const attendanceTab = (
    <AttendanceMarkPanel
      sessions={sessionsQuery.data?.data ?? []}
      studentPackages={studentPackagesQuery.data ?? []}
      classroomStatus={classroom.status}
      loadingSessions={sessionsQuery.isLoading}
      selectedSessionId={attendanceSessionId}
      onSelectedSessionIdChange={setAttendanceSessionId}
      onRenewNow={() => setRenewalModalOpen(true)}
      isActive={activeTab === 'attendance'}
    />
  )

  function handleAddPackage(tuitionPackageId: number) {
    addClassPackage.mutate(
      { tuitionPackageId },
      {
        onSuccess: () => {
          message.success('Đã thêm gói học phí cho lớp')
          setAddPackageModalOpen(false)
        },
        onError: showErrorMessage,
      },
    )
  }

  function handleDeactivatePackage(tuitionPackageId: number) {
    deactivateClassPackage.mutate(tuitionPackageId, {
      onSuccess: () => {
        message.success('Đã ngừng áp dụng gói học phí cho lớp')
      },
      onError: showErrorMessage,
    })
  }

  function handleEnrollStudent(payload: EnrollStudentPayload) {
    enrollStudent.mutate(payload, {
      onSuccess: (enrollment) => {
        message.success('Đã ghi danh học viên')
        setEnrollModalOpen(false)
        Modal.success({
          title: 'Hóa đơn đầu tiên đã được tạo',
          content: (
            <Descriptions column={1} size="small">
              <Descriptions.Item label="Học viên">
                {formatStudentLabel(enrollment.studentCode, enrollment.studentName)}
              </Descriptions.Item>
              <Descriptions.Item label="Gói học phí">
                {enrollment.invoice?.packageNameSnapshot}
              </Descriptions.Item>
              <Descriptions.Item label="Phải đóng">
                <MoneyText value={enrollment.invoice?.finalAmount} />
              </Descriptions.Item>
              <Descriptions.Item label="Đã đóng">
                <MoneyText value={enrollment.invoice?.paidAmount} />
              </Descriptions.Item>
              <Descriptions.Item label="Còn lại">
                <MoneyText value={enrollment.invoice?.remainingAmount} />
              </Descriptions.Item>
            </Descriptions>
          ),
          okText: 'Đóng',
        })
      },
      onError: showErrorMessage,
    })
  }

  function handlePreviewChangePackage(payload: ChangePackagePreviewPayload) {
    previewChangePackage.mutate(payload, {
      onError: showErrorMessage,
    })
  }

  function handleChangePackage(payload: ChangePackagePayload) {
    changePackage.mutate(payload, {
      onSuccess: (result) => {
        message.success('Đã đổi gói học phí')
        Modal.success({
          title: 'Đổi gói thành công',
          content: (
            <Descriptions column={1} size="small">
              <Descriptions.Item label="Gói mới">
                {result.calculation.newPackageName}
              </Descriptions.Item>
              <Descriptions.Item label="Số tiền cần đóng">
                <MoneyText value={result.calculation.amountToPay} />
              </Descriptions.Item>
              <Descriptions.Item label="Hóa đơn mới">
                {result.newInvoice ? result.newInvoice.invoiceCode : 'Không tạo vì không còn phải thu'}
              </Descriptions.Item>
            </Descriptions>
          ),
          okText: 'Đóng',
        })
        closeChangePackageModal()
      },
      onError: showErrorMessage,
    })
  }

  function closeChangePackageModal() {
    setChangingPackage(undefined)
    previewChangePackage.reset()
    changePackage.reset()
  }

  function handleGenerateSessions(payload: GenerateClassSessionsPayload) {
    generateSessions.mutate(payload, {
      onSuccess: (result) => {
        message.success(`Đã tạo ${result.createdCount} buổi, bỏ qua ${result.skippedCount} buổi trùng`)
        setGenerateSessionsOpen(false)
      },
      onError: showErrorMessage,
    })
  }

  function handleRestoreSession(sessionId: number) {
    restoreClassSession.mutate(sessionId, {
      onSuccess: () => {
        message.success('Đã khôi phục buổi học')
      },
      onError: showErrorMessage,
    })
  }

  function handleCancelSession(payload: CancelClassSessionPayload) {
    if (!cancelingSession) {
      return
    }

    if (cancelSessionMode === 'correction') {
      correctionCancelClassSession.mutate(
        { id: cancelingSession.id, payload },
        {
          onSuccess: () => {
            message.success('Đã hoàn tác điểm danh và hủy buổi học')
            setCancelingSession(undefined)
          },
          onError: showErrorMessage,
        },
      )
      return
    }

    cancelClassSession.mutate(
      { id: cancelingSession.id, payload },
      {
        onSuccess: () => {
          message.success('Đã hủy buổi học')
          setCancelingSession(undefined)
        },
        onError: showErrorMessage,
      },
    )
  }

  function showErrorMessage(error: unknown) {
    if (isAxiosError(error)) {
      message.error(error.response?.data?.message ?? 'Có lỗi xảy ra')
      return
    }

    message.error('Có lỗi xảy ra')
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          {classroom.className}
        </Title>
        <Text type="secondary">Thông tin chi tiết lớp học và các phần sẽ được triển khai sau.</Text>
      </Space>

      {outOfSessionsCount > 0 || lowSessionsCount > 0 ? (
        <Alert
          type={outOfSessionsCount > 0 ? 'error' : 'warning'}
          showIcon
          message={`${outOfSessionsCount} học viên đã hết buổi, ${lowSessionsCount} học viên sắp hết buổi.`}
        />
      ) : null}

      <Space>
        {canEnroll ? (
          <Button type="primary" onClick={() => setEnrollModalOpen(true)}>
            Ghi danh học viên
          </Button>
        ) : (
          <Text type="secondary">Không thể ghi danh khi lớp đã kết thúc hoặc đã hủy.</Text>
        )}
        <Button onClick={() => setRenewalModalOpen(true)} disabled={classPackages.length === 0}>
          Gia hạn hàng loạt
        </Button>
      </Space>

      <Card>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'info',
              label: 'Thông tin lớp',
              children: infoTab,
            },
            {
              key: 'packages',
              label: 'Gói học phí',
              children: packagesTab,
            },
            {
              key: 'students',
              label: 'Học viên',
              children: (
                <Table
                  rowKey="id"
                  dataSource={activeEnrollments}
                  loading={enrollmentsQuery.isLoading || studentPackagesQuery.isLoading}
                  pagination={false}
                  columns={[
                    { title: 'Mã học viên', dataIndex: 'studentCode', key: 'studentCode' },
                    { title: 'Tên học viên', dataIndex: 'studentName', key: 'studentName' },
                    {
                      title: 'Gói gần nhất',
                      key: 'latestPackageName',
                      render: (_, enrollment) =>
                        progressByEnrollmentId.get(enrollment.id)?.latestPackageName
                        ?? enrollment.packageNameSnapshot,
                    },
                    {
                      title: 'Học phí',
                      key: 'latestPackagePrice',
                      render: (_, enrollment) => {
                        const progress = progressByEnrollmentId.get(enrollment.id)
                        const price = progress?.latestPackagePrice

                        return price != null ? <MoneyText value={price} /> : '-'
                      },
                    },
                    {
                      title: 'Tổng buổi',
                      key: 'totalSessions',
                      render: (_, enrollment) => progressByEnrollmentId.get(enrollment.id)?.totalSessions ?? '-',
                    },
                    {
                      title: 'Đã học',
                      key: 'usedSessions',
                      render: (_, enrollment) => progressByEnrollmentId.get(enrollment.id)?.usedSessions ?? '-',
                    },
                    {
                      title: 'Còn lại',
                      key: 'remainingSessions',
                      render: (_, enrollment) =>
                        progressByEnrollmentId.get(enrollment.id)?.remainingSessions ?? '-',
                    },
                    {
                      title: 'Buổi bù',
                      key: 'makeupAvailableSessions',
                      render: (_, enrollment) =>
                        progressByEnrollmentId.get(enrollment.id)?.makeupAvailableSessions ?? '-',
                    },
                    {
                      title: 'Ngày bắt đầu',
                      dataIndex: 'startDate',
                      key: 'startDate',
                      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
                    },
                    {
                      title: 'Thao tác',
                      key: 'actions',
                      render: (_, enrollment) => {
                        const progress = progressByEnrollmentId.get(enrollment.id)
                        const disabledReason = getChangePackageDisabledReason(progress)

                        return (
                          <Tooltip title={disabledReason}>
                            <span>
                              <Button
                                type="link"
                                disabled={Boolean(disabledReason)}
                                onClick={() => {
                                  if (progress) {
                                    setChangingPackage(progress)
                                  }
                                }}
                              >
                                Đổi gói
                              </Button>
                            </span>
                          </Tooltip>
                        )
                      },
                    },
                  ]}
                />
              ),
            },
            {
              key: 'sessions',
              label: 'Buổi học',
              children: sessionsTab,
            },
            {
              key: 'attendance',
              label: 'Điểm danh',
              children: attendanceTab,
            },
          ]}
        />
      </Card>

      <AddClassPackageModal
        open={addPackageModalOpen}
        tuitionPackages={tuitionPackagesQuery.data?.data ?? []}
        linkedTuitionPackageIds={classPackages.map((classPackage) => classPackage.tuitionPackageId)}
        loading={tuitionPackagesQuery.isLoading}
        submitting={addClassPackage.isPending}
        onCancel={() => setAddPackageModalOpen(false)}
        onSubmit={handleAddPackage}
      />

      <EnrollStudentModal
        open={enrollModalOpen}
        classroomId={classroom.id}
        classroomName={classroom.className}
        classroomStartDate={classroom.startDate}
        classroomDaysOfWeek={classroom.daysOfWeek}
        sessions={sessionsQuery.data?.data ?? []}
        students={eligibleStudentsQuery.data ?? []}
        classPackages={classPackages}
        loadingStudents={eligibleStudentsQuery.isLoading}
        loadingPackages={classPackagesQuery.isLoading}
        submitting={enrollStudent.isPending}
        onCancel={() => setEnrollModalOpen(false)}
        onSubmit={handleEnrollStudent}
      />

      <RenewAllPackagesModal
        open={renewalModalOpen}
        classroomId={classroom.id}
        classPackages={classPackages}
        onCancel={() => setRenewalModalOpen(false)}
        onSuccess={() => {
          void studentPackagesQuery.refetch()
          void queryClient.invalidateQueries({ queryKey: attendanceKeys.all })
          setRenewalModalOpen(false)
        }}
      />

      <GenerateSessionsModal
        open={generateSessionsOpen}
        classroomId={classroom.id}
        submitting={generateSessions.isPending}
        onCancel={() => setGenerateSessionsOpen(false)}
        onSubmit={handleGenerateSessions}
      />

      <CancelSessionModal
        open={Boolean(cancelingSession)}
        mode={cancelSessionMode}
        session={cancelingSession}
        submitting={cancelClassSession.isPending || correctionCancelClassSession.isPending}
        onCancel={() => setCancelingSession(undefined)}
        onSubmit={handleCancelSession}
      />

      <ChangePackageModal
        open={Boolean(changingPackage)}
        currentPackage={changingPackage}
        classPackages={classPackages}
        preview={previewChangePackage.data}
        loadingPackages={classPackagesQuery.isLoading}
        previewing={previewChangePackage.isPending}
        previewError={
          previewChangePackage.isError
            ? 'Không thể tính bù trừ. Vui lòng kiểm tra lại gói mới và thử lại.'
            : undefined
        }
        submitting={changePackage.isPending}
        onPreview={handlePreviewChangePackage}
        onSubmit={handleChangePackage}
        onCancel={closeChangePackageModal}
      />

    </Space>
  )
}

function formatTime(value: string) {
  return value.slice(0, 5)
}
