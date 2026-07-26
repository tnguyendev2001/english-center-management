import { DatePicker, Form, Input, InputNumber, Modal, Select } from 'antd'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { useEffect, useMemo } from 'react'
import type { FinancialAccount, TransferPayload } from '../financeTypes'

interface TransferFormValues {
  transactionDate: Dayjs
  sourceAccountId: number
  destinationAccountId: number
  amount: number
  description: string
  referenceNo?: string
}

interface TransferModalProps {
  open: boolean
  submitting: boolean
  accounts: FinancialAccount[]
  onCancel: () => void
  onSubmit: (payload: TransferPayload) => void
}

export function TransferModal({
  open,
  submitting,
  accounts,
  onCancel,
  onSubmit,
}: TransferModalProps) {
  const [form] = Form.useForm<TransferFormValues>()

  const accountOptions = useMemo(
    () =>
      accounts
        .filter((account) => account.active)
        .map((account) => ({
          label: `${account.code} - ${account.name}`,
          value: account.id,
        })),
    [accounts],
  )

  useEffect(() => {
    if (open) {
      form.resetFields()
      form.setFieldsValue({
        transactionDate: dayjs(),
      })
    }
  }, [form, open])

  function handleFinish(values: TransferFormValues) {
    onSubmit({
      transactionDate: values.transactionDate.format('YYYY-MM-DD'),
      sourceAccountId: values.sourceAccountId,
      destinationAccountId: values.destinationAccountId,
      amount: values.amount,
      description: values.description,
      referenceNo: values.referenceNo ?? null,
    })
  }

  return (
    <Modal
      title="Chuyển tiền giữa tài khoản"
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText="Chuyển tiền"
      cancelText="Hủy"
      destroyOnHidden
    >
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          label="Ngày chuyển"
          name="transactionDate"
          rules={[{ required: true, message: 'Vui lòng chọn ngày' }]}
        >
          <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item
          label="Tài khoản nguồn"
          name="sourceAccountId"
          rules={[{ required: true, message: 'Vui lòng chọn tài khoản nguồn' }]}
        >
          <Select options={accountOptions} placeholder="Chọn tài khoản nguồn" />
        </Form.Item>

        <Form.Item
          label="Tài khoản đích"
          name="destinationAccountId"
          dependencies={['sourceAccountId']}
          rules={[
            { required: true, message: 'Vui lòng chọn tài khoản đích' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('sourceAccountId') !== value) {
                  return Promise.resolve()
                }
                return Promise.reject(new Error('Tài khoản đích phải khác tài khoản nguồn'))
              },
            }),
          ]}
        >
          <Select options={accountOptions} placeholder="Chọn tài khoản đích" />
        </Form.Item>

        <Form.Item
          label="Số tiền"
          name="amount"
          rules={[{ required: true, message: 'Vui lòng nhập số tiền' }]}
        >
          <InputNumber min={1} precision={0} addonAfter="VND" style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item
          label="Nội dung"
          name="description"
          rules={[{ required: true, message: 'Vui lòng nhập nội dung' }]}
        >
          <Input.TextArea rows={3} placeholder="Mô tả lần chuyển tiền" />
        </Form.Item>

        <Form.Item label="Mã tham chiếu" name="referenceNo">
          <Input placeholder="Tùy chọn" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
