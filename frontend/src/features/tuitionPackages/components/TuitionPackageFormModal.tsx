import { Form, Input, InputNumber, Modal, Select } from 'antd'
import { useEffect } from 'react'
import type {
  TuitionPackage,
  TuitionPackagePayload,
  TuitionPackageStatus,
} from '../tuitionPackageTypes'

interface TuitionPackageFormValues {
  name: string
  sessionsPerWeek?: number | null
  totalSessions: number
  expectedMonths?: number | null
  price: number
  status: TuitionPackageStatus
}

interface TuitionPackageFormModalProps {
  open: boolean
  initialTuitionPackage?: TuitionPackage
  submitting: boolean
  onCancel: () => void
  onSubmit: (payload: TuitionPackagePayload) => void
}

const statusOptions: { label: string; value: TuitionPackageStatus }[] = [
  { label: 'Đang áp dụng', value: 'ACTIVE' },
  { label: 'Ngừng áp dụng', value: 'INACTIVE' },
]

export function TuitionPackageFormModal({
  open,
  initialTuitionPackage,
  submitting,
  onCancel,
  onSubmit,
}: TuitionPackageFormModalProps) {
  const [form] = Form.useForm<TuitionPackageFormValues>()
  const isEdit = Boolean(initialTuitionPackage)

  useEffect(() => {
    if (!open) {
      return
    }

    if (initialTuitionPackage) {
      form.setFieldsValue({
        name: initialTuitionPackage.name,
        sessionsPerWeek: initialTuitionPackage.sessionsPerWeek ?? undefined,
        totalSessions: initialTuitionPackage.totalSessions,
        expectedMonths: initialTuitionPackage.expectedMonths ?? undefined,
        price: initialTuitionPackage.price,
        status: initialTuitionPackage.status,
      })
      return
    }

    form.resetFields()
    form.setFieldValue('status', 'ACTIVE')
  }, [form, initialTuitionPackage, open])

  function handleFinish(values: TuitionPackageFormValues) {
    onSubmit({
      name: values.name,
      sessionsPerWeek: values.sessionsPerWeek ?? null,
      totalSessions: values.totalSessions,
      expectedMonths: values.expectedMonths ?? null,
      price: values.price,
      status: values.status,
    })
  }

  return (
    <Modal
      title={isEdit ? 'Cập nhật gói học phí' : 'Thêm gói học phí'}
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText={isEdit ? 'Cập nhật' : 'Thêm mới'}
      cancelText="Hủy"
      destroyOnHidden
    >
      <Form form={form} layout="vertical" onFinish={handleFinish} initialValues={{ status: 'ACTIVE' }}>
        <Form.Item
          label="Tên gói"
          name="name"
          rules={[{ required: true, message: 'Vui lòng nhập tên gói' }]}
        >
          <Input placeholder="VD: Gói 8 buổi" />
        </Form.Item>

        <Form.Item label="Số buổi mỗi tuần" name="sessionsPerWeek">
          <InputNumber min={1} precision={0} placeholder="VD: 2" style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item
          label="Tổng số buổi"
          name="totalSessions"
          rules={[{ required: true, message: 'Vui lòng nhập tổng số buổi' }]}
        >
          <InputNumber min={1} precision={0} placeholder="VD: 8" style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item label="Số tháng dự kiến" name="expectedMonths">
          <InputNumber min={1} precision={0} placeholder="VD: 1" style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item
          label="Học phí"
          name="price"
          rules={[{ required: true, message: 'Vui lòng nhập học phí' }]}
        >
          <InputNumber
            min={1}
            precision={0}
            placeholder="VD: 500000"
            style={{ width: '100%' }}
            addonAfter="VND"
          />
        </Form.Item>

        <Form.Item
          label="Trạng thái"
          name="status"
          rules={[{ required: true, message: 'Vui lòng chọn trạng thái' }]}
        >
          <Select options={statusOptions} />
        </Form.Item>
      </Form>
    </Modal>
  )
}
