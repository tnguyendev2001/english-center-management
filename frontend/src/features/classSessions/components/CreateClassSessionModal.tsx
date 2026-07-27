import { Alert, DatePicker, Form, Input, Modal, Space, TimePicker, Typography } from 'antd'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { useEffect, useMemo } from 'react'
import type { Classroom } from '../../classrooms/classroomTypes'
import { formatDaysOfWeek } from '../../classrooms/classroomTypes'
import {
  disableInvalidClassroomStartDates,
  isDateMatchingDaysOfWeek,
} from '../../classrooms/classroomScheduleUtils'
import type { CreateClassSessionPayload } from '../classSessionTypes'

const { Text } = Typography

interface CreateClassSessionFormValues {
  sessionDate: Dayjs
  startTime: Dayjs
  endTime: Dayjs
  note?: string
}

interface CreateClassSessionModalProps {
  open: boolean
  classroom?: Classroom
  submitting: boolean
  onCancel: () => void
  onSubmit: (payload: CreateClassSessionPayload) => void
}

export function CreateClassSessionModal({
  open,
  classroom,
  submitting,
  onCancel,
  onSubmit,
}: CreateClassSessionModalProps) {
  const [form] = Form.useForm<CreateClassSessionFormValues>()
  const daysOfWeek = classroom?.daysOfWeek ?? []

  const disableDate = useMemo(() => {
    const weekdayDisabled = disableInvalidClassroomStartDates(daysOfWeek)
    const today = dayjs().startOf('day')
    const classStart = classroom?.startDate ? dayjs(classroom.startDate).startOf('day') : today

    return (current: Dayjs) => {
      if (!current) {
        return false
      }
      if (current.isBefore(today, 'day')) {
        return true
      }
      if (current.isBefore(classStart, 'day')) {
        return true
      }
      return weekdayDisabled(current)
    }
  }, [classroom?.startDate, daysOfWeek])

  useEffect(() => {
    if (!open || !classroom) {
      return
    }

    const nextDate = findNextValidStudyDate(classroom)
    form.setFieldsValue({
      sessionDate: nextDate,
      startTime: parseTime(classroom.startTime),
      endTime: parseTime(classroom.endTime),
      note: undefined,
    })
  }, [classroom, form, open])

  function handleFinish(values: CreateClassSessionFormValues) {
    if (!classroom) {
      return
    }

    onSubmit({
      classroomId: classroom.id,
      sessionDate: values.sessionDate.format('YYYY-MM-DD'),
      startTime: values.startTime.format('HH:mm:ss'),
      endTime: values.endTime.format('HH:mm:ss'),
      note: values.note?.trim() || null,
    })
  }

  return (
    <Modal
      title="Tạo buổi học"
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText="Tạo buổi học"
      cancelText="Hủy"
      destroyOnHidden
      width={560}
    >
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item label="Lớp học">
          <Input
            value={classroom ? `${classroom.classCode} - ${classroom.className}` : ''}
            disabled
          />
        </Form.Item>

        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message={`Lịch học: ${formatDaysOfWeek(daysOfWeek)}`}
          description={
            classroom?.room
              ? `Phòng mặc định: ${classroom.room}`
              : 'Ngày học phải trùng lịch của lớp.'
          }
        />

        <Form.Item
          label="Ngày học"
          name="sessionDate"
          rules={[
            { required: true, message: 'Vui lòng chọn ngày học' },
            {
              validator: async (_, value?: Dayjs) => {
                if (!value) {
                  return
                }
                if (value.isBefore(dayjs(), 'day')) {
                  throw new Error('Không thể tạo buổi học trong quá khứ.')
                }
                if (!isDateMatchingDaysOfWeek(value, daysOfWeek)) {
                  throw new Error('Ngày học không khớp với lịch học của lớp.')
                }
              },
            },
          ]}
        >
          <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} disabledDate={disableDate} />
        </Form.Item>

        <Space style={{ width: '100%' }} size="middle" align="start">
          <Form.Item
            label="Giờ bắt đầu"
            name="startTime"
            rules={[{ required: true, message: 'Vui lòng chọn giờ bắt đầu' }]}
            style={{ width: 240 }}
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
                  const start = form.getFieldValue('startTime') as Dayjs | undefined
                  if (value && start && !value.isAfter(start)) {
                    throw new Error('Giờ kết thúc phải sau giờ bắt đầu.')
                  }
                },
              },
            ]}
            style={{ width: 240 }}
          >
            <TimePicker format="HH:mm" style={{ width: '100%' }} />
          </Form.Item>
        </Space>

        <Form.Item label="Phòng">
          <Input value={classroom?.room || '-'} disabled />
          <Text type="secondary">Phòng lấy từ thông tin lớp học.</Text>
        </Form.Item>

        <Form.Item label="Ghi chú" name="note">
          <Input.TextArea rows={3} placeholder="Tùy chọn" />
        </Form.Item>
      </Form>
    </Modal>
  )
}

function parseTime(value: string) {
  return dayjs(`2026-01-01T${value}`)
}

function findNextValidStudyDate(classroom: Classroom): Dayjs {
  const today = dayjs().startOf('day')
  const classStart = dayjs(classroom.startDate).startOf('day')
  let cursor = today.isBefore(classStart) ? classStart : today

  for (let i = 0; i < 21; i += 1) {
    if (isDateMatchingDaysOfWeek(cursor, classroom.daysOfWeek)) {
      return cursor
    }
    cursor = cursor.add(1, 'day')
  }

  return today
}
