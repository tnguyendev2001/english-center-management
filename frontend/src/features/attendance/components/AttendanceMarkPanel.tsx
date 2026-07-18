import { Alert, Button, Descriptions, Form, Input, message, Modal, Radio, Select, Space, Table } from 'antd'
import { isAxiosError } from 'axios'
import dayjs from 'dayjs'
import { useEffect, useMemo, useState } from 'react'
import { StatusTag } from '../../../components/common/StatusTag'
import { formatStudentLabel, studentCodeColumn, studentNameColumn } from '../../../components/common/studentDisplay'
import type { ClassSession } from '../../classSessions/classSessionTypes'
import type { ClassroomStatus } from '../../classrooms/classroomTypes'
import type { EnrollmentLearningProgress } from '../../studentPackages/studentPackageTypes'
import { buildProgressByStudentId } from '../../studentPackages/studentPackageUtils'
import { pickDefaultSessionId } from '../attendanceSessionSelection'
import {
  useAttendance,
  useAttendanceReadiness,
  useAttendanceRoster,
  useMarkAttendance,
} from '../attendanceQueries'
import type { Attendance, AttendanceStatus } from '../attendanceTypes'

interface AttendanceMarkPanelProps {
  sessions: ClassSession[]
  studentPackages: EnrollmentLearningProgress[]
  classroomStatus: ClassroomStatus
  loadingSessions: boolean
  selectedSessionId?: number
  onSelectedSessionIdChange?: (sessionId: number | undefined) => void
  onRenewNow?: () => void
  isActive?: boolean
}

interface AttendanceMarkFormValues {
  statuses: Record<string, AttendanceStatus>
  notes: Record<string, string>
  correctionReason?: string
}

function isExcusedCorrection(
  previousStatus: AttendanceStatus | undefined,
  nextStatus: AttendanceStatus,
): boolean {
  return previousStatus === 'EXCUSED' && (nextStatus === 'PRESENT' || nextStatus === 'ABSENT')
}

function formatTime(value: string) {
  return value.slice(0, 5)
}

export function AttendanceMarkPanel({
  sessions,
  studentPackages,
  classroomStatus,
  loadingSessions,
  selectedSessionId,
  onSelectedSessionIdChange,
  onRenewNow,
  isActive = true,
}: AttendanceMarkPanelProps) {
  const [form] = Form.useForm<AttendanceMarkFormValues>()
  const [statuses, setStatuses] = useState<Record<string, AttendanceStatus>>({})
  const [blockedModalOpen, setBlockedModalOpen] = useState(false)
  const canMarkAttendance = classroomStatus === 'ONGOING'
  const selectedSession = sessions.find((session) => session.id === selectedSessionId)
  const rosterQuery = useAttendanceRoster(selectedSessionId)
  const attendanceQuery = useAttendance(selectedSessionId)
  const readinessQuery = useAttendanceReadiness(
    selectedSessionId,
    isActive && canMarkAttendance && Boolean(selectedSession) && selectedSession?.status !== 'CANCELED',
  )
  const markAttendance = useMarkAttendance()
  const blockedStudents = readinessQuery.data?.blockedStudents ?? []
  const isAttendanceBlocked = blockedStudents.length > 0

  const rosterStudents = useMemo(
    () => rosterQuery.data?.students ?? [],
    [rosterQuery.data?.students],
  )

  const progressByStudentId = useMemo(
    () => buildProgressByStudentId(studentPackages),
    [studentPackages],
  )

  const previousAttendanceByStudentId = useMemo(() => {
    const map = new Map<number, Attendance>()
    for (const record of attendanceQuery.data ?? []) {
      map.set(record.studentId, record)
    }
    return map
  }, [attendanceQuery.data])

  const needsExcusedCorrection = useMemo(
    () =>
      rosterStudents.some((student) => {
        const previousStatus = previousAttendanceByStudentId.get(student.studentId)?.status
        const nextStatus = statuses[String(student.studentId)]
        return nextStatus != null && isExcusedCorrection(previousStatus, nextStatus)
      }),
    [previousAttendanceByStudentId, rosterStudents, statuses],
  )

  useEffect(() => {
    if (!isActive || selectedSessionId != null || sessions.length === 0) {
      return
    }

    const defaultSessionId = pickDefaultSessionId(sessions)
    if (defaultSessionId != null) {
      onSelectedSessionIdChange?.(defaultSessionId)
    }
  }, [isActive, onSelectedSessionIdChange, selectedSessionId, sessions])

  useEffect(() => {
    if (isAttendanceBlocked) {
      setBlockedModalOpen(true)
      return
    }

    setBlockedModalOpen(false)
  }, [isAttendanceBlocked])

  useEffect(() => {
    if (!selectedSessionId) {
      return
    }

    const nextStatuses: Record<string, AttendanceStatus> = {}
    const nextNotes: Record<string, string> = {}
    rosterStudents.forEach((student) => {
      const existing = attendanceQuery.data?.find((item) => item.studentId === student.studentId)
      const studentKey = String(student.studentId)
      nextStatuses[studentKey] = existing?.status ?? 'PRESENT'
      nextNotes[studentKey] = existing?.note ?? ''
    })
    setStatuses(nextStatuses)
    form.setFieldsValue({
      statuses: nextStatuses,
      notes: nextNotes,
      correctionReason: undefined,
    })
  }, [attendanceQuery.data, form, rosterStudents, selectedSessionId])

  function handleMarkAllPresent() {
    if (!selectedSession || !canMarkAttendance || selectedSession.status === 'CANCELED') {
      return
    }

    const nextStatuses: Record<string, AttendanceStatus> = {}
    rosterStudents.forEach((student) => {
      nextStatuses[String(student.studentId)] = 'PRESENT'
    })
    setStatuses(nextStatuses)
    form.setFieldsValue({ statuses: nextStatuses })
  }

  function handleSave(values: AttendanceMarkFormValues) {
    if (!selectedSession) {
      message.error('Vui lòng chọn buổi học')
      return
    }

    if (!canMarkAttendance) {
      message.error('Chỉ lớp đang diễn ra mới được điểm danh')
      return
    }

    if (selectedSession.status === 'CANCELED') {
      message.error('Không thể điểm danh buổi đã hủy')
      return
    }

    if (isAttendanceBlocked) {
      message.error('Một số học viên đã hết buổi. Vui lòng gia hạn gói trước khi điểm danh.')
      return
    }

    const correctionReason = values.correctionReason?.trim() || null
    if (needsExcusedCorrection && !correctionReason) {
      message.error('Vui lòng nhập lý do điều chỉnh điểm danh')
      return
    }

    markAttendance.mutate(
      {
        sessionId: selectedSession.id,
        items: rosterStudents.map((student) => {
          const studentKey = String(student.studentId)
          const previousStatus = previousAttendanceByStudentId.get(student.studentId)?.status
          const nextStatus = values.statuses[studentKey]
          const changedFromExcused = isExcusedCorrection(previousStatus, nextStatus)
          const note = values.notes[studentKey]?.trim() || null

          return {
            studentId: student.studentId,
            status: nextStatus,
            note,
            correctionReason: changedFromExcused ? correctionReason : null,
          }
        }),
      },
      {
        onSuccess: () => {
          message.success('Đã lưu điểm danh')
          form.setFieldValue('correctionReason', undefined)
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
      {!canMarkAttendance ? (
        <Alert
          type="warning"
          showIcon
          message="Lớp học chưa hoặc không còn trong trạng thái đang diễn ra, không thể điểm danh."
        />
      ) : null}

      {selectedSession ? (
        <Descriptions bordered size="small" column={2}>
          <Descriptions.Item label="Buổi">{selectedSession.sessionNo}</Descriptions.Item>
          <Descriptions.Item label="Ngày học">
            {dayjs(selectedSession.sessionDate).format('DD/MM/YYYY')}
          </Descriptions.Item>
          <Descriptions.Item label="Giờ học">
            {formatTime(selectedSession.startTime)} - {formatTime(selectedSession.endTime)}
          </Descriptions.Item>
          <Descriptions.Item label="Trạng thái">
            <StatusTag status={selectedSession.status} />
          </Descriptions.Item>
        </Descriptions>
      ) : (
        <Alert type="info" showIcon message="Chưa chọn buổi học để điểm danh." />
      )}

      <Select
        showSearch
        allowClear
        loading={loadingSessions}
        optionFilterProp="label"
        placeholder="Chọn buổi khác"
        style={{ width: 360 }}
        value={selectedSessionId}
        onChange={(value) => onSelectedSessionIdChange?.(value)}
        options={sessions.map((session) => ({
          value: session.id,
          label: `Buổi ${session.sessionNo} - ${dayjs(session.sessionDate).format('DD/MM/YYYY')}`,
        }))}
      />

      {selectedSession?.status === 'CANCELED' ? (
        <Alert type="warning" showIcon message="Buổi học đã hủy, không thể điểm danh." />
      ) : null}

      <Modal
        title="Một số học viên đã hết buổi"
        open={blockedModalOpen && isAttendanceBlocked}
        okText="Gia hạn ngay"
        cancelText="Đóng"
        onOk={() => {
          setBlockedModalOpen(false)
          onRenewNow?.()
        }}
        onCancel={() => setBlockedModalOpen(false)}
      >
        <Space direction="vertical" size={4}>
          <span>Vui lòng gia hạn gói trước khi điểm danh.</span>
          {blockedStudents.map((student) => (
            <span key={student.studentId}>
              {formatStudentLabel(student.studentCode, student.studentName)}: {student.reason}
            </span>
          ))}
        </Space>
      </Modal>

      {isAttendanceBlocked ? (
        <Alert
          type="error"
          showIcon
          message="Một số học viên đã hết buổi. Vui lòng gia hạn gói trước khi điểm danh."
          description={
            <Space direction="vertical" size={2}>
              {blockedStudents.map((student) => (
                <span key={student.studentId}>
                  {formatStudentLabel(student.studentCode, student.studentName)}: {student.reason}
                </span>
              ))}
              <Button type="primary" onClick={onRenewNow}>
                Gia hạn ngay
              </Button>
            </Space>
          }
        />
      ) : null}

      {selectedSession && canMarkAttendance ? (
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSave}
          onValuesChange={(_, allValues) => {
            if (allValues.statuses) {
              setStatuses(allValues.statuses)
            }
          }}
        >
          <Space style={{ marginBottom: 16 }}>
            <Button
              onClick={handleMarkAllPresent}
              disabled={!canMarkAttendance || selectedSession.status === 'CANCELED'}
            >
              Tất cả có mặt
            </Button>
          </Space>

          <Table
            rowKey="studentId"
            loading={rosterQuery.isLoading || attendanceQuery.isLoading || readinessQuery.isLoading}
            pagination={false}
            dataSource={rosterStudents}
            columns={[
              studentCodeColumn(),
              studentNameColumn(),
              {
                title: 'Tổng buổi',
                key: 'totalSessions',
                render: (_, student) => progressByStudentId.get(student.studentId)?.totalSessions ?? '-',
              },
              {
                title: 'Đã học',
                key: 'usedSessions',
                render: (_, student) => progressByStudentId.get(student.studentId)?.usedSessions ?? '-',
              },
              {
                title: 'Còn lại',
                key: 'remainingSessions',
                render: (_, student) =>
                  progressByStudentId.get(student.studentId)?.remainingSessions ?? '-',
              },
              {
                title: 'Trạng thái',
                key: 'status',
                render: (_, student) => (
                  <Form.Item
                    name={['statuses', String(student.studentId)]}
                    style={{ margin: 0 }}
                  >
                    <Radio.Group
                      disabled={!canMarkAttendance || selectedSession.status === 'CANCELED'}
                      options={[
                        { label: 'Có mặt', value: 'PRESENT' },
                        { label: 'Vắng', value: 'ABSENT' },
                        { label: 'Xin nghỉ', value: 'EXCUSED' },
                      ]}
                    />
                  </Form.Item>
                ),
              },
              {
                title: 'Ghi chú',
                key: 'note',
                render: (_, student) => (
                  <Form.Item name={['notes', String(student.studentId)]} style={{ margin: 0 }}>
                    <Input
                      placeholder="Ghi chú"
                      disabled={!canMarkAttendance || selectedSession.status === 'CANCELED'}
                    />
                  </Form.Item>
                ),
              },
            ]}
          />

          {needsExcusedCorrection ? (
            <Alert
              type="warning"
              showIcon
              message="Buổi bù phát sinh từ lần xin nghỉ này sẽ bị hủy."
              style={{ marginTop: 16 }}
            />
          ) : null}

          {needsExcusedCorrection ? (
            <Form.Item
              label="Lý do điều chỉnh"
              name="correctionReason"
              rules={[{ required: true, message: 'Vui lòng nhập lý do điều chỉnh điểm danh' }]}
              style={{ marginTop: 16 }}
            >
              <Input.TextArea
                rows={3}
                placeholder="Nhập lý do điều chỉnh từ xin nghỉ sang có mặt/vắng"
              />
            </Form.Item>
          ) : null}

          <Button
            type="primary"
            htmlType="submit"
            loading={markAttendance.isPending}
            disabled={
              !canMarkAttendance ||
              !selectedSession ||
              selectedSession.status === 'CANCELED' ||
              readinessQuery.isLoading ||
              isAttendanceBlocked
            }
            style={{ marginTop: 16 }}
          >
            Lưu điểm danh
          </Button>
        </Form>
      ) : null}
    </Space>
  )
}
