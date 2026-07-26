import { Alert, DatePicker, Descriptions, Form, Input, InputNumber, Modal, Select } from 'antd'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { useEffect, useMemo } from 'react'
import { MoneyText } from '../../../components/common/MoneyText'
import { formatStudentLabel } from '../../../components/common/studentDisplay'
import {
  useFinanceAccounts,
  useFinanceConfig,
  useFinancialPeriods,
} from '../../finance/financeQueries'
import type { Invoice } from '../../invoices/invoiceTypes'
import type { CreatePaymentPayload, PaymentMethod } from '../paymentTypes'

interface PaymentFormValues {
  amount: number
  paymentDate: Dayjs
  method: PaymentMethod
  financialAccountId?: number
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
  const method = Form.useWatch('method', form)
  const paymentDate = Form.useWatch('paymentDate', form)
  const accountsQuery = useFinanceAccounts(open)
  const periodsQuery = useFinancialPeriods(open)
  const configQuery = useFinanceConfig(open)
  const showAccountSelect = method === 'BANK_TRANSFER' || method === 'OTHER'

  const closedPeriodKeys = useMemo(() => {
    const keys = new Set<string>()
    for (const period of periodsQuery.data ?? []) {
      if (period.status === 'CLOSED') {
        keys.add(`${period.year}-${period.month}`)
      }
    }
    return keys
  }, [periodsQuery.data])

  const businessDate = useMemo(() => {
    const value = configQuery.data?.businessDate ?? configQuery.data?.today
    return value ? dayjs(value) : dayjs()
  }, [configQuery.data?.businessDate, configQuery.data?.today])

  const isSelectedDateClosed = useMemo(() => {
    if (!paymentDate) {
      return false
    }
    return closedPeriodKeys.has(`${paymentDate.year()}-${paymentDate.month() + 1}`)
  }, [closedPeriodKeys, paymentDate])

  const isSelectedDateAfterBusiness = useMemo(() => {
    if (!paymentDate) {
      return false
    }
    return paymentDate.isAfter(businessDate, 'day')
  }, [businessDate, paymentDate])

  const accountOptions = useMemo(() => {
    const accounts = accountsQuery.data ?? []
    const preferredType = method === 'BANK_TRANSFER' ? 'BANK' : method === 'CASH' ? 'CASH' : undefined

    return accounts
      .filter((account) => account.active)
      .filter((account) => !preferredType || account.type === preferredType || account.type === 'OTHER')
      .map((account) => ({
        label: `${account.code} - ${account.name}`,
        value: account.id,
      }))
  }, [accountsQuery.data, method])

  useEffect(() => {
    if (open) {
      form.resetFields()
      const todayClosed = closedPeriodKeys.has(`${businessDate.year()}-${businessDate.month() + 1}`)
      form.setFieldsValue({
        amount: invoice?.remainingAmount,
        paymentDate: todayClosed ? undefined : businessDate,
        method: 'CASH',
      })
    }
  }, [form, invoice, open, closedPeriodKeys, businessDate])

  useEffect(() => {
    if (!showAccountSelect) {
      form.setFieldValue('financialAccountId', undefined)
    }
  }, [form, showAccountSelect])

  function handleFinish(values: PaymentFormValues) {
    onSubmit({
      amount: values.amount,
      paymentDate: values.paymentDate.format('YYYY-MM-DD'),
      method: values.method,
      financialAccountId: values.financialAccountId ?? null,
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
      okButtonProps={{ disabled: isSelectedDateClosed || isSelectedDateAfterBusiness }}
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
          extra="Ngày thu quyết định kỳ sổ thu chi. Hóa đơn cũ vẫn thu được nếu ngày thu thuộc tháng đang mở."
        >
          <DatePicker
            format="DD/MM/YYYY"
            style={{ width: '100%' }}
            disabledDate={(current) => {
              if (!current) {
                return false
              }
              if (current.isAfter(businessDate, 'day')) {
                return true
              }
              return closedPeriodKeys.has(`${current.year()}-${current.month() + 1}`)
            }}
          />
        </Form.Item>

        {isSelectedDateClosed ? (
          <Alert
            type="error"
            showIcon
            style={{ marginBottom: 16 }}
            message={`Tháng ${paymentDate?.format('MM/YYYY')} đã khóa sổ. Vui lòng chọn ngày thanh toán thuộc kỳ đang mở hoặc mở lại tháng.`}
          />
        ) : null}
        {isSelectedDateAfterBusiness ? (
          <Alert
            type="error"
            showIcon
            style={{ marginBottom: 16 }}
            message="Ngày thanh toán không được lớn hơn ngày hiện tại."
          />
        ) : null}

        <Form.Item
          label="Phương thức"
          name="method"
          rules={[{ required: true, message: 'Vui lòng chọn phương thức' }]}
        >
          <Select options={methodOptions} />
        </Form.Item>

        {showAccountSelect ? (
          <Form.Item
            label="Tài khoản thu"
            name="financialAccountId"
            rules={
              method === 'OTHER'
                ? [{ required: true, message: 'Vui lòng chọn tài khoản thu' }]
                : undefined
            }
          >
            <Select
              allowClear={method !== 'OTHER'}
              options={accountOptions}
              loading={accountsQuery.isLoading}
              placeholder="Chọn tài khoản ghi nhận"
            />
          </Form.Item>
        ) : null}

        <Form.Item label="Ghi chú" name="note">
          <Input.TextArea rows={3} placeholder="Ghi chú thêm nếu có" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
