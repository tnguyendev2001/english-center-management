import {
  Alert,
  Button,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { isAxiosError } from 'axios'
import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../api/apiResponse'
import {
  createUser,
  disableUser,
  enableUser,
  resetUserPassword,
  searchUsers,
} from '../../auth/authApi'
import type { AccountRole, AccountStatus, CreateUserResponse, UserAccount } from '../../auth/authTypes'

interface StudentOption {
  id: number
  studentCode: string
  fullName: string
}

interface TeacherOption {
  id: number
  teacherCode: string
  fullName: string
  email?: string | null
}

export function UserManagementPage() {
  const queryClient = useQueryClient()
  const [username, setUsername] = useState('')
  const [role, setRole] = useState<AccountRole | undefined>()
  const [status, setStatus] = useState<AccountStatus | undefined>()
  const [createOpen, setCreateOpen] = useState(false)
  const [temporaryPassword, setTemporaryPassword] = useState<string | null>(null)
  const [form] = Form.useForm()
  const selectedRole = Form.useWatch('role', form) as AccountRole | undefined

  const usersQuery = useQuery({
    queryKey: ['admin-users', username, role, status],
    queryFn: () => searchUsers({ username: username || undefined, role, status, page: 0, size: 100 }),
  })

  const studentsQuery = useQuery({
    queryKey: ['students-without-account'],
    queryFn: async () => {
      const response = await httpClient.get<ApiResponse<StudentOption[]>>('/students/without-account')
      return response.data.data
    },
    enabled: createOpen && selectedRole === 'STUDENT',
  })

  const teachersQuery = useQuery({
    queryKey: ['teachers-without-account'],
    queryFn: async () => {
      const response = await httpClient.get<ApiResponse<TeacherOption[]>>('/teachers/without-account')
      return response.data.data
    },
    enabled: createOpen && selectedRole === 'TEACHER',
  })

  const createMutation = useMutation({
    mutationFn: createUser,
    onSuccess: (result: CreateUserResponse) => {
      setTemporaryPassword(result.temporaryPassword)
      setCreateOpen(false)
      form.resetFields()
      queryClient.invalidateQueries({ queryKey: ['admin-users'] })
    },
    onError: (error) => {
      message.error(isAxiosError(error) ? error.response?.data?.message ?? 'Tạo tài khoản thất bại' : 'Tạo tài khoản thất bại')
    },
  })

  const columns = useMemo(
    () => [
      { title: 'Tên đăng nhập', dataIndex: 'username' },
      {
        title: 'Vai trò',
        dataIndex: 'role',
        render: (value: AccountRole) => <Tag>{value}</Tag>,
      },
      {
        title: 'Hồ sơ liên kết',
        render: (_: unknown, record: UserAccount) => {
          if (record.role === 'STUDENT') {
            return record.studentName
              ? `${record.studentCode ?? ''} - ${record.studentName}`
              : '-'
          }
          if (record.role === 'TEACHER') {
            return record.teacherName
              ? `${record.teacherCode ?? ''} - ${record.teacherName}`
              : '-'
          }
          return '-'
        },
      },
      {
        title: 'Trạng thái',
        dataIndex: 'status',
        render: (value: AccountStatus) => (
          <Tag color={value === 'ACTIVE' ? 'green' : value === 'DISABLED' ? 'red' : 'orange'}>
            {value}
          </Tag>
        ),
      },
      {
        title: 'Đăng nhập gần nhất',
        dataIndex: 'lastLoginAt',
        render: (value?: string | null) => value ?? '-',
      },
      {
        title: 'Bắt buộc đổi mật khẩu',
        dataIndex: 'mustChangePassword',
        render: (value: boolean) => (value ? 'Có' : 'Không'),
      },
      {
        title: 'Thao tác',
        render: (_: unknown, record: UserAccount) => (
          <Space wrap>
            {record.status === 'DISABLED' ? (
              <Button
                size="small"
                onClick={async () => {
                  await enableUser(record.id)
                  message.success('Đã kích hoạt tài khoản')
                  queryClient.invalidateQueries({ queryKey: ['admin-users'] })
                }}
              >
                Kích hoạt
              </Button>
            ) : (
              <Button
                size="small"
                danger
                onClick={async () => {
                  await disableUser(record.id)
                  message.success('Đã vô hiệu hóa tài khoản')
                  queryClient.invalidateQueries({ queryKey: ['admin-users'] })
                }}
              >
                Vô hiệu hóa
              </Button>
            )}
            <Button
              size="small"
              onClick={async () => {
                const result = await resetUserPassword(record.id)
                setTemporaryPassword(result.temporaryPassword)
                queryClient.invalidateQueries({ queryKey: ['admin-users'] })
              }}
            >
              Đặt lại mật khẩu
            </Button>
          </Space>
        ),
      },
    ],
    [queryClient],
  )

  return (
    <div>
      <Space style={{ width: '100%', justifyContent: 'space-between', marginBottom: 16 }} wrap>
        <Typography.Title level={3} style={{ margin: 0 }}>
          Quản lý tài khoản
        </Typography.Title>
        <Button type="primary" onClick={() => setCreateOpen(true)}>
          Tạo tài khoản
        </Button>
      </Space>

      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          placeholder="Tìm tên đăng nhập"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          style={{ width: 220 }}
          allowClear
        />
        <Select
          allowClear
          placeholder="Vai trò"
          style={{ width: 160 }}
          value={role}
          onChange={setRole}
          options={[
            { value: 'ADMIN', label: 'ADMIN' },
            { value: 'TEACHER', label: 'TEACHER' },
            { value: 'STUDENT', label: 'STUDENT' },
          ]}
        />
        <Select
          allowClear
          placeholder="Trạng thái"
          style={{ width: 160 }}
          value={status}
          onChange={setStatus}
          options={[
            { value: 'ACTIVE', label: 'ACTIVE' },
            { value: 'DISABLED', label: 'DISABLED' },
            { value: 'LOCKED', label: 'LOCKED' },
          ]}
        />
      </Space>

      <Table
        rowKey="id"
        loading={usersQuery.isLoading}
        dataSource={usersQuery.data?.items ?? []}
        columns={columns}
        pagination={false}
      />

      <Modal
        title="Tạo tài khoản"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={createMutation.isPending}
        destroyOnHidden
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ role: 'STUDENT' }}
          onFinish={(values) => {
            createMutation.mutate({
              username: values.username,
              role: values.role,
              studentId: values.role === 'STUDENT' ? values.studentId : null,
              teacherId: values.role === 'TEACHER' ? values.teacherId : null,
            })
          }}
        >
          <Form.Item name="role" label="Vai trò" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'ADMIN', label: 'ADMIN' },
                { value: 'TEACHER', label: 'TEACHER' },
                { value: 'STUDENT', label: 'STUDENT' },
              ]}
            />
          </Form.Item>

          {selectedRole === 'STUDENT' ? (
            <Form.Item name="studentId" label="Học viên" rules={[{ required: true, message: 'Chọn học viên' }]}>
              <Select
                showSearch
                optionFilterProp="label"
                options={(studentsQuery.data ?? []).map((student) => ({
                  value: student.id,
                  label: `${student.studentCode} - ${student.fullName}`,
                }))}
                onChange={(studentId) => {
                  const student = studentsQuery.data?.find((item) => item.id === studentId)
                  if (student) {
                    form.setFieldValue('username', student.studentCode)
                  }
                }}
              />
            </Form.Item>
          ) : null}

          {selectedRole === 'TEACHER' ? (
            <Form.Item name="teacherId" label="Giáo viên" rules={[{ required: true, message: 'Chọn giáo viên' }]}>
              <Select
                showSearch
                optionFilterProp="label"
                options={(teachersQuery.data ?? []).map((teacher) => ({
                  value: teacher.id,
                  label: `${teacher.teacherCode} - ${teacher.fullName}`,
                }))}
                onChange={(teacherId) => {
                  const teacher = teachersQuery.data?.find((item) => item.id === teacherId)
                  if (teacher) {
                    form.setFieldValue('username', teacher.email || teacher.teacherCode)
                  }
                }}
              />
            </Form.Item>
          ) : null}

          <Form.Item name="username" label="Tên đăng nhập" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="Mật khẩu tạm thời"
        open={Boolean(temporaryPassword)}
        onCancel={() => setTemporaryPassword(null)}
        footer={[
          <Button
            key="copy"
            type="primary"
            onClick={async () => {
              if (temporaryPassword) {
                await navigator.clipboard.writeText(temporaryPassword)
                message.success('Đã sao chép mật khẩu')
              }
            }}
          >
            Sao chép
          </Button>,
          <Button key="close" onClick={() => setTemporaryPassword(null)}>
            Đóng
          </Button>,
        ]}
      >
        <Alert
          type="warning"
          showIcon
          message="Mật khẩu này chỉ hiển thị một lần. Hãy sao chép và gửi cho người dùng."
          style={{ marginBottom: 16 }}
        />
        <Typography.Paragraph copyable strong style={{ fontSize: 18 }}>
          {temporaryPassword}
        </Typography.Paragraph>
      </Modal>
    </div>
  )
}
