import {
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
  Typography,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { AddClassPackageModal } from '../../classPackages/components/AddClassPackageModal'
import {
  useAddClassPackage,
  useClassPackages,
  useDeactivateClassPackage,
} from '../../classPackages/classPackageQueries'
import type { ClassPackage } from '../../classPackages/classPackageTypes'
import { AttendanceMarkPanel } from '../../attendance/components/AttendanceMarkPanel'
import { CancelSessionModal } from '../../classSessions/components/CancelSessionModal'
import { GenerateSessionsModal } from '../../classSessions/components/GenerateSessionsModal'
import {
  useCancelClassSession,
  useClassSessions,
  useGenerateClassSessions,
} from '../../classSessions/classSessionQueries'
import type {
  CancelClassSessionPayload,
  ClassSession,
  GenerateClassSessionsPayload,
} from '../../classSessions/classSessionTypes'
import { EnrollStudentModal } from '../../enrollments/components/EnrollStudentModal'
import { useEnrollments, useEnrollStudent } from '../../enrollments/enrollmentQueries'
import type { EnrollStudentPayload } from '../../enrollments/enrollmentTypes'
import { useStudents } from '../../students/studentQueries'
import type { StudentSearchParams } from '../../students/studentTypes'
import { useTuitionPackages } from '../../tuitionPackages/tuitionPackageQueries'
import type { TuitionPackageSearchParams } from '../../tuitionPackages/tuitionPackageTypes'
import { useClassroomDetail } from '../classroomQueries'
import { formatDaysOfWeek } from '../classroomTypes'

const { Title, Text } = Typography

export function ClassroomDetailPage() {
  const { id } = useParams()
  const classroomId = Number(id)
  const [addPackageModalOpen, setAddPackageModalOpen] = useState(false)
  const [enrollModalOpen, setEnrollModalOpen] = useState(false)
  const [generateSessionsOpen, setGenerateSessionsOpen] = useState(false)
  const [cancelingSession, setCancelingSession] = useState<ClassSession>()
  const tuitionPackageParams: TuitionPackageSearchParams = useMemo(
    () => ({
      page: 0,
      size: 100,
    }),
    [],
  )
  const studentParams: StudentSearchParams = useMemo(
    () => ({
      page: 0,
      size: 100,
    }),
    [],
  )
  const classroomQuery = useClassroomDetail(classroomId)
  const classPackagesQuery = useClassPackages(classroomId)
  const tuitionPackagesQuery = useTuitionPackages(tuitionPackageParams)
  const studentsQuery = useStudents(studentParams)
  const sessionsQuery = useClassSessions({ classroomId, page: 0, size: 100 })
  const enrollmentsQuery = useEnrollments({ page: 0, size: 100 })
  const addClassPackage = useAddClassPackage(classroomId)
  const deactivateClassPackage = useDeactivateClassPackage(classroomId)
  const enrollStudent = useEnrollStudent()
  const generateSessions = useGenerateClassSessions()
  const cancelClassSession = useCancelClassSession()

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
  const activeEnrollments = (enrollmentsQuery.data?.data ?? []).filter(
    (enrollment) => enrollment.classroomId === classroomId && enrollment.status === 'ACTIVE',
  )

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
        <Space>
          <Button
            type="link"
            danger
            disabled={session.status === 'CANCELED'}
            onClick={() => setCancelingSession(session)}
          >
            Hủy buổi
          </Button>
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
      enrollments={activeEnrollments}
      loadingSessions={sessionsQuery.isLoading}
      loadingEnrollments={enrollmentsQuery.isLoading}
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
              <Descriptions.Item label="Học viên">{enrollment.studentName}</Descriptions.Item>
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

  function handleGenerateSessions(payload: GenerateClassSessionsPayload) {
    generateSessions.mutate(payload, {
      onSuccess: () => {
        message.success('Đã tạo lịch học')
        setGenerateSessionsOpen(false)
      },
      onError: showErrorMessage,
    })
  }

  function handleCancelSession(payload: CancelClassSessionPayload) {
    if (!cancelingSession) {
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

      <Space>
        <Button type="primary" onClick={() => setEnrollModalOpen(true)}>
          Ghi danh học viên
        </Button>
      </Space>

      <Card>
        <Tabs
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
                  loading={enrollmentsQuery.isLoading}
                  pagination={false}
                  columns={[
                    { title: 'Học viên', dataIndex: 'studentName', key: 'studentName' },
                    { title: 'Gói học phí', dataIndex: 'packageNameSnapshot', key: 'packageNameSnapshot' },
                    {
                      title: 'Ngày bắt đầu',
                      dataIndex: 'startDate',
                      key: 'startDate',
                      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
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
        students={studentsQuery.data?.data ?? []}
        classPackages={classPackages}
        loadingStudents={studentsQuery.isLoading}
        loadingPackages={classPackagesQuery.isLoading}
        submitting={enrollStudent.isPending}
        onCancel={() => setEnrollModalOpen(false)}
        onSubmit={handleEnrollStudent}
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
        session={cancelingSession}
        submitting={cancelClassSession.isPending}
        onCancel={() => setCancelingSession(undefined)}
        onSubmit={handleCancelSession}
      />

    </Space>
  )
}

function formatTime(value: string) {
  return value.slice(0, 5)
}
