import { Form, Input, InputNumber, Modal, Select } from 'antd'
import { useEffect, useMemo } from 'react'
import type {
  CategoryDirection,
  CategoryPayload,
  TransactionCategory,
} from '../financeTypes'

interface CategoryFormValues {
  code: string
  name: string
  direction: CategoryDirection
  parentId?: number
  displayOrder?: number
}

interface CategoryFormModalProps {
  open: boolean
  initialCategory?: TransactionCategory
  categories: TransactionCategory[]
  submitting: boolean
  defaultDirection?: CategoryDirection
  lockDirection?: boolean
  simplified?: boolean
  onCancel: () => void
  onSubmit: (payload: CategoryPayload) => void
}

const directionOptions: { label: string; value: CategoryDirection }[] = [
  { label: 'Thu', value: 'INCOME' },
  { label: 'Chi', value: 'EXPENSE' },
]

function slugCode(name: string, direction: CategoryDirection) {
  const base = name
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_|_$/g, '')
    .slice(0, 24)
  const prefix = direction === 'INCOME' ? 'INC' : 'EXP'
  return `${prefix}_${base || 'NEW'}`
}

export function CategoryFormModal({
  open,
  initialCategory,
  categories,
  submitting,
  defaultDirection = 'EXPENSE',
  lockDirection = false,
  simplified = false,
  onCancel,
  onSubmit,
}: CategoryFormModalProps) {
  const [form] = Form.useForm<CategoryFormValues>()
  const isEdit = Boolean(initialCategory)
  const direction = Form.useWatch('direction', form)

  const parentOptions = useMemo(
    () =>
      categories
        .filter(
          (category) =>
            category.active &&
            category.direction === direction &&
            category.id !== initialCategory?.id,
        )
        .map((category) => ({
          label: `${category.code} - ${category.name}`,
          value: category.id,
        })),
    [categories, direction, initialCategory?.id],
  )

  useEffect(() => {
    if (!open) {
      return
    }

    if (initialCategory) {
      form.setFieldsValue({
        code: initialCategory.code,
        name: initialCategory.name,
        direction: initialCategory.direction,
        parentId: initialCategory.parentId ?? undefined,
        displayOrder: initialCategory.displayOrder,
      })
      return
    }

    form.resetFields()
    form.setFieldsValue({
      direction: defaultDirection,
      displayOrder: 0,
    })
  }, [form, initialCategory, open, defaultDirection])

  function handleFinish(values: CategoryFormValues) {
    const code = values.code?.trim() || slugCode(values.name, values.direction)
    onSubmit({
      code,
      name: values.name,
      direction: values.direction,
      parentId: values.parentId ?? null,
      displayOrder: values.displayOrder ?? null,
    })
  }

  return (
    <Modal
      title={isEdit ? 'Cập nhật danh mục' : 'Thêm danh mục'}
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
          label="Tên danh mục"
          name="name"
          rules={[{ required: true, message: 'Vui lòng nhập tên danh mục' }]}
        >
          <Input
            placeholder="VD: Tiền thuê mặt bằng"
            onBlur={(event) => {
              if (isEdit || form.getFieldValue('code')) {
                return
              }
              const currentDirection = form.getFieldValue('direction') ?? defaultDirection
              form.setFieldValue('code', slugCode(event.target.value, currentDirection))
            }}
          />
        </Form.Item>

        <Form.Item
          label="Mã danh mục"
          name="code"
          rules={
            simplified && !isEdit
              ? []
              : [{ required: true, message: 'Vui lòng nhập mã danh mục' }]
          }
          hidden={simplified && !isEdit}
        >
          <Input placeholder="VD: EXP_RENT" disabled={isEdit && initialCategory?.systemCategory} />
        </Form.Item>

        <Form.Item
          label="Loại"
          name="direction"
          rules={[{ required: true, message: 'Vui lòng chọn loại' }]}
        >
          <Select
            options={directionOptions}
            disabled={lockDirection || (isEdit && initialCategory?.systemCategory)}
            onChange={() => form.setFieldValue('parentId', undefined)}
          />
        </Form.Item>

        {!simplified ? (
          <>
            <Form.Item label="Danh mục cha" name="parentId">
              <Select allowClear options={parentOptions} placeholder="Không có" />
            </Form.Item>

            <Form.Item label="Thứ tự hiển thị" name="displayOrder">
              <InputNumber min={0} precision={0} style={{ width: '100%' }} />
            </Form.Item>
          </>
        ) : null}
      </Form>
    </Modal>
  )
}
