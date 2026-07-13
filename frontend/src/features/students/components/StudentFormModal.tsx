import { DatePicker, Form, Input, Modal, Select, Typography } from 'antd'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { useEffect } from 'react'
import type { Student, StudentPayload, StudentStatus } from '../studentTypes'

interface StudentFormValues {
  fullName: string
  dateOfBirth?: Dayjs
  phone?: string
  parentName?: string
  parentPhone?: string
  address?: string
  status: StudentStatus
  note?: string
}

interface StudentFormModalProps {
  open: boolean
  initialStudent?: Student
  submitting: boolean
  onCancel: () => void
  onSubmit: (payload: StudentPayload) => void
}

const statusOptions: { label: string; value: StudentStatus }[] = [
  { label: 'Đang học', value: 'ACTIVE' },
  { label: 'Tạm nghỉ', value: 'ON_HOLD' },
  { label: 'Ngừng học', value: 'INACTIVE' },
]

export function StudentFormModal({
  open,
  initialStudent,
  submitting,
  onCancel,
  onSubmit,
}: StudentFormModalProps) {
  const [form] = Form.useForm<StudentFormValues>()
  const isEdit = Boolean(initialStudent)

  useEffect(() => {
    if (!open) {
      return
    }

    if (initialStudent) {
      form.setFieldsValue({
        fullName: initialStudent.fullName,
        dateOfBirth: initialStudent.dateOfBirth ? dayjs(initialStudent.dateOfBirth) : undefined,
        phone: initialStudent.phone ?? undefined,
        parentName: initialStudent.parentName ?? undefined,
        parentPhone: initialStudent.parentPhone ?? undefined,
        address: initialStudent.address ?? undefined,
        status: initialStudent.status,
        note: initialStudent.note ?? undefined,
      })
      return
    }

    form.resetFields()
    form.setFieldValue('status', 'ACTIVE')
  }, [form, initialStudent, open])

  function handleFinish(values: StudentFormValues) {
    onSubmit({
      fullName: values.fullName,
      dateOfBirth: values.dateOfBirth?.format('YYYY-MM-DD') ?? null,
      phone: values.phone ?? null,
      parentName: values.parentName ?? null,
      parentPhone: values.parentPhone ?? null,
      address: values.address ?? null,
      status: values.status,
      note: values.note ?? null,
    })
  }

  return (
    <Modal
      title={isEdit ? 'Cập nhật học viên' : 'Thêm học viên'}
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText={isEdit ? 'Cập nhật' : 'Thêm mới'}
      cancelText="Hủy"
      destroyOnHidden
    >
      <Form form={form} layout="vertical" onFinish={handleFinish} initialValues={{ status: 'ACTIVE' }}>
        <Form.Item label="Mã học viên">
          {isEdit ? (
            <Input value={initialStudent?.studentCode} disabled />
          ) : (
            <Typography.Text type="secondary">
              Mã học viên sẽ được tự động tạo sau khi lưu
            </Typography.Text>
          )}
        </Form.Item>

        <Form.Item
          label="Họ tên"
          name="fullName"
          rules={[{ required: true, message: 'Vui lòng nhập họ tên' }]}
        >
          <Input placeholder="Nhập họ tên học viên" />
        </Form.Item>

        <Form.Item label="Ngày sinh" name="dateOfBirth">
          <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item label="Số điện thoại" name="phone">
          <Input placeholder="Nhập số điện thoại học viên" />
        </Form.Item>

        <Form.Item label="Tên phụ huynh" name="parentName">
          <Input placeholder="Nhập tên phụ huynh" />
        </Form.Item>

        <Form.Item label="SĐT phụ huynh" name="parentPhone">
          <Input placeholder="Nhập số điện thoại phụ huynh" />
        </Form.Item>

        <Form.Item
          label="Trạng thái"
          name="status"
          rules={[{ required: true, message: 'Vui lòng chọn trạng thái' }]}
        >
          <Select options={statusOptions} />
        </Form.Item>

        <Form.Item label="Địa chỉ" name="address">
          <Input.TextArea rows={2} placeholder="Nhập địa chỉ" />
        </Form.Item>

        <Form.Item label="Ghi chú" name="note">
          <Input.TextArea rows={3} placeholder="Ghi chú thêm nếu có" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
