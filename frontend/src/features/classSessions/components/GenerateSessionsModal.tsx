import { DatePicker, Form, InputNumber, Modal } from 'antd'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { useEffect } from 'react'
import type { GenerateClassSessionsPayload } from '../classSessionTypes'

interface GenerateSessionsFormValues {
  numberOfSessions?: number
  fromDate?: Dayjs
  toDate?: Dayjs
}

interface GenerateSessionsModalProps {
  open: boolean
  classroomId: number
  submitting: boolean
  onCancel: () => void
  onSubmit: (payload: GenerateClassSessionsPayload) => void
}

export function GenerateSessionsModal({
  open,
  classroomId,
  submitting,
  onCancel,
  onSubmit,
}: GenerateSessionsModalProps) {
  const [form] = Form.useForm<GenerateSessionsFormValues>()

  useEffect(() => {
    if (open) {
      form.resetFields()
      form.setFieldsValue({
        numberOfSessions: 1,
        fromDate: dayjs(),
      })
    }
  }, [form, open])

  function handleFinish(values: GenerateSessionsFormValues) {
    onSubmit({
      classroomId,
      numberOfSessions: values.numberOfSessions ?? null,
      fromDate: values.fromDate?.format('YYYY-MM-DD') ?? null,
      toDate: values.toDate?.format('YYYY-MM-DD') ?? null,
    })
  }

  return (
    <Modal
      title="Tạo lịch học"
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText="Tạo lịch"
      cancelText="Hủy"
      destroyOnHidden
    >
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item label="Số buổi cần tạo" name="numberOfSessions">
          <InputNumber min={1} precision={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item label="Từ ngày" name="fromDate">
          <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item label="Đến ngày" name="toDate">
          <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    </Modal>
  )
}
