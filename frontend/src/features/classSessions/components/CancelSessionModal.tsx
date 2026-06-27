import { Form, Input, Modal } from 'antd'
import { useEffect } from 'react'
import type { CancelClassSessionPayload, ClassSession } from '../classSessionTypes'

interface CancelSessionFormValues {
  reason: string
}

interface CancelSessionModalProps {
  open: boolean
  session?: ClassSession
  submitting: boolean
  onCancel: () => void
  onSubmit: (payload: CancelClassSessionPayload) => void
}

export function CancelSessionModal({
  open,
  session,
  submitting,
  onCancel,
  onSubmit,
}: CancelSessionModalProps) {
  const [form] = Form.useForm<CancelSessionFormValues>()

  useEffect(() => {
    if (open) {
      form.resetFields()
    }
  }, [form, open])

  function handleFinish(values: CancelSessionFormValues) {
    onSubmit({ reason: values.reason })
  }

  return (
    <Modal
      title="Hủy buổi học"
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText="Hủy buổi"
      cancelText="Đóng"
      okButtonProps={{ danger: true }}
      destroyOnHidden
    >
      <p>Buổi học #{session?.sessionNo} sẽ không tính là buổi đã học.</p>
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          label="Lý do hủy"
          name="reason"
          rules={[{ required: true, message: 'Vui lòng nhập lý do hủy buổi' }]}
        >
          <Input.TextArea rows={3} />
        </Form.Item>
      </Form>
    </Modal>
  )
}
