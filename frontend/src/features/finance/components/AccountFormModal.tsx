import { DatePicker, Form, Input, InputNumber, Modal, Select } from 'antd'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { useEffect } from 'react'
import type { AccountPayload, FinancialAccount, FinancialAccountType } from '../financeTypes'

interface AccountFormValues {
  code: string
  name: string
  type: FinancialAccountType
  openingBalance: number
  openingBalanceDate: Dayjs
  note?: string
  displayOrder?: number
}

interface AccountFormModalProps {
  open: boolean
  initialAccount?: FinancialAccount
  submitting: boolean
  onCancel: () => void
  onSubmit: (payload: AccountPayload) => void
}

const typeOptions: { label: string; value: FinancialAccountType }[] = [
  { label: 'Tiền mặt', value: 'CASH' },
  { label: 'Ngân hàng', value: 'BANK' },
  { label: 'Khác', value: 'OTHER' },
]

export function AccountFormModal({
  open,
  initialAccount,
  submitting,
  onCancel,
  onSubmit,
}: AccountFormModalProps) {
  const [form] = Form.useForm<AccountFormValues>()
  const isEdit = Boolean(initialAccount)

  useEffect(() => {
    if (!open) {
      return
    }

    if (initialAccount) {
      form.setFieldsValue({
        code: initialAccount.code,
        name: initialAccount.name,
        type: initialAccount.type,
        openingBalance: initialAccount.openingBalance,
        openingBalanceDate: dayjs(initialAccount.openingBalanceDate),
        note: initialAccount.note ?? undefined,
        displayOrder: initialAccount.displayOrder,
      })
      return
    }

    form.resetFields()
    form.setFieldsValue({
      type: 'CASH',
      openingBalance: 0,
      openingBalanceDate: dayjs(),
      displayOrder: 0,
    })
  }, [form, initialAccount, open])

  function handleFinish(values: AccountFormValues) {
    onSubmit({
      code: values.code,
      name: values.name,
      type: values.type,
      openingBalance: values.openingBalance,
      openingBalanceDate: values.openingBalanceDate.format('YYYY-MM-DD'),
      note: values.note ?? null,
      displayOrder: values.displayOrder ?? null,
    })
  }

  return (
    <Modal
      title={isEdit ? 'Cập nhật tài khoản' : 'Thêm tài khoản'}
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText={isEdit ? 'Cập nhật' : 'Thêm mới'}
      cancelText="Hủy"
      destroyOnHidden
    >
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          label="Mã tài khoản"
          name="code"
          rules={[{ required: true, message: 'Vui lòng nhập mã tài khoản' }]}
        >
          <Input placeholder="VD: CASH_MAIN" disabled={isEdit} />
        </Form.Item>

        <Form.Item
          label="Tên tài khoản"
          name="name"
          rules={[{ required: true, message: 'Vui lòng nhập tên tài khoản' }]}
        >
          <Input placeholder="VD: Quỹ tiền mặt" />
        </Form.Item>

        <Form.Item
          label="Loại tài khoản"
          name="type"
          rules={[{ required: true, message: 'Vui lòng chọn loại' }]}
        >
          <Select options={typeOptions} />
        </Form.Item>

        <Form.Item
          label="Số dư đầu kỳ"
          name="openingBalance"
          rules={[{ required: true, message: 'Vui lòng nhập số dư đầu kỳ' }]}
        >
          <InputNumber precision={0} addonAfter="VND" style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item
          label="Ngày số dư đầu kỳ"
          name="openingBalanceDate"
          rules={[{ required: true, message: 'Vui lòng chọn ngày' }]}
        >
          <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item label="Thứ tự hiển thị" name="displayOrder">
          <InputNumber min={0} precision={0} style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item label="Ghi chú" name="note">
          <Input.TextArea rows={3} placeholder="Ghi chú thêm nếu có" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
