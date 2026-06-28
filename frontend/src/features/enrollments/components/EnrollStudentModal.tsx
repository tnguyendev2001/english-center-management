import { DatePicker, Descriptions, Form, Input, InputNumber, Modal, Select } from 'antd'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { useEffect, useMemo } from 'react'
import { MoneyText } from '../../../components/common/MoneyText'
import type { ClassPackage } from '../../classPackages/classPackageTypes'
import type { ClassDayOfWeek } from '../../classrooms/classroomTypes'
import type { ClassSession } from '../../classSessions/classSessionTypes'
import type { Student } from '../../students/studentTypes'
import {
  disableInvalidLearningDates,
  formatSessionOptionLabel,
  getEnrollableSessions,
} from '../enrollmentLearningDateUtils'
import type { EnrollStudentPayload } from '../enrollmentTypes'

interface EnrollStudentFormValues {
  studentId: number
  tuitionPackageId: number
  enrollmentDate: Dayjs
  learningStartDate: Dayjs | string
  discountAmount?: number
  note?: string
}

interface EnrollStudentModalProps {
  open: boolean
  classroomId: number
  classroomName: string
  classroomStartDate: string
  classroomDaysOfWeek: ClassDayOfWeek[]
  sessions: ClassSession[]
  students: Student[]
  classPackages: ClassPackage[]
  loadingStudents: boolean
  loadingPackages: boolean
  submitting: boolean
  onCancel: () => void
  onSubmit: (payload: EnrollStudentPayload) => void
}

export function EnrollStudentModal({
  open,
  classroomId,
  classroomName,
  classroomStartDate,
  classroomDaysOfWeek,
  sessions,
  students,
  classPackages,
  loadingStudents,
  loadingPackages,
  submitting,
  onCancel,
  onSubmit,
}: EnrollStudentModalProps) {
  const [form] = Form.useForm<EnrollStudentFormValues>()
  const selectedPackageId = Form.useWatch('tuitionPackageId', form)
  const discountAmount = Form.useWatch('discountAmount', form) ?? 0

  const enrollableSessions = useMemo(
    () => getEnrollableSessions(sessions, classroomStartDate),
    [classroomStartDate, sessions],
  )
  const hasEnrollableSessions = enrollableSessions.length > 0
  const defaultLearningStartDate = useMemo(
    () => dayjs(classroomStartDate),
    [classroomStartDate],
  )
  const disableLearningDate = useMemo(
    () => disableInvalidLearningDates(classroomStartDate, classroomDaysOfWeek),
    [classroomDaysOfWeek, classroomStartDate],
  )

  useEffect(() => {
    if (open) {
      form.resetFields()
      form.setFieldsValue({
        enrollmentDate: dayjs(),
        learningStartDate: hasEnrollableSessions
          ? enrollableSessions[0].sessionDate
          : defaultLearningStartDate,
        discountAmount: 0,
      })
    }
  }, [defaultLearningStartDate, enrollableSessions, form, hasEnrollableSessions, open])

  const selectedPackage = useMemo(
    () => classPackages.find((classPackage) => classPackage.tuitionPackageId === selectedPackageId),
    [classPackages, selectedPackageId],
  )
  const finalAmount = selectedPackage ? Math.max(selectedPackage.price - discountAmount, 0) : 0

  function handleFinish(values: EnrollStudentFormValues) {
    const learningStartDate =
      typeof values.learningStartDate === 'string'
        ? values.learningStartDate
        : values.learningStartDate.format('YYYY-MM-DD')

    onSubmit({
      studentId: values.studentId,
      classroomId,
      tuitionPackageId: values.tuitionPackageId,
      enrollmentDate: values.enrollmentDate.format('YYYY-MM-DD'),
      learningStartDate,
      discountAmount: values.discountAmount ?? 0,
      note: values.note ?? null,
    })
  }

  return (
    <Modal
      title="Ghi danh học viên"
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText="Ghi danh"
      cancelText="Hủy"
      destroyOnHidden
      width={720}
    >
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Descriptions column={1} size="small" style={{ marginBottom: 16 }}>
          <Descriptions.Item label="Lớp học">{classroomName}</Descriptions.Item>
        </Descriptions>

        <Form.Item
          label="Học viên"
          name="studentId"
          rules={[{ required: true, message: 'Vui lòng chọn học viên' }]}
        >
          <Select
            showSearch
            loading={loadingStudents}
            optionFilterProp="label"
            placeholder="Chọn học viên"
            notFoundContent="Không có học viên đủ điều kiện ghi danh"
            options={students.map((student) => ({
              label: `${student.fullName} (${student.studentCode})`,
              value: student.id,
            }))}
          />
        </Form.Item>

        <Form.Item
          label="Gói học phí"
          name="tuitionPackageId"
          rules={[{ required: true, message: 'Vui lòng chọn gói học phí' }]}
        >
          <Select
            showSearch
            loading={loadingPackages}
            optionFilterProp="label"
            placeholder="Chọn gói học phí của lớp"
            options={classPackages.map((classPackage) => ({
              label: `${classPackage.packageName} - ${classPackage.totalSessions} buổi`,
              value: classPackage.tuitionPackageId,
            }))}
          />
        </Form.Item>

        <Form.Item
          label="Ngày ghi danh"
          name="enrollmentDate"
          rules={[{ required: true, message: 'Vui lòng chọn ngày ghi danh' }]}
        >
          <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
        </Form.Item>

        {hasEnrollableSessions ? (
          <Form.Item
            label="Ngày bắt đầu học"
            name="learningStartDate"
            rules={[{ required: true, message: 'Vui lòng chọn buổi học đầu tiên' }]}
            extra="Chọn buổi học đầu tiên từ lịch đã tạo"
          >
            <Select
              placeholder="Chọn buổi học đầu tiên"
              options={enrollableSessions.map((session) => ({
                label: formatSessionOptionLabel(session),
                value: session.sessionDate,
              }))}
            />
          </Form.Item>
        ) : (
          <Form.Item
            label="Ngày bắt đầu học"
            name="learningStartDate"
            rules={[{ required: true, message: 'Vui lòng chọn ngày bắt đầu học' }]}
          >
            <DatePicker
              format="DD/MM/YYYY"
              style={{ width: '100%' }}
              disabledDate={disableLearningDate}
            />
          </Form.Item>
        )}

        <Form.Item label="Giảm giá" name="discountAmount">
          <InputNumber min={0} precision={0} addonAfter="VND" style={{ width: '100%' }} />
        </Form.Item>

        <Descriptions column={1} bordered size="small" style={{ marginBottom: 16 }}>
          <Descriptions.Item label="Học phí gốc">
            <MoneyText value={selectedPackage?.price} />
          </Descriptions.Item>
          <Descriptions.Item label="Số buổi">
            {selectedPackage?.totalSessions ?? '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Số tiền phải đóng">
            <MoneyText value={finalAmount} />
          </Descriptions.Item>
        </Descriptions>

        <Form.Item label="Ghi chú" name="note">
          <Input.TextArea rows={3} placeholder="Ghi chú thêm nếu có" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
