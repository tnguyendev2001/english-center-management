import { Button, Card, Input, message, Space, Table, Typography } from 'antd'
import type { TablePaginationConfig } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { StatusTag } from '../../../components/common/StatusTag'
import { StudentFormModal } from '../components/StudentFormModal'
import { useCreateStudent, useStudents, useUpdateStudent } from '../studentQueries'
import type { Student, StudentPayload, StudentSearchParams } from '../studentTypes'

const { Title, Text } = Typography

export function StudentListPage() {
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

  const studentsQuery = useStudents(params)
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
      onSuccess: () => {
        message.success('Đã thêm học viên')
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
          Học viên
        </Title>
        <Text type="secondary">Quản lý thông tin học viên của trung tâm.</Text>
      </Space>

      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
            <Input.Search
              allowClear
              placeholder="Tìm theo mã, họ tên hoặc số điện thoại"
              style={{ width: 360 }}
              onSearch={handleSearch}
            />
            <Button type="primary" onClick={openCreateModal}>
              Thêm học viên
            </Button>
          </Space>

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
