import { Button, Card, Input, message, Select, Space, Table, Tag, Typography } from 'antd'
import type { TablePaginationConfig } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ActiveFilterTags } from '../../../components/common/ActiveFilterTags'
import { StatusTag } from '../../../components/common/StatusTag'
import { studentCodeColumn, studentNameColumn } from '../../../components/common/studentDisplay'
import { useUrlEnumParam } from '../../../hooks/useUrlEnumParam'
import { useSessionWarnings } from '../../dashboard/dashboardQueries'
import type { SessionWarning } from '../../dashboard/dashboardTypes'
import { StudentFormModal } from '../components/StudentFormModal'
import { useCreateStudent, useStudents, useUpdateStudent } from '../studentQueries'
import type { Student, StudentPayload, StudentSearchParams } from '../studentTypes'

const { Title, Text } = Typography

const REMAINING_OPTIONS = ['ZERO', 'LOW', 'AVAILABLE'] as const
type RemainingFilter = (typeof REMAINING_OPTIONS)[number]

const REMAINING_LABELS: Record<RemainingFilter, string> = {
  ZERO: 'Đã hết buổi',
  LOW: 'Sắp hết buổi',
  AVAILABLE: 'Còn buổi',
}

export function StudentListPage() {
  const navigate = useNavigate()
  const remainingParam = useUrlEnumParam('remaining', REMAINING_OPTIONS)
  const remainingFilter = remainingParam.value

  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingStudent, setEditingStudent] = useState<Student>()

  const params: StudentSearchParams = useMemo(
    () => ({
      keyword: keyword || undefined,
      page,
      size,
    }),
    [keyword, page, size],
  )

  const progressMode = Boolean(remainingFilter)
  const studentsQuery = useStudents(params, !progressMode)
  const warningsQuery = useSessionWarnings(
    { remainingThreshold: 2, remaining: remainingFilter },
    progressMode,
  )
  const createStudent = useCreateStudent()
  const updateStudent = useUpdateStudent()

  const columns: ColumnsType<Student> = [
    {
      title: 'Mã học viên',
      dataIndex: 'studentCode',
      key: 'studentCode',
      render: (studentCode: string, student) => (
        <Link to={`/students/${student.id}`}>{studentCode}</Link>
      ),
    },
    {
      title: 'Họ tên',
      dataIndex: 'fullName',
      key: 'fullName',
    },
    {
      title: 'Số điện thoại',
      dataIndex: 'phone',
      key: 'phone',
      render: (value?: string | null) => value || '-',
    },
    {
      title: 'Phụ huynh',
      dataIndex: 'parentName',
      key: 'parentName',
      render: (value?: string | null) => value || '-',
    },
    {
      title: 'SĐT phụ huynh',
      dataIndex: 'parentPhone',
      key: 'parentPhone',
      render: (value?: string | null) => value || '-',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <StatusTag status={status} />,
    },
    {
      title: 'Cập nhật',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY HH:mm'),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, student) => (
        <Button type="link" onClick={() => openEditModal(student)}>
          Sửa
        </Button>
      ),
    },
  ]

  const progressColumns: ColumnsType<SessionWarning> = [
    studentCodeColumn(),
    studentNameColumn(),
    { title: 'Lớp học', dataIndex: 'classroomName' },
    {
      title: 'Trạng thái ghi danh',
      key: 'status',
      render: () => <StatusTag status="ACTIVE" />,
    },
    { title: 'Tổng buổi', dataIndex: 'totalSessions' },
    { title: 'Đã dùng', dataIndex: 'usedSessions' },
    { title: 'Còn lại', dataIndex: 'remainingSessions' },
    {
      title: 'Cảnh báo',
      dataIndex: 'warningMessage',
      render: (value: string, record) => (
        <Tag color={record.remainingSessions <= 0 ? 'red' : record.remainingSessions <= 2 ? 'orange' : 'green'}>
          {value}
        </Tag>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, record) => (
        <Space size="small" wrap>
          <Button type="link" onClick={() => navigate(`/students/${record.studentId}`)}>
            Xem học viên
          </Button>
          <Button type="link" onClick={() => navigate(`/classrooms/${record.classroomId}`)}>
            Xem ghi danh / Gia hạn
          </Button>
        </Space>
      ),
    },
  ]

  function openCreateModal() {
    setEditingStudent(undefined)
    setModalOpen(true)
  }

  function openEditModal(student: Student) {
    setEditingStudent(student)
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setEditingStudent(undefined)
  }

  function handleSearch(value: string) {
    setKeyword(value.trim())
    setPage(0)
  }

  function handleTableChange(pagination: TablePaginationConfig) {
    setPage((pagination.current ?? 1) - 1)
    setSize(pagination.pageSize ?? 10)
  }

  function handleRemainingChange(value: RemainingFilter | undefined) {
    remainingParam.setValue(value)
    setPage(0)
  }

  function handleSubmit(payload: StudentPayload) {
    if (editingStudent) {
      updateStudent.mutate(
        { id: editingStudent.id, payload },
        {
          onSuccess: () => {
            message.success('Đã cập nhật học viên')
            closeModal()
          },
          onError: showErrorMessage,
        },
      )
      return
    }

    createStudent.mutate(payload, {
      onSuccess: (student) => {
        message.success(`Tạo học viên thành công. Mã học viên: ${student.studentCode}`)
        closeModal()
      },
      onError: showErrorMessage,
    })
  }

  function showErrorMessage(error: unknown) {
    if (isAxiosError(error)) {
      message.error(error.response?.data?.message ?? 'Có lỗi xảy ra')
      return
    }

    message.error('Có lỗi xảy ra')
  }

  const subtitle = remainingFilter
    ? `Học viên – ${REMAINING_LABELS[remainingFilter]}`
    : 'Quản lý thông tin học viên của trung tâm.'

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Học viên
        </Title>
        <Text type="secondary">{subtitle}</Text>
      </Space>

      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
            <Space wrap>
              <Input.Search
                allowClear
                placeholder="Tìm theo mã, họ tên hoặc số điện thoại"
                style={{ width: 320 }}
                onSearch={handleSearch}
                disabled={progressMode}
              />
              <Select
                allowClear
                placeholder="Tình trạng số buổi"
                style={{ width: 200 }}
                value={remainingFilter}
                onChange={(value) => handleRemainingChange(value)}
                options={[
                  { value: 'ZERO', label: 'Đã hết buổi' },
                  { value: 'LOW', label: 'Sắp hết buổi' },
                  { value: 'AVAILABLE', label: 'Còn buổi' },
                ]}
              />
            </Space>
            {!progressMode ? (
              <Button type="primary" onClick={openCreateModal}>
                Thêm học viên
              </Button>
            ) : null}
          </Space>

          <ActiveFilterTags
            tags={
              remainingFilter
                ? [
                    {
                      key: 'remaining',
                      label: REMAINING_LABELS[remainingFilter],
                      color: remainingFilter === 'ZERO' ? 'red' : remainingFilter === 'LOW' ? 'orange' : 'green',
                      onClose: () => handleRemainingChange(undefined),
                    },
                  ]
                : []
            }
            onClearAll={() => handleRemainingChange(undefined)}
          />

          {progressMode ? (
            <Table
              rowKey="enrollmentId"
              columns={progressColumns}
              dataSource={warningsQuery.data ?? []}
              loading={warningsQuery.isLoading}
              pagination={{ pageSize: 10, showSizeChanger: true }}
              locale={{ emptyText: 'Không có học viên khớp bộ lọc.' }}
            />
          ) : (
            <Table
              rowKey="id"
              columns={columns}
              dataSource={studentsQuery.data?.data ?? []}
              loading={studentsQuery.isLoading}
              pagination={{
                current: (studentsQuery.data?.meta?.page ?? page) + 1,
                pageSize: studentsQuery.data?.meta?.size ?? size,
                total: studentsQuery.data?.meta?.totalElements ?? 0,
                showSizeChanger: true,
              }}
              onChange={handleTableChange}
            />
          )}
        </Space>
      </Card>

      <StudentFormModal
        open={modalOpen}
        initialStudent={editingStudent}
        submitting={createStudent.isPending || updateStudent.isPending}
        onCancel={closeModal}
        onSubmit={handleSubmit}
      />
    </Space>
  )
}
