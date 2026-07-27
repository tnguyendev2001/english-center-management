import { Alert, DatePicker, Descriptions, Form, Input, message, Modal, Select } from 'antd'
import { isAxiosError } from 'axios'
import dayjs, { type Dayjs } from 'dayjs'
import { useEffect, useMemo } from 'react'
import { useClassrooms } from '../../classrooms/classroomQueries'
import {
  useCancelEnrollment,
  useHoldEnrollment,
  useReactivateEnrollment,
  useStopEnrollment,
  useTransferEnrollment,
} from '../enrollmentQueries'

export type EnrollmentLifecycleAction = 'hold' | 'reactivate' | 'stop' | 'transfer' | 'cancel'

export interface EnrollmentLifecycleTarget {
  id: number
  studentCode: string
  studentName: string
  classroomId: number
  classroomName: string
  remainingSessions: number
}

interface EnrollmentLifecycleFormValues {
  effectiveDate?: Dayjs | null
  expectedReturnDate?: Dayjs | null
  targetClassroomId?: number
  targetLearningStartDate?: Dayjs | null
  reason?: string
}

interface EnrollmentLifecycleModalProps {
  open: boolean
  action: EnrollmentLifecycleAction
  enrollment?: EnrollmentLifecycleTarget
  onCancel: () => void
  onSuccess?: () => void
}

const actionLabels: Record<EnrollmentLifecycleAction, string> = {
  hold: 'Bảo lưu',
  reactivate: 'Học lại',
  stop: 'Ngừng học',
  transfer: 'Chuyển lớp',
  cancel: 'Hủy ghi danh',
}

export function EnrollmentLifecycleModal({
  open,
  action,
  enrollment,
  onCancel,
  onSuccess,
}: EnrollmentLifecycleModalProps) {
  const [form] = Form.useForm<EnrollmentLifecycleFormValues>()
  const classroomsQuery = useClassrooms({ page: 0, size: 100 })
  const holdEnrollment = useHoldEnrollment()
  const reactivateEnrollment = useReactivateEnrollment()
  const stopEnrollment = useStopEnrollment()
  const transferEnrollment = useTransferEnrollment()
  const cancelEnrollment = useCancelEnrollment()

  const targetClassrooms = useMemo(
    () =>
      (classroomsQuery.data?.data ?? []).filter(
        (classroom) =>
          classroom.id !== enrollment?.classroomId &&
          (classroom.status === 'PLANNED' || classroom.status === 'ONGOING'),
      ),
    [classroomsQuery.data?.data, enrollment?.classroomId],
  )

  useEffect(() => {
    if (!open) {
      return
    }

    form.resetFields()
    form.setFieldsValue({
      effectiveDate: dayjs(),
      targetLearningStartDate: dayjs(),
    })
  }, [form, open, action, enrollment?.id])

  const submitting =
    holdEnrollment.isPending ||
    reactivateEnrollment.isPending ||
    stopEnrollment.isPending ||
    transferEnrollment.isPending ||
    cancelEnrollment.isPending

  function finishSuccess(successMessage: string) {
    message.success(successMessage)
    onSuccess?.()
    onCancel()
  }

  function handleSubmit(values: EnrollmentLifecycleFormValues) {
    if (!enrollment) {
      return
    }

    const reason = values.reason?.trim() ?? ''

    if (action === 'hold') {
      holdEnrollment.mutate(
        {
          id: enrollment.id,
          payload: {
            effectiveDate: values.effectiveDate?.format('YYYY-MM-DD') ?? null,
            expectedReturnDate: values.expectedReturnDate?.format('YYYY-MM-DD') ?? null,
            reason,
          },
        },
        {
          onSuccess: () => finishSuccess('Đã bảo lưu'),
          onError: showErrorMessage,
        },
      )
      return
    }

    if (action === 'reactivate') {
      reactivateEnrollment.mutate(
        {
          id: enrollment.id,
          payload: {
            effectiveDate: values.effectiveDate?.format('YYYY-MM-DD') ?? null,
            reason: reason || null,
          },
        },
        {
          onSuccess: () => finishSuccess('Đã cho học lại'),
          onError: showErrorMessage,
        },
      )
      return
    }

    if (action === 'stop') {
      stopEnrollment.mutate(
        {
          id: enrollment.id,
          payload: {
            effectiveDate: values.effectiveDate?.format('YYYY-MM-DD') ?? null,
            reason,
          },
        },
        {
          onSuccess: () => finishSuccess('Đã ngừng học'),
          onError: showErrorMessage,
        },
      )
      return
    }

    if (action === 'transfer') {
      if (!values.targetClassroomId || !values.targetLearningStartDate) {
        return
      }

      transferEnrollment.mutate(
        {
          id: enrollment.id,
          payload: {
            targetClassroomId: values.targetClassroomId,
            targetLearningStartDate: values.targetLearningStartDate.format('YYYY-MM-DD'),
            reason,
          },
        },
        {
          onSuccess: (result) => {
            finishSuccess(`Đã chuyển ${result.transferredSessions} buổi sang lớp mới`)
            if (result.warningMessage) {
              message.warning(result.warningMessage)
            }
          },
          onError: showErrorMessage,
        },
      )
      return
    }

    cancelEnrollment.mutate(
      {
        id: enrollment.id,
        payload: {
          effectiveDate: values.effectiveDate?.format('YYYY-MM-DD') ?? null,
          reason,
        },
      },
      {
        onSuccess: () => finishSuccess('Đã hủy ghi danh'),
        onError: showErrorMessage,
      },
    )
  }

  return (
    <Modal
      title={actionLabels[action]}
      open={open}
      okText={actionLabels[action]}
      cancelText="Đóng"
      okButtonProps={{ danger: action === 'cancel' }}
      confirmLoading={submitting}
      onOk={() => form.submit()}
      onCancel={onCancel}
      destroyOnHidden
    >
      {enrollment ? (
        <Descriptions bordered size="small" column={1} style={{ marginBottom: 16 }}>
          <Descriptions.Item label="Học viên">
            {enrollment.studentCode} - {enrollment.studentName}
          </Descriptions.Item>
          <Descriptions.Item label="Lớp hiện tại">{enrollment.classroomName}</Descriptions.Item>
          {action === 'transfer' ? (
            <Descriptions.Item label="Số buổi còn lại">
              {enrollment.remainingSessions}
            </Descriptions.Item>
          ) : null}
        </Descriptions>
      ) : null}

      {action === 'cancel' ? (
        <Alert
          type="error"
          showIcon
          message="Hủy ghi danh sẽ đồng thời hủy các hóa đơn chưa có thanh toán của ghi danh này. Dữ liệu vẫn được giữ lại trong lịch sử."
          style={{ marginBottom: 16 }}
        />
      ) : null}

      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        {action === 'hold' || action === 'reactivate' || action === 'stop' || action === 'cancel' ? (
          <Form.Item
            label="Ngày hiệu lực"
            name="effectiveDate"
            rules={
              action === 'cancel'
                ? [{ required: true, message: 'Vui lòng chọn ngày hiệu lực' }]
                : undefined
            }
            extra={
              action === 'reactivate'
                ? 'Nếu ngày chọn không có buổi học, hệ thống sẽ lấy ngày buổi học hợp lệ gần nhất để điểm danh trở lại.'
                : undefined
            }
          >
            <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
          </Form.Item>
        ) : null}

        {action === 'hold' ? (
          <Form.Item label="Ngày dự kiến học lại" name="expectedReturnDate">
            <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
          </Form.Item>
        ) : null}

        {action === 'transfer' ? (
          <>
            <Form.Item
              label="Lớp chuyển đến"
              name="targetClassroomId"
              rules={[{ required: true, message: 'Vui lòng chọn lớp chuyển đến' }]}
            >
              <Select
                showSearch
                optionFilterProp="label"
                loading={classroomsQuery.isLoading}
                placeholder="Chọn lớp đang hoặc sắp diễn ra"
                options={targetClassrooms.map((classroom) => ({
                  value: classroom.id,
                  label: `${classroom.classCode} - ${classroom.className}`,
                }))}
              />
            </Form.Item>
            <Form.Item
              label="Ngày bắt đầu học"
              name="targetLearningStartDate"
              rules={[{ required: true, message: 'Vui lòng chọn ngày bắt đầu học' }]}
            >
              <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
            </Form.Item>
          </>
        ) : null}

        <Form.Item
          label={action === 'cancel' ? 'Lý do hủy' : 'Lý do'}
          name="reason"
          rules={
            action === 'reactivate'
              ? undefined
              : [{ required: true, whitespace: true, message: 'Vui lòng nhập lý do' }]
          }
        >
          <Input.TextArea
            rows={3}
            placeholder={
              action === 'reactivate'
                ? 'Nhập lý do (không bắt buộc)'
                : action === 'cancel'
                  ? 'Ví dụ: Tạo nhầm ghi danh'
                  : 'Nhập lý do'
            }
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}

function showErrorMessage(error: unknown) {
  if (isAxiosError(error)) {
    message.error(error.response?.data?.message ?? 'Có lỗi xảy ra')
    return
  }

  message.error('Có lỗi xảy ra')
}
