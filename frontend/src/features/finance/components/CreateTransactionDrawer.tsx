import {
  Button,
  Collapse,
  DatePicker,
  Divider,
  Drawer,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Tag,
} from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useEffect, useMemo, useRef, useState } from 'react'
import {
  getQuickCategories,
  listActiveTemplates,
  rememberLastAccount,
  rememberLastTransactionType,
  rememberUsedCategory,
  resolveDefaultAccount,
  resolveDefaultTransactionType,
  saveTransactionTemplate,
  type FinanceTransactionTemplate,
  type ManualTransactionType,
} from '../financePreferences'
import { suggestTransactionDescription } from '../suggestTransactionDescription'
import type {
  CategoryDirection,
  CategoryPayload,
  FinancialAccount,
  ManualExpensePayload,
  ManualIncomePayload,
  TransactionCategory,
  TransferPayload,
} from '../financeTypes'
import { CategoryFormModal } from './CategoryFormModal'

type TransactionFormValues = {
  transactionDate: Dayjs
  accountId?: number
  destinationAccountId?: number
  categoryId?: number
  amount?: number
  description?: string
  payerOrPayee?: string
  referenceNo?: string
}

export type TransactionPrefill = {
  type?: ManualTransactionType
  transactionDate?: string
  accountId?: number
  destinationAccountId?: number
  categoryId?: number
  amount?: number
  description?: string
  payerOrPayee?: string
  referenceNo?: string
}

interface CreateTransactionDrawerProps {
  open: boolean
  submitting: boolean
  accounts: FinancialAccount[]
  categories: TransactionCategory[]
  prefill?: TransactionPrefill | null
  creatingCategory: boolean
  onCancel: () => void
  onSubmitExpense: (payload: ManualExpensePayload) => void
  onSubmitIncome: (payload: ManualIncomePayload) => void
  onSubmitTransfer: (payload: TransferPayload) => void
  onCreateCategory: (payload: CategoryPayload) => Promise<TransactionCategory>
}

const typeOptions: { label: string; value: ManualTransactionType }[] = [
  { label: 'Khoản chi', value: 'EXPENSE' },
  { label: 'Khoản thu', value: 'INCOME' },
  { label: 'Chuyển tiền', value: 'TRANSFER' },
]

function formatAmountDisplay(value?: string | number) {
  if (value === undefined || value === null || value === '') {
    return ''
  }
  const digits = String(value).replace(/\D/g, '')
  if (!digits) {
    return ''
  }
  return Number(digits).toLocaleString('en-US')
}

function parseAmountInput(value?: string) {
  const digits = (value ?? '').replace(/\D/g, '')
  return digits ? Number(digits) : undefined
}

export function CreateTransactionDrawer({
  open,
  submitting,
  accounts,
  categories,
  prefill,
  creatingCategory,
  onCancel,
  onSubmitExpense,
  onSubmitIncome,
  onSubmitTransfer,
  onCreateCategory,
}: CreateTransactionDrawerProps) {
  const [form] = Form.useForm<TransactionFormValues>()
  const [type, setType] = useState<ManualTransactionType>('EXPENSE')
  const [categoryModalOpen, setCategoryModalOpen] = useState(false)
  const [templates, setTemplates] = useState<FinanceTransactionTemplate[]>([])
  const descriptionTouchedRef = useRef(false)
  const selectedCategoryId = Form.useWatch('categoryId', form)

  const activeAccounts = useMemo(() => accounts.filter((account) => account.active), [accounts])
  const direction: CategoryDirection = type === 'INCOME' ? 'INCOME' : 'EXPENSE'

  const accountOptions = useMemo(
    () =>
      activeAccounts.map((account) => ({
        label: `${account.code} - ${account.name}`,
        value: account.id,
      })),
    [activeAccounts],
  )

  const activeCategories = useMemo(
    () =>
      categories.filter((category) => {
        if (!category.active) {
          return false
        }
        if (type === 'TRANSFER') {
          return false
        }
        if (category.direction !== direction) {
          return false
        }
        // Tuition income is created only via Payment module.
        if (direction === 'INCOME' && category.code.toUpperCase() === 'TUITION') {
          return false
        }
        return true
      }),
    [categories, direction, type],
  )

  const categoryOptions = useMemo(() => {
    const system = activeCategories.filter((category) => category.systemCategory)
    const custom = activeCategories.filter((category) => !category.systemCategory)
    const toOption = (category: TransactionCategory) => ({
      label: `${category.name} (${category.code})`,
      value: category.id,
    })
    return [
      {
        label: 'Danh mục hệ thống',
        options: system.map(toOption),
      },
      {
        label: 'Danh mục tùy chỉnh',
        options: custom.map(toOption),
      },
    ].filter((group) => group.options.length > 0)
  }, [activeCategories])

  const quickCategories = useMemo(
    () => (type === 'TRANSFER' ? [] : getQuickCategories(categories, direction)),
    [categories, direction, type],
  )

  useEffect(() => {
    if (!open) {
      return
    }
    descriptionTouchedRef.current = Boolean(prefill?.description)
    setTemplates(listActiveTemplates())

    const nextType = prefill?.type ?? resolveDefaultTransactionType()
    setType(nextType)

    const defaultAccountId =
      prefill?.accountId && activeAccounts.some((account) => account.id === prefill.accountId)
        ? prefill.accountId
        : resolveDefaultAccount(accounts)

    form.resetFields()
    form.setFieldsValue({
      transactionDate: prefill?.transactionDate ? dayjs(prefill.transactionDate) : dayjs(),
      accountId: defaultAccountId,
      destinationAccountId: prefill?.destinationAccountId,
      categoryId: prefill?.categoryId,
      amount: prefill?.amount,
      description: prefill?.description,
      payerOrPayee: prefill?.payerOrPayee,
      referenceNo: prefill?.referenceNo,
    })
  }, [open, prefill, form, accounts, activeAccounts])

  function applyCategorySuggestion(categoryId?: number) {
    if (!categoryId || descriptionTouchedRef.current) {
      return
    }
    const category = categories.find((item) => item.id === categoryId)
    const suggestion = suggestTransactionDescription(category)
    if (suggestion) {
      form.setFieldValue('description', suggestion)
    }
  }

  function handleTypeChange(nextType: ManualTransactionType) {
    setType(nextType)
    rememberLastTransactionType(nextType)
    descriptionTouchedRef.current = false
    form.setFieldsValue({
      categoryId: undefined,
      destinationAccountId: undefined,
      description: undefined,
    })
  }

  function handleSelectCategory(categoryId: number) {
    form.setFieldValue('categoryId', categoryId)
    applyCategorySuggestion(categoryId)
  }

  function handleApplyTemplate(templateId: number | string) {
    const template = templates.find((item) => item.id === templateId)
    if (!template) {
      return
    }
    setType(template.type)
    rememberLastTransactionType(template.type)
    descriptionTouchedRef.current = Boolean(template.description)
    form.setFieldsValue({
      transactionDate: dayjs(),
      accountId:
        template.accountId && activeAccounts.some((account) => account.id === template.accountId)
          ? template.accountId
          : resolveDefaultAccount(accounts),
      destinationAccountId: template.destinationAccountId,
      categoryId: template.categoryId,
      amount: template.amount,
      description: template.description,
      payerOrPayee: undefined,
      referenceNo: undefined,
    })
  }

  function handleSaveAsTemplate() {
    const values = form.getFieldsValue()
    const name = window.prompt('Tên giao dịch thường dùng')
    if (!name?.trim()) {
      return
    }
    saveTransactionTemplate({
      name: name.trim(),
      type,
      accountId: values.accountId,
      destinationAccountId: values.destinationAccountId,
      categoryId: values.categoryId,
      amount: values.amount,
      description: values.description,
    })
    setTemplates(listActiveTemplates())
  }

  async function handleCreateCategory(payload: CategoryPayload) {
    const created = await onCreateCategory(payload)
    setCategoryModalOpen(false)
    form.setFieldValue('categoryId', created.id)
    applyCategorySuggestion(created.id)
  }

  function handleFinish(values: TransactionFormValues) {
    if (!values.amount || values.amount <= 0) {
      return
    }

    if (type === 'TRANSFER') {
      if (!values.accountId || !values.destinationAccountId || !values.description) {
        return
      }
      rememberLastAccount(values.accountId)
      rememberLastTransactionType('TRANSFER')
      onSubmitTransfer({
        transactionDate: values.transactionDate.format('YYYY-MM-DD'),
        sourceAccountId: values.accountId,
        destinationAccountId: values.destinationAccountId,
        amount: values.amount,
        description: values.description,
        referenceNo: values.referenceNo ?? null,
      })
      return
    }

    if (!values.accountId || !values.categoryId || !values.description) {
      return
    }

    rememberLastAccount(values.accountId)
    rememberLastTransactionType(type)
    rememberUsedCategory(values.categoryId, direction)

    const payload = {
      transactionDate: values.transactionDate.format('YYYY-MM-DD'),
      accountId: values.accountId,
      categoryId: values.categoryId,
      amount: values.amount,
      payerOrPayee: values.payerOrPayee ?? null,
      referenceNo: values.referenceNo ?? null,
      description: values.description,
      attachmentReference: null,
    }

    if (type === 'EXPENSE') {
      onSubmitExpense(payload)
    } else {
      onSubmitIncome(payload)
    }
  }

  const submitLabel =
    type === 'EXPENSE' ? 'Lưu khoản chi' : type === 'INCOME' ? 'Lưu khoản thu' : 'Xác nhận chuyển tiền'

  const accountLabel = type === 'INCOME' ? 'Thu vào tài khoản' : 'Chi từ tài khoản'

  return (
    <>
      <Drawer
        title="Tạo giao dịch"
        open={open}
        onClose={onCancel}
        width={480}
        destroyOnHidden
        extra={
          <Space>
            <Button onClick={onCancel}>Hủy</Button>
            <Button type="primary" loading={submitting} onClick={() => form.submit()}>
              {submitLabel}
            </Button>
          </Space>
        }
      >
        <Form form={form} layout="vertical" onFinish={handleFinish}>
          <Form.Item label="Loại giao dịch">
            <Select
              value={type}
              options={typeOptions}
              onChange={handleTypeChange}
              disabled={submitting}
            />
          </Form.Item>

          {templates.length > 0 ? (
            <Form.Item label="Giao dịch thường dùng">
              <Select
                allowClear
                placeholder="Chọn mẫu để điền nhanh"
                options={templates.map((template) => ({
                  label: template.name,
                  value: template.id,
                }))}
                onChange={handleApplyTemplate}
              />
            </Form.Item>
          ) : null}

          <Form.Item
            label="Ngày"
            name="transactionDate"
            rules={[{ required: true, message: 'Vui lòng chọn ngày' }]}
          >
            <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
          </Form.Item>

          {type === 'TRANSFER' ? (
            <>
              <Form.Item
                label="Tài khoản nguồn"
                name="accountId"
                rules={[{ required: true, message: 'Vui lòng chọn tài khoản nguồn' }]}
              >
                <Select options={accountOptions} placeholder="Chọn tài khoản nguồn" showSearch optionFilterProp="label" />
              </Form.Item>
              <Form.Item
                label="Tài khoản đích"
                name="destinationAccountId"
                dependencies={['accountId']}
                rules={[
                  { required: true, message: 'Vui lòng chọn tài khoản đích' },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value || getFieldValue('accountId') !== value) {
                        return Promise.resolve()
                      }
                      return Promise.reject(new Error('Tài khoản đích phải khác tài khoản nguồn'))
                    },
                  }),
                ]}
              >
                <Select options={accountOptions} placeholder="Chọn tài khoản đích" showSearch optionFilterProp="label" />
              </Form.Item>
            </>
          ) : (
            <Form.Item
              label={accountLabel}
              name="accountId"
              rules={[{ required: true, message: 'Vui lòng chọn tài khoản' }]}
            >
              <Select options={accountOptions} placeholder="Chọn tài khoản" showSearch optionFilterProp="label" />
            </Form.Item>
          )}

          {type !== 'TRANSFER' ? (
            <>
              {quickCategories.length > 0 ? (
                <Form.Item label="Danh mục hay dùng">
                  <Space wrap size={[8, 8]}>
                    {quickCategories.map((category) => (
                      <Tag.CheckableTag
                        key={category.id}
                        checked={selectedCategoryId === category.id}
                        onChange={() => handleSelectCategory(category.id)}
                      >
                        {category.name}
                      </Tag.CheckableTag>
                    ))}
                  </Space>
                </Form.Item>
              ) : null}

              <Form.Item
                label="Danh mục"
                name="categoryId"
                rules={[{ required: true, message: 'Vui lòng chọn danh mục' }]}
              >
                <Select
                  showSearch
                  placeholder="Tìm theo tên hoặc mã"
                  optionFilterProp="label"
                  filterOption={(input, option) => {
                    const label = String(option?.label ?? '').toLowerCase()
                    return label.includes(input.trim().toLowerCase())
                  }}
                  options={categoryOptions}
                  onChange={(value) => applyCategorySuggestion(value)}
                  popupRender={(menu) => (
                    <>
                      {menu}
                      <Divider style={{ margin: '8px 0' }} />
                      <Button
                        type="link"
                        style={{ width: '100%' }}
                        onClick={() => setCategoryModalOpen(true)}
                      >
                        + Tạo danh mục mới
                      </Button>
                    </>
                  )}
                />
              </Form.Item>
            </>
          ) : null}

          <Form.Item
            label="Số tiền"
            name="amount"
            rules={[
              { required: true, message: 'Vui lòng nhập số tiền' },
              {
                validator: (_, value) => {
                  if (value == null || value <= 0) {
                    return Promise.reject(new Error('Số tiền phải lớn hơn 0'))
                  }
                  return Promise.resolve()
                },
              },
            ]}
          >
            <InputNumber
              min={1}
              precision={0}
              addonAfter="VND"
              style={{ width: '100%' }}
              formatter={(value) => formatAmountDisplay(value)}
              parser={(value) => parseAmountInput(value) as unknown as 1}
            />
          </Form.Item>

          <Form.Item
            label="Nội dung"
            name="description"
            rules={[{ required: true, message: 'Vui lòng nhập nội dung' }]}
          >
            <Input.TextArea
              rows={3}
              placeholder={type === 'TRANSFER' ? 'Mô tả lần chuyển tiền' : 'Mô tả giao dịch'}
              onChange={() => {
                descriptionTouchedRef.current = true
              }}
            />
          </Form.Item>

          <Collapse
            ghost
            items={[
              {
                key: 'more',
                label: 'Thông tin thêm',
                children: (
                  <>
                    {type !== 'TRANSFER' ? (
                      <Form.Item
                        label={type === 'EXPENSE' ? 'Người nhận / Bên nhận' : 'Người nộp'}
                        name="payerOrPayee"
                      >
                        <Input placeholder="Tùy chọn" />
                      </Form.Item>
                    ) : null}
                    <Form.Item label="Số hóa đơn / Mã chứng từ" name="referenceNo">
                      <Input placeholder="Tùy chọn" />
                    </Form.Item>
                  </>
                ),
              },
            ]}
          />

          <Divider />
          <Button type="link" onClick={handleSaveAsTemplate} style={{ paddingLeft: 0 }}>
            Lưu thành giao dịch thường dùng
          </Button>
        </Form>
      </Drawer>

      <CategoryFormModal
        open={categoryModalOpen}
        categories={categories}
        submitting={creatingCategory}
        defaultDirection={direction}
        lockDirection
        simplified
        onCancel={() => setCategoryModalOpen(false)}
        onSubmit={(payload) => {
          void handleCreateCategory(payload)
        }}
      />
    </>
  )
}
