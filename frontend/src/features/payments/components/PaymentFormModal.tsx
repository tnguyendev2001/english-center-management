import { DatePicker, Descriptions, Form, Input, InputNumber, Modal, Select } from 'antd'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { useEffect } from 'react'
import { MoneyText } from '../../../components/common/MoneyText'
import { formatStudentLabel } from '../../../components/common/studentDisplay'
import type { Invoice } from '../../invoices/invoiceTypes'
import type { CreatePaymentPayload, PaymentMethod } from '../paymentTypes'

interface PaymentFormValues {
  amount: number
  paymentDate: Dayjs
  method: PaymentMethod
  note?: string
}

interface PaymentFormModalProps {
  open: boolean
  invoice?: Invoice
  submitting: boolean
  onCancel: () => void
  onSubmit: (payload: CreatePaymentPayload) => void
}

const methodOptions: { label: string; value: PaymentMethod }[] = [
  { label: 'Tiền mặt', value: 'CASH' },
  { label: 'Chuyển khoản', value: 'BANK_TRANSFER' },
  { label: 'Khác', value: 'OTHER' },
]

export function PaymentFormModal({
  open,
  invoice,
  submitting,
  onCancel,
  onSubmit,
}: PaymentFormModalProps) {
  const [form] = Form.useForm<PaymentFormValues>()

  useEffect(() => {
    if (open) {
      form.resetFields()
      form.setFieldsValue({
        amount: invoice?.remainingAmount,
        paymentDate: dayjs(),
        method: 'CASH',
      })
    }
  }, [form, invoice, open])

  function handleFinish(values: PaymentFormValues) {
    onSubmit({
      amount: values.amount,
      paymentDate: values.paymentDate.format('YYYY-MM-DD'),
      method: values.method,
      note: values.note ?? null,
    })
  }

  return (
    <Modal
      title="Thu tiền học phí"
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText="Thu tiền"
      cancelText="Hủy"
      destroyOnHidden
    >
      {invoice ? (
        <Descriptions column={1} size="small" style={{ marginBottom: 16 }}>
          <Descriptions.Item label="Học viên">
            {formatStudentLabel(invoice.studentCode, invoice.studentName)}
          </Descriptions.Item>
          <Descriptions.Item label="Lớp học">{invoice.classroomName}</Descriptions.Item>
          <Descriptions.Item label="Còn phải đóng">
            <MoneyText value={invoice.remainingAmount} />
          </Descriptions.Item>
        </Descriptions>
      ) : null}

      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          label="Số tiền"
          name="amount"
          rules={[{ required: true, message: 'Vui lòng nhập số tiền' }]}
        >
          <InputNumber
            min={1}
            max={invoice?.remainingAmount}
            precision={0}
            addonAfter="VND"
            style={{ width: '100%' }}
          />
        </Form.Item>

        <Form.Item
          label="Ngày thu"
          name="paymentDate"
          rules={[{ required: true, message: 'Vui lòng chọn ngày thu' }]}
        >
          <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item
          label="Phương thức"
          name="method"
          rules={[{ required: true, message: 'Vui lòng chọn phương thức' }]}
        >
          <Select options={methodOptions} />
        </Form.Item>

        <Form.Item label="Ghi chú" name="note">
          <Input.TextArea rows={3} placeholder="Ghi chú thêm nếu có" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
