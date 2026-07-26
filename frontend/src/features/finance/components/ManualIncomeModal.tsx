import { DatePicker, Form, Input, InputNumber, Modal, Select } from 'antd'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { useEffect, useMemo } from 'react'
import type { FinancialAccount, ManualIncomePayload, TransactionCategory } from '../financeTypes'

interface ManualIncomeFormValues {
  transactionDate: Dayjs
  accountId: number
  categoryId: number
  amount: number
  payerOrPayee?: string
  referenceNo?: string
  description: string
  attachmentReference?: string
}

interface ManualIncomeModalProps {
  open: boolean
  submitting: boolean
  accounts: FinancialAccount[]
  categories: TransactionCategory[]
  onCancel: () => void
  onSubmit: (payload: ManualIncomePayload) => void
}

export function ManualIncomeModal({
  open,
  submitting,
  accounts,
  categories,
  onCancel,
  onSubmit,
}: ManualIncomeModalProps) {
  const [form] = Form.useForm<ManualIncomeFormValues>()

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

  const categoryOptions = useMemo(
    () =>
      categories
        .filter((category) => category.active && category.direction === 'INCOME')
        .map((category) => ({
          label: `${category.code} - ${category.name}`,
          value: category.id,
        })),
    [categories],
  )

  useEffect(() => {
    if (open) {
      form.resetFields()
      form.setFieldsValue({
        transactionDate: dayjs(),
      })
    }
  }, [form, open])

  function handleFinish(values: ManualIncomeFormValues) {
    onSubmit({
      transactionDate: values.transactionDate.format('YYYY-MM-DD'),
      accountId: values.accountId,
      categoryId: values.categoryId,
      amount: values.amount,
      payerOrPayee: values.payerOrPayee ?? null,
      referenceNo: values.referenceNo ?? null,
      description: values.description,
      attachmentReference: values.attachmentReference ?? null,
    })
  }

  return (
    <Modal
      title="Thêm khoản thu"
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText="Ghi nhận"
      cancelText="Hủy"
      destroyOnHidden
    >
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          label="Ngày giao dịch"
          name="transactionDate"
          rules={[{ required: true, message: 'Vui lòng chọn ngày' }]}
        >
          <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item
          label="Tài khoản"
          name="accountId"
          rules={[{ required: true, message: 'Vui lòng chọn tài khoản' }]}
        >
          <Select options={accountOptions} placeholder="Chọn tài khoản" />
        </Form.Item>

        <Form.Item
          label="Danh mục thu"
          name="categoryId"
          rules={[{ required: true, message: 'Vui lòng chọn danh mục' }]}
        >
          <Select options={categoryOptions} placeholder="Chọn danh mục" />
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
          <Input.TextArea rows={3} placeholder="Mô tả khoản thu" />
        </Form.Item>

        <Form.Item label="Người nộp / Bên nộp" name="payerOrPayee">
          <Input placeholder="Tùy chọn" />
        </Form.Item>

        <Form.Item label="Mã tham chiếu" name="referenceNo">
          <Input placeholder="Tùy chọn" />
        </Form.Item>

        <Form.Item label="Tài liệu đính kèm" name="attachmentReference">
          <Input placeholder="Link hoặc mã tài liệu" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
