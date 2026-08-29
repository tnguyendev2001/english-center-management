import { DatePicker, Form, Input, Modal, message } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { isAxiosError } from 'axios'
import { useAdjustPackagePeriodStart } from '../../studentPackages/studentPackageQueries'
import type { StudentPackagePeriod } from '../../studentPackages/studentPackageTypes'

interface AdjustmentFormValues {
  periodStartDate: Dayjs
  reason: string
}

interface AdjustPackagePeriodStartModalProps {
  period: StudentPackagePeriod
  onClose: () => void
}

export function AdjustPackagePeriodStartModal({
  period,
  onClose,
}: AdjustPackagePeriodStartModalProps) {
  const [form] = Form.useForm<AdjustmentFormValues>()
  const adjustPeriod = useAdjustPackagePeriodStart(period.studentPackageId)

  async function submit() {
    const values = await form.validateFields()
    adjustPeriod.mutate(
      {
        periodStartDate: values.periodStartDate.format('YYYY-MM-DD'),
        reason: values.reason.trim(),
      },
      {
        onSuccess: () => {
          message.success('Đã điều chỉnh ngày bắt đầu kỳ học phí')
          onClose()
        },
        onError: (error) => {
          if (isAxiosError(error)) {
            message.error(error.response?.data?.message ?? 'Không thể điều chỉnh ngày bắt đầu kỳ')
            return
          }
          message.error('Không thể điều chỉnh ngày bắt đầu kỳ')
        },
      },
    )
  }

  return (
    <Modal
      title="Điều chỉnh ngày bắt đầu kỳ học phí"
      open
      onCancel={onClose}
      onOk={() => void submit()}
      okText="Lưu điều chỉnh"
      cancelText="Hủy"
      confirmLoading={adjustPeriod.isPending}
      destroyOnHidden
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          periodStartDate: period.effectivePeriodStartDate
            ? dayjs(period.effectivePeriodStartDate)
            : undefined,
          reason: '',
        }}
      >
        <Form.Item
          name="periodStartDate"
          label="Ngày bắt đầu kỳ học phí"
          rules={[{ required: true, message: 'Vui lòng chọn ngày bắt đầu kỳ' }]}
        >
          <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item
          name="reason"
          label="Lý do điều chỉnh"
          rules={[
            { required: true, whitespace: true, message: 'Vui lòng nhập lý do điều chỉnh' },
            { max: 1000, message: 'Lý do không được vượt quá 1000 ký tự' },
          ]}
        >
          <Input.TextArea rows={4} maxLength={1000} showCount />
        </Form.Item>
      </Form>
    </Modal>
  )
}
