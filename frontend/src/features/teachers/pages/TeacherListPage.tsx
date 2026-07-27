import { Button, Form, Input, Modal, Select, Space, Table, Typography, message } from 'antd'
import { isAxiosError } from 'axios'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../api/apiResponse'

interface Teacher {
  id: number
  teacherCode: string
  fullName: string
  email?: string | null
  phone?: string | null
  status: 'ACTIVE' | 'INACTIVE'
  hasUserAccount: boolean
}

export function TeacherListPage() {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const teachersQuery = useQuery({
    queryKey: ['teachers'],
    queryFn: async () => {
      const response = await httpClient.get<ApiResponse<Teacher[]>>('/teachers', {
        params: { page: 0, size: 100 },
      })
      return response.data.data
    },
  })

  const createMutation = useMutation({
    mutationFn: async (values: {
      teacherCode: string
      fullName: string
      email?: string
      phone?: string
      status: 'ACTIVE' | 'INACTIVE'
    }) => {
      const response = await httpClient.post<ApiResponse<Teacher>>('/teachers', values)
      return response.data.data
    },
    onSuccess: () => {
      message.success('Đã tạo giáo viên')
      setOpen(false)
      form.resetFields()
      queryClient.invalidateQueries({ queryKey: ['teachers'] })
    },
    onError: (error) => {
      message.error(isAxiosError(error) ? error.response?.data?.message ?? 'Tạo thất bại' : 'Tạo thất bại')
    },
  })

  return (
    <div>
      <Space style={{ width: '100%', justifyContent: 'space-between', marginBottom: 16 }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          Giáo viên
        </Typography.Title>
        <Button type="primary" onClick={() => setOpen(true)}>
          Thêm giáo viên
        </Button>
      </Space>

      <Table
        rowKey="id"
        loading={teachersQuery.isLoading}
        dataSource={teachersQuery.data ?? []}
        columns={[
          {
            title: 'Mã',
            dataIndex: 'teacherCode',
            render: (code: string, teacher: Teacher) => (
              <Link to={`/teachers/${teacher.id}`}>{code}</Link>
            ),
          },
          {
            title: 'Họ tên',
            dataIndex: 'fullName',
            render: (name: string, teacher: Teacher) => (
              <Link to={`/teachers/${teacher.id}`}>{name}</Link>
            ),
          },
          { title: 'Email', dataIndex: 'email' },
          { title: 'Trạng thái', dataIndex: 'status' },
          {
            title: 'Tài khoản',
            dataIndex: 'hasUserAccount',
            render: (value: boolean) => (value ? 'Đã có' : 'Chưa có'),
          },
          {
            title: 'Thao tác',
            key: 'actions',
            render: (_: unknown, teacher: Teacher) => (
              <Link to={`/teachers/${teacher.id}`}>Chi tiết / Phân công lớp</Link>
            ),
          },
        ]}
        pagination={false}
      />

      <Modal
        title="Thêm giáo viên"
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={createMutation.isPending}
        destroyOnHidden
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ status: 'ACTIVE' }}
          onFinish={(values) => createMutation.mutate(values)}
        >
          <Form.Item name="teacherCode" label="Mã giáo viên" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="fullName" label="Họ tên" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="email" label="Email">
            <Input />
          </Form.Item>
          <Form.Item name="phone" label="Số điện thoại">
            <Input />
          </Form.Item>
          <Form.Item name="status" label="Trạng thái" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'ACTIVE', label: 'ACTIVE' },
                { value: 'INACTIVE', label: 'INACTIVE' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
