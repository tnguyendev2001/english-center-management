import { Button, Card, Input, message, Space, Table, Typography } from 'antd'
import type { TablePaginationConfig } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { StatusTag } from '../../../components/common/StatusTag'
import { ClassroomFormModal } from '../components/ClassroomFormModal'
import { useClassrooms, useCreateClassroom, useUpdateClassroom } from '../classroomQueries'
import type { Classroom, ClassroomPayload, ClassroomSearchParams } from '../classroomTypes'
import { formatDaysOfWeek } from '../classroomTypes'

const { Title, Text } = Typography

export function ClassroomListPage() {
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingClassroom, setEditingClassroom] = useState<Classroom>()

  const params: ClassroomSearchParams = useMemo(
    () => ({
      keyword: keyword || undefined,
      page,
      size,
    }),
    [keyword, page, size],
  )

  const classroomsQuery = useClassrooms(params)
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
      dataIndex: 'teacherName',
      key: 'teacherName',
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
        <Button type="link" onClick={() => openEditModal(classroom)}>
          Sửa
        </Button>
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
        <Text type="secondary">Quản lý lớp tiếng Anh, giáo viên, phòng học và lịch học.</Text>
      </Space>

      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
            <Input.Search
              allowClear
              placeholder="Tìm theo mã lớp, tên lớp, giáo viên hoặc phòng"
              style={{ width: 420 }}
              onSearch={handleSearch}
            />
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
