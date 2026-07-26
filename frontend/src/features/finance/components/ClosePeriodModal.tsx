import { Checkbox, Form, Input, Modal } from 'antd'
import { useEffect } from 'react'
import type { ClosePeriodPayload, FinancialPeriod } from '../financeTypes'

interface ClosePeriodFormValues {
  note?: string
  overrideReconciliationMismatch?: boolean
  closedBy?: string
}

interface ClosePeriodModalProps {
  open: boolean
  period?: FinancialPeriod
  submitting: boolean
  onCancel: () => void
  onSubmit: (payload: ClosePeriodPayload) => void
}

export function ClosePeriodModal({
  open,
  period,
  submitting,
  onCancel,
  onSubmit,
}: ClosePeriodModalProps) {
  const [form] = Form.useForm<ClosePeriodFormValues>()

  useEffect(() => {
    if (open) {
      form.resetFields()
    }
  }, [form, open])

  function handleFinish(values: ClosePeriodFormValues) {
    onSubmit({
      note: values.note ?? null,
      overrideReconciliationMismatch: values.overrideReconciliationMismatch ?? false,
      closedBy: values.closedBy ?? null,
    })
  }

  return (
    <Modal
      title="Chốt kỳ tài chính"
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText="Chốt kỳ"
      cancelText="Hủy"
      destroyOnHidden
    >
      <p>
        Chốt kỳ {period ? `${String(period.month).padStart(2, '0')}/${period.year}` : ''} sẽ khóa
        sổ thu chi của tháng này.
      </p>

      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item label="Ghi chú" name="note">
          <Input.TextArea rows={3} placeholder="Ghi chú khi chốt kỳ" />
        </Form.Item>

        <Form.Item label="Người chốt" name="closedBy">
          <Input placeholder="Tên người chốt (tùy chọn)" />
        </Form.Item>

        <Form.Item name="overrideReconciliationMismatch" valuePropName="checked">
          <Checkbox>Cho phép chốt khi đối soát chưa khớp</Checkbox>
        </Form.Item>
      </Form>
    </Modal>
  )
}
