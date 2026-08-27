import { Alert, DatePicker, Descriptions, Form, Input, message, Modal, Select, Typography } from 'antd'
import { isAxiosError } from 'axios'
import dayjs, { type Dayjs } from 'dayjs'
import { useEffect, useMemo } from 'react'
import type { ApiErrorResponse } from '../../../api/apiResponse'
import { useClassSessions } from '../../classSessions/classSessionQueries'
import { isDateMatchingDaysOfWeek } from '../../classrooms/classroomScheduleUtils'
import type { ClassDayOfWeek } from '../../classrooms/classroomTypes'
import { formatDaysOfWeek } from '../../classrooms/classroomTypes'
import { useClassroomDetail, useClassrooms } from '../../classrooms/classroomQueries'
import { renderClassStudyDayCell } from '../../classrooms/classStudyDatePicker'
import {
  useCancelEnrollment,
  useChangeLearningStartDate,
  useEnrollmentLifecycleContext,
  usePauseEnrollment,
  useReactivateEnrollment,
  useTransferEnrollment,
} from '../enrollmentQueries'

export type EnrollmentLifecycleAction =
  | 'pause'
  | 'reactivate'
  | 'changeStartDate'
  | 'transfer'
  | 'cancel'

export interface EnrollmentLifecycleTarget {
  id: number
  studentCode: string
  studentName: string
  classroomId: number
  classroomName: string
  remainingSessions: number
  startDate?: string | null
  status?: 'ACTIVE' | 'ON_HOLD' | 'STOPPED' | 'TRANSFERRED' | 'CANCELED'
}

interface EnrollmentLifecycleFormValues {
  pauseStatus?: 'ON_HOLD' | 'STOPPED'
  effectiveDate?: Dayjs | null
  expectedReturnDate?: Dayjs | null
  learningStartDate?: Dayjs | null
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
  pause: 'Tạm nghỉ',
  reactivate: 'Học lại',
  changeStartDate: 'Chỉnh sửa',
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
  const classroomQuery = useClassroomDetail(enrollment?.classroomId ?? Number.NaN)
  const targetClassroomId = Form.useWatch('targetClassroomId', form)
  const pauseStatus = Form.useWatch('pauseStatus', form)
  const pauseEnrollment = usePauseEnrollment()
  const changeLearningStartDate = useChangeLearningStartDate()
  const reactivateEnrollment = useReactivateEnrollment()
  const transferEnrollment = useTransferEnrollment()
  const cancelEnrollment = useCancelEnrollment()
  const needsLifecycleContext =
    action === 'pause' || action === 'reactivate' || action === 'changeStartDate'
  const lifecycleContextQuery = useEnrollmentLifecycleContext(
    enrollment?.id,
    open && needsLifecycleContext,
  )
  const upcomingSessionsQuery = useClassSessions(
    {
      classroomId: enrollment?.classroomId,
      fromDate: dayjs().format('YYYY-MM-DD'),
      page: 0,
      size: 8,
      sort: 'sessionDate',
      direction: 'ASC',
    },
    open && action === 'reactivate' && enrollment != null,
  )

  const latestAttendanceDate = lifecycleContextQuery.data?.latestAttendanceDate ?? null
  const earliestInactiveDate = lifecycleContextQuery.data?.earliestInactiveDate ?? null
  const inactiveFrom = lifecycleContextQuery.data?.inactiveFrom ?? null
  const currentLearningStartDate =
    lifecycleContextQuery.data?.learningStartDate ?? enrollment?.startDate ?? null
  const earliestValidAttendanceDate =
    lifecycleContextQuery.data?.earliestValidAttendanceDate ?? null
  const firstPeriodEndDate = lifecycleContextQuery.data?.firstPeriodEndDate ?? null
  const upcomingSessionDates = useMemo(
    () =>
      (upcomingSessionsQuery.data?.data?.content ?? [])
        .filter((session) => session.status !== 'CANCELED')
        .map((session) => session.sessionDate),
    [upcomingSessionsQuery.data?.data?.content],
  )

  const currentClassroom = classroomQuery.data
  const classDaysOfWeek = currentClassroom?.daysOfWeek ?? []
  const classStudyDayCellRender = useMemo(
    () => renderClassStudyDayCell(classDaysOfWeek),
    [classDaysOfWeek],
  )
  const classScheduleLabel = classDaysOfWeek.length > 0 ? formatDaysOfWeek(classDaysOfWeek) : null

  const targetClassroom = useMemo(
    () => (classroomsQuery.data?.data ?? []).find((classroom) => classroom.id === targetClassroomId),
    [classroomsQuery.data?.data, targetClassroomId],
  )
  const targetDaysOfWeek = targetClassroom?.daysOfWeek ?? []
  const targetStudyDayCellRender = useMemo(
    () => renderClassStudyDayCell(targetDaysOfWeek),
    [targetDaysOfWeek],
  )
  const targetClassrooms = useMemo(
    () =>
      (classroomsQuery.data?.data ?? []).filter(
        (classroom) =>
          classroom.id !== enrollment?.classroomId &&
          (classroom.status === 'PLANNED' || classroom.status === 'ONGOING'),
      ),
    [classroomsQuery.data?.data, enrollment?.classroomId],
  )

  const pauseStatusOptions =
    enrollment?.status === 'ON_HOLD'
      ? [{ value: 'STOPPED' as const, label: 'Ngừng học' }]
      : [
          { value: 'ON_HOLD' as const, label: 'Bảo lưu' },
          { value: 'STOPPED' as const, label: 'Ngừng học' },
        ]

  useEffect(() => {
    if (!open) {
      return
    }

    form.resetFields()
    if (action === 'transfer') {
      form.setFieldsValue({
        targetLearningStartDate: dayjs(),
      })
    }
    if (action === 'pause') {
      form.setFieldsValue({
        pauseStatus: enrollment?.status === 'ON_HOLD' ? 'STOPPED' : undefined,
      })
    }
    if (action === 'changeStartDate' && enrollment?.startDate) {
      form.setFieldsValue({
        learningStartDate: dayjs(enrollment.startDate),
      })
    }
  }, [form, open, action, enrollment?.id, enrollment?.startDate, enrollment?.status])

  useEffect(() => {
    if (!open || action !== 'changeStartDate' || !currentLearningStartDate) {
      return
    }
    form.setFieldsValue({
      learningStartDate: dayjs(currentLearningStartDate),
    })
  }, [form, open, action, currentLearningStartDate])

  const submitting =
    pauseEnrollment.isPending ||
    changeLearningStartDate.isPending ||
    reactivateEnrollment.isPending ||
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

    if (action === 'pause') {
      if (!values.pauseStatus || !values.effectiveDate) {
        return
      }
      pauseEnrollment.mutate(
        {
          id: enrollment.id,
          payload: {
            status: values.pauseStatus,
            effectiveDate: values.effectiveDate.format('YYYY-MM-DD'),
            expectedReturnDate:
              values.pauseStatus === 'ON_HOLD'
                ? (values.expectedReturnDate?.format('YYYY-MM-DD') ?? null)
                : null,
            reason,
          },
        },
        {
          onSuccess: () =>
            finishSuccess(values.pauseStatus === 'ON_HOLD' ? 'Đã bảo lưu' : 'Đã ngừng học'),
          onError: showErrorMessage,
        },
      )
      return
    }

    if (action === 'changeStartDate') {
      if (!values.learningStartDate) {
        return
      }
      changeLearningStartDate.mutate(
        {
          id: enrollment.id,
          payload: {
            learningStartDate: values.learningStartDate.format('YYYY-MM-DD'),
            reason: reason || null,
          },
        },
        {
          onSuccess: () => finishSuccess('Đã chỉnh sửa ngày bắt đầu học'),
          onError: showErrorMessage,
        },
      )
      return
    }

    if (action === 'reactivate') {
      if (!values.effectiveDate) {
        return
      }
      reactivateEnrollment.mutate(
        {
          id: enrollment.id,
          payload: {
            effectiveDate: values.effectiveDate.format('YYYY-MM-DD'),
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
      { id: enrollment.id, payload: { reason } },
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
          {action === 'changeStartDate' && currentLearningStartDate ? (
            <Descriptions.Item label="Ngày bắt đầu học hiện tại">
              {formatDate(currentLearningStartDate)}
            </Descriptions.Item>
          ) : null}
        </Descriptions>
      ) : null}

      {action === 'cancel' ? (
        <Alert
          type="error"
          showIcon
          message="Hành động này chỉ dành cho ghi danh tạo nhầm. Dữ liệu sẽ được hủy nhưng vẫn giữ lịch sử."
          style={{ marginBottom: 16 }}
        />
      ) : null}

      {action === 'pause' ? (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message={
            latestAttendanceDate
              ? `Buổi đã điểm danh gần nhất: ${formatDate(latestAttendanceDate)}. Ngày nghỉ sớm nhất có thể chọn: ${formatDate(earliestInactiveDate ?? latestAttendanceDate)}.`
              : 'Ngày bắt đầu nghỉ phải sau buổi điểm danh còn hiệu lực gần nhất. Hệ thống không tự xóa dữ liệu điểm danh cũ.'
          }
        />
      ) : null}

      {action === 'changeStartDate' ? (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="Chỉnh sửa sẽ thay đổi các buổi học viên xuất hiện trên điểm danh."
          description={
            earliestValidAttendanceDate
              ? `Điểm danh còn hiệu lực sớm nhất: ${formatDate(earliestValidAttendanceDate)}. Không thể chọn ngày sau ngày này, trừ khi hoàn tác các buổi điểm danh đó.`
              : 'Chỉ các buổi điểm danh còn hiệu lực (chưa hoàn tác) mới chặn việc dời ngày bắt đầu học sang sau.'
          }
        />
      ) : null}

      {action === 'reactivate' && inactiveFrom ? (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message={`Đã nghỉ từ: ${formatDate(inactiveFrom)}`}
          description={
            upcomingSessionDates.length > 0 ? (
              <Typography.Text type="secondary">
                Ngày học sắp tới: {upcomingSessionDates.map((date) => formatDate(date)).join(', ')}
              </Typography.Text>
            ) : null
          }
        />
      ) : null}

      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        {action === 'pause' ? (
          <>
            <Form.Item
              label="Trạng thái"
              name="pauseStatus"
              rules={[{ required: true, message: 'Vui lòng chọn trạng thái' }]}
            >
              <Select
                placeholder="Chọn Bảo lưu hoặc Ngừng học"
                options={pauseStatusOptions}
              />
            </Form.Item>
            <Form.Item
              label="Ngày bắt đầu nghỉ"
              name="effectiveDate"
              rules={[{ required: true, message: 'Vui lòng chọn ngày bắt đầu nghỉ' }]}
            >
              <DatePicker
                format="DD/MM/YYYY"
                style={{ width: '100%' }}
                disabledDate={(current) => disableInactiveDate(current, latestAttendanceDate)}
              />
            </Form.Item>
            {pauseStatus === 'ON_HOLD' ? (
              <Form.Item label="Ngày dự kiến học lại" name="expectedReturnDate">
                <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
              </Form.Item>
            ) : null}
          </>
        ) : null}

        {action === 'changeStartDate' ? (
          <Form.Item
            label="Ngày bắt đầu học"
            name="learningStartDate"
            rules={[{ required: true, message: 'Vui lòng chọn ngày bắt đầu học' }]}
            extra={
              classScheduleLabel
                ? `Ngày học của lớp: ${classScheduleLabel}. Các ngày này được tô sáng trên lịch.`
                : undefined
            }
          >
            <DatePicker
              format="DD/MM/YYYY"
              style={{ width: '100%' }}
              cellRender={classStudyDayCellRender}
              disabledDate={(current) =>
                disableLearningStartDate(
                  current,
                  currentClassroom?.startDate,
                  classDaysOfWeek,
                  earliestValidAttendanceDate,
                  firstPeriodEndDate,
                )
              }
            />
          </Form.Item>
        ) : null}

        {action === 'reactivate' ? (
          <Form.Item
            label="Ngày bắt đầu học lại"
            name="effectiveDate"
            rules={[{ required: true, message: 'Vui lòng chọn ngày bắt đầu học lại' }]}
            extra={
              classScheduleLabel
                ? `Ngày học của lớp: ${classScheduleLabel}. Các ngày này được tô sáng trên lịch.`
                : undefined
            }
          >
            <DatePicker
              format="DD/MM/YYYY"
              style={{ width: '100%' }}
              cellRender={classStudyDayCellRender}
              disabledDate={(current) =>
                disableReactivateDate(
                  current,
                  inactiveFrom,
                  currentClassroom?.startDate,
                  classDaysOfWeek,
                )
              }
            />
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
              extra={
                targetDaysOfWeek.length > 0
                  ? `Ngày học của lớp chuyển đến: ${formatDaysOfWeek(targetDaysOfWeek)}. Các ngày này được tô sáng trên lịch.`
                  : 'Chọn lớp chuyển đến để thấy ngày học cố định'
              }
            >
              <DatePicker
                format="DD/MM/YYYY"
                style={{ width: '100%' }}
                cellRender={targetStudyDayCellRender}
                disabledDate={(current) =>
                  disableNonClassStudyDate(
                    current,
                    targetClassroom?.startDate,
                    targetDaysOfWeek,
                  )
                }
              />
            </Form.Item>
          </>
        ) : null}

        <Form.Item
          label="Lý do"
          name="reason"
          rules={
            action === 'reactivate' || action === 'changeStartDate'
              ? undefined
              : [{ required: true, whitespace: true, message: 'Vui lòng nhập lý do' }]
          }
        >
          <Input.TextArea
            rows={3}
            placeholder={
              action === 'reactivate' || action === 'changeStartDate'
                ? 'Nhập lý do (không bắt buộc)'
                : 'Nhập lý do'
            }
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}

function formatDate(value: string) {
  return dayjs(value).format('DD/MM/YYYY')
}

function disableInactiveDate(current: Dayjs, latestAttendanceDate?: string | null) {
  if (!latestAttendanceDate) {
    return false
  }
  return !current.isAfter(dayjs(latestAttendanceDate), 'day')
}

function disableLearningStartDate(
  current: Dayjs,
  classroomStartDate?: string,
  daysOfWeek: ClassDayOfWeek[] = [],
  earliestValidAttendanceDate?: string | null,
  firstPeriodEndDate?: string | null,
) {
  if (disableNonClassStudyDate(current, classroomStartDate, daysOfWeek)) {
    return true
  }
  if (earliestValidAttendanceDate && current.isAfter(dayjs(earliestValidAttendanceDate), 'day')) {
    return true
  }
  if (firstPeriodEndDate && !current.isBefore(dayjs(firstPeriodEndDate), 'day')) {
    return true
  }
  return false
}

function disableReactivateDate(
  current: Dayjs,
  inactiveFrom?: string | null,
  classroomStartDate?: string,
  daysOfWeek: ClassDayOfWeek[] = [],
) {
  if (inactiveFrom && !current.isAfter(dayjs(inactiveFrom), 'day')) {
    return true
  }

  return disableNonClassStudyDate(current, classroomStartDate, daysOfWeek)
}

function disableNonClassStudyDate(
  current: Dayjs,
  classroomStartDate?: string,
  daysOfWeek: ClassDayOfWeek[] = [],
) {
  if (classroomStartDate && current.isBefore(dayjs(classroomStartDate), 'day')) {
    return true
  }

  if (daysOfWeek.length === 0) {
    return false
  }

  return !isDateMatchingDaysOfWeek(current, daysOfWeek)
}

function showErrorMessage(error: unknown) {
  if (isAxiosError(error)) {
    const data = error.response?.data as ApiErrorResponse | undefined
    const fieldMessage = data?.errors?.[0]?.message
    const text =
      data?.message && data.message !== 'Validation failed'
        ? data.message
        : (fieldMessage ?? 'Có lỗi xảy ra')
    message.error({
      content: <span style={{ whiteSpace: 'pre-line' }}>{text}</span>,
      duration: 8,
    })
    return
  }

  message.error('Có lỗi xảy ra')
}
