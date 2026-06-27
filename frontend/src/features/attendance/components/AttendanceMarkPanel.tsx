import { Alert, Button, Form, message, Radio, Select, Space, Table } from 'antd'
import { isAxiosError } from 'axios'
import dayjs from 'dayjs'
import { useEffect, useMemo, useState } from 'react'
import type { ClassSession } from '../../classSessions/classSessionTypes'
import type { Enrollment } from '../../enrollments/enrollmentTypes'
import { useAttendance, useMarkAttendance } from '../attendanceQueries'
import type { AttendanceStatus } from '../attendanceTypes'

interface AttendanceMarkPanelProps {
  sessions: ClassSession[]
  enrollments: Enrollment[]
  loadingSessions: boolean
  loadingEnrollments: boolean
  defaultSessionId?: number
}

export function AttendanceMarkPanel({
  sessions,
  enrollments,
  loadingSessions,
  loadingEnrollments,
  defaultSessionId,
}: AttendanceMarkPanelProps) {
  const [form] = Form.useForm<Record<string, AttendanceStatus>>()
  const [selectedSessionId, setSelectedSessionId] = useState<number | undefined>(defaultSessionId)
  const selectedSession = sessions.find((session) => session.id === selectedSessionId)
  const attendanceQuery = useAttendance(selectedSessionId)
  const markAttendance = useMarkAttendance()

  const activeEnrollments = useMemo(
    () =>
      enrollments.filter(
        (enrollment) =>
          enrollment.status === 'ACTIVE' && enrollment.classroomId === selectedSession?.classroomId,
      ),
    [enrollments, selectedSession?.classroomId],
  )

  useEffect(() => {
    if (!selectedSessionId) {
      return
    }

    const values: Record<string, AttendanceStatus> = {}
    activeEnrollments.forEach((enrollment) => {
      const existing = attendanceQuery.data?.find((item) => item.studentId === enrollment.studentId)
      values[String(enrollment.studentId)] = existing?.status ?? 'PRESENT'
    })
    form.setFieldsValue(values)
  }, [activeEnrollments, attendanceQuery.data, form, selectedSessionId])

  function handleSave(values: Record<string, AttendanceStatus>) {
    if (!selectedSession) {
      message.error('Vui lòng chọn buổi học')
      return
    }

    if (selectedSession.status === 'CANCELED') {
      message.error('Không thể điểm danh buổi đã hủy')
      return
    }

    markAttendance.mutate(
      {
        sessionId: selectedSession.id,
        items: activeEnrollments.map((enrollment) => ({
          studentId: enrollment.studentId,
          status: values[String(enrollment.studentId)],
          note: null,
        })),
      },
      {
        onSuccess: () => {
          message.success('Đã lưu điểm danh')
        },
        onError: showErrorMessage,
      },
    )
  }

  function showErrorMessage(error: unknown) {
    if (isAxiosError(error)) {
      message.error(error.response?.data?.message ?? 'Có lỗi xảy ra')
      return
    }

    message.error('Có lỗi xảy ra')
  }

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Select
        showSearch
        allowClear
        loading={loadingSessions}
        optionFilterProp="label"
        placeholder="Chọn buổi học để điểm danh"
        style={{ width: 360 }}
        value={selectedSessionId}
        onChange={setSelectedSessionId}
        options={sessions.map((session) => ({
          value: session.id,
          label: `Buổi ${session.sessionNo} - ${session.classroomName} - ${dayjs(
            session.sessionDate,
          ).format('DD/MM/YYYY')}`,
        }))}
      />

      {selectedSession?.status === 'CANCELED' ? (
        <Alert type="warning" showIcon message="Buổi học đã hủy, không thể điểm danh." />
      ) : null}

      <Form form={form} onFinish={handleSave}>
        <Table
          rowKey="studentId"
          loading={loadingEnrollments || attendanceQuery.isLoading}
          pagination={false}
          dataSource={activeEnrollments}
          columns={[
            {
              title: 'Học viên',
              dataIndex: 'studentName',
              key: 'studentName',
            },
            {
              title: 'Trạng thái',
              key: 'status',
              render: (_, enrollment) => (
                <Form.Item name={String(enrollment.studentId)} style={{ margin: 0 }}>
                  <Radio.Group
                    disabled={selectedSession?.status === 'CANCELED'}
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

        <Button
          type="primary"
          htmlType="submit"
          loading={markAttendance.isPending}
          disabled={!selectedSession || selectedSession.status === 'CANCELED'}
          style={{ marginTop: 16 }}
        >
          Lưu điểm danh
        </Button>
      </Form>
    </Space>
  )
}
