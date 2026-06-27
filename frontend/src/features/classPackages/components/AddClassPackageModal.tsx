import { Form, Modal, Select } from 'antd'
import { useEffect } from 'react'
import type { TuitionPackage } from '../../tuitionPackages/tuitionPackageTypes'

interface AddClassPackageFormValues {
  tuitionPackageId: number
}

interface AddClassPackageModalProps {
  open: boolean
  tuitionPackages: TuitionPackage[]
  linkedTuitionPackageIds: number[]
  loading: boolean
  submitting: boolean
  onCancel: () => void
  onSubmit: (tuitionPackageId: number) => void
}

export function AddClassPackageModal({
  open,
  tuitionPackages,
  linkedTuitionPackageIds,
  loading,
  submitting,
  onCancel,
  onSubmit,
}: AddClassPackageModalProps) {
  const [form] = Form.useForm<AddClassPackageFormValues>()

  useEffect(() => {
    if (open) {
      form.resetFields()
    }
  }, [form, open])

  const options = tuitionPackages
    .filter(
      (tuitionPackage) =>
        tuitionPackage.status === 'ACTIVE' && !linkedTuitionPackageIds.includes(tuitionPackage.id),
    )
    .map((tuitionPackage) => ({
      label: `${tuitionPackage.name} - ${tuitionPackage.totalSessions} buổi - ${formatMoney(
        tuitionPackage.price,
      )}`,
      value: tuitionPackage.id,
    }))

  function handleFinish(values: AddClassPackageFormValues) {
    onSubmit(values.tuitionPackageId)
  }

  return (
    <Modal
      title="Thêm gói học phí cho lớp"
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText="Thêm"
      cancelText="Hủy"
      destroyOnHidden
    >
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          label="Gói học phí"
          name="tuitionPackageId"
          rules={[{ required: true, message: 'Vui lòng chọn gói học phí' }]}
        >
          <Select
            showSearch
            loading={loading}
            options={options}
            optionFilterProp="label"
            placeholder="Chọn gói học phí đang áp dụng"
            notFoundContent="Không có gói học phí phù hợp"
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}

function formatMoney(value: number) {
  return new Intl.NumberFormat('vi-VN').format(value)
}
