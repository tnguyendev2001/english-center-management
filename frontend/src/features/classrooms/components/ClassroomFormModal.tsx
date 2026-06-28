import { Alert, DatePicker, Form, Input, Modal, Select, Space, TimePicker } from 'antd'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { useEffect, useMemo } from 'react'
import type { Classroom, ClassroomPayload, ClassroomStatus, ClassDayOfWeek } from '../classroomTypes'
import { CLASS_DAY_OF_WEEK_OPTIONS } from '../classroomTypes'
import {
  CLASSROOM_DAYS_OF_WEEK_REQUIRED_MESSAGE,
  CLASSROOM_START_DATE_MISMATCH_MESSAGE,
  disableInvalidClassroomStartDates,
  isDateMatchingDaysOfWeek,
} from '../classroomScheduleUtils'

interface ClassroomFormValues {
  classCode: string
  className: string
  level: string
  teacherName: string
  room?: string
  startDate: Dayjs
  expectedEndDate?: Dayjs
  daysOfWeek: ClassDayOfWeek[]
  startTime: Dayjs
  endTime: Dayjs
  status: ClassroomStatus
  note?: string
}

interface ClassroomFormModalProps {
  open: boolean
  initialClassroom?: Classroom
  submitting: boolean
  onCancel: () => void
  onSubmit: (payload: ClassroomPayload) => void
}

const statusOptions: { label: string; value: ClassroomStatus }[] = [
  { label: 'Dự kiến', value: 'PLANNED' },
  { label: 'Đang học', value: 'ONGOING' },
  { label: 'Hoàn thành', value: 'COMPLETED' },
  { label: 'Đã hủy', value: 'CANCELED' },
]

export function ClassroomFormModal({
  open,
  initialClassroom,
  submitting,
  onCancel,
  onSubmit,
}: ClassroomFormModalProps) {
  const [form] = Form.useForm<ClassroomFormValues>()
  const isEdit = Boolean(initialClassroom)
  const daysOfWeek = Form.useWatch('daysOfWeek', form) ?? []
  const startDate = Form.useWatch('startDate', form)

  const disableStartDate = useMemo(
    () => disableInvalidClassroomStartDates(daysOfWeek),
    [daysOfWeek],
  )
  const startDateMismatch = useMemo(() => {
    if (!startDate || daysOfWeek.length === 0) {
      return false
    }

    return !isDateMatchingDaysOfWeek(startDate, daysOfWeek)
  }, [daysOfWeek, startDate])

  useEffect(() => {
    if (!open) {
      return
    }

    if (initialClassroom) {
      form.setFieldsValue({
        classCode: initialClassroom.classCode,
        className: initialClassroom.className,
        level: initialClassroom.level,
        teacherName: initialClassroom.teacherName,
        room: initialClassroom.room ?? undefined,
        startDate: dayjs(initialClassroom.startDate),
        expectedEndDate: initialClassroom.expectedEndDate
          ? dayjs(initialClassroom.expectedEndDate)
          : undefined,
        daysOfWeek: initialClassroom.daysOfWeek,
        startTime: parseTime(initialClassroom.startTime),
        endTime: parseTime(initialClassroom.endTime),
        status: initialClassroom.status,
        note: initialClassroom.note ?? undefined,
      })
      return
    }

    form.resetFields()
    form.setFieldValue('status', 'PLANNED')
  }, [form, initialClassroom, open])

  useEffect(() => {
    if (!open || !startDate || daysOfWeek.length === 0) {
      return
    }

    if (!isDateMatchingDaysOfWeek(startDate, daysOfWeek)) {
      form.setFieldValue('startDate', undefined)
    }
  }, [daysOfWeek, form, open, startDate])

  function handleFinish(values: ClassroomFormValues) {
    onSubmit({
      classCode: values.classCode,
      className: values.className,
      level: values.level,
      teacherName: values.teacherName,
      room: values.room ?? null,
      startDate: values.startDate.format('YYYY-MM-DD'),
      expectedEndDate: values.expectedEndDate?.format('YYYY-MM-DD') ?? null,
      daysOfWeek: values.daysOfWeek,
      startTime: values.startTime.format('HH:mm:ss'),
      endTime: values.endTime.format('HH:mm:ss'),
      status: values.status,
      note: values.note ?? null,
    })
  }

  return (
    <Modal
      title={isEdit ? 'Cập nhật lớp học' : 'Thêm lớp học'}
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText={isEdit ? 'Cập nhật' : 'Thêm mới'}
      cancelText="Hủy"
      destroyOnHidden
      width={720}
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={handleFinish}
        initialValues={{ status: 'PLANNED' }}
      >
        <Space size="middle" style={{ width: '100%' }} align="start">
          <Form.Item
            label="Mã lớp"
            name="classCode"
            rules={[{ required: true, message: 'Vui lòng nhập mã lớp' }]}
            style={{ width: 320 }}
          >
            <Input placeholder="VD: CLS001" />
          </Form.Item>

          <Form.Item
            label="Tên lớp"
            name="className"
            rules={[{ required: true, message: 'Vui lòng nhập tên lớp' }]}
            style={{ width: 320 }}
          >
            <Input placeholder="VD: Starter A" />
          </Form.Item>
        </Space>

        <Space size="middle" style={{ width: '100%' }} align="start">
          <Form.Item
            label="Trình độ"
            name="level"
            rules={[{ required: true, message: 'Vui lòng nhập trình độ' }]}
            style={{ width: 320 }}
          >
            <Input placeholder="VD: Starter, Movers, IELTS 5.0" />
          </Form.Item>

          <Form.Item
            label="Giáo viên"
            name="teacherName"
            rules={[{ required: true, message: 'Vui lòng nhập tên giáo viên' }]}
            style={{ width: 320 }}
          >
            <Input placeholder="Nhập tên giáo viên" />
          </Form.Item>
        </Space>

        <Space size="middle" style={{ width: '100%' }} align="start">
          <Form.Item label="Phòng học" name="room" style={{ width: 320 }}>
            <Input placeholder="VD: Phòng 1" />
          </Form.Item>

          <Form.Item
            label="Trạng thái"
            name="status"
            rules={[{ required: true, message: 'Vui lòng chọn trạng thái' }]}
            style={{ width: 320 }}
          >
            <Select options={statusOptions} />
          </Form.Item>
        </Space>

        <Form.Item
          label="Ngày học trong tuần"
          name="daysOfWeek"
          rules={[{ required: true, message: CLASSROOM_DAYS_OF_WEEK_REQUIRED_MESSAGE }]}
        >
          <Select
            mode="multiple"
            placeholder="Chọn ngày học trong tuần"
            options={CLASS_DAY_OF_WEEK_OPTIONS}
          />
        </Form.Item>

        {startDateMismatch ? (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
            message={CLASSROOM_START_DATE_MISMATCH_MESSAGE}
          />
        ) : null}

        <Space size="middle" style={{ width: '100%' }} align="start">
          <Form.Item
            label="Ngày bắt đầu"
            name="startDate"
            rules={[
              { required: true, message: 'Vui lòng chọn ngày bắt đầu' },
              {
                validator: async (_, value?: Dayjs) => {
                  if (!value) {
                    return
                  }

                  if (daysOfWeek.length === 0) {
                    throw new Error(CLASSROOM_DAYS_OF_WEEK_REQUIRED_MESSAGE)
                  }

                  if (!isDateMatchingDaysOfWeek(value, daysOfWeek)) {
                    throw new Error(CLASSROOM_START_DATE_MISMATCH_MESSAGE)
                  }
                },
              },
            ]}
            extra="Ngày bắt đầu phải là buổi học đầu tiên và trùng lịch học đã chọn"
            style={{ width: 320 }}
          >
            <DatePicker
              format="DD/MM/YYYY"
              style={{ width: '100%' }}
              disabled={daysOfWeek.length === 0}
              disabledDate={disableStartDate}
            />
          </Form.Item>

          <Form.Item
            label="Ngày kết thúc dự kiến"
            name="expectedEndDate"
            rules={[
              {
                validator: async (_, value?: Dayjs) => {
                  const selectedStartDate = form.getFieldValue('startDate') as Dayjs | undefined
                  if (value && selectedStartDate && value.isBefore(selectedStartDate, 'day')) {
                    throw new Error('Ngày kết thúc dự kiến không được trước ngày bắt đầu.')
                  }
                },
              },
            ]}
            style={{ width: 320 }}
          >
            <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
          </Form.Item>
        </Space>

        <Space size="middle" style={{ width: '100%' }} align="start">
          <Form.Item
            label="Giờ bắt đầu"
            name="startTime"
            rules={[{ required: true, message: 'Vui lòng chọn giờ bắt đầu' }]}
            style={{ width: 320 }}
          >
            <TimePicker format="HH:mm" style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            label="Giờ kết thúc"
            name="endTime"
            rules={[
              { required: true, message: 'Vui lòng chọn giờ kết thúc' },
              {
                validator: async (_, value?: Dayjs) => {
                  const selectedStartTime = form.getFieldValue('startTime') as Dayjs | undefined
                  if (value && selectedStartTime && !value.isAfter(selectedStartTime)) {
                    throw new Error('Giờ kết thúc phải sau giờ bắt đầu.')
                  }
                },
              },
            ]}
            style={{ width: 320 }}
          >
            <TimePicker format="HH:mm" style={{ width: '100%' }} />
          </Form.Item>
        </Space>

        <Form.Item label="Ghi chú" name="note">
          <Input.TextArea rows={3} placeholder="Ghi chú thêm nếu có" />
        </Form.Item>
      </Form>
    </Modal>
  )
}

function parseTime(value: string) {
  return dayjs(`2026-01-01T${value}`)
}
