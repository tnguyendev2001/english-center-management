import { Alert, Form, Input, Modal } from 'antd'
import { useEffect } from 'react'
import type { CancelPayload, CashTransaction } from '../financeTypes'

interface CancelFormValues {
  reason: string
}

interface CancelTransactionModalProps {
  open: boolean
  transaction?: CashTransaction
  submitting: boolean
  onCancel: () => void
  onSubmit: (payload: CancelPayload) => void
}

export function CancelTransactionModal({
  open,
  transaction,
  submitting,
  onCancel,
  onSubmit,
}: CancelTransactionModalProps) {
  const [form] = Form.useForm<CancelFormValues>()

  useEffect(() => {
    if (open) {
      form.resetFields()
    }
  }, [form, open])

  function handleFinish(values: CancelFormValues) {
    onSubmit({ reason: values.reason })
  }

  return (
    <Modal
      title="Hủy giao dịch"
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText="Hủy giao dịch"
      cancelText="Đóng"
      okButtonProps={{ danger: true }}
      destroyOnHidden
    >
      <Alert
        type="warning"
        showIcon
        style={{ marginBottom: 16 }}
        message="Giao dịch sẽ được giữ lại trong lịch sử nhưng không còn được tính vào số dư và báo cáo."
      />
      <p>
        Mã giao dịch: <strong>{transaction?.transactionCode}</strong>
      </p>

      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          label="Lý do hủy"
          name="reason"
          rules={[{ required: true, message: 'Vui lòng nhập lý do hủy' }]}
        >
          <Input.TextArea rows={3} placeholder="Nhập lý do hủy giao dịch" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
