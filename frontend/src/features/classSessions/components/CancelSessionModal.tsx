import { Form, Input, Modal } from 'antd'
import { useEffect } from 'react'
import type { CancelClassSessionPayload, ClassSession } from '../classSessionTypes'

interface CancelSessionFormValues {
  reason: string
}

interface CancelSessionModalProps {
  open: boolean
  mode?: 'normal' | 'correction'
  session?: ClassSession
  submitting: boolean
  onCancel: () => void
  onSubmit: (payload: CancelClassSessionPayload) => void
}

export function CancelSessionModal({
  open,
  mode = 'normal',
  session,
  submitting,
  onCancel,
  onSubmit,
}: CancelSessionModalProps) {
  const [form] = Form.useForm<CancelSessionFormValues>()
  const isCorrection = mode === 'correction'

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
      title={isCorrection ? 'Hoàn tác điểm danh & hủy buổi' : 'Hủy buổi học'}
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText={isCorrection ? 'Xác nhận hoàn tác & hủy' : 'Hủy buổi'}
      cancelText="Đóng"
      okButtonProps={{ danger: true }}
      destroyOnHidden
    >
      {isCorrection ? (
        <p>
          Buổi học #{session?.sessionNo} đã điểm danh sẽ bị hủy. Điểm danh vẫn được giữ để đối chiếu
          nhưng sẽ không còn tính vào buổi đã học. Buổi bù phát sinh từ buổi này cũng sẽ bị hủy.
        </p>
      ) : (
        <p>Buổi học #{session?.sessionNo} sẽ không tính là buổi đã học.</p>
      )}
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
