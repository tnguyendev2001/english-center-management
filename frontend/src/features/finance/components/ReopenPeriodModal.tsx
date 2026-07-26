import { Form, Input, Modal } from 'antd'
import { useEffect } from 'react'
import type { FinancialPeriod, ReopenPeriodPayload } from '../financeTypes'

interface ReopenPeriodFormValues {
  reason: string
  reopenedBy?: string
}

interface ReopenPeriodModalProps {
  open: boolean
  period?: FinancialPeriod
  submitting: boolean
  onCancel: () => void
  onSubmit: (payload: ReopenPeriodPayload) => void
}

export function ReopenPeriodModal({
  open,
  period,
  submitting,
  onCancel,
  onSubmit,
}: ReopenPeriodModalProps) {
  const [form] = Form.useForm<ReopenPeriodFormValues>()

  useEffect(() => {
    if (open) {
      form.resetFields()
    }
  }, [form, open])

  function handleFinish(values: ReopenPeriodFormValues) {
    onSubmit({
      reason: values.reason,
      reopenedBy: values.reopenedBy ?? null,
    })
  }

  return (
    <Modal
      title="Mở lại kỳ tài chính"
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText="Mở lại kỳ"
      cancelText="Hủy"
      okButtonProps={{ danger: true }}
      destroyOnHidden
    >
      <p>
        Mở lại kỳ {period ? `${String(period.month).padStart(2, '0')}/${period.year}` : ''} để cho
        phép chỉnh sửa sổ thu chi.
      </p>

      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          label="Lý do mở lại"
          name="reason"
          rules={[{ required: true, message: 'Vui lòng nhập lý do mở lại' }]}
        >
          <Input.TextArea rows={3} placeholder="Nhập lý do mở lại kỳ" />
        </Form.Item>

        <Form.Item label="Người mở lại" name="reopenedBy">
          <Input placeholder="Tên người mở lại (tùy chọn)" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
