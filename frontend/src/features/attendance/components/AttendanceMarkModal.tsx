import { Form, Modal, Radio, Table } from 'antd'
import { useEffect } from 'react'
import { studentCodeColumn, studentNameColumn } from '../../../components/common/studentDisplay'
import type { Enrollment } from '../../enrollments/enrollmentTypes'
import type { Attendance, AttendanceStatus, MarkAttendancePayload } from '../attendanceTypes'

interface AttendanceMarkModalProps {
  open: boolean
  sessionId?: number
  enrollments: Enrollment[]
  attendance: Attendance[]
  submitting: boolean
  onCancel: () => void
  onSubmit: (payload: MarkAttendancePayload) => void
}

export function AttendanceMarkModal({
  open,
  sessionId,
  enrollments,
  attendance,
  submitting,
  onCancel,
  onSubmit,
}: AttendanceMarkModalProps) {
  const [form] = Form.useForm<Record<string, AttendanceStatus>>()

  useEffect(() => {
    if (!open) {
      return
    }
    const values: Record<string, AttendanceStatus> = {}
    enrollments.forEach((enrollment) => {
      const existing = attendance.find((item) => item.studentId === enrollment.studentId)
      values[String(enrollment.studentId)] = existing?.status ?? 'PRESENT'
    })
    form.setFieldsValue(values)
  }, [attendance, enrollments, form, open])

  function handleFinish(values: Record<string, AttendanceStatus>) {
    if (!sessionId) {
      return
    }

    onSubmit({
      sessionId,
      items: enrollments.map((enrollment) => ({
        studentId: enrollment.studentId,
        status: values[String(enrollment.studentId)],
        note: null,
      })),
    })
  }

  return (
    <Modal
      title="Điểm danh"
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      okText="Lưu điểm danh"
      cancelText="Hủy"
      width={760}
      destroyOnHidden
    >
      <Form form={form} onFinish={handleFinish}>
        <Table
          rowKey="studentId"
          pagination={false}
          dataSource={enrollments}
          columns={[
            studentCodeColumn(),
            studentNameColumn(),
            {
              title: 'Trạng thái',
              key: 'status',
              render: (_, enrollment) => (
                <Form.Item name={String(enrollment.studentId)} style={{ margin: 0 }}>
                  <Radio.Group
                    options={[
                      { label: 'Có mặt', value: 'PRESENT' },
                      { label: 'Vắng', value: 'ABSENT' },
                      { label: 'Xin nghỉ', value: 'EXCUSED' },
                    ]}
                  />
                </Form.Item>
              ),
            },
          ]}
        />
      </Form>
    </Modal>
  )
}
