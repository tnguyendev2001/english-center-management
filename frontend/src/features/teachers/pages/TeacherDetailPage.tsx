import {
  Button,
  Card,
  Descriptions,
  Drawer,
  Empty,
  Form,
  List,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { isAxiosError } from 'axios'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../api/apiResponse'
import { StatusTag } from '../../../components/common/StatusTag'
import {
  useAssignClassroomTeacher,
  useClassrooms,
  useUnassignedClassrooms,
} from '../../classrooms/classroomQueries'
import type { Classroom } from '../../classrooms/classroomTypes'
import { formatDaysOfWeek } from '../../classrooms/classroomTypes'

const { Title, Text } = Typography

interface Teacher {
  id: number
  teacherCode: string
  fullName: string
  email?: string | null
  phone?: string | null
  status: 'ACTIVE' | 'INACTIVE'
  hasUserAccount: boolean
  note?: string | null
}

interface UserAccount {
  id: number
  username: string
  role: string
  status: string
  teacherId?: number | null
}

export function TeacherDetailPage() {
  const { id } = useParams()
  const teacherId = Number(id)
  const [assignOpen, setAssignOpen] = useState(false)
  const [form] = Form.useForm<{ classroomId: number }>()
  const assignTeacher = useAssignClassroomTeacher()

  const teacherQuery = useQuery({
    queryKey: ['teachers', teacherId],
    queryFn: async () => {
      const response = await httpClient.get<ApiResponse<Teacher>>(`/teachers/${teacherId}`)
      return response.data.data
    },
    enabled: Number.isFinite(teacherId),
  })

  const classroomsQuery = useClassrooms({
    teacherId: Number.isFinite(teacherId) ? teacherId : undefined,
    page: 0,
    size: 100,
  })

  const unassignedQuery = useUnassignedClassrooms(assignOpen)

  const usersQuery = useQuery({
    queryKey: ['admin-users', 'by-teacher', teacherId],
    queryFn: async () => {
      const response = await httpClient.get<ApiResponse<UserAccount[]>>('/admin/users', {
        params: { page: 0, size: 100 },
      })
      return response.data.data
    },
    enabled: Number.isFinite(teacherId),
  })

  const linkedAccount = useMemo(
    () => (usersQuery.data ?? []).find((account) => account.teacherId === teacherId),
    [teacherId, usersQuery.data],
  )

  const classrooms = classroomsQuery.data?.data ?? []
  const activeClassrooms = classrooms.filter(
    (classroom) => classroom.status === 'PLANNED' || classroom.status === 'ONGOING',
  )
  const activeStudents = classrooms.reduce(
    (sum, classroom) => sum + (classroom.activeStudentCount ?? 0),
    0,
  )

  function handleAssign(values: { classroomId: number }) {
    assignTeacher.mutate(
      { id: values.classroomId, teacherId },
      {
        onSuccess: () => {
          message.success('Đã phân công lớp cho giáo viên')
          setAssignOpen(false)
          form.resetFields()
        },
        onError: (error) => {
          message.error(
            isAxiosError(error) ? error.response?.data?.message ?? 'Phân công thất bại' : 'Phân công thất bại',
          )
        },
      },
    )
  }

  if (!Number.isFinite(teacherId)) {
    return <Empty description="Không tìm thấy giáo viên" />
  }

  const teacher = teacherQuery.data

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
        <Space direction="vertical" size={4}>
          <Title level={2} style={{ margin: 0 }}>
            {teacher?.fullName ?? 'Chi tiết giáo viên'}
          </Title>
          <Text type="secondary">Hồ sơ giáo viên, tài khoản liên kết và lớp đang phụ trách.</Text>
        </Space>
        <Button type="primary" onClick={() => setAssignOpen(true)}>
          Phân công lớp
        </Button>
      </Space>

      <Card loading={teacherQuery.isLoading}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="Mã GV">{teacher?.teacherCode}</Descriptions.Item>
          <Descriptions.Item label="Trạng thái">
            {teacher ? <StatusTag status={teacher.status} /> : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Email">{teacher?.email || '-'}</Descriptions.Item>
          <Descriptions.Item label="Số điện thoại">{teacher?.phone || '-'}</Descriptions.Item>
          <Descriptions.Item label="Tài khoản liên kết" span={2}>
            {linkedAccount ? (
              <Space>
                <span>{linkedAccount.username}</span>
                <Tag>{linkedAccount.status}</Tag>
              </Space>
            ) : (
              <Tag color="warning">Chưa liên kết tài khoản</Tag>
            )}
          </Descriptions.Item>
          <Descriptions.Item label="Số lớp đang hoạt động">{activeClassrooms.length}</Descriptions.Item>
          <Descriptions.Item label="Học viên đang học (ước tính)">{activeStudents}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="Lớp đang phân công" loading={classroomsQuery.isLoading}>
        <Table<Classroom>
          rowKey="id"
          dataSource={classrooms}
          pagination={false}
          locale={{ emptyText: 'Chưa được phân công lớp nào' }}
          columns={[
            {
              title: 'Mã lớp',
              dataIndex: 'classCode',
              render: (code: string, classroom) => (
                <Link to={`/classrooms/${classroom.id}`}>{code}</Link>
              ),
            },
            { title: 'Tên lớp', dataIndex: 'className' },
            {
              title: 'Lịch',
              key: 'schedule',
              render: (_, classroom) =>
                `${formatDaysOfWeek(classroom.daysOfWeek)}, ${classroom.startTime.slice(0, 5)}`,
            },
            {
              title: 'Ngày bắt đầu',
              dataIndex: 'startDate',
              render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
            },
            {
              title: 'Trạng thái',
              dataIndex: 'status',
              render: (status: string) => <StatusTag status={status} />,
            },
            {
              title: 'Học viên',
              dataIndex: 'activeStudentCount',
              render: (value?: number) => value ?? 0,
            },
          ]}
        />
      </Card>

      <Drawer
        title="Phân công lớp"
        open={assignOpen}
        onClose={() => setAssignOpen(false)}
        width={420}
      >
        <Form form={form} layout="vertical" onFinish={handleAssign}>
          <Form.Item
            name="classroomId"
            label="Chọn lớp chưa phân công hoặc đổi giáo viên"
            rules={[{ required: true, message: 'Vui lòng chọn lớp' }]}
          >
            <Select
              showSearch
              loading={unassignedQuery.isLoading}
              placeholder="Chọn lớp"
              optionFilterProp="label"
              options={(unassignedQuery.data ?? []).map((classroom) => ({
                value: classroom.id,
                label: `${classroom.classCode} - ${classroom.className}`,
              }))}
            />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={assignTeacher.isPending} block>
            Gán giáo viên này
          </Button>
        </Form>

        <List
          style={{ marginTop: 24 }}
          header="Lớp chưa phân công"
          dataSource={unassignedQuery.data ?? []}
          locale={{ emptyText: 'Không còn lớp chưa phân công' }}
          renderItem={(classroom) => (
            <List.Item>
              {classroom.classCode} - {classroom.className} ({classroom.status})
            </List.Item>
          )}
        />
      </Drawer>
    </Space>
  )
}
