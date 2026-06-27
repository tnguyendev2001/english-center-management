import { Form, Input, Modal } from 'antd'
import { useEffect } from 'react'
import type { CancelPaymentPayload, Payment } from '../paymentTypes'

interface CancelPaymentFormValues {
  reason: string
}

interface CancelPaymentModalProps {
  open: boolean
  payment?: Payment
  submitting: boolean
  onCancel: () => void
  onSubmit: (payload: CancelPaymentPayload) => void
}

export function CancelPaymentModal({
  open,
  payment,
  submitting,
  onCancel,
  onSubmit,
}: CancelPaymentModalProps) {
  const [form] = Form.useForm<CancelPaymentFormValues>()

  useEffect(() => {
    if (open) {
      form.resetFields()
    }
  }, [form, open])

  function handleFinish(values: CancelPaymentFormValues) {
    onSubmit({
      reason: values.reason,
    })
  }

  return (
    <Modal
      title="Hủy thanh toán"
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText="Hủy thanh toán"
      cancelText="Đóng"
      okButtonProps={{ danger: true }}
      destroyOnHidden
    >
      <p>
        Thanh toán {payment?.paymentCode} sẽ được chuyển sang trạng thái đã hủy và không còn được
        tính vào số tiền đã đóng.
      </p>

      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          label="Lý do hủy"
          name="reason"
          rules={[{ required: true, message: 'Vui lòng nhập lý do hủy' }]}
        >
          <Input.TextArea rows={3} placeholder="Nhập lý do hủy thanh toán" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
