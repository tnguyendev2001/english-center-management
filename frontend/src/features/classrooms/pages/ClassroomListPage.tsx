import { Button, Card, Input, message, Select, Space, Table, Tag, Typography } from 'antd'
import type { TablePaginationConfig } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { StatusTag } from '../../../components/common/StatusTag'
import { ClassroomFormModal } from '../components/ClassroomFormModal'
import {
  useActiveTeachers,
  useClassrooms,
  useCreateClassroom,
  useUpdateClassroom,
} from '../classroomQueries'
import type { Classroom, ClassroomPayload, ClassroomSearchParams } from '../classroomTypes'
import { formatDaysOfWeek } from '../classroomTypes'

const { Title, Text } = Typography

type AssignmentFilter = 'ALL' | 'ASSIGNED' | 'UNASSIGNED'

export function ClassroomListPage() {
  const [keyword, setKeyword] = useState('')
  const [teacherId, setTeacherId] = useState<number>()
  const [assignmentFilter, setAssignmentFilter] = useState<AssignmentFilter>('ALL')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingClassroom, setEditingClassroom] = useState<Classroom>()

  const params: ClassroomSearchParams = useMemo(
    () => ({
      keyword: keyword || undefined,
      teacherId: assignmentFilter === 'UNASSIGNED' ? undefined : teacherId,
      unassignedOnly: assignmentFilter === 'UNASSIGNED' ? true : undefined,
      assignedOnly: assignmentFilter === 'ASSIGNED' ? true : undefined,
      page,
      size,
    }),
    [assignmentFilter, keyword, page, size, teacherId],
  )

  const classroomsQuery = useClassrooms(params)
  const teachersQuery = useActiveTeachers()
  const createClassroom = useCreateClassroom()
  const updateClassroom = useUpdateClassroom()

  const columns: ColumnsType<Classroom> = [
    {
      title: 'Mã lớp',
      dataIndex: 'classCode',
      key: 'classCode',
      render: (classCode: string, classroom) => (
        <Link to={`/classrooms/${classroom.id}`}>{classCode}</Link>
      ),
    },
    {
      title: 'Tên lớp',
      dataIndex: 'className',
      key: 'className',
    },
    {
      title: 'Trình độ',
      dataIndex: 'level',
      key: 'level',
    },
    {
      title: 'Giáo viên',
      key: 'teacher',
      render: (_, classroom) => {
        if (!classroom.teacherAssigned || !classroom.teacherId) {
          return <Tag color="warning">Chưa phân công</Tag>
        }

        return (
          <Space size={4} wrap>
            <span>{classroom.teacherName}</span>
            {classroom.teacherStatus === 'INACTIVE' ? <Tag>Ngừng</Tag> : null}
          </Space>
        )
      },
    },
    {
      title: 'Phòng',
      dataIndex: 'room',
      key: 'room',
      render: (value?: string | null) => value || '-',
    },
    {
      title: 'Lịch học',
      key: 'schedule',
      render: (_, classroom) =>
        `${formatDaysOfWeek(classroom.daysOfWeek)}, ${formatTime(classroom.startTime)} - ${formatTime(classroom.endTime)}`,
    },
    {
      title: 'Ngày bắt đầu',
      dataIndex: 'startDate',
      key: 'startDate',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
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
      render: (_, classroom) => (
        <Space size={0}>
          <Button type="link" onClick={() => openEditModal(classroom)}>
            {classroom.teacherAssigned ? 'Đổi giáo viên' : 'Phân công giáo viên'}
          </Button>
          <Button type="link" onClick={() => openEditModal(classroom)}>
            Sửa
          </Button>
        </Space>
      ),
    },
  ]

  function openCreateModal() {
    setEditingClassroom(undefined)
    setModalOpen(true)
  }

  function openEditModal(classroom: Classroom) {
    setEditingClassroom(classroom)
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setEditingClassroom(undefined)
  }

  function handleSearch(value: string) {
    setKeyword(value.trim())
    setPage(0)
  }

  function handleTableChange(pagination: TablePaginationConfig) {
    setPage((pagination.current ?? 1) - 1)
    setSize(pagination.pageSize ?? 10)
  }

  function handleSubmit(payload: ClassroomPayload) {
    if (editingClassroom) {
      updateClassroom.mutate(
        { id: editingClassroom.id, payload },
        {
          onSuccess: () => {
            message.success('Đã cập nhật lớp học')
            closeModal()
          },
          onError: showErrorMessage,
        },
      )
      return
    }

    createClassroom.mutate(payload, {
      onSuccess: () => {
        message.success('Đã thêm lớp học')
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

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Lớp học
        </Title>
        <Text type="secondary">Quản lý lớp tiếng Anh, phân công giáo viên, phòng học và lịch học.</Text>
      </Space>

      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
            <Space wrap>
              <Input.Search
                allowClear
                placeholder="Tìm theo mã lớp, tên lớp, giáo viên hoặc phòng"
                style={{ width: 360 }}
                onSearch={handleSearch}
              />
              <Select
                allowClear
                placeholder="Lọc theo giáo viên"
                style={{ width: 240 }}
                loading={teachersQuery.isLoading}
                disabled={assignmentFilter === 'UNASSIGNED'}
                value={teacherId}
                onChange={(value) => {
                  setTeacherId(value)
                  setPage(0)
                }}
                options={(teachersQuery.data ?? []).map((teacher) => ({
                  value: teacher.id,
                  label: `${teacher.fullName} (${teacher.teacherCode})`,
                }))}
              />
              <Select
                style={{ width: 200 }}
                value={assignmentFilter}
                onChange={(value: AssignmentFilter) => {
                  setAssignmentFilter(value)
                  if (value === 'UNASSIGNED') {
                    setTeacherId(undefined)
                  }
                  setPage(0)
                }}
                options={[
                  { value: 'ALL', label: 'Tất cả phân công' },
                  { value: 'ASSIGNED', label: 'Đã phân công' },
                  { value: 'UNASSIGNED', label: 'Chưa phân công' },
                ]}
              />
            </Space>
            <Button type="primary" onClick={openCreateModal}>
              Thêm lớp học
            </Button>
          </Space>

          <Table
            rowKey="id"
            columns={columns}
            dataSource={classroomsQuery.data?.data ?? []}
            loading={classroomsQuery.isLoading}
            pagination={{
              current: (classroomsQuery.data?.meta?.page ?? page) + 1,
              pageSize: classroomsQuery.data?.meta?.size ?? size,
              total: classroomsQuery.data?.meta?.totalElements ?? 0,
              showSizeChanger: true,
            }}
            onChange={handleTableChange}
          />
        </Space>
      </Card>

      <ClassroomFormModal
        open={modalOpen}
        initialClassroom={editingClassroom}
        submitting={createClassroom.isPending || updateClassroom.isPending}
        onCancel={closeModal}
        onSubmit={handleSubmit}
      />
    </Space>
  )
}

function formatTime(value: string) {
  return value.slice(0, 5)
}
